package cs451;

import cs451.Links.*;

public class Message {

    private String sourceIp;
    private int sourcePort;
    private int hostId;
    private String contents;
    private int id;

   
    public Message(int hostId, int id, String sourceIp, int sourcePort, String contents) {
        this.hostId = hostId;
        this.id = id;
        this.sourceIp = sourceIp;
        this.sourcePort = sourcePort;
        this.contents = contents;
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
        return hostId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public String getContents() {
        return contents;
    }

    public int getId() {
        return this.id;
    }

}