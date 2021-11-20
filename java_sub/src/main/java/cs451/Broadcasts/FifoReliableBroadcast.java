// inspired from https://www.baeldung.com/java-observer-pattern
package cs451.Broadcasts;

import cs451.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.net.UnknownHostException;

public class FifoReliableBroadcast implements Observer {

    private UniformReliableBroadcast urb;
    private int hostId;
    private HashMap<Integer, String> systemHosts = new HashMap<Integer, String>();
    private int[] next;
    private ArrayList<String> log = new ArrayList<>();
    private ArrayList<String> delivered = new ArrayList<>();
    private HashMap<String, Message> pending = new HashMap<String, Message>();

    public FifoReliableBroadcast(int hostId, HashMap<Integer, String> systemHosts) throws IOException { 
        this.hostId = hostId;
        this.systemHosts = systemHosts;
        this.urb = new UniformReliableBroadcast(this.hostId, this.systemHosts, this);
        this.next = new int[systemHosts.size()];
        for(int i = 0; i<next.length; ++i) {
            this.next[i] = 1;
        }
    }

    public void broadcast(Message msg) throws UnknownHostException, IOException {
        this.urb.broadcast(msg);
        this.log.add("b " + String.valueOf(msg.getId()));
    }

    @Override
    public void deliver(Message msg, int currentSenderId) throws UnknownHostException, IOException {
        String originalUniqueId = msg.getOriginalUniqueId();
        int orginialSenderId = msg.getOriginalHostId();
        this.pending.put(originalUniqueId, msg);
        String msgUniqueIdToDeliver = getMsgOgUniqueId(orginialSenderId);
        while(this.pending.keySet().contains(msgUniqueIdToDeliver)) {
            this.next[orginialSenderId-1] += 1;
            Message msgToDeliver = this.pending.get(msgUniqueIdToDeliver);
            this.pending.remove(msgUniqueIdToDeliver);
            deliverToLog(msgToDeliver.getOriginalUniqueId());
            msgUniqueIdToDeliver = getMsgOgUniqueId(orginialSenderId);
        }
    }

    private void deliverToLog(String msgOgUniqueId) {
        String[] splitOgUniqueId = msgOgUniqueId.split("\\s+");
        this.log.add("d " + splitOgUniqueId[1] + " " + splitOgUniqueId[0]);
        this.delivered.add(msgOgUniqueId);
    }

    private String getMsgOgUniqueId(int ogSenderId) {
        String nextMsgId = String.valueOf(this.next[ogSenderId-1]);
        return nextMsgId + " " + String.valueOf(ogSenderId);
    }

    public int getHostId() {
        return this.hostId;
    }

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