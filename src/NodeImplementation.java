import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * AJUNTAR ELS IMPLEMENTATIONS VELLS EN AQUESTA CLASSE I FER LA RECURSIVITAT
 */
public class NodeImplementation extends UnicastRemoteObject implements NodeInter {
    private String id;
    private File folder;
    private NodeInter parentNode = null;
    private Map<String, NodeInter> childrenNodes = new HashMap<>();
    private Map<String, NewFileContents> filesMap = new HashMap<>();
    private String lastAddedNodeId;
    private String ip;
    private int rmiPort;


    protected NodeImplementation(File folder, String ownId, NodeInter parentNode, int rmiPort) throws IOException, NoSuchAlgorithmException {
        this.folder = folder;
        this.id = ownId;
        this.parentNode = parentNode;
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        }
        this.rmiPort = rmiPort;
        updateFilesMap();
    }

    protected NodeImplementation(File folder, String ownId, int rmiPort) throws IOException, NoSuchAlgorithmException {
        this.folder = folder;
        this.id = ownId;
        this.parentNode = null;
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        }
        this.rmiPort = rmiPort;
        updateFilesMap();
    }


    // SERVER methods

    @Override
    public void registerNode(NodeInter node, String id) throws RemoteException {
        synchronized (this) {
            System.out.println("Registering node " + id + " ...");
            childrenNodes.put(id, node);
            lastAddedNodeId = id;
            this.notify();
        }
    }
    public void notifyCorrectRegistration() throws RemoteException {
        try {
            System.out.println("Calling client " + lastAddedNodeId + " ...");
            childrenNodes.get(lastAddedNodeId).notifyRegistered(lastAddedNodeId);


        } catch (RemoteException e) {
            System.err.println("error in notifying correct registration: " + e.toString()); e.printStackTrace();
            System.err.println();
        }
    }

    @Override
    public Map<String, NewFileContents> getListOfFiles(Map<String, NewFileContents> currentList, List<String> alreadyAskedNodes) throws IOException, NoSuchAlgorithmException {
//        synchronized (this) {
            if (alreadyAskedNodes.contains(this.id)) {
                this.notify();
                return currentList;
            } else {
                updateFilesMap();
                for (String fileContentsHash : this.filesMap.keySet()) {
                    if (currentList.containsKey(fileContentsHash)) {
                        NewFileContents fileContents = currentList.get(fileContentsHash);
                        NewFileContents sameFileContents = this.filesMap.get(fileContentsHash);
                        fileContents.addAllNames(sameFileContents.getName());
                        fileContents.addAllDescriptions(sameFileContents.getDescription());
                        fileContents.addAllKeywords(sameFileContents.getKeywords());
                        fileContents.addAllContainingNode(sameFileContents.getContainingNodes()); // Aixo no vol puto funcionar
                    } else {
                        currentList.put(fileContentsHash, this.filesMap.get(fileContentsHash));
                    }
                }
                alreadyAskedNodes.add(this.id);
                if (this.parentNode != null) {
                    return this.parentNode.getListOfFiles(currentList, alreadyAskedNodes);
                } else {
                    return currentList;
                }
            }
//        }
    }

    @Override
    public byte[] downloadFileFromServer(String fileHash) throws RemoteException {
        byte [] mydata;

        File serverpathfile = filesMap.get(fileHash).getFile();
        mydata=new byte[(int) serverpathfile.length()];
        FileInputStream in;
        try {
            in = new FileInputStream(serverpathfile);
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

        return mydata;
    }

    private void updateFilesMap() throws IOException, NoSuchAlgorithmException {
        ArrayList<String> currentFiles = new ArrayList<>();

        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            //Use SHA-1 algorithm
            MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
            String hash = getFileChecksum(shaDigest, fileEntry);
            currentFiles.add(hash);

            // Update filesMap if any file has been added to the content folder
            if (!filesMap.containsKey(hash)) {
                NewFileContents fileContents = new NewFileContents(fileEntry, ip, rmiPort);
                fileContents.setHash(hash);
                fileContents.setFile(fileEntry);
                fileContents.addDescription("patata");
                fileContents.addKeyword("patata");
                filesMap.put(hash, fileContents);
            }
        }

        // Remove any file that has been removed from the folder
        for (String fileHash : filesMap.keySet()) {
            if (!currentFiles.contains(fileHash)) {
                removeFile(fileHash);
            }
        }

    }

    private void removeFile(String hash) {
        filesMap.remove(hash);
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }

    public File getFolder() {
        return folder;
    }

    public NodeInter getParentNode() {
        return parentNode;
    }

    public Map<String, NodeInter> getChildrenNodes() {
        return childrenNodes;
    }

    public Map<String, NewFileContents> getFilesMap() {
        return filesMap;
    }

    public String getLastAddedNodeId() {
        return lastAddedNodeId;
    }

// CLIENT methods

    @Override
    public void notifyRegistered(String id) throws RemoteException {
        System.out.println("Notified that node " + id + " was registered");
        synchronized (this) {
            this.notify();
        }
    }

    @Override
    public List<String> selectFiles(Map<String, NewFileContents> fileContentsMap) throws RemoteException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter attribute to search by:");
        System.out.println("1 - Hash code of the desired file");
        System.out.println("2 - Name");
        System.out.println("3 - Description");
        System.out.println("4 - Keywords");

        int attribute = scanner.nextInt();
        while (attribute < 0 || attribute > 4) {
            System.out.println("Please select a valid number\n");
            attribute = scanner.nextInt();
        }

        String attributeName = switch (attribute) {
            case 1 -> "hash";
            case 2 -> "name";
            case 3 -> "description";
            case 4 -> "keywords";
            default -> "";
        };

        System.out.println("Enter string to search in '" + attributeName + "' \n");
        String line = scanner.next();
        scanner.close();
        return selectByAttribute(fileContentsMap, attributeName, line);
    }

    private ArrayList<String> selectByAttribute(Map<String, NewFileContents> fileContentsMap, String attribute, String line) {

        ArrayList<String> files = new ArrayList<>();
        ArrayList<NewFileContents> fileContentsList = new ArrayList<>(fileContentsMap.values());

        StringTokenizer st = new StringTokenizer(line);

        while (st.hasMoreTokens()) {
            String attributeValue = st.nextToken();
            for (NewFileContents file : fileContentsList) {
                String hash = file.searchByAttribute(attribute, attributeValue);
                if (!hash.equals(""))
                    files.add(hash);
            }
        }
        return files;
    }
}
