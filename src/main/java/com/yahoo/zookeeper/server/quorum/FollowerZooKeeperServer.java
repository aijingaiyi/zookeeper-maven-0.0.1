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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.yahoo.jute.Record;
import com.yahoo.zookeeper.server.FinalRequestProcessor;
import com.yahoo.zookeeper.server.RequestProcessor;
import com.yahoo.zookeeper.server.Request;
import com.yahoo.zookeeper.server.ServerCnxn;
import com.yahoo.zookeeper.server.SyncRequestProcessor;
import com.yahoo.zookeeper.server.ZooKeeperServer;
import com.yahoo.zookeeper.server.ZooLog;
import com.yahoo.zookeeper.txn.TxnHeader;

/**
 * Just like the standard ZooKeeperServer. We just replace the request
 * processors: FollowerRequestProcessor -> CommitProcessor ->
 * FinalRequestProcessor
 * 
 * A SyncRequestProcessor is also spawn off to log proposals from the leader.
 */
class FollowerZooKeeperServer extends ZooKeeperServer {
    Follower follower;

    CommitProcessor commitProcessor;

    SyncRequestProcessor syncProcessor;

    long serverId;

    /**
     * @param port
     * @param dataDir
     * @throws IOException
     */
    FollowerZooKeeperServer(long serverId, File dataDir, File dataLogDir,
            Follower follower) throws IOException {
        super(dataDir, dataLogDir, follower.self.tickTime);
        this.serverId = serverId;
        this.follower = follower;
    }

    protected void createSessionTracker() {
        sessionTracker = new FollowerSessionTracker(this, sessionsWithTimeouts);
    }

    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        commitProcessor = new CommitProcessor(finalProcessor);
        firstProcessor = new FollowerRequestProcessor(this, commitProcessor);
        syncProcessor = new SyncRequestProcessor(this,
                new SendAckRequestProcessor(follower));
    }

    @Override
    protected void revalidateSession(ServerCnxn cnxn, long sessionId,
            int sessionTimeout) throws IOException, InterruptedException {
        follower.validateSession(cnxn, sessionId, sessionTimeout);
    }

    /**
     * @return
     */
    public HashMap<Long, Integer> getTouchSnapshot() {
        if (sessionTracker != null) {
            return ((FollowerSessionTracker) sessionTracker).snapshot();
        }
        return new HashMap<Long, Integer>();
    }

    @Override
    public long getServerId() {
        return serverId;
    }

    LinkedBlockingQueue<Request> pendingTxns = new LinkedBlockingQueue<Request>();

    public void logRequest(TxnHeader hdr, Record txn) {
        Request request = new Request(null, hdr.getClientId(), hdr.getCxid(),
                hdr.getType(), null, null);
        request.hdr = hdr;
        request.txn = txn;
        request.zxid = hdr.getZxid();
        if ((request.zxid & 0xffffffffL) != 0) {
            pendingTxns.add(request);
        }
        syncProcessor.processRequest(request);
    }

    public void commit(long zxid) {
        if (pendingTxns.size() == 0) {
            ZooLog.logWarn("Committing " + Long.toHexString(zxid)
                    + " without seeing txn");
            return;
        }
        long firstElementZxid = pendingTxns.element().zxid;
        if (firstElementZxid != zxid) {
            ZooLog.logError("Committing " + Long.toHexString(zxid)
                    + " but next pending txn "
                    + Long.toHexString(firstElementZxid));
            System.exit(12);
        }
        Request request = pendingTxns.remove();
        commitProcessor.commit(request);
    }

    @Override
    public int getGlobalOutstandingLimit() {
        return super.getGlobalOutstandingLimit()
                / (follower.self.quorumPeers.size() - 1);
    }

    public void shutdown() {
        try {
            super.shutdown();
        } catch (Exception e) {
            ZooLog.logException(e);
        }
        try {
            if (syncProcessor != null) {
                syncProcessor.shutdown();
            }
        } catch (Exception e) {
            ZooLog.logException(e);
        }
    }

}
