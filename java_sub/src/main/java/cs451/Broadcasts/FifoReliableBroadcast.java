// inspired from https://www.baeldung.com/java-observer-pattern
package cs451.Broadcasts;

import cs451.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.net.UnknownHostException;

public class FifoReliableBroadcast implements Observer {

    //private static final int PORT_PREFIX = 11000;

    private UniformReliableBroadcast urb;
    //private Observer loggerObserver;
    //private int port;
    private int hostId;
    //private String ip;
    private HashMap<Integer, String> systemHosts = new HashMap<Integer, String>();
    private int[] next;
    //private ArrayList<String> deliveredLog = new ArrayList<>();
    private ArrayList<String> log = new ArrayList<>();
    private ArrayList<String> delivered = new ArrayList<>();
    private HashMap<String, Message> pending = new HashMap<String, Message>();
    //private int localSeqNum = 0;

    public FifoReliableBroadcast(int hostId, HashMap<Integer, String> systemHosts) throws IOException { //, Observer loggerObserver) throws IOException {
        this.hostId = hostId;
        //this.port = getPortFromId(hostId);
        //this.ip = systemHosts.get(this.hostId);
        this.systemHosts = systemHosts;
        this.urb = new UniformReliableBroadcast(this.hostId, this.systemHosts);//, this);
        //this.loggerObserver = loggerObserver;
        this.next = new int[systemHosts.size()];
        for(int i = 0; i<next.length-1; ++i) {
            this.next[i] = 1;
        }
    }

    public void broadcast(Message msg) throws UnknownHostException, IOException {
        //localSeqNum += 1;
        //msg.setFifoSeqNum(localSeqNum);
        this.urb.broadcast(msg);
        this.log.add("b " + String.valueOf(msg.getId()));
    }

    @Override
    public void deliver(Message msg, int currentSenderId) throws UnknownHostException, IOException {
        //String fullUniqueId = getOrginialUniqueIdWithSeqNum(msg);
        String originalUniqueId = msg.getOriginalUniqueId();
        int orginialSenderId = msg.getOriginalHostId();
        this.pending.put(originalUniqueId, msg);
        String msgUniqueIdToDeliver = getMsgOgUniqueId(orginialSenderId);
        while(this.pending.keySet().contains(msgUniqueIdToDeliver)) {
            this.next[orginialSenderId-1] += 1;
            Message msgToDeliver = this.pending.get(msgUniqueIdToDeliver);
            this.pending.remove(msgUniqueIdToDeliver);
            //this.loggerObserver.deliver(msgToDeliver);
            deliverToLog(msgToDeliver);
            msgUniqueIdToDeliver = getMsgOgUniqueId(orginialSenderId);
        }
    }

    /*
    private int getPortFromId(int hostId) {
        return PORT_PREFIX + hostId;
    }*/

    private void deliverToLog(Message msg) {
        this.log.add("d " + msg.getOriginalHostId() + " " + String.valueOf(msg.getId()));
        this.delivered.add(msg.getOverallUniqueId());
        System.out.println("* * * *" + msg.getRcvdFromMsg() + " : delivered to FIFO * * * *");
    }
    
    /*
    private String getOrginialUniqueIdWithSeqNum(Message msg) {
        int seqNum = msg.getFifoSeqNum();
        String originalUniqueId = msg.getOriginalUniqueId();
        return originalUniqueId + " " + String.valueOf(seqNum);
    }

    private boolean checkIfPendingMessages(int senderId) {
        return this.pending.keySet().contains(getMsgOgUniqueId(senderId));
    }*/

    private String getMsgOgUniqueId(int ogSenderId) {
        String nextMsgId = String.valueOf(this.next[ogSenderId-1]);
        return nextMsgId + " " + String.valueOf(ogSenderId);
    }

    public int getHostId() {
        return this.hostId;
    }

    /*
    public ArrayList<String> getDeliveredLog() {
        return this.deliveredLog;
    }*/

    public ArrayList<String> getLog() {
        return this.log;
    }

    public boolean isClosed() {
        return this.urb.isClosed();
    }

    public void close() {
        this.urb.close();
    }
}