// Inspired from https://www.baeldung.com/udp-in-java 
// https://www.geeksforgeeks.org/working-udp-datagramsockets-java/
// https://stackoverflow.com/questions/10055913/set-timeout-for-socket-receive

package cs451.Links;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket; 
import java.util.ArrayList;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.net.SocketException;
import cs451.*;

public class FairLossLink  {
    
    private static final String IP_START_REGEX = "/";
    private static final int RECEIVE_BUFF_LENGTH = 4096;

    private DatagramSocket socket;
    private String ip;
    private int port;
    private int hostId;
    private byte[] sendBuf;
    private byte[] receiveBuf = new byte[RECEIVE_BUFF_LENGTH];
    private boolean receiving = true;
    private ArrayList<String> delivered = new ArrayList();

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
        sendBuf = message.getMessage().getBytes();
        DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(destIp.split(IP_START_REGEX)[0]), destPort);
        socket.send(packet);
        System.out.println("Sent: " + message.getMessage());
    }

    public DatagramPacket receive() throws IOException {
        connectSocket();
        DatagramPacket rcvPacket = new DatagramPacket(receiveBuf, RECEIVE_BUFF_LENGTH);
        socket.setSoTimeout(1000);   // set the timeout in millisecounds.
        while(this.receiving){        // recieve data until timeout
            try {
                socket.receive(rcvPacket);
                String rcvd = "rcvd from " + rcvPacket.getAddress() + ", " + rcvPacket.getPort() + ": "+ new String(rcvPacket.getData(), 0, rcvPacket.getLength());
                System.out.println(rcvd);
                delivered.add(rcvd);
                this.receiveBuf = new byte[RECEIVE_BUFF_LENGTH];
                return rcvPacket;
            }
            catch (SocketTimeoutException e) {
                // timeout exception.
                System.out.println("Timeout reached!!! in FairLossLink of host " + String.valueOf(hostId));
            }
        }
        return rcvPacket;
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
        socket.close();
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

    public ArrayList<String> getDelivered() {
        return this.delivered;
    }

    public void connectSocket() throws UnknownHostException {
        if(socket.isClosed()) {
            socket.connect(InetAddress.getByName(ip), port);
        }
    }

}