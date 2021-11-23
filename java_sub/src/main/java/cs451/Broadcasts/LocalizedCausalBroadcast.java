package cs451.Broadcasts;

import cs451.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.net.UnknownHostException;

public class LocalizedCausalBroadcast implements Observer {

    private UniformReliableBroadcast urb;
    private int hostId;
    private HashMap<Integer, String> systemHosts = new HashMap<Integer, String>();
    private int[] next;
    private ArrayList<String> log = new ArrayList<>();
    private ArrayList<String> delivered = new ArrayList<>();
    private HashMap<String, Message> pending = new HashMap<String, Message>();

    public LocalizedCausalBroadcast(int hostId, HashMap<Integer, String> systemHosts) throws IOException { 
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
    public void deliver(Message msg, int currentSederId) throws UnknownHostException, IOException {
        // TODO Auto-generated method stub
        
    }

}
