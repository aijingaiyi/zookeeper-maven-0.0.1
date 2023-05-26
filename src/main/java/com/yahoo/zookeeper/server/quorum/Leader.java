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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.yahoo.jute.BinaryOutputArchive;
import com.yahoo.zookeeper.server.FinalRequestProcessor;
import com.yahoo.zookeeper.server.Request;
import com.yahoo.zookeeper.server.RequestProcessor;
import com.yahoo.zookeeper.server.ZooLog;
import com.yahoo.zookeeper.server.quorum.QuorumPeer.ServerState;

/**
 * This class has the control logic for the Leader.
 */
public class Leader {
    static public class Proposal {
        public QuorumPacket packet;

        public int ackCount;

        public Request request;

        public String toString() {
            return packet.getType() + ", " + packet.getZxid() + ", " + request;
        }
    }

    LeaderZooKeeperServer zk;

    QuorumPeer self;

    // list of all the followers
    public HashSet<FollowerHandler> followers = new HashSet<FollowerHandler>();

    // list of followers that are ready to follow (i.e synced with the leader)
    public HashSet<FollowerHandler> forwardingFollowers = new HashSet<FollowerHandler>();

    /**
     * Adds follower to the leader.
     * 
     * @param follower
     *                instance of follower handle
     */
    void addFollowerHandler(FollowerHandler follower) {
        synchronized (followers) {
            followers.add(follower);
        }
    }

    /**
     * Remove the follower from the followers list
     * 
     * @param follower
     */
    void removeFollowerHandler(FollowerHandler follower) {
        synchronized (forwardingFollowers) {
            forwardingFollowers.remove(follower);
        }
        synchronized (followers) {
            followers.remove(follower);
        }
    }

    ServerSocket ss;

    Leader(QuorumPeer self) throws IOException {
        this.self = self;
        try {
            ss = new ServerSocket(self.myQuorumAddr.getPort());
        } catch (BindException e) {
            ZooLog.logError("Couldn't bind to port "
                    + self.myQuorumAddr.getPort());
            throw e;
        }
    }

    /**
     * This message type is sent by the leader to indicate it's zxid and if
     * needed, its database.
     */
    final static int NEWLEADER = 10;

    /**
     * This message type is sent by a follower to indicate the last zxid in its
     * log.
     */
    final static int LASTZXID = 11;

    /**
     * This message type is sent by the leader to indicate that the follower is
     * now uptodate andt can start responding to clients.
     */
    final static int UPTODATE = 12;

    /**
     * This message type is sent to a leader to request and mutation operation.
     * The payload will consist of a request header followed by a request.
     */
    final static int REQUEST = 1;

    /**
     * This message type is sent by a leader to propose a mutation.
     */
    final static int PROPOSAL = 2;

    /**
     * This message type is sent by a follower after it has synced a proposal.
     */
    final static int ACK = 3;

    /**
     * This message type is sent by a leader to commit a proposal and cause
     * followers to start serving the corresponding data.
     */
    final static int COMMIT = 4;

    /**
     * This message type is enchanged between follower and leader (initiated by
     * follower) to determine liveliness.
     */
    final static int PING = 5;

    /**
     * This message type is to validate a session that should be active.
     */
    final static int REVALIDATE = 6;

    private ConcurrentLinkedQueue<Proposal> outstandingProposals = new ConcurrentLinkedQueue<Proposal>();

    ConcurrentLinkedQueue<Proposal> toBeApplied = new ConcurrentLinkedQueue<Proposal>();

    Proposal newLeaderProposal = new Proposal();

    /**
     * This method is main function that is called to lead
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    void lead() throws IOException, InterruptedException {
        self.tick = 0;
        zk = new LeaderZooKeeperServer(self.getId(), self.dataDir,
                self.dataLogDir, this);
        zk.loadData();
        zk.startup();
        long epoch = self.getLastLoggedZxid() >> 32L;
        epoch++;
        zk.setZxid(epoch << 32L);
        zk.dataTree.lastProcessedZxid = zk.getZxid();
        lastProposed = zk.getZxid();
        newLeaderProposal.packet = new QuorumPacket(NEWLEADER, zk.getZxid(),
                null, null);
        if ((newLeaderProposal.packet.getZxid() & 0xffffffffL) != 0) {
            ZooLog.logError("NEWLEADER proposal has Zxid of "
                    + newLeaderProposal.packet.getZxid());
        }
        outstandingProposals.add(newLeaderProposal);
        new Thread() {
            public void run() {
                try {
                    while (true) {
                        Socket s = ss.accept();
                        s.setSoTimeout(self.tickTime * self.syncLimit);
                        s.setTcpNoDelay(true);
                        new FollowerHandler(s, Leader.this);
                    }
                } catch (Exception e) {
                    // 
                }
            }
        }.start();
        // We have to get at least a majority of servers in sync with
        // us. We do this by waiting for the NEWLEADER packet to get
        // acknowledged
        newLeaderProposal.ackCount++;
        while (newLeaderProposal.ackCount <= self.quorumPeers.size() / 2) {
            if (self.tick > self.initLimit) {
                // Followers aren't syncing fast enough,
                // renounce leadership!
                shutdown("Waiting for " + (self.quorumPeers.size() / 2)
                        + " followers, only synced with "
                        + newLeaderProposal.ackCount);
                if (followers.size() >= self.quorumPeers.size() / 2) {
                    ZooLog
                            .logWarn("Enough followers present. Perhaps the initTicks need to be increased.");
                }
                self.state = ServerState.LOOKING;
                return;
            }
            Thread.sleep(self.tickTime);
            self.tick++;
        }
        if (!System.getProperty("zookeeper.leaderServes", "yes").equals("no")) {
            self.cnxnFactory.setZooKeeperServer(zk);
        }
        // Everything is a go, simply start counting the ticks
        synchronized (this) {
            notifyAll();
        }
        // We ping twice a tick, so we only update the tick every other
        // iteration
        boolean tickSkip = true;

        while (true) {
            Thread.sleep(self.tickTime / 2);
            if (!tickSkip) {
                self.tick++;
            }
            int syncedCount = 0;
            // lock on the followers when we use it.
            synchronized (followers) {
                for (FollowerHandler f : followers) {
                    if (f.synced()) {
                        syncedCount++;
                    }
                    f.ping();
                }
            }
            if (!tickSkip && syncedCount < self.quorumPeers.size() / 2) {
                // Lost quorum, shutdown
                shutdown("Only " + syncedCount + " followers, need "
                        + (self.quorumPeers.size() / 2));
                // make sure the order is the same!
                // the leader goes to looking
                self.state = ServerState.LOOKING;

                return;
            }
            tickSkip = !tickSkip;
        }
    }

    boolean isShutdown;

    /**
     * Close down all the FollowerHandlers
     */
    void shutdown(String reason) {
        if (isShutdown) {
            return;
        }

        ZooLog
                .logException(new Exception("shutdown Leader! reason: "
                        + reason));
        // NIO should not accept conenctions
        self.cnxnFactory.setZooKeeperServer(null);
        // clear all the connections
        self.cnxnFactory.clear();
        // shutdown the previous zk
        if (zk != null) {
            zk.shutdown();
        }
        try {
            ss.close();
        } catch (IOException e) {
            ZooLog.logException(e);
        }
        synchronized (followers) {
            for (Iterator<FollowerHandler> it = followers.iterator(); it
                    .hasNext();) {
                FollowerHandler f = it.next();
                it.remove();
                f.shutdown();
            }
        }
        isShutdown = true;
    }

    /**
     * Keep a count of acks that are received by the leader for a particular
     * proposal
     * 
     * @param zxid
     *                the zxid of the proposal sent out
     * @param followerAddr
     */
    synchronized public void processAck(long zxid, SocketAddress followerAddr) {
        boolean first = true;
        /*
         * ZooLog.logError("Ack zxid: " + Long.toHexString(zxid)); for (Proposal
         * p : outstandingProposals) { long packetZxid = p.packet.getZxid();
         * ZooLog.logError("outstanding proposal: " +
         * Long.toHexString(packetZxid)); } ZooLog.logError("outstanding
         * proposals all");
         */
        if (outstandingProposals.size() == 0) {
            return;
        }
        if (outstandingProposals.peek().packet.getZxid() > zxid) {
            // The proposal has already been committed
            return;
        }
        for (Proposal p : outstandingProposals) {
            long packetZxid = p.packet.getZxid();
            if (packetZxid == zxid) {
                p.ackCount++;
                // ZooLog.logException(new RuntimeException(), "Count for " +
                // Long.toHexString(zxid) + " is " + p.ackCount);
                if (p.ackCount > self.quorumPeers.size() / 2) {
                    if (!first) {
                        ZooLog.logError("Commiting " + Long.toHexString(zxid)
                                + " from " + followerAddr + " not first!");
                        ZooLog.logError("First is "
                                + outstandingProposals.element().packet);
                        System.exit(13);
                    }
                    outstandingProposals.remove();
                    if (p.request != null) {
                        toBeApplied.add(p);
                    }
                    // We don't commit the new leader proposal
                    if ((zxid & 0xffffffffL) != 0) {
                        if (p.request == null) {
                            ZooLog.logError("Going to commmit null: " + p);
                        }
                        commit(zxid);
                        zk.commitProcessor.commit(p.request);
                    }
                }
                return;
            } else {
                first = false;
            }
        }
        ZooLog.logError("Trying to commit future proposal: "
                + Long.toHexString(zxid) + " from " + followerAddr);
    }

    static class ToBeAppliedRequestProcessor implements RequestProcessor {
        private RequestProcessor next;

        private ConcurrentLinkedQueue<Proposal> toBeApplied;

        /**
         * This request processor simply maintains the toBeApplied list. For
         * this to work next must be a FinalRequestProcessor and
         * FinalRequestProcessor.processRequest MUST process the request
         * synchronously!
         * 
         * @param next
         *                a reference to the FinalRequestProcessor
         */
        ToBeAppliedRequestProcessor(RequestProcessor next,
                ConcurrentLinkedQueue<Proposal> toBeApplied) {
            if (!(next instanceof FinalRequestProcessor)) {
                throw new RuntimeException(ToBeAppliedRequestProcessor.class
                        .getName()
                        + " must be connected to "
                        + FinalRequestProcessor.class.getName()
                        + " not "
                        + next.getClass().getName());
            }
            this.toBeApplied = toBeApplied;
            this.next = next;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.yahoo.zookeeper.server.RequestProcessor#processRequest(com.yahoo.zookeeper.server.Request)
         */
        public void processRequest(Request request) {
            // request.addRQRec(">tobe");
            next.processRequest(request);
            Proposal p = toBeApplied.peek();
            if (p != null && p.request != null
                    && p.request.zxid == request.zxid) {
                toBeApplied.remove();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.yahoo.zookeeper.server.RequestProcessor#shutdown()
         */
        public void shutdown() {
            next.shutdown();
        }
    }

    /**
     * send a packet to all the followers ready to follow
     * 
     * @param qp
     *                the packet to be sent
     */
    void sendPacket(QuorumPacket qp) {
        synchronized (forwardingFollowers) {
            for (FollowerHandler f : forwardingFollowers) {
                f.queuePacket(qp);
            }
        }
    }

    long lastCommitted;

    /**
     * Create a commit packet and send it to all the members of the quorum
     * 
     * @param zxid
     */
    public void commit(long zxid) {
        lastCommitted = zxid;
        QuorumPacket qp = new QuorumPacket(Leader.COMMIT, zxid, null, null);
        sendPacket(qp);
    }

    long lastProposed;

    /**
     * create a proposal and send it out to all the members
     * 
     * @param request
     * @return the proposal that is queued to send to all the members
     */
    public Proposal propose(Request request) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
        try {
            request.hdr.serialize(boa, "hdr");
            if (request.txn != null) {
                request.txn.serialize(boa, "txn");
            }
            baos.close();
        } catch (IOException e) {
            // This really should be impossible
            ZooLog.logException(e);
        }
        QuorumPacket pp = new QuorumPacket(Leader.PROPOSAL, request.zxid, baos
                .toByteArray(), null);
        Proposal p = new Proposal();
        p.packet = pp;
        p.request = request;
        synchronized (this) {
            outstandingProposals.add(p);
            lastProposed = p.packet.getZxid();
            sendPacket(pp);
        }
        return p;
    }

    /**
     * lets the leader know that a follower is capable of following and is done
     * syncing
     * 
     * @param handler
     *                handler of the follower
     * @return last proposed zxid
     */
    synchronized public long startForwarding(FollowerHandler handler,
            long lastSeenZxid) {
        // Queue up any outstanding requests enabling the receipt of
        // new requests
        if (lastProposed > lastSeenZxid) {
            for (Proposal p : toBeApplied) {
                if (p.packet.getZxid() <= lastSeenZxid) {
                    continue;
                }
                handler.queuePacket(p.packet);
                // Since the proposal has been committed we need to send the
                // commit message
                // also
                QuorumPacket qp = new QuorumPacket(Leader.COMMIT, p.packet
                        .getZxid(), null, null);
                handler.queuePacket(qp);
            }
            for (Proposal p : outstandingProposals) {
                if (p.packet.getZxid() <= lastSeenZxid) {
                    continue;
                }
                handler.queuePacket(p.packet);
            }
        }
        synchronized (forwardingFollowers) {
            forwardingFollowers.add(handler);
            return lastProposed;
        }
    }

}
