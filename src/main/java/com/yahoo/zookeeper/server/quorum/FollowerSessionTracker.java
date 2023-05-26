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

package com.yahoo.zookeeper.server.quorum;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.yahoo.zookeeper.KeeperException;
import com.yahoo.zookeeper.server.SessionTracker;

/**
 * This is really just a shell of a SessionTracker that tracks session activity
 * to be forwarded to the Leader using a PING.
 */
public class FollowerSessionTracker implements SessionTracker {
    SessionExpirer expirer;

    HashMap<Long, Integer> touchTable = new HashMap<Long, Integer>();

    private ConcurrentHashMap<Long, Integer> sessionsWithTimeouts;

    /**
     * 
     */
    public FollowerSessionTracker(SessionExpirer expirer,
            ConcurrentHashMap<Long, Integer> sessionsWithTimeouts) {
        this.expirer = expirer;
        this.sessionsWithTimeouts = sessionsWithTimeouts;
        nextSessionId ^= expirer.getServerId() << 24;
    }

    synchronized public void removeSession(long sessionId) {
        sessionsWithTimeouts.remove(sessionId);
        touchTable.remove(sessionId);
    }

    public void shutdown() {
    }

    synchronized public void addSession(long sessionId, int sessionTimeout) {
        sessionsWithTimeouts.put(sessionId, sessionTimeout);
        touchTable.put(sessionId, sessionTimeout);
    }

    synchronized public boolean touchSession(long sessionId, int sessionTimeout) {
        touchTable.put(sessionId, sessionTimeout);
        return true;
    }

    synchronized HashMap<Long, Integer> snapshot() {
        HashMap<Long, Integer> oldTouchTable = touchTable;
        touchTable = new HashMap<Long, Integer>();
        return oldTouchTable;
    }

    long nextSessionId = System.currentTimeMillis() << 24;

    synchronized public long createSession(int sessionTimeout) {
        return (nextSessionId++);
    }

    public void checkSession(long sessionId) throws KeeperException {
        // Nothing to do here. Sessions are checked at the Leader
    }
}
