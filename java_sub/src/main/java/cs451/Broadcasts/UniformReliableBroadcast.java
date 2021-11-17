// https://www.baeldung.com/java-observer-pattern
package cs451.Broadcasts;

import cs451.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.net.UnknownHostException;

public class UniformReliableBroadcast implements Observer {

    private static final int PORT_PREFIX = 11000;

    private BestEffortBroadcast beb;
    private Observer fifoObserver;
    private int port;
    private int hostId;
    private String ip;
    private HashMap<Integer, String> systemHosts = new HashMap<Integer, String>();
    private ArrayList<String> delivered = new ArrayList<>();
    private HashMap<Integer, ArrayList<Message>> pending = new HashMap<Integer, ArrayList<Message>>();
    private HashMap<String, ArrayList<Integer>> ack = new HashMap<String, ArrayList<Integer>>();

    public UniformReliableBroadcast(int hostId, HashMap<Integer, String> systemHosts, Observer fifoObserver) throws IOException {
        this.hostId = hostId;
        this.port = getPortFromId(hostId);
        this.ip = systemHosts.get(this.hostId);
        this.systemHosts = systemHosts;
        this.beb = new BestEffortBroadcast(this.hostId, this.systemHosts, this);
        this.fifoObserver = fifoObserver;
    }

    public void broadcast(Message msg) throws IOException, UnknownHostException {
        addToPending(this.hostId, msg);
        //deliver(msg); // what should be in log is msg is being re-broadcast: original sender, new sender??
        this.beb.broadcast(msg);
    }

    @Override
    public void deliver(Message msg) throws UnknownHostException, IOException {
        int currentSenderId = msg.getCurrentSenderId();
        addtoAck(msg, currentSenderId);
        if (!isPending(msg)) {
            broadcast(msg);
        }
        deliverPendings();
    }

    public void deliverPendings() throws UnknownHostException, IOException { //à avoir qui run constamment?
        for (int processId : this.pending.keySet()) {
            for(Message msg: this.pending.get(processId)) {
                String msgOgUniqueId = msg.getOriginalUniqueId();
                if(canDeliver(msgOgUniqueId) && !this.delivered.contains(msgOgUniqueId)) {
                    this.delivered.add(msgOgUniqueId);
                    this.fifoObserver.deliver(msg); // deliver pour URB ça veut dire quoi???
                }
            }
        }
    }

    private int getPortFromId(int hostId) {
        return PORT_PREFIX + hostId;
    }
 
    private boolean canDeliver(String msgOriginalUniqueId) {
        int N = this.systemHosts.size();
        return (this.ack.get(msgOriginalUniqueId)).size() > (N/2.0);
    }
    
    private void addToPending(int id, Message msg) {
        if(this.pending.keySet().contains(id)) {
            this.pending.get(id).add(msg);
        } else {
            ArrayList<Message> pendingForId = new ArrayList<>();
            pendingForId.add(msg);
            this.pending.put(id, pendingForId);
        }
    }

    private boolean isPending(Message msg) {
        int ogHostId = msg.getOriginalHostId();
        if(this.pending.keySet().contains(ogHostId)) {
            return this.pending.get(ogHostId).contains(msg);
        } else {
            return false;
        }
    }

    private void addtoAck(Message msg, int id) {
        String msgOriginalUniqueId = msg.getOriginalUniqueId();
        if(this.ack.keySet().contains(msgOriginalUniqueId)) {
            this.ack.get(msgOriginalUniqueId).add(id);
        } else {
            ArrayList<Integer> ackForMsg = new ArrayList<>();
            ackForMsg.add(id);
            this.ack.put(msgOriginalUniqueId, ackForMsg);
        }
    }

    public ArrayList<String> getDelivered() {
        return this.delivered;
    }

    public int getPort() {
        return this.port;
    }

    public int gethostId() {
        return this.hostId;
    }

    public String getIp() {
        return ip;
    }

    public HashMap<Integer, String>  getSystemHosts() {
        return this.systemHosts;
    }

}