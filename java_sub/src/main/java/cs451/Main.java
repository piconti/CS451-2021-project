package cs451;

import java.io.IOException;
import java.net.SocketException;
import java.lang.ClassNotFoundException;
import cs451.Parsers.*;
import cs451.Broadcasts.*;
import cs451.Links.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static Parser parser;
    public static PerfectLink link;
    public static LocalizedCausalBroadcast lcb;
    //public static FifoReliableBroadcast fifo;
    //public static BestEffortBroadcast beb;
    //public static UniformReliableBroadcast urb;
    public static int[] myDependencies;
    public static int numMessagesToSend;
    public static int receiverHost;
    public static ArrayList<String> lcbLog = new ArrayList<>();


    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        try {
            System.out.println("Writing output.");
            try {
                lcbLog = lcb.getLog();
            } catch(NullPointerException e) {
                System.out.println("NullPointerException because no log exists. Generating empty log");
                lcbLog.add("");
            }
            parser.writeToOutput(lcbLog);

            if(!lcb.isClosed()) {
                System.out.println("closing link " + lcb.getHostId());
                lcb.close();
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
        myDependencies = parser.getDependencies(parser.myId());
        
        System.out.println("m: " + String.valueOf(numMessagesToSend));
        System.out.println("dependencies of host: " + String.valueOf(parser.myId()));
        for(int dep: myDependencies) {
            System.out.println("- " + String.valueOf(dep));
        }
        System.out.println();

        int currentM = 1;

        HashMap<Integer, String> systemHosts = new HashMap<Integer, String>();
        for (Host host: parser.hosts()) {
            systemHosts.put(host.getId(), host.getIp());
        }

        lcb = new LocalizedCausalBroadcast(parser.myId(), systemHosts, myDependencies);
        
        System.out.println("My id: " + parser.myId());

        System.out.println("Broadcasting and delivering messages...\n");

        while(currentM<=numMessagesToSend) {
            String contents = "m " + String.valueOf(currentM);
            Message m = new Message(lcb.getHostId(), currentM, contents, true);
            lcb.broadcast(m);
            currentM++;
        }
       
        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
