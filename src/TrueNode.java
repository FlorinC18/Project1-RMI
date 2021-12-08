import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrueNode {
    public static void main(String args[]) {
        String id = UUID.randomUUID().toString();
        if (args.length == 3 && args[0].equals("isolated")) {
            File folder = new File(args[2]);
            if (folder.exists() && folder.isDirectory()) {
                IsolatedNodeThread isolatedServerThread = new IsolatedNodeThread(Integer.parseInt(args[1]), folder, id);
                new Thread(isolatedServerThread).start();
            }
        } else if (args.length == 5 && args[0].equals("connected") && isValidIPAddress(args[1])) {
            File folder = new File(args[4]);
            if (folder.exists() && folder.isDirectory()) {
                IsolatedNodeThread connectedServerThread = new IsolatedNodeThread(Integer.parseInt(args[3]), folder, id);
                ConnectedNodeThread clientThread = new ConnectedNodeThread(args[1], Integer.parseInt(args[2]), folder, id);
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

    private static Registry startRegistry(Integer port) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(port);
            registry.list( );
            // The above call will throw an exception
            // if the registry does not already exist
            return registry;
        }

        catch (RemoteException ex) {
            // No valid registry at that port.
            System.out.println("RMI registry cannot be located " );
            Registry registry= LocateRegistry.createRegistry(port);
            System.out.println("RMI registry created at port " + port);
            return registry;
        }
    }

    private static boolean isValidIPAddress(String ip) {
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
