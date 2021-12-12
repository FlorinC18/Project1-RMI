import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
                    Thread.sleep(5000);
//                    synchronized (node) {
//                        node.wait();
                    /*
                        main ui
                     */
                    int option = 0;
                    System.out.println("\n" +
                            "---------------------------------------\n" +
                            "|                                     |\n" +
                            "|         WECOLME TO P2P POCHO        |\n" +
                            "|                                     |\n" +
                            "---------------------------------------\n");
                    do {

                        System.out.println("WHAT DO YOU WANT TO DO?");
                        System.out.println("1 - UPLOAD FILES\n" +
                                "2 - SEARCH & DOWNLOAD FILES\n" +
                                "3 - EDIT YOUR FILES\n" +
                                "4 - DELETE YOUR FILES\n" +
                                "5 - EXIT\n");
//                    }
                        Scanner input = new Scanner(System.in);
                        option = input.nextInt();
                        input.close();
                        switch (option){
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 4:
                                break;
                            default:
                                System.err.println("Select a valid option");
                        }
                    } while (option != 5);
                    System.out.println("See you soon!");
                } catch (RemoteException e) {
                    System.err.println("remote exception: " + e.toString()); e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("client exception: " + e.toString()); e.printStackTrace();
                }
            }
        }
    }

    private void uploadFiles(NodeInter node) throws IOException, NoSuchAlgorithmException {
        System.out.println("Please insert the path to the desired file or folder to upload");
        Scanner input = new Scanner(System.in);
        String path = input.nextLine();
        node.uploadFile(path);
    }


    private void searchAndDownload(NodeInter node){

    }


    private void editYourFiles(NodeInter node) throws IOException {
        System.out.println("Please select the file you want to edit:");
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
        Scanner input = new Scanner(System.in);
        String name = input.nextLine();
        while (!namingMap.containsKey(name)) {
            System.err.println("THIS FILE DOESN'T EXIST, TRY AGAIN");
            name = input.nextLine();
        }
        String hash = namingMap.get(name);
        System.out.println("What do you want to edit?\n" +
                "1 - Name\n" +
                "2 - Description\n" +
                "3 - Keywords\n");
        int option = input.nextInt();
        switch(option){
            case 1:
                System.out.println("Insert the new name:");
                String newName = input.nextLine();
                node.changeFileName(hash, newName);
                break;
            case 2:
                System.out.println("Insert the new description:");
                String newDesc = input.nextLine();
                node.changeFileDescription(hash, newDesc);
                break;
            case 3:
                System.out.println("Insert the new keywords separated with commas:");
                String line = input.nextLine();
                StringTokenizer st = new StringTokenizer(line, ",");
                List<String> keywords = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    keywords.add(st.nextToken().trim());
                }
                node.changeFileKeywords(hash, keywords);
                break;
        }
    }

    private void deleteYourFiles(NodeInter node) throws IOException, NoSuchAlgorithmException{
        System.out.println("Please select the file you want to edit:");
        Map<String, String> namingMap = showLocalFiles(node);
        Scanner input = new Scanner(System.in);
        node.deleteFile(getFileHash(input, namingMap));

    }

    private Map<String, String> showLocalFiles(NodeInter node){
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

    private String getFileHash(Scanner input, Map<String, String> namingMap){
        String name = input.nextLine();
        while (!namingMap.containsKey(name)) {
            System.err.println("THIS FILE DOESN'T EXIST, TRY AGAIN");
            name = input.nextLine();
        }
        return namingMap.get(name);
    }

//    Map<String, NewFileContents> contentsOnTheNetwork = new HashMap<>();
//    List<String> nodeIds = new ArrayList<>();
//    contentsOnTheNetwork = stub.getListOfFiles(contentsOnTheNetwork, nodeIds);
//                    if (contentsOnTheNetwork.isEmpty()) {
//        System.out.println("esta feo q no trobis res imbesil");
//    }
//                    for (NewFileContents file: contentsOnTheNetwork.values()) {
//        System.out.println(file.toString());
//    }
//
//    List<String> fileHashes = node.selectFiles(contentsOnTheNetwork);
//                    for (String hash: fileHashes) {
//        String fileName = contentsOnTheNetwork.get(hash).getName().get(0);
//        Map<String, List<Integer>> containingNodes = contentsOnTheNetwork.get(hash).getContainingNodes();
//                        /*
//                            1 - mostrar ip i ports
//                            2 - usuari selecciona ip i port
//                            3 - conectarse a host
//                         */
//
//
//        String newHostIP = (String) containingNodes.keySet().toArray()[0];
//        int newHostPort = containingNodes.get(hostIP).get(0);
//
//
//        if (!hostIP.equals(newHostIP) || hostPort != newHostPort) {
//            hostIP = newHostIP;
//            hostPort = newHostPort;
//            registry = LocateRegistry.getRegistry(hostIP, hostPort);
//            stub = (NodeInter) registry.lookup("Node");
//            stub.registerNode(node, id);
////                            System.out.println("Client registered, waiting for notification");
//        }
//
//        byte [] mydata = stub.downloadFileFromServer(hash);
//        System.out.println("downloading file '" + fileName +"' ...");
//        File clientpathfile = new File(folder.getAbsolutePath()+ "\\" + fileName);
//        FileOutputStream out = new FileOutputStream(clientpathfile);
//        out.write(mydata);
//        out.flush();
//        out.close();
//        System.out.println("Completed!");
//    }

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
