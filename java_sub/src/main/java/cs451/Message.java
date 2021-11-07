// Inspired from https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
// https://www.baeldung.com/java-serialization 

package cs451;

import cs451.Links.*;
import java.io.Serializable;
import java.lang.NullPointerException;
import java.lang.ClassNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public class Message implements Serializable {

    private String sourceIp;
    private int sourcePort;
    private int hostId;
    private String contents;
    private int id;
    private boolean msg; 
    private String destinationIp;
    private int destinationPort;
    //private static ByteArrayOutputStream bos = new ByteArrayOutputStream();
    //private static ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

   
    public Message(int hostId, int id, String sourceIp, int sourcePort, String contents, boolean msg) {
        this.hostId = hostId;
        this.id = id;
        this.sourceIp = sourceIp;
        this.sourcePort = sourcePort;
        this.contents = contents;
        this.msg = msg;
    }

    public int[] identify() {
        int[] identifier = new int[2];
        identifier[0] = id;
        identifier[1] = hostId;
        return identifier;
    }

    public String getUniqueId() {
        return String.valueOf(id) + " " + String.valueOf(hostId);
    }

    public String getMessage() {
        return getUniqueId() + " m: " + contents;
    }

    public void showMessage() {
        System.out.println(getMessage());
    }

    public int getHostId() {
        return this.hostId;
    }

    public String getSourceIp() {
        return this.sourceIp;
    }

    public int getSourcePort() {
        return this.sourcePort;
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

    public String getUniqueIdOfAckedMsg() {
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
        return "rcvd from " + this.sourceIp + ", " + this.sourcePort + ": " + this.getContents();
    }



}