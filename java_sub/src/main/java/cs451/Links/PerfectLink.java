// Inspired from https://www.baeldung.com/udp-in-java 
// https://www.geeksforgeeks.org/working-udp-datagramsockets-java/
// https://stackoverflow.com/questions/10055913/set-timeout-for-socket-receive 
// https://beginnersbook.com/2013/03/multithreading-in-java/
// https://dzone.com/articles/java-thread-tutorial-creating-threads-and-multithr
// https://www.callicoder.com/java-concurrency-issues-and-thread-synchronization/



package cs451.Links;

import cs451.*;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket; 
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.lang.ClassNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.io.Serializable;
import java.util.HashMap;
import java.lang.Thread;
import java.lang.Runnable;


public class PerfectLink implements Runnable {

    private static final String IP_START_REGEX = "/";
    private static final int RECEIVE_BUFF_LENGTH = 2048;
    private static final int PORT_PREFIX = 11000;
    private Thread receiveThread;

    private FairLossLink flLink;
    private DatagramSocket socket;
    private String ip;
    private int port;
    private int hostId;
    private byte[] sendBuf;
    private byte[] receiveBuf = new byte[RECEIVE_BUFF_LENGTH];
    private HashMap<String, Message> leftToAck = new HashMap<String, Message>();
    private ArrayList<Message> delivered = new ArrayList<>();
    private ArrayList<String> deliveredLog = new ArrayList<>();
    private ArrayList<String> sentLog = new ArrayList<>();
    private boolean receiving = true; // ajouter atomic
    //private boolean continueSending = true;

    public PerfectLink(String ip, int port, int hostId) throws IOException {
        this.flLink = new FairLossLink(ip, port, hostId);
        this.ip = flLink.getIp();
        this.port = flLink.getPort();
        this.socket = flLink.getSocket();
        this.hostId = flLink.getHostId();
        Thread receiveThread = new Thread(this, "Perfect Link receive thread");
        receiveThread.start();
    }

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
    } 

    public void send(Message message, String destinationIp, int destinationPort) throws IOException, UnknownHostException {
        connectSocket();
        //boolean acked = false;
        
        flLink.send(message, destinationIp, destinationPort);
        if(!leftToAck.containsKey(message.getUniqueId())) {
            message.setDestination(destinationIp, destinationPort);
            this.leftToAck.put(message.getUniqueId(), message);
        }
        /*while(!acked){
            flLink.send(message, destinationIp, destinationPort);
            acked = checkIfAcked(message, destinationPort);
        }*/
        //sentLog.add("b " + message.getId());
    }

    public void resendAllLeftToAck() throws IOException, UnknownHostException {
        for (Message m : this.leftToAck.values()) {
            int port = m.getDestinationPort();
            if(port != -1) {
                send(m, m.getDestinationIp(), port);
            } else {
                System.out.println("Cannot resend message " + m.getUniqueId() + " as it does nothave a destination.");
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
    /*  int timeoutCount = 0;
        connectSocket();
        DatagramPacket rcvPacket = new DatagramPacket(receiveBuf, RECEIVE_BUFF_LENGTH);
        socket.setSoTimeout(2000);   // set the timeout in millisecounds.
        while(true){        // recieve data until timeout
            try {
                if(isClosed()) {
                    break;
                }
                socket.receive(rcvPacket);
                deliver(rcvPacket);
                this.receiveBuf = new byte[RECEIVE_BUFF_LENGTH]; 
            }
            catch (SocketTimeoutException e) {
                // timeout exception.
                System.out.println("Timeout reached!!! ");
                if(stopReceiving){
                    break;
                }
            }
            if(stopReceiving){
                break;
            }
        }
    }*/

    

    /*// TODO DELETE
    private boolean checkIfAcked(Message message, int destinationPort) throws SocketException, UnknownHostException, IOException {
        connectSocket();
        DatagramPacket ackPacket = new DatagramPacket(receiveBuf, RECEIVE_BUFF_LENGTH);
        socket.setSoTimeout(2000);
        while(true) {
            try {
                socket.receive(ackPacket);
                String contents = new String(ackPacket.getData(), 0, ackPacket.getLength());
                if((destinationPort == ackPacket.getPort()) && contents.contains("ack")) {
                    System.out.println("Host " + ackPacket.getAddress() + ", " + ackPacket.getPort() + " acked");
                    return true;
                } else {
                    deliver(ackPacket);
                }
                this.receiveBuf = new byte[RECEIVE_BUFF_LENGTH];
            }
            catch (SocketTimeoutException e) {
                // timeout exception.
                System.out.println("Timeout reached!!! in PerfectLink of host " + String.valueOf(hostId));
                return false;
            }
        }
    }*/

    private synchronized void deliver(Message rcvdMsg) throws UnknownHostException, IOException{
        String rcvd = rcvdMsg.getRcvdFromMsg(); 
        String uniqueId = rcvdMsg.getUniqueId();
        String senderIp = rcvdMsg.getSourceIp();
        int senderPort = rcvdMsg.getSourcePort();
        if(rcvdMsg.isMsg()) {
            if(!delivered.contains(rcvdMsg)) {
                System.out.println(rcvd + " : delivered");
                //String data = new String(packet.getData(), 0, packet.getLength());
                //String identifier = getIdentifierFromData(data);
                sendAck(senderIp, senderPort, uniqueId);
                //String senderId = getSenderIdFromPort(packet.getPort());
                //int rcvdId = getMessageIdFromData(data);
                deliveredLog.add("d " + rcvdMsg.getHostId() + " " + String.valueOf(rcvdMsg.getId()));
                delivered.add(rcvdMsg);
            } else {
                System.out.println(rcvd + " : already delivered");
            }
        } else {
            String msgUniqueId = rcvdMsg.getUniqueIdOfAckedMsg();
            if(leftToAck.containsKey(msgUniqueId)) {
                Message ackedMsg = this.leftToAck.remove(msgUniqueId);
                System.out.println("Host " + senderIp + ", " + senderPort + " acked " + msgUniqueId);
                //System.out.println("acked msg id: " + String.valueOf(ackedMsg.getId())); /// GENERATES NULLPOINTER EXCEPTION
                this.sentLog.add("b " + String.valueOf(ackedMsg.getId()));
            } else {
                System.out.println("Host " + senderIp + ", " + senderPort + " acked " + msgUniqueId + " but ack already received");
            }
        } 
    }

    // todo delete?
    /*private String getRcvdFromPacket(DatagramPacket dp) {
        return "rcvd from " + dp.getAddress() + ", " + dp.getPort() + ": "+ new String(dp.getData(), 0, dp.getLength());
    }*/

    private void sendAck(String senderIp, int senderPort, String identifier) throws UnknownHostException, IOException {
        //String senderPort = getSenderIdFromPort(packet.getPort());
        String contents = "ack " + identifier;
        Message ack = new Message(this.hostId, 0, ip, port, contents, false);
        //connectSocket();
        flLink.send(ack, senderIp, senderPort);
    }

    // TODO Delete
    /*private int getMessageIdFromData(String data) {
        String[] splitData = data.split("\\s+");
        System.out.println(data.charAt(0));
        return Integer.parseInt(splitData[0]);
    }*/

    // TODO Delete
    /*private String getIdentifierFromData(String data) {
        String[] splitData = data.split("m");
        String identifier = splitData[0];
        System.out.println("identifier :" + identifier);
        return identifier;
    }*/

    // TODO Delete
    /*private String getSenderIdFromPort(int port) {
        return String.valueOf(port-PORT_PREFIX);
    }*/

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
        socket.close();
    }

    /*public boolean getStopReceiving() {
        return this.stopReceiving;
    }*/

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

    /*public boolean continueSending() {
        return this.continueSending;
    }

    public void setContinueSending(boolean newVal) {
        this.continueSending = newVal;
    }

    public void open() throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket(port, InetAddress.getByName(ip));
    }*/

    public ArrayList<Message> getDelivered() {
        return this.delivered;
    }

    public ArrayList<String> getDeliveredLog() {
        return this.deliveredLog;
    }

    public ArrayList<String> getSentLog() {
        return this.sentLog;
    }

    public void connectSocket() throws UnknownHostException {
        if(socket.isClosed()) {
            socket.connect(InetAddress.getByName(ip), port);
        }
    }

    public boolean isClosed() {
        return this.socket.isClosed();
    }

}