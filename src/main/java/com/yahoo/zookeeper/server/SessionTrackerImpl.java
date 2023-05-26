/*
 * Copyright 2008, Yahoo! Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yahoo.zookeeper.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import com.yahoo.zookeeper.KeeperException;

/**
 * This is a full featured SessionTracker. It tracks session in grouped by tick
 * interval. It always rounds up the tick interval to provide a sort of grace
 * period. Sessions are thus expired in batches made up of sessions that expire
 * in a given interval.
 */
public class SessionTrackerImpl extends Thread implements SessionTracker {
    HashMap<Long, Session> sessionsById = new HashMap<Long, Session>();

    HashMap<Long, SessionSet> sessionSets = new HashMap<Long, SessionSet>();

    ConcurrentHashMap<Long, Integer> sessionsWithTimeout;

    long nextExpirationTime;

    int expirationInterval;

    public static class Session {
        Session(long sessionId, long expireTime) {
            this.sessionId = sessionId;
            this.tickTime = expireTime;
        }

        long tickTime;

        long sessionId;
    }

    static class SessionSet {
        long expireTime;

        HashSet<Session> sessions = new HashSet<Session>();
    }

    SessionExpirer expirer;

    private long roundToInterval(long time) {
        // We give a one interval grace period
        return (time / expirationInterval + 1) * expirationInterval;
    }

    public SessionTrackerImpl(SessionExpirer expirer,
            ConcurrentHashMap<Long, Integer> sessionsWithTimeout, int tickTime) {
        super("SessionTracker");
        this.expirer = expirer;
        this.expirationInterval = tickTime;
        this.sessionsWithTimeout = sessionsWithTimeout;
        nextExpirationTime = roundToInterval(System.currentTimeMillis());
        for (long id : sessionsWithTimeout.keySet()) {
            addSession(id, sessionsWithTimeout.get(id));
        }
        start();
    }

    boolean running = true;

    synchronized public void finish() {
        running = false;
        this.notifyAll();
    }

    volatile long currentTime;

    synchronized public String toString() {
        StringBuffer sb = new StringBuffer("Session Sets ("
                + sessionSets.size() + "):\n");
        ArrayList<Long> keys = new ArrayList<Long>(sessionSets.keySet());
        Collections.sort(keys);
        for (long time : keys) {
            sb.append(sessionSets.get(time).sessions.size() + " expire at "
                    + new Date(time) + ":\n");
            for (Session s : sessionSets.get(time).sessions) {
                sb.append("\t" + s.sessionId + "\n");
            }
        }
        return sb.toString();
    }

    synchronized public void run() {
        try {
            while (running) {
                currentTime = System.currentTimeMillis();
                if (nextExpirationTime > currentTime) {
                    this.wait(nextExpirationTime - currentTime);
                    continue;
                }
                SessionSet set;
                set = sessionSets.remove(nextExpirationTime);
                if (set != null) {
                    for (Session s : set.sessions) {
                        sessionsById.remove(s.sessionId);
                        ZooLog.logWarn("Expiring "
                                + Long.toHexString(s.sessionId));
                        expirer.expire(s.sessionId);
                    }
                }
                nextExpirationTime += expirationInterval;
            }
        } catch (InterruptedException e) {
            ZooLog.logException(e);
        }
        ZooLog.logTextTraceMessage("SessionTrackerImpl exited loop!",
                ZooLog.textTraceMask);
    }

    synchronized public boolean touchSession(long sessionId, int timeout) {
        ZooLog.logTextTraceMessage("SessionTrackerImpl --- Touch session: "
                + sessionId + " with timeout " + timeout,
                ZooLog.CLIENT_PING_TRACE_MASK);
        Session s = sessionsById.get(sessionId);
        if (s == null) {
            return false;
        }
        long expireTime = roundToInterval(System.currentTimeMillis() + timeout);
        if (s.tickTime >= expireTime) {
            // Nothing needs to be done
            return true;
        }
        SessionSet set = sessionSets.get(s.tickTime);
        if (set != null) {
            set.sessions.remove(s);
        }
        s.tickTime = expireTime;
        set = sessionSets.get(s.tickTime);
        if (set == null) {
            set = new SessionSet();
            set.expireTime = expireTime;
            sessionSets.put(expireTime, set);
        }
        set.sessions.add(s);
        return true;
    }

    synchronized public void removeSession(long sessionId) {
        Session s = sessionsById.remove(sessionId);
        sessionsWithTimeout.remove(sessionId);
        ZooLog.logTextTraceMessage("SessionTrackerImpl --- Removing "
                + sessionId, ZooLog.SESSION_TRACE_MASK);
        if (s != null) {
            sessionSets.get(s.tickTime).sessions.remove(s);
        }
    }

    public void shutdown() {
        running = false;
        ZooLog.logTextTraceMessage("Shutdown SessionTrackerImpl!",
                ZooLog.textTraceMask);
    }

    long nextSessionId = System.currentTimeMillis() << 24;

    synchronized public long createSession(int sessionTimeout) {
        addSession(nextSessionId, sessionTimeout);
        return nextSessionId++;
    }

    synchronized public void addSession(long id, int sessionTimeout) {
        sessionsWithTimeout.put(id, sessionTimeout);
        if (sessionsById.get(id) == null) {
            Session s = new Session(id, 0);
            sessionsById.put(id, s);
            ZooLog.logTextTraceMessage("SessionTrackerImpl --- Adding " + id
                    + " " + sessionTimeout, ZooLog.SESSION_TRACE_MASK);
        } else {
            ZooLog.logTextTraceMessage(
                    "SessionTrackerImpl --- Existing session " + id + " "
                            + sessionTimeout, ZooLog.SESSION_TRACE_MASK);
        }
        touchSession(id, sessionTimeout);
    }

    public void checkSession(long sessionId) throws KeeperException {
        if (sessionsById.get(sessionId) == null) {
            throw new KeeperException(KeeperException.Code.SessionExpired);
        }
    }
}
