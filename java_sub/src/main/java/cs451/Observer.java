// inspired from https://www.baeldung.com/java-observer-pattern
package cs451;

import java.io.IOException;
import java.net.UnknownHostException;

public interface Observer {
    
    public void deliver(Message msg) throws UnknownHostException, IOException;
}
