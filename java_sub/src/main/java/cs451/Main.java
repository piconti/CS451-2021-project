package cs451;

import java.io.IOException;
import java.net.SocketException;
import java.lang.ClassNotFoundException;
import cs451.Parsers.*;
import cs451.Broadcasts.FifoReliableBroadcast;
import cs451.Links.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static Parser parser;
    public static PerfectLink link;
    public static FifoReliableBroadcast fifo;
    //public static BestEffortBroadcast beb;
    //public static UniformReliableBroadcast urb;
    public static int numMessagesToSend;
    public static int receiverHost;
    public static ArrayList<String> fifoLog = new ArrayList<>();
    //public static ArrayList<String> deliveredLog = new ArrayList<>();
    //private static final int PORT_PREFIX = 11000;


    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        try {
            System.out.println("Writing output.");
            try {
                fifoLog = fifo.getLog();
            } catch(NullPointerException e) {
                System.out.println("NullPointerException because no log exists. Generating empty log");
                fifoLog.add("");
            }
            parser.writeToOutput(fifoLog);
            //deliveredLog = fifo.getDeliveredLog();
            //parser.writeToOutput(senderLog);
            /*
            if(parser.myId() == receiverHost) {
                link.setReceiving(false);
                //Thread.sleep(2000);
                deliveredLog = link.getDeliveredLog();
                parser.writeToOutput(deliveredLog);
            } else {
                //link.setContinueSending(false);
                if(!parser.wrotetoOutput()) {
                    senderLog = link.getSentLog();
                    parser.writeToOutput(senderLog);
                }
            }*/
            //Thread.sleep(2000);
            if(!fifo.isClosed()) {
                System.out.println("closing link " + fifo.getHostId());
                fifo.close();
            }
        } catch (Exception e) {
            System.out.println("Something went wrong when wirting to the output file.");
            e.printStackTrace();
        }
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException, IOException, SocketException, ClassNotFoundException {
        
        parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        System.out.println("Doing some initialization\n");

        numMessagesToSend = parser.getNumMessages();
        //receiverHost = parser.getReceiverId();

        System.out.println("m: " + String.valueOf(numMessagesToSend));
        //System.out.println("i: " + String.valueOf(receiverHost));
        System.out.println();

        int currentM = 1;

        /*
        int currentHostPort = PORT_PREFIX + parser.myId();
        int receiverHostPort = PORT_PREFIX + receiverHost;
        */

        HashMap<Integer, String> systemHosts = new HashMap<Integer, String>();
        for (Host host: parser.hosts()) {
            systemHosts.put(host.getId(), host.getIp());
        }

        fifo = new FifoReliableBroadcast(parser.myId(), systemHosts);
        //beb = new BestEffortBroadcast(parser.myId(), systemHosts, urbObserver)
        //Perfectlink = new PerfectLink("localhost", currentHostPort, parser.myId());

        System.out.println("My id: " + parser.myId());

        System.out.println("Broadcasting and delivering messages...\n");

        //Thread.sleep(20000);
        while(currentM<=numMessagesToSend) {// && link.continueSending()) {
            String contents = "m " + String.valueOf(currentM);
            Message m = new Message(fifo.getHostId(), currentM, contents, true);
            fifo.broadcast(m);
            currentM++;
            Thread.sleep(500);
        }
        /*
        if(parser.myId() == receiverHost) {
            if(!link.isClosed()) {
                link.receive();
            }
        } else {
            //senderLog = link.lauchSending(numMessagesToSend, receiverHost);
            while(currentM<=numMessagesToSend) {// && link.continueSending()) {
                String contents = "m " + String.valueOf(currentM);
                Message m = new Message(link.getHostId(), currentM, contents, true);
                link.send(m, "localhost", receiverHostPort);
                currentM++;
                Thread.sleep(500);
            }
            while(link.hasLeftToAck()) {
                Thread.sleep(2000);
                link.resendAllLeftToAck();
            }
            senderLog = link.getSentLog();
            parser.writeToOutput(senderLog);   
        }*/

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
