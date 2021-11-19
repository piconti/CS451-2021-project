// inspir4ed from https://www.baeldung.com/java-observer-pattern
package cs451.Broadcasts;

import cs451.*;
import cs451.Links.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.net.UnknownHostException;

public class BestEffortBroadcast implements Observer {

    private static final int PORT_PREFIX = 11000;
    //private static final String localIp = "localhost";

    private PerfectLink pLink;
    private Observer urbObserver;
    private String ip;
    private int port;
    private int hostId;
    //private ArrayList<String> log = new ArrayList<>();
    private HashMap<Integer, String> systemHosts = new HashMap<Integer, String>();
    private ArrayList<Message> delivered = new ArrayList<>();

    public BestEffortBroadcast(int hostId, HashMap<Integer, String> systemHosts, Observer urbObserver) throws IOException {
        this.hostId = hostId;
        this.port = getPortFromId(hostId);
        this.ip = systemHosts.get(this.hostId);
        this.pLink = new PerfectLink(this.ip, this.port, this.hostId, this);
        this.urbObserver = urbObserver;
        this.systemHosts = systemHosts;
    }

    public void broadcast(Message msg) throws IOException, UnknownHostException {
        Message message = changeSenderIfNeeded(msg);
        //this.log.add("b " + String.valueOf(msg.getId()));
        ArrayList<Integer> destinationPorts = new ArrayList<>();
        ArrayList<String> destinationIps = new ArrayList<>();
        for(int destId: this.systemHosts.keySet()) {
            destinationPorts.add(getPortFromId(destId));
            destinationIps.add(this.systemHosts.get(destId));
        }
        System.out.println("·· Broadcasting m " + msg.getOverallUniqueId() + " with BEB ··");
        this.pLink.sendMultiple(message, destinationIps, destinationPorts);
    }

    public void broadcastOneByOne(Message msg) throws IOException, UnknownHostException {
        Message message = changeSenderIfNeeded(msg);
        for(int destId: this.systemHosts.keySet()) {
            this.pLink.send(message, this.systemHosts.get(destId), getPortFromId(destId)); 
        }
    }

    @Override
    public void deliver(Message msg, int currentSenderId) throws UnknownHostException, IOException {
        int senderId = msg.getCurrentSenderId();
        if(senderId != currentSenderId){
            System.out.println("!!! senderId != currentSenderId !!!! - Beb");
        }
        this.urbObserver.deliver(msg, currentSenderId);
        this.delivered.add(msg);
        //deliverToLog(msg);
        //System.out.println("* * " + msg.getRcvdFromMsg() + " : delivered to BEB * *");
    }

    private int getPortFromId(int hostId) {
        return PORT_PREFIX + hostId;
    }

    private Message changeSenderIfNeeded(Message msg) {
        if(msg.getCurrentSenderId() != this.hostId) {
            return new Message(msg, this.hostId);
        } else {
            return msg;
        }
    }

    /*
    private void deliverToLog(Message msg) {
        this.log.add("d " + msg.getOriginalHostId() + " " + String.valueOf(msg.getId()));
        this.delivered.add(msg);
        System.out.println("* * * *" + msg.getRcvdFromMsg() + " : delivered to BEB * * * *");
    }*/

    public PerfectLink getLink() {
        return this.pLink;
    }

    public int getPort() {
        return this.port;
    }

    public int gethostId() {
        return this.hostId;
    }
    
    public HashMap<Integer, String> getSystemHosts() {
        return this.systemHosts;
    }

    public boolean isClosed() {
        return this.pLink.isClosed();
    }

    public void close() {
        this.pLink.close();
    }

    /*
    public ArrayList<String> getLog() {
        return this.log;
    }*/

    public int getHostId() {
        return this.hostId;
    }


}
