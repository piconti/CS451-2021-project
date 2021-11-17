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

    private int getPortFromId(int hostId) {
        return PORT_PREFIX + hostId;
    }

    public void broadcast(Message msg) throws IOException, UnknownHostException {
        msg.addCurrentSenderId(this.hostId);
        for(int destId: this.systemHosts.keySet()) {
            this.pLink.send(msg, this.systemHosts.get(destId), getPortFromId(destId)); 
        }
    }

    @Override
    public void deliver(Message msg) throws UnknownHostException, IOException {
        this.delivered.add(msg);
        this.urbObserver.deliver(msg);
    }

    /* // TODO DELETE?
    public ArrayList<Message> getDelivered() {
        return this.pLink.getDelivered();
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

    /*
    public void updateSystemHosts(ArrayList<int> newHosts) {
        this.systemHostIds = newHosts;
    }

    public void suspectHost(int supectedHostId) {
        this.systemHostIds.remove(supectedHostId);
    }

    public boolean checkIfSuspected(int hostToCheck) {
        return this.systemHostIds.contains(hostToCheck);
    }
    */
    
    public HashMap<Integer, String> getSystemHosts() {
        return this.systemHosts;
    }

}