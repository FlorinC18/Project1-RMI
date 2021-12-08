
/*
    args isolated: isolated port-to-expose path-to-folder-with-contents
    args connected: connected host-ip host-port port-to-expose path-to-folder-with-contents
    portatil florin: 192.168.1.131
    portatil florin-work: 192.168.1.26
 */
import java.io.File;
import java.util.regex.*;

public class Node {
    public static void main(String args[]) {
        if (args.length == 3 && args[0].equals("isolated")) {
            File folder = new File(args[2]);
            if (folder.exists() && folder.isDirectory()) {
                IsolatedNodeThread isolatedServerThread = new IsolatedNodeThread(Integer.parseInt(args[1]), folder);
                new Thread(isolatedServerThread).start();
            }
        } else if (args.length == 5 && args[0].equals("connected") && isValidIPAddress(args[1])) {
            File folder = new File(args[4]);
            if (folder.exists() && folder.isDirectory()) {
                IsolatedNodeThread connectedServerThread = new IsolatedNodeThread(Integer.parseInt(args[3]), folder);
                ConnectedNodeThread clientThread = new ConnectedNodeThread(args[1], Integer.parseInt(args[2]), folder);
                new Thread(connectedServerThread).start();
                new Thread(clientThread).start();
            }
        } else {
            System.err.println(args.length);

            System.err.println("Parameters needed for execution:");
            System.err.println("- Start as isolated node: isolated port-to-expose path-to-folder-with-contents");
            System.err.println("- Start as connected node: connected host-ip host-port port-to-expose path-to-folder-with-contents");
            System.exit(0);
        }

    }


    public static boolean isValidIPAddress(String ip) {
        // Regex for digit from 0 to 255.
        String zeroTo255
                = "(\\d{1,2}|(0|1)\\"
                + "d{2}|2[0-4]\\d|25[0-5])";

        // Regex for a digit from 0 to 255 and
        // followed by a dot, repeat 4 times.
        // this is the regex to validate an IP address.
        String regex
                = zeroTo255 + "\\."
                + zeroTo255 + "\\."
                + zeroTo255 + "\\."
                + zeroTo255;

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        // If the IP address is empty
        // return false
        if (ip == null) {
            return false;
        }

        // Pattern class contains matcher() method
        // to find matching between given IP address
        // and regular expression.
        Matcher m = p.matcher(ip);

        // Return if the IP address
        // matched the ReGex
        return m.matches();
    }
}
