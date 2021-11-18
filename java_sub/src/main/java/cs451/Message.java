// Inspired from https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
// https://www.baeldung.com/java-serialization 

package cs451;

import java.io.Serializable;
import java.lang.NullPointerException;
import java.lang.ClassNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
//import java.util.ArrayList;


public class Message implements Serializable {

    //private static final String localIp = "localhost";
    private static final int PORT_PREFIX = 11000;

    private final int originalHostId;
    private String contents;
    private int id;
    private boolean msg; 
    private String destinationIp;
    private int destinationPort;
    //private ArrayList<Integer> senderIds = new ArrayList<>(); //needed? check after
    private int senderId;
    private String currentSenderIp;
    private int fifoSeqNum;
    //private static ByteArrayOutputStream bos = new ByteArrayOutputStream();
    //private static ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

   
    public Message(int hostId, int id, String contents, boolean msg) {
        this.originalHostId = hostId;
        this.id = id;
        this.contents = contents;
        this.msg = msg;
        this.senderId = hostId;
        //this.senderIds.add(this.senderId);
    }

    public Message(Message msgToCopy, int newSenderId) {
        this.originalHostId = msgToCopy.getOriginalHostId();
        this.id = msgToCopy.getId();
        this.contents = msgToCopy.getContents();
        this.msg = msgToCopy.isMsg();
        this.senderId = newSenderId;
        //this.senderIds.add(this.senderId);
    }

    public Message(Message msgToCopy, String destinationIp, int destinationPort) {
        this.originalHostId = msgToCopy.getOriginalHostId();
        this.id = msgToCopy.getId();
        this.contents = msgToCopy.getContents();
        this.msg = msgToCopy.isMsg();
        this.senderId = msgToCopy.getCurrentSenderId();
        this.destinationIp = destinationIp;
        this.destinationPort = destinationPort;
        //this.senderIds.add(this.senderId);
    }

    /* // never used
    public int[] identify() {
        int[] identifier = new int[2];
        identifier[0] = this.id;
        identifier[1] = this.originalHostId;
        return identifier;
    }*/

    
    public String getOriginalUniqueId() {
        // original unique id: "messageId orginialSenderId"
        return String.valueOf(this.id) + " " + String.valueOf(this.originalHostId);
    }

    /*
    public String getUniqueId() {
        // unique id: "messageId currentSederId"
        return String.valueOf(this.id) + " " + String.valueOf(this.senderId);
    }*/

    public String getOverallUniqueId() {
        // overall unique id: "messageId orginialSenderId currentSederId"
        return String.valueOf(this.id) + " " + String.valueOf(this.originalHostId) + " " + String.valueOf(this.senderId);
    }

    public String getOverallUniqueIdToPrint() {
        // overall unique id: "messageId orginialSenderId currentSederId"
        return "msgId: " + String.valueOf(this.id) + ", ogSender: " + String.valueOf(this.originalHostId) + ", currentSender: " + String.valueOf(this.senderId);
    }

    public int[] getOverallUniqueIdTab() {
        // overall unique id as array: [messageId, orginialSenderId, currentSederId]
        return new int[] {this.id, this.originalHostId, this.senderId};
    }

    public String getMessage() {
        return getOverallUniqueIdToPrint() + " : [" + contents + "]";
    }

    public void showMessage() {
        System.out.println(getMessage());
    }

    public int getOriginalHostId() {
        return this.originalHostId;
    }

    public String getContents() {
        return this.contents;
    }

    public int getId() {
        return this.id;
    }

    public boolean isMsg() {
        return this.msg;
    }
    
    public int getCurrentSenderId() {
        return this.senderId;
    }

    public int getCurrentSenderPort() {
        return getPortFromId(this.senderId);
    }

    public void setCurrentSenderIp(String ip) {
        this.currentSenderIp = ip;
    }

    public String getCurrentSenderIp() {
        try {
            return this.currentSenderIp;
        } catch (NullPointerException e) {
            System.out.println("the destination ip of the message was not defined.");
            return "localhost";
        }
    }

    /* // TODO delete?
    public ArrayList<Integer> getAllSenderIds() {
        return this.senderIds;
    }*/

    public void addCurrentSenderId(int id) {
        if(id != this.senderId) {
            this.senderId = id;
            //this.senderIds.add(id);
        }
    }

    public String getOverallUniqueIdOfAckedMsg() {
        if(!this.msg) {
            String[] id = this.contents.split("ack ");
            return id[1];
        } else {
            return "";
        }
    }

    public void setDestination(String destinationIp, int destinationPort) {
        this.destinationIp = destinationIp;
        this.destinationPort = destinationPort;
    }

    
    public String getDestinationIp() {
        try {
            return this.destinationIp;
        } catch (NullPointerException e) {
            System.out.println("the destination ip of the message was not defined.");
            return "localhost";
        }
    }

    public int getDestinationPort() {
        try {
            return this.destinationPort;
        } catch (NullPointerException e) {
            System.out.println("the destination port of the message was not defined.");
            return -1;
        } 
    }

    public void setFifoSeqNum(int seqNum) {
        this.fifoSeqNum = seqNum;
    }

    public int getFifoSeqNum() {
        try {
            return this.fifoSeqNum;
        } catch (NullPointerException e) {
            System.out.println("the sequence number of the message was not defined.");
            return -1;
        } 
    }

    public byte[] convertToBytes() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            return bos.toByteArray();
        }
    }

    public static Message convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            return (Message) in.readObject();
        }
    }

    public String getRcvdFromMsg() {
        String rcvd = "rcvd from host " + this.senderId;
        if(this.senderId != this.originalHostId) {
            return rcvd + " (original host: " + this.originalHostId + "): " + this.getContents();
        } else {
            return rcvd + ": [" + this.getContents() + "]";
        }
    }

    private int getPortFromId(int hostId) {
        return PORT_PREFIX + hostId;
    }


}