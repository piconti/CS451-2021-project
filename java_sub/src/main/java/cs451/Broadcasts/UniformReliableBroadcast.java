// https://www.baeldung.com/java-observer-pattern
package cs451.Broadcasts;

import cs451.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.net.UnknownHostException;

public class UniformReliableBroadcast implements Observer {

    private static final int PORT_PREFIX = 11000;

    private BestEffortBroadcast beb;
    private Observer observer;
    private int port;
    private int hostId;
    private String ip;
    private HashMap<Integer, String> systemHosts = new HashMap<Integer, String>();
    private ArrayList<String> delivered = new ArrayList<>();
    private ArrayList<String> log = new ArrayList<>();
    private ConcurrentHashMap<Integer, ConcurrentHashMap<String, Message>> pending = new ConcurrentHashMap<Integer, ConcurrentHashMap<String, Message>>();
    private ConcurrentHashMap<String, ArrayList<Integer>> ack = new ConcurrentHashMap<String, ArrayList<Integer>>();

    public UniformReliableBroadcast(int hostId, HashMap<Integer, String> systemHosts, Observer observer) throws IOException {
        this.hostId = hostId;
        this.port = getPortFromId(hostId);
        this.ip = systemHosts.get(this.hostId);
        this.systemHosts = systemHosts;
        this.beb = new BestEffortBroadcast(this.hostId, this.systemHosts, this);
        this.observer = observer;
    }

    public void broadcast(Message msg) throws IOException, UnknownHostException {
        addToPending(this.hostId, msg);
        this.log.add("b " + String.valueOf(msg.getId()));
        //System.out.println("··· Broadcasting m " + msg.getOverallUniqueId() + " with URB ···");
        this.beb.broadcast(msg);
    }

    @Override
    public void deliver(Message msg, int currentSenderId) throws UnknownHostException, IOException {
        int senderId = msg.getCurrentSenderId();
        if(senderId != currentSenderId){
            System.out.println("!!! Current sender actualized before delivering !!!! - URB for m: " + msg.getOverallUniqueId());
        }
        addtoAck(msg, currentSenderId);
        //System.out.println("(Inside URB) – Msg " + msg.getOverallUniqueId() + ": isPending = " + isPending(msg));
        if (!isPending(msg)) {
            Message newMsg = changeSenderIfNeeded(msg);
            addToPending(newMsg.getOriginalHostId(), newMsg);
            this.beb.broadcast(newMsg);
        }
        deliverPendings();
    }


    @Override
    public void deliver(Message msg) throws UnknownHostException, IOException {}

    public void deliverPendings() throws UnknownHostException, IOException { //à avoir qui run constamment?
        for (int processId : this.pending.keySet()) {
            for(String msgOgUniqueId: this.pending.get(processId).keySet()) {
                if(canDeliver(msgOgUniqueId) && !this.delivered.contains(msgOgUniqueId)) {
                    Message message = this.pending.get(processId).get(msgOgUniqueId);
                    this.delivered.add(msgOgUniqueId);
                    //this.fifoObserver.deliver(message); 
                    this.observer.deliver(message);
                }
            }
        }
    }

    private Message changeSenderIfNeeded(Message msg) {
        if(msg.getCurrentSenderId() != this.hostId) {
            return new Message(msg, this.hostId);
        } else {
            return msg;
        }
    }

    private int getPortFromId(int hostId) {
        return PORT_PREFIX + hostId;
    }
 
    private boolean canDeliver(String msgOriginalUniqueId) {
        int N = this.systemHosts.size();
        try {
            return (this.ack.get(msgOriginalUniqueId)).size() > (N/2.0);
        } catch(NullPointerException e) {
            //System.out.println("Null pointer exception inside CanDeliver of URB: msg " + msgOriginalUniqueId + " is not in the keySet of ack." );
            return false;
        }
    }
    
    private void addToPending(int id, Message msg) {
        if(this.pending.keySet().contains(id)) {
            this.pending.get(id).put(msg.getOriginalUniqueId(), msg);
        } else {
            ConcurrentHashMap<String, Message> pendingForId = new ConcurrentHashMap<String, Message>();
            pendingForId.put(msg.getOriginalUniqueId(), msg);
            this.pending.put(id, pendingForId);
        }
    }

    private boolean isPending(Message msg) {
        int ogHostId = msg.getOriginalHostId();
        if(this.pending.keySet().contains(ogHostId)) {
            return this.pending.get(ogHostId).keySet().contains(msg.getOriginalUniqueId());
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

    public int getHostId() {
        return this.hostId;
    }

    public String getIp() {
        return ip;
    }

    public HashMap<Integer, String>  getSystemHosts() {
        return this.systemHosts;
    }

    public boolean isClosed() {
        return this.beb.isClosed();
    }

    public void close() {
        this.beb.close();
    }

    public ArrayList<String> getLog() {
        return this.log;
    }
    

}