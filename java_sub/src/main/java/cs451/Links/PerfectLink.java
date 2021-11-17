// Inspired from https://www.baeldung.com/udp-in-java 
// https://beginnersbook.com/2013/03/multithreading-in-java/
// https://dzone.com/articles/java-thread-tutorial-creating-threads-and-multithr
// https://www.callicoder.com/java-concurrency-issues-and-thread-synchronization/
// https://www.baeldung.com/java-concurrent-map

package cs451.Links;

import cs451.*;
import java.util.ArrayList;
import java.net.UnknownHostException;
import java.lang.ClassNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Thread;
//import java.lang.Runnable;


public class PerfectLink extends Thread {

    private Thread receiveThread;
    private Thread resendAcksThread;
    private FairLossLink flLink;
    private Observer bebObserver;
    private String ip;
    private int port;
    private int hostId;
    private ConcurrentHashMap<String, Message> leftToAck = new ConcurrentHashMap<String, Message>();
    private ArrayList<String> delivered = new ArrayList<>();
    private ArrayList<String> deliveredLog = new ArrayList<>();
    private ArrayList<String> sentLog = new ArrayList<>();
    private boolean receiving = true; // ajouter atomic
    //private boolean continueSending = true;

    public PerfectLink(String ip, int port, int hostId, Observer bebObserver) throws IOException {
        this.flLink = new FairLossLink(ip, port, hostId);
        this.ip = flLink.getIp();
        this.port = flLink.getPort();
        this.hostId = flLink.getHostId();
        this.bebObserver = bebObserver;
        this.receiveThread = new ReceiverThread();
        this.resendAcksThread = new SenderThread();
        receiveThread.setDaemon(true);
        resendAcksThread.setDaemon(true);
        receiveThread.start();
        resendAcksThread.start();
    }

    /*
    public void run() {
        System.out.println("Inside PerfectLink's run()");
        try {
            receive();
        } catch(SocketException e) {
            System.out.println("Issue with the socket.");
        } catch(UnknownHostException e) {
            System.out.println("Issue with the Host.");
        } catch(IOException e) {
            e.printStackTrace();
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Perfect Link of host " + this.hostId +  ": receive thread is over");
    } */

    public void sendMultiple(Message message, ArrayList<String> destinationIps, ArrayList<Integer> destinationPorts) throws IOException, UnknownHostException {
        for(int i=0; i<destinationIps.size(); ++i) {
            send(message, destinationIps.get(i), destinationPorts.get(i));
        }
    }


    public void send(Message message, String destinationIp, int destinationPort) throws IOException, UnknownHostException {
        
        flLink.send(message, destinationIp, destinationPort);
        if(!this.leftToAck.containsKey(message.getOverallUniqueId())) {
            message.setDestination(destinationIp, destinationPort);
            this.sentLog.add("b " + String.valueOf(message.getId()));
            this.leftToAck.put(message.getOverallUniqueId(), message);
        }
    }

    public void resendAllLeftToAck() throws IOException, UnknownHostException {
        for (Message m : this.leftToAck.values()) {
            int port = m.getDestinationPort();
            if(port != -1) {
                send(m, m.getDestinationIp(), port);
            } else {
                System.out.println("Cannot resend message " + m.getOverallUniqueId() + " as it does not have a destination.");
            }
        }
    }

    public void receive() throws SocketException, UnknownHostException, IOException, ClassNotFoundException {
        while(this.receiving) {
            flLink.setReceiving(true);
            Message msg = flLink.receive();
            if(!msg.getContents().equals("")) {
                deliver(msg);
            }
        }
    }

    public synchronized void deliver(Message rcvdMsg) throws UnknownHostException, IOException{
        String senderIp = rcvdMsg.getCurrentSenderIp();
        int senderPort = rcvdMsg.getCurrentSenderPort();
        if(rcvdMsg.isMsg()) {
            String overallUniqueId = rcvdMsg.getOverallUniqueId();
            if(!delivered.contains(overallUniqueId)) {
                this.bebObserver.deliver(rcvdMsg);
                this.delivered.add(overallUniqueId);
                sendAck(senderIp, senderPort, rcvdMsg.getOverallUniqueIdTab());
            } else {
                System.out.println(rcvdMsg.getRcvdFromMsg() + " : already delivered");
            }
        } else {
            String msgUniqueId = rcvdMsg.getUniqueIdOfAckedMsg();
            if(leftToAck.containsKey(msgUniqueId)) {
                this.leftToAck.remove(msgUniqueId);
                System.out.println("Host " + senderIp + ", " + senderPort + " acked " + msgUniqueId);
            } else {
                System.out.println("Host " + senderIp + ", " + senderPort + " acked " + msgUniqueId + " but ack already received");
            }
        } 
    }

    /*
    private synchronized void addToDelivered(String overallUniqueId) {
        //this.deliveredLog.add("d " + msg.getOriginalHostId() + " " + String.valueOf(msg.getId()));
        this.delivered.add(overallUniqueId);
        System.out.println(msg.getRcvdFromMsg() + " : delivered");
    }*/


    private void sendAck(String senderIp, int senderPort, int[] fullId) throws UnknownHostException, IOException {
        String contents = "ack " + String.valueOf(fullId[0]) + " sent by " + String.valueOf(fullId[2]);
        contents += ", original sender: " + String.valueOf(fullId[2]);
        Message ack = new Message(this.hostId, 0, contents, false);
        flLink.send(ack, senderIp, senderPort);
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public int getHostId() {
        return this.hostId;
    }

    public void close() {
        this.flLink.close();
    }

    public synchronized void setReceiving(boolean newVal) {
        this.receiving = newVal;
        this.flLink.setReceiving(newVal);
    }

    public synchronized void stopReceiving() {
        this.receiving = false;
        this.flLink.setReceiving(false);
    }

    public synchronized void startReceiving() {
        this.receiving = true;
        this.flLink.setReceiving(true);
    }

    public boolean isReceiving() {
        return this.receiving;
    }

    public boolean hasLeftToAck() {
        return !this.leftToAck.isEmpty();
    }

    public ArrayList<String> getDelivered() {
        return this.delivered;
    }

    public ArrayList<String> getDeliveredLog() {
        return this.deliveredLog;
    }

    public ArrayList<String> getSentLog() {
        return this.sentLog;
    }

    public boolean isClosed() {
        return this.flLink.isClosed();
    }

    private class ReceiverThread extends Thread {

        private ReceiverThread() {}

        public void run() {
            System.out.println("Inside PerfectLink's ReceiverThread run()");
            try {
                receive();
            } catch(SocketException e) {
                System.out.println("Issue with the socket.");
            } catch(UnknownHostException e) {
                System.out.println("Issue with the Host.");
            } catch(IOException e) {
                e.printStackTrace();
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("Perfect Link of host " + hostId +  ": receive thread is over");
        }
    }

    private class SenderThread extends Thread {

        private SenderThread() {}

        public void run() {
            System.out.println("Inside PerfectLink's SenderThread run()");
            try {
                while(true) { 
                    resendAllLeftToAck();
                }
            } catch(SocketException e) {
                System.out.println("Issue with the socket.");
            } catch(UnknownHostException e) {
                System.out.println("Issue with the Host.");
            } catch(IOException e) {
                e.printStackTrace();
            }
            System.out.println("Perfect Link of host " + hostId +  ": sender thread is over");
        }
    }

}