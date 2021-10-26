// File writing inspired by https://www.baeldung.com/java-write-to-file

package cs451.Parsers;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import cs451.*;

public class OutputParser {

    private static final String OUTPUT_KEY = "--output";

    private String path;
    private boolean wroteToOutput = false;

    public boolean populate(String key, String value) {
        if (!key.equals(OUTPUT_KEY)) {
            return false;
        }

        File file = new File(value);
        path = file.getPath();
        return true;
    }

    public String getPath() {
        return path;
    }

    public void writeToOutput(ArrayList<String> log) throws IOException, FileNotFoundException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        for(String s: log) {
            writer.write(s);
            writer.newLine();
        }
        this.wroteToOutput = true;
        writer.close();
    }

    public boolean wrotetoOutput() {
        return this.wroteToOutput;
    }

}
