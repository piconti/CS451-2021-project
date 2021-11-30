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
    private ArrayList<String> log = new ArrayList<>();
    private ArrayList<String> delivered = new ArrayList<>();
    private int[] vectorClock;
    private int[] affected;
    private ArrayList<HashMap<int[], String>> pending = new ArrayList<HashMap<int[], String>>();
    private ArrayList<ArrayList<int[]>> deliverable = new ArrayList<ArrayList<int[]>>();
    private int localSeqNumber = 0;

    public LocalizedCausalBroadcast(int hostId, HashMap<Integer, String> systemHosts, int[] affected) throws IOException { 
        this.hostId = hostId;
        this.systemHosts = systemHosts;
        this.urb = new UniformReliableBroadcast(this.hostId, this.systemHosts, this);
        this.vectorClock = new int[systemHosts.size()];
        this.affected = affected;
    }

    public void broadcast(Message msg) throws UnknownHostException, IOException {
        int[] wClock = vectorClock;
        wClock[this.hostId-1] = localSeqNumber;
        msg.setClock(wClock);
        this.urb.broadcast(msg);
        localSeqNumber += 1;
        this.log.add("b " + String.valueOf(msg.getId()));
    }

    @Override
    public void deliver(Message msg) throws UnknownHostException, IOException {
        // p dans le pseudo code: original sender ou current sender???
        String msgOgUniqueId = msg.getOriginalUniqueId();
        int[] wClock = msg.getClock();
        addToPending(msgOgUniqueId, wClock);
        while(canDeliver()) {
            for(int p=1; p<=this.deliverable.size(); ++p) {
                for(int[] w: this.deliverable.get(p-1)) {
                    String ogUniqueId = this.pending.get(p-1).get(w);
                    pending.get(p-1).remove(w);
                    this.vectorClock[p-1] += 1;
                    deliverToLog(ogUniqueId);
                }
            }
        }
    }

    @Override
    public void deliver(Message msg, int currentSederId) throws UnknownHostException, IOException {}

    private void addToPending(String msgOgUniqueId, int[] wClock) {
        // code if original sender id
        // get original host id as int
        String[] splitOgUniqueId = msgOgUniqueId.split("\\s+");
        int hostId = Integer.parseInt(splitOgUniqueId[1]);
        // get the hashmap of all received msg from this host at index hostId-1
        HashMap<int[], String> currentPendingForHost = this.pending.get(hostId - 1);
        // add this message to the hashmap
        currentPendingForHost.put(wClock, msgOgUniqueId);
        // replace  with the new hashmap inside pending
        this.pending.set(hostId - 1, currentPendingForHost);
    }

    private boolean canDeliver() {
        // returns true if there are elements in pending corresponding to condition. puts them inside deliverable

        // if deliverable is not empty, don't update it and empty it before
        if(this.deliverable.isEmpty()) {
            // for each host in the system
            for(int p=1; p<=this.pending.size(); ++p) {
                                                            //-> which one???
            // for each host it's affected by  
            //for(int p: this.affected) {

                // look at the vector clocks of messges they sent
                for(int[] w: this.pending.get(p-1).keySet()) {
                    // get all the already deliverable elems sent by p if there are some
                    ArrayList<int[]> deliverableForP = new ArrayList<>();
                    if(!this.deliverable.isEmpty()) {
                        deliverableForP = this.deliverable.get(p-1);
                    }
                    // if W ≤ V and W not already in deliverable, add it
                    if(isSmallerThanClock(w) && !deliverableForP.contains(w)) {
                        deliverableForP.add(w);
                        this.deliverable.set(p-1, deliverableForP);
                    } 
                }
            }
        }
        // true iff deliverable is not empty after updating it
        return !this.deliverable.isEmpty();
    }

    private boolean isSmallerThanClock(int[] w) {
        //for(int i=0; i<w.length; i++) {
        for(int p: this.affected) {
            if(w[p-1] > this.vectorClock[p-1]) {
            //if(w[i] > this.vectorClock[i]) {
                return false;
            }
        }
        return true;
    }

    private void deliverToLog(String msgOgUniqueId) {
        String[] splitOgUniqueId = msgOgUniqueId.split("\\s+");
        this.log.add("d " + splitOgUniqueId[1] + " " + splitOgUniqueId[0]);
        this.delivered.add(msgOgUniqueId);
    }

}
