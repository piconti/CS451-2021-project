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


public class PerfectLink extends Thread {

    private static final int PORT_PREFIX = 11000;
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


    public void sendMultiple(Message message, ArrayList<String> destinationIps, ArrayList<Integer> destinationPorts) throws IOException, UnknownHostException {
        //System.out.println("\n· Broadcasting " + message.getOverallUniqueId());
        for(int i=0; i<destinationIps.size(); ++i) {
            send(message, destinationIps.get(i), destinationPorts.get(i));
        }
    }


    public synchronized void send(Message message, String destinationIp, int destinationPort) throws IOException, UnknownHostException {
        
        flLink.send(message, destinationIp, destinationPort);
        String leftToAckKey = getLeftToAckKey(message.getOverallUniqueId(), destinationPort);
        if(!this.leftToAck.containsKey(leftToAckKey)) {
            Message newMsg = new Message(message, destinationIp, destinationPort);
            this.sentLog.add("b " + String.valueOf(newMsg.getId()));
            //System.out.println("____ Putting msg: " + leftToAckKey + " in leftToAck");
            this.leftToAck.put(leftToAckKey, newMsg);
        }
    }

    public void resendAllLeftToAck() throws IOException, UnknownHostException {
        //System.out.println("____ Inside resend all left to ack: " + this.leftToAck.size() + " messages left to ack");
        for (Message m : this.leftToAck.values()) {
            int port = m.getDestinationPort();
            if(port != -1) {
                //System.out.println("Resending : " + m.getOverallUniqueId() + " to " + String.valueOf(port));
                flLink.send(m, m.getDestinationIp(), port);
            } else {
                System.out.println("Cannot resend message " + m.getOverallUniqueId() + " as it does not have a destination.");
            }
        }
    }

    public void receive() throws SocketException, UnknownHostException, IOException, ClassNotFoundException {
        while(this.receiving) {
            flLink.setReceiving(true);
            Message msg = flLink.receive();
            int currentSenderId = msg.getCurrentSenderId();
            if(!msg.getContents().equals("")) {
                deliver(msg, currentSenderId);
            }
        }
    }

    private String getLeftToAckKey(String msgOverallUniqueId, int destinationPort) {
        return msgOverallUniqueId + " " + String.valueOf(destinationPort);
    }

    public synchronized void deliver(Message rcvdMsg, int currentSenderId) throws UnknownHostException, IOException{
        String senderIp = rcvdMsg.getCurrentSenderIp();
        int senderPort = rcvdMsg.getCurrentSenderPort();
        if(senderPort != getPortFromId(currentSenderId)){
            System.out.println("!!! Current sender actualized before delivering !!!! - PerfectLink");
        }
        if(rcvdMsg.isMsg()) {
            String overallUniqueId = rcvdMsg.getOverallUniqueId();
            if(!delivered.contains(overallUniqueId)) {
                sendAck(senderIp, senderPort, overallUniqueId);
                this.bebObserver.deliver(rcvdMsg, currentSenderId);
                //System.out.println("--> * " + rcvdMsg.getRcvdFromMsg() + " : received in Perfectlink, sent to beb *");
                this.delivered.add(overallUniqueId);
            } //else {
                //System.out.println("  --> " + rcvdMsg.getRcvdFromMsg() + " : already received");
            //}
        } else {
            String msgOverallUniqueId = rcvdMsg.getOverallUniqueIdOfAckedMsg();
            String leftToAckKey = getLeftToAckKey(msgOverallUniqueId, senderPort);
            if(leftToAck.containsKey(leftToAckKey)) {
                //System.out.println("____ Removing msg: " + leftToAckKey + " from leftToAck");
                this.leftToAck.remove(leftToAckKey);
                //System.out.println("--> Host " + senderPort + ", acked m " + msgOverallUniqueId);
            } //else {
                //System.out.println("  --> Host " + senderPort + ", acked m " + msgOverallUniqueId + " but ack already received");
            //}
        } 
    }

    private int getPortFromId(int hostId) {
        return PORT_PREFIX + hostId;
    }

    private void sendAck(String senderIp, int senderPort, String fullId) throws UnknownHostException, IOException {
        String contents = "ack " + fullId;
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
                while(true) {
                    receive();
                }
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
                    SenderThread.sleep(2000);
                    resendAllLeftToAck();
                }
            } catch(SocketException e) {
                System.out.println("Issue with the socket.");
            } catch(UnknownHostException e) {
                System.out.println("Issue with the Host.");
            } catch(IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Perfect Link of host " + hostId +  ": sender thread is over");
        }
    }

}