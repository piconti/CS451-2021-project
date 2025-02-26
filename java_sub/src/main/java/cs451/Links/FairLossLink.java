// Inspired from https://www.baeldung.com/udp-in-java 
// https://www.geeksforgeeks.org/working-udp-datagramsockets-java/
// https://stackoverflow.com/questions/10055913/set-timeout-for-socket-receive

package cs451.Links;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket; 
import java.util.ArrayList;
import java.net.SocketTimeoutException;
import java.lang.ClassNotFoundException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.net.SocketException;
import cs451.*;

public class FairLossLink {
    
    private static final String IP_START_REGEX = "/";
    private static final int RECEIVE_BUFF_LENGTH = 2048;

    private DatagramSocket socket;
    private String ip;
    private int port;
    private int hostId;
    private byte[] sendBuf;
    private byte[] receiveBuf = new byte[RECEIVE_BUFF_LENGTH];
    private boolean receiving = true;
    private ArrayList<Message> delivered = new ArrayList<>();
    private Message emptyMsg = new Message(0, 0, "", false);

    public FairLossLink(String ip, int port, int hostId) throws SocketException, UnknownHostException {
        this.ip = ip;
        this.port = port;
        this.hostId = hostId;
        System.out.println("My ip: " + ip);
        System.out.println("My port: " + port);
        System.out.println("My hostId: " + hostId);
        this.socket = new DatagramSocket(port, InetAddress.getByName(ip));
    }

    public void send(Message message, String destIp, int destPort) throws IOException, UnknownHostException {
        connectSocket();
        Message newMsg = changeSenderIfNeeded(message);
        sendBuf = newMsg.convertToBytes(); 
        DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(destIp.split(IP_START_REGEX)[0]), destPort);
        socket.send(packet);
        //System.out.println("Sent: " + newMsg.getMessage() + " to " + String.valueOf(destPort));
    }

    private Message changeSenderIfNeeded(Message msg) {
        if(msg.getCurrentSenderId() != this.hostId) {
            return new Message(msg, this.hostId);
        } else {
            return msg;
        }
    }


    public Message receive() throws IOException, ClassNotFoundException {
        connectSocket();
        DatagramPacket rcvPacket = new DatagramPacket(this.receiveBuf, RECEIVE_BUFF_LENGTH);
        socket.setSoTimeout(1000);   // set the timeout in millisecounds.
        while(true){        // recieve data until timeout
            try {
                this.receiveBuf = new byte[RECEIVE_BUFF_LENGTH];
                rcvPacket = new DatagramPacket(this.receiveBuf, RECEIVE_BUFF_LENGTH);
                socket.receive(rcvPacket);
                Message msg = Message.convertFromBytes(this.receiveBuf);
                msg.setCurrentSenderIp(String.valueOf(rcvPacket.getAddress()));
                //String rcvd = msg.getRcvdFromMsg();
                //System.out.println("Inside FairLossLink: " + rcvd);
                delivered.add(msg);
                return msg;
            }
            catch (SocketTimeoutException e) {
                // timeout exception.
                System.out.println("Timeout reached!!! in FairLossLink receive of host " + String.valueOf(hostId));
            } finally {
                if(!this.receiving) {
                    break;
                }
            }
        }
        return emptyMsg;
    }


    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public DatagramSocket getSocket() {
        return this.socket;
    }

    public int getHostId() {
        return this.hostId;
    }

    public void close() {
        this.socket.close();
    }

    public boolean isReceiving() {
        return this.receiving;
    }

    public void setReceiving(boolean newVal) {
        this.receiving = newVal;
    }

    public void open() throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket(port, InetAddress.getByName(ip));
    }

    public ArrayList<Message> getDelivered() {
        return this.delivered;
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