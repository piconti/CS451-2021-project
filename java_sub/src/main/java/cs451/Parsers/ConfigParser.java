// File reading inspired from https://www.baeldung.com/reading-file-in-java 

package cs451.Parsers;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import cs451.*;

public class ConfigParser {

    private String path;
    private final int LEN_VALUES=2;

    public boolean populate(String value) {
        File file = new File(value);
        path = file.getPath();
        return true;
    }

    public String getPath() {
        return path;
    }

    public int[] readConfig() throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(this.path));
        String currentLine = reader.readLine();
        reader.close();

        String[] line = currentLine.split("\\s+");
        //assertEquals(LEN_VALUES, line.length);
        int[] values = new int[LEN_VALUES];
        for(int i=0; i<LEN_VALUES; i++) {
            values[i] = Integer.valueOf(line[i]);
        }
        return values;
    }

}
