import javax.swing.*;
import java.io.*;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    args isolated: isolated port-to-expose path-to-folder-with-contents
    args connected: connected host-ip host-port port-to-expose path-to-folder-with-contents
    portatil florin: 192.168.1.131
    portatil florin-work: 192.168.1.26

    TrueNode:::
    args: port-to-expose path-to-folder host-ip host-port
 */

public class TrueNode {
    private static int exposedPort;
    private static File folder;
    private static String hostIP;
    private static int hostPort;
    private static String id;
    private static String mode;
    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));


    public static void main(String args[]) throws IOException, NoSuchAlgorithmException {
        id = UUID.randomUUID().toString();
        if (args.length == 4) {
            exposedPort = Integer.parseInt(args[0]);
            folder = new File(args[1]);
            hostIP = args[2];
            hostPort = Integer.parseInt(args[3]);
            mode = "connected";
        } else if (args.length == 2) {
            exposedPort = Integer.parseInt(args[0]);
            folder = new File(args[1]);
            hostIP = null;
            hostPort = 0;
            mode = "isolated";
        } else {
            System.err.println("Parameters needed for execution:");
            System.err.println("port-to-expose path-to-folder host-ip host-port");
            System.exit(0);
        }
        System.setSecurityManager(new RMISecurityManager());
        if (folder.exists() && folder.isDirectory()) {
            // isolated
            if (mode.equals("isolated")) {
                NodeImplementation node = new NodeImplementation(folder, id, exposedPort);
                ServerThread serverThread = new ServerThread(exposedPort, node);
                new Thread(serverThread).start();
            }

            // connected
            if (isValidIPAddress(hostIP) && hostPort != 0){
                try {
                    Registry registry = LocateRegistry.getRegistry(hostIP, hostPort);
                    NodeInter stub = (NodeInter) registry.lookup("Node");
                    System.out.println("Connected to Node " + hostIP + ": " + hostPort);
                    NodeImplementation node = new NodeImplementation(folder, id, stub, exposedPort);
                    if (mode.equals("connected")) {
                        ServerThread serverThread = new ServerThread(exposedPort, node);
                        new Thread(serverThread).start();
                    }
                    stub.registerNode(node, id);
                    System.out.println("Client registered, waiting for notification");
                    System.out.println("Waiting for registry to start...");
                    Thread.sleep(5000);
//                    synchronized (node) {
//                        node.wait();
                    showMenuActions(node, registry, stub);

//                    }
                } catch (RemoteException e) {
                    System.err.println("remote exception: " + e.toString()); e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("client exception: " + e.toString()); e.printStackTrace();
                }
            }
        }
    }

    private static void uploadFiles(NodeInter node) throws IOException, NoSuchAlgorithmException {
        System.out.println("Please insert the path to the desired file or folder to upload");
        String path = br.readLine();
//        input.close();
        node.uploadFile(path);
        System.out.println("Your files are now:");
        showLocalFiles(node);
    }


    private static void searchAndDownload(NodeInter node){

    }


    private static void editYourFiles(NodeInter node) throws IOException {
        System.out.println("Please select the file you want to edit:");
        Map<String, String> namingMap = showLocalFiles(node);
        String hash = getFileHash(namingMap);
        System.out.println("What do you want to edit?\n" +
                "1 - Name\n" +
                "2 - Description\n" +
                "3 - Keywords\n");
        String line = br.readLine();
        int option = Integer.parseInt(line);
        switch (option) {
            case 1 -> {
                System.out.println("Insert the new name:");
                String newName = br.readLine();
                node.changeFileName(hash, newName);
            }
            case 2 -> {
                System.out.println("Insert the new description:");
                String newDesc = br.readLine();
                node.changeFileDescription(hash, newDesc);
            }
            case 3 -> {
                System.out.println("Insert the new keywords separated with commas:");
                line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, ",");
                List<String> keywords = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    keywords.add(st.nextToken().trim());
                }
                node.changeFileKeywords(hash, keywords);
            }
        }
        System.out.println("Your files are now:");
        showLocalFiles(node);
    }

    private static void deleteYourFiles(NodeInter node) throws IOException, NoSuchAlgorithmException{
        System.out.println("Please select the file you want to edit:");
        Map<String, String> namingMap = showLocalFiles(node);

        node.deleteFile(getFileHash(namingMap));
        System.out.println("Your files are now:");
        showLocalFiles(node);

    }

    private static Map<String, String> showLocalFiles(NodeInter node) throws RemoteException {
        Map<String, NewFileContents> ownFiles = node.getOwnFiles();
        Map<String, String> namingMap = new HashMap<>();
        for (String key: ownFiles.keySet()) {
            String name = ownFiles.get(key).getName().get(0);
            List<String> desc = ownFiles.get(key).getDescription();
            List<String> kw = ownFiles.get(key).getKeywords();
            namingMap.put(name, ownFiles.get(key).getHash());
            System.out.println(
                    "\n---------------------------------------\n" +
                    "Name: " + name + "\n" +
                    "Description: " + desc.toString() +"\n" +
                    "Keywords: " + kw.toString() +"\n" +
                    "---------------------------------------\n"
            );
        }
        return namingMap;
    }

    private static String getFileHash(Map<String, String> namingMap) throws IOException {
        //Scanner input2 = new Scanner(System.in);
        //String name = input2.nextLine();


        String name = br.readLine();

        while (!namingMap.containsKey(name)) {
            System.err.println("THIS FILE DOESN'T EXIST, TRY AGAIN");
            //name = input2.nextLine();
            name = br.readLine();
        }
        //input2.close();

        return namingMap.get(name);
    }



    private static boolean isValidIPAddress(String ip) {
        if (ip == null)
            return false;
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
