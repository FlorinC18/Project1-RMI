import java.io.*;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    args: port-to-expose path-to-folder host-ip host-port
 */

public class Node {
    private static int exposedPort;
    private static File folder;
    private static String hostIP;
    private static int hostPort;
    private static String id;
    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private static NodeImplementation node;

    private static class Tuple<X, Y> {
        private final X x;
        private final Y y;

        private Tuple(X x, Y y) {
            this.x = x;
            this.y = y;
        }
    }


    public static void main(String[] args) {
        id = UUID.randomUUID().toString();
        if (args.length == 4) {
            exposedPort = Integer.parseInt(args[0]);
            folder = new File(args[1]);
            hostIP = args[2];
            hostPort = Integer.parseInt(args[3]);
        } else if (args.length == 2) {
            exposedPort = Integer.parseInt(args[0]);
            folder = new File(args[1]);
            hostIP = null;
            hostPort = 0;
        } else {
            System.err.println("Parameters needed for execution:");
            System.err.println("port-to-expose path-to-folder host-ip host-port");
            System.exit(0);
        }

        if (folder.exists() && folder.isDirectory()) {
                try {
                    node = new NodeImplementation(folder, id, exposedPort);
                    Registry ownRegistry = startRegistry(exposedPort);
                    ownRegistry.rebind("Node", node);
                    Registry otherRegistry = null;
                    NodeInter stub = null;
                    if (isValidIPAddress(hostIP) && hostPort != 0) {
                        otherRegistry = LocateRegistry.getRegistry(hostIP, hostPort);
                        stub = (NodeInter) otherRegistry.lookup("Node");
                        node.addKnownNode(stub);
                        stub.registerNode(node, id);
                        System.out.println("Connected to Node " + hostIP + ": " + hostPort);
                    }

                    System.out.println("Client registered, waiting for notification");
                    showMenuActions(node, otherRegistry, stub);

                } catch (RemoteException e) {
                    System.err.println("remote exception: " + e.toString()); e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("client exception: " + e.toString()); e.printStackTrace();
                }

        }
    }

    private static void showMenuActions(NodeInter node, Registry registry, NodeInter stub){
        try {
            int option = 0;
            System.out.println("\n" +
                    "---------------------------------------\n" +
                    "|                                     |\n" +
                    "|        WECOLME TO P2PNetwork        |\n" +
                    "|                                     |\n" +
                    "---------------------------------------\n");
            while (true) {
                System.out.println("WHAT DO YOU WANT TO DO?");
                System.out.println("1 - UPLOAD FILES\n" +
                        "2 - SEARCH & DOWNLOAD FILES\n" +
                        "3 - EDIT YOUR FILES\n" +
                        "4 - DELETE YOUR FILES\n" +
                        "5 - EXIT\n");

                option = Integer.parseInt(br.readLine());
                switch (option) {
                    case 1 -> uploadFiles(node);
                    case 2 -> searchAndDownload(node);
                    case 3 -> editYourFiles(node);
                    case 4 -> deleteYourFiles(node);
                    case 5 -> {System.out.println("See you soon!"); System.exit(0);}
                    default -> System.err.println("Select a valid option");
                }
            }
        } catch (RemoteException e) {
            System.err.println("remote exception: " + e.toString()); e.printStackTrace();
        } catch (Exception e) {
            System.err.println("client exception: " + e.toString()); e.printStackTrace();
        }
    }


    private static void uploadFiles(NodeInter node) throws IOException, NoSuchAlgorithmException {
        System.out.println("Please insert the path to the desired file or folder to upload");
        String path = br.readLine();
        node.uploadFile(path);
        System.out.println("\nYour files have been updated:");
        showLocalFiles(node);
    }


    private static void searchAndDownload(NodeInter node) throws IOException, NoSuchAlgorithmException{
        System.out.println("Which file do you want to download?");
        Map<String, FileManager> contentsOnTheNetwork = new HashMap<>();
        List<String> nodeIds = new ArrayList<>();
        contentsOnTheNetwork = node.getListOfFiles(contentsOnTheNetwork, nodeIds);
        if (contentsOnTheNetwork.isEmpty()) {
            System.out.println("No Files found!");
        } else {
            for (FileManager file: contentsOnTheNetwork.values()) {
                System.out.println(file.toString());
            }

            List<String> fileHashes = node.selectFiles(contentsOnTheNetwork, br);
            Map<String, FileManager> ownFiles = node.getOwnFiles();

            downloadFiles(fileHashes, ownFiles, contentsOnTheNetwork);

        }
        System.out.println("\nYour files have been updated:");
        showLocalFiles(node);
    }


    private static void downloadFiles(List<String> fileHashes, Map<String, FileManager> ownFiles, Map<String, FileManager> contentsOnTheNetwork) throws IOException {
        for (String hash: fileHashes) {
            if (!ownFiles.containsKey(hash)) {
                String fileName = contentsOnTheNetwork.get(hash).getName().get(0);
                Map<String, List<Integer>> containingNodes = contentsOnTheNetwork.get(hash).getContainingNodes();

                Map<String, Integer> chunksConfig = getChunksConfig(contentsOnTheNetwork, hash);

                int totalFileChunks = chunksConfig.get("totalFileChunks");
                int chunkSize = chunksConfig.get("chunkSize");
                int lastChunkSize = chunksConfig.get("lastChunkSize");
                    /* current:
                    Map <Tuple<hostIP, Port>, List<int> chunks>
                     */
                Map<Tuple<String, Integer>, List<Integer>> chunksOfEachSource = assignChunksToEachSource(containingNodes, totalFileChunks);

                System.out.println("\nDownloading file '" + fileName + "' ...");
                ConcurrentHashMap<Integer, Path> chunksMap = downloadOneFile(chunksOfEachSource, chunkSize, totalFileChunks, lastChunkSize, hash, fileName);

                rebuildFileFromChunks(chunksMap, fileName);
                System.out.println("Download Completed!\n");
            } else {
                System.out.println("You already have this file: " + ownFiles.get(hash).getName());
            }
        }
    }

    private static Map<String, Integer> getChunksConfig(Map<String, FileManager> contentsOnTheNetwork, String hash) {
        int chunkSize = 1024 * 1024;
        int lastChunkSize = 0;
        double fileLength = contentsOnTheNetwork.get(hash).getFileLength();
        int totalBytesRead = 0;
        int totalFileChunks = 0;
        while (totalBytesRead < fileLength) {
            totalFileChunks++;
            int bytesRemaining = (int) fileLength - totalBytesRead;
            if (bytesRemaining < chunkSize) {
                lastChunkSize = bytesRemaining;
                totalBytesRead += lastChunkSize;
            } else {
                totalBytesRead += chunkSize;
            }
        }
        Map<String, Integer> config = new HashMap<>();
        config.put("totalFileChunks", totalFileChunks);
        config.put("chunkSize", chunkSize);
        config.put("lastChunkSize", lastChunkSize);
        return config;
    }


    private static Map<Tuple<String, Integer>, List<Integer>> assignChunksToEachSource(Map<String, List<Integer>> containingNodes, int totalFileChunks) {
        Map<Tuple<String, Integer>, List<Integer>> chunksOfEachSource = new HashMap<>();
        String[] hostIps = containingNodes.keySet().toArray(new String[0]);

        for (int chunk = 0; chunk < totalFileChunks; chunk++) {
            // hostsIPs = [A, B]
            // chunk = 0...5
            // 1: 0%2 = 0 -> A
            // 2: 1%2 = 1 -> B
            // 3: 2%2 = 0 -> A
            // 4: 3%2 = 1 -> B
            // 5: 4%2 = 0 -> A
            // round-robin

            String newHostIP = hostIps[chunk % hostIps.length];
            List<Integer> ports = containingNodes.get(newHostIP);
            int newHostPort = ports.get(chunk % ports.size());
            Tuple<String, Integer> source = new Tuple<>(newHostIP, newHostPort);
            if (!chunksOfEachSource.containsKey(source)) {
                chunksOfEachSource.put(source, List.of(chunk));
            } else {
                List<Integer> chunkList = chunksOfEachSource.get(source);
                chunkList.add(chunk);
                chunksOfEachSource.put(source, chunkList);
            }
        }
        return chunksOfEachSource;
    }


    private static ConcurrentHashMap<Integer, Path> downloadOneFile(Map<Tuple<String, Integer>, List<Integer>> chunksOfEachSource, int chunkSize, int totalFileChunks, int lastChunkSize, String hash, String fileName) {
        ConcurrentHashMap<Integer, Path> chunksMap = new ConcurrentHashMap<>();
        for (Tuple<String, Integer> source : chunksOfEachSource.keySet()) {
            hostIP = source.x;
            hostPort = source.y;
            List<Integer> chunks = chunksOfEachSource.get(source);
            try {
                // connect to node
                Registry registry = LocateRegistry.getRegistry(hostIP, hostPort);
                NodeInter stub = (NodeInter) registry.lookup("Node");
                stub.registerNode(node, id);

                // download each chunk to temporal a file via parallelStream()
                NodeInter finalStub = stub;
                int finalChunkSize = chunkSize;
                int finalTotalFileChunks = totalFileChunks;
                int finalLastChunkSize = lastChunkSize;
                chunks.parallelStream().forEach(chunk -> {
                    Path path;
                    if (chunk == finalTotalFileChunks - 1) {
                        path = downloadChunk(chunk, finalStub, hash, finalLastChunkSize, fileName);
                    } else {
                        path = downloadChunk(chunk, finalStub, hash, finalChunkSize, fileName);
                    }
                    chunksMap.put(chunk, path);
                });

            } catch (Exception e) {
                System.err.println("client exception: " + e.toString());
                e.printStackTrace();
            }
        }
        return chunksMap;
    }


    private static Path downloadChunk(Integer chunk, NodeInter stub, String hash, Integer chunkSize, String fileName) {
        try {
            byte[] mydata = stub.downloadFileFromServer(hash, chunk, chunkSize);
            File clientpathfile = new File(folder.getAbsolutePath() + "\\" + fileName + "-" + chunk); // UBUNTU: canviar "\\" per "/"
            FileOutputStream out = new FileOutputStream(clientpathfile);
            out.write(mydata);
            out.flush();
            out.close();
            return clientpathfile.toPath();
        } catch (Exception e) {
            System.err.println("client exception: " + e.toString()); e.printStackTrace();
        }
        return null;
    }

    private static void rebuildFileFromChunks(ConcurrentHashMap<Integer, Path> chunksMap, String fileName) throws IOException {
        List<Integer> sortedKeys = new ArrayList<>(chunksMap.size());
        sortedKeys.addAll(chunksMap.keySet());
        Collections.sort(sortedKeys);
        for(int chunk = 0; chunk < chunksMap.size(); chunk ++) {
            byte[] mydata = getChunkFile(chunksMap.get(chunk));
            File finalFile = new File(folder.getAbsolutePath() + "\\" + fileName); // UBUNTU: canviar "\\" per "/"
            FileOutputStream out = new FileOutputStream(finalFile, true);
            out.write(mydata);
            out.flush();
            out.close();
        }
    }

    private static byte[] getChunkFile(Path pathToChunkFile) {
        byte [] mydata;

        File chunkPathFile = pathToChunkFile.toFile();
        mydata=new byte[(int) chunkPathFile.length()];
        FileInputStream in;
        try {
            in = new FileInputStream(chunkPathFile);
            try {
                in.read(mydata, 0, mydata.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        chunkPathFile.delete();
        return mydata;
    }


    private static void editYourFiles(NodeInter node) throws IOException, NoSuchAlgorithmException {
        System.out.println("\nThese are your current files:");
        Map<String, String> namingMap = showLocalFiles(node);
        System.out.println("Please select the file you want to edit by entering its full name (name.extension):");
        String hash = getFileHash(namingMap);
        System.out.println("\nWhich attribute do you want to edit?\n" +
                "1 - Name\n" +
                "2 - Description\n" +
                "3 - Keywords\n");
        String line = br.readLine();
        int option = Integer.parseInt(line);
        switch (option) {
            case 1 -> {
                System.out.println("\nInsert the new name:");
                String newName = br.readLine();
                node.changeFileName(hash, newName);
            }
            case 2 -> {
                System.out.println("\nInsert the new description:");
                String newDesc = br.readLine();
                node.changeFileDescription(hash, newDesc);
            }
            case 3 -> {
                System.out.println("\nInsert the new keywords separated with commas:");
                line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, ",");
                List<String> keywords = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    keywords.add(st.nextToken().trim());
                }
                node.changeFileKeywords(hash, keywords);
            }
        }
        System.out.println("\nYour files have been updated:");
        showLocalFiles(node);
    }

    private static void deleteYourFiles(NodeInter node) throws IOException, NoSuchAlgorithmException {
        System.out.println("\nThese are your current files:");
        Map<String, String> namingMap = showLocalFiles(node);
        System.out.println("\nPlease select the file you want to delete:");

        node.deleteFile(getFileHash(namingMap));
        System.out.println("\nYour files have been updated:");
        showLocalFiles(node);
    }

    private static Map<String, String> showLocalFiles(NodeInter node) throws IOException, NoSuchAlgorithmException {
        Map<String, FileManager> ownFiles = node.getOwnFiles();
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
        String name = br.readLine();

        while (!namingMap.containsKey(name)) {
            System.err.println("\nTHIS FILE DOESN'T EXIST, TRY AGAIN");
            name = br.readLine();
        }

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
}
