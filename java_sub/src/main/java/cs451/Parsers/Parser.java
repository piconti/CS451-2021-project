package cs451.Parsers;

import java.util.List;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import cs451.*;

public class Parser {

    private String[] args;
    private long pid;
    private IdParser idParser;
    private HostsParser hostsParser;
    private OutputParser outputParser;
    private ConfigParser configParser;

    public Parser(String[] args) {
        this.args = args;
    }

    public void parse() {
        pid = ProcessHandle.current().pid();

        idParser = new IdParser();
        hostsParser = new HostsParser();
        outputParser = new OutputParser();
        configParser = new ConfigParser();

        int argsNum = args.length;
        if (argsNum != Constants.ARG_LIMIT_CONFIG) {
            help();
        }

        if (!idParser.populate(args[Constants.ID_KEY], args[Constants.ID_VALUE])) {
            help();
        }

        if (!hostsParser.populate(args[Constants.HOSTS_KEY], args[Constants.HOSTS_VALUE])) {
            help();
        }

        if (!hostsParser.inRange(idParser.getId())) {
            help();
        }

        if (!outputParser.populate(args[Constants.OUTPUT_KEY], args[Constants.OUTPUT_VALUE])) {
            help();
        }

        if (!configParser.populate(args[Constants.CONFIG_VALUE])) {
            help();
        }
    }

    private void help() {
        System.err.println("Usage: ./run.sh --id ID --hosts HOSTS --output OUTPUT CONFIG");
        System.exit(1);
    }

    public int myId() {
        return idParser.getId();
    }

    public List<Host> hosts() {
        return hostsParser.getHosts();
    }

    public String output() {
        return outputParser.getPath();
    }

    public String config() {
        return configParser.getPath();
    }

    public int[] readConfig() throws FileNotFoundException, IOException {
        return configParser.readConfig();
    }

    public int getNumMessages() throws FileNotFoundException, IOException {
        return configParser.readConfigCausal().get(0)[0];
    }

    public int getReceiverId() throws FileNotFoundException, IOException {
        return configParser.readConfig()[1];
    }

    /*public int[] getDependencies(int hostId) throws FileNotFoundException, IOException {
        int[] myDep = configParser.readConfigCausal().get(hostId);
        int[] myDependencies = new int[myDep.length-1];
        if(hostId == myDep[0]) {
            for(int i=1; i<myDependencies.length; i++) {
                myDependencies[i-1] = myDep[i];
            }
        } else {
            System.out.println("problem when parsing the dependencies");
            throw new IllegalArgumentException();
        }
        return myDependencies;
    }*/

    public int[] getDependencies(int hostId) throws FileNotFoundException, IOException {
        return configParser.readConfigCausal().get(hostId);
    }

    public void writeToOutput(ArrayList<String> log) throws IOException, FileNotFoundException {
        outputParser.writeToOutput(log);
    }

    public boolean wrotetoOutput() {
        return outputParser.wrotetoOutput();
    }

}
