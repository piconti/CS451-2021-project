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
    //private Observer fifoObserver;
    private int port;
    private int hostId;
    private String ip;
    private HashMap<Integer, String> systemHosts = new HashMap<Integer, String>();
    private ArrayList<String> delivered = new ArrayList<>();
    private ArrayList<String> log = new ArrayList<>();
    private ConcurrentHashMap<Integer, ArrayList<String>> pending = new ConcurrentHashMap<Integer, ArrayList<String>>();
    private ConcurrentHashMap<String, ArrayList<Integer>> ack = new ConcurrentHashMap<String, ArrayList<Integer>>();

    public UniformReliableBroadcast(int hostId, HashMap<Integer, String> systemHosts) throws IOException {//, Observer fifoObserver) throws IOException {
        this.hostId = hostId;
        this.port = getPortFromId(hostId);
        this.ip = systemHosts.get(this.hostId);
        this.systemHosts = systemHosts;
        this.beb = new BestEffortBroadcast(this.hostId, this.systemHosts, this);
        //this.fifoObserver = fifoObserver;
    }

    public void broadcast(Message msg) throws IOException, UnknownHostException {
        addToPending(this.hostId, msg);
        this.log.add("b " + String.valueOf(msg.getId()));
        System.out.println("··· Broadcasting m " + msg.getOverallUniqueId() + " with URB ···");
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

    public void deliverPendings() throws UnknownHostException, IOException { //à avoir qui run constamment?
        for (int processId : this.pending.keySet()) {
            for(String msgOgUniqueId: this.pending.get(processId)) {
                //String msgOgUniqueId = msg.getOriginalUniqueId();
                if(canDeliver(msgOgUniqueId) && !this.delivered.contains(msgOgUniqueId)) {
                    //Message message = this.pending.get(processId).get(msgOgUniqueId);
                    
                    deliverToLog(msgOgUniqueId);
                    //this.delivered.add(msgOgUniqueId);
                    //this.fifoObserver.deliver(msg, msg.getCurrentSenderId()); 
                    //System.out.println("* * * " + msg.getRcvdFromMsg() + " : delivered to URB * * *");
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

    private void deliverToLog(String msgOgUniqueId) {
        String[] splitOgUniqueId = msgOgUniqueId.split("\\s+");
        this.log.add("d " + splitOgUniqueId[1] + " " + splitOgUniqueId[0]);
        this.delivered.add(msgOgUniqueId);
        System.out.println("************ Delivered " + msgOgUniqueId + " *****************");
    }

    private String[] getMsgOgUniqueIdAsArray(String ogUniqueId) {
        return ogUniqueId.split("\\s+");
    }

    private int getPortFromId(int hostId) {
        return PORT_PREFIX + hostId;
    }
 
    private boolean canDeliver(String msgOriginalUniqueId) {
        int N = this.systemHosts.size();
        /*System.out.println("******* INSIDE CANDELIVER OF URB: KEYS OF ACK: ********");
        for(String key: this.ack.keySet()) {
            System.out.println("*******   - " + key);
        }*/
        try {
            //System.out.println("inside canDeliver for m " + msgOriginalUniqueId);
            //System.out.println("this.ack.get(msgOriginalUniqueId).size() > (N/2.0): " + String.valueOf(this.ack.get(msgOriginalUniqueId).size()) + " > " + String.valueOf(N/2.0));
            return (this.ack.get(msgOriginalUniqueId)).size() > (N/2.0);
        } catch(NullPointerException e) {
            System.out.println("Null pointer exception inside CanDeliver of URB: msg " + msgOriginalUniqueId + " is not in the keySet of ack." );
            return false;
        }
    }
    
    private void addToPending(int id, Message msg) {
        if(this.pending.keySet().contains(id)) {
            this.pending.get(id).add(msg.getOriginalUniqueId());
        } else {
            ArrayList<String> pendingForId = new ArrayList<>();
            pendingForId.add(msg.getOriginalUniqueId());
            this.pending.put(id, pendingForId);
        }
    }

    private boolean isPending(Message msg) {
        int ogHostId = msg.getOriginalHostId();
        if(this.pending.keySet().contains(ogHostId)) {
            return this.pending.get(ogHostId).contains(msg.getOriginalUniqueId());
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