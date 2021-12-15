import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class NodeImplementation extends UnicastRemoteObject implements NodeInter {
    private String id;
    private File folder;
    private Map<String, NodeInter> knownNodes = new HashMap<>();
    private Map<String, FileManager> filesMap = new HashMap<>();
    private String lastAddedNodeId;
    private String ip;
    private int rmiPort;

    protected NodeImplementation(File folder, String ownId, int rmiPort) throws IOException, NoSuchAlgorithmException {
        this.folder = folder;
        this.id = ownId;
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        }
        this.rmiPort = rmiPort;
        updateFilesMap();
    }

    // Server methods
    @Override
    public void registerNode(NodeInter node, String id) throws RemoteException {
        System.out.println("Registering node " + id + " ...");
        knownNodes.put(id, node);
        lastAddedNodeId = id;
    }

    @Override
    public Map<String, FileManager> getListOfFiles(Map<String, FileManager> currentList, List<String> alreadyAskedNodes) throws IOException, NoSuchAlgorithmException {
            if (alreadyAskedNodes.contains(this.id)) {
                return currentList;
            } else {
                updateFilesMap();
                for (String fileContentsHash : this.filesMap.keySet()) {
                    if (currentList.containsKey(fileContentsHash)) {
                        FileManager fileContents = currentList.get(fileContentsHash);
                        FileManager sameFileContents = this.filesMap.get(fileContentsHash);
                        fileContents.addAllNames(sameFileContents.getName());
                        fileContents.addAllDescriptions(sameFileContents.getDescription());
                        fileContents.addAllKeywords(sameFileContents.getKeywords());
                        fileContents.addAllContainingNode(sameFileContents.getContainingNodes());
                    } else {
                        currentList.put(fileContentsHash, this.filesMap.get(fileContentsHash));
                    }
                }
                alreadyAskedNodes.add(this.id);

                for (NodeInter node:this.knownNodes.values()) {
                    if (!alreadyAskedNodes.contains(node.getId())) {
                        currentList.putAll(node.getListOfFiles(currentList, alreadyAskedNodes));
                    }
                }
                return currentList;
            }
    }

    @Override
    public byte[] downloadFileFromServer(String fileHash, Integer requestedChunk, Integer chunkSize) throws RemoteException {
        byte [] mydata;

        File serverpathfile = filesMap.get(fileHash).getFile();
        long skipSize = requestedChunk * 1024 * 1024;
        mydata=new byte[chunkSize];
        FileInputStream in;
        try {
            in = new FileInputStream(serverpathfile);
            try {
                in.skip(skipSize);
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
                FileManager fileContents = new FileManager(fileEntry, ip, rmiPort);
                fileContents.setHash(hash);
                fileContents.setFile(fileEntry);
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

    public void addKnownNode(NodeInter parentNode) throws RemoteException {
        this.knownNodes.put(parentNode.getId(), parentNode);
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
    public List<String> selectFiles(Map<String, FileManager> fileContentsMap, BufferedReader br) throws IOException {
        System.out.println("Enter attribute to search by:");
        System.out.println("1 - Hash code of the desired file");
        System.out.println("2 - Name");
        System.out.println("3 - Description");
        System.out.println("4 - Keywords\n");



        int attribute = Integer.parseInt(br.readLine());
        while (attribute < 0 || attribute > 4) {
            System.out.println("\nPlease select a valid number\n");
            attribute = Integer.parseInt(br.readLine());
        }

        String attributeName = switch (attribute) {
            case 1 -> "hash";
            case 2 -> "name";
            case 3 -> "description";
            case 4 -> "keywords";
            default -> "";
        };

        System.out.println("\nEnter value to search in '" + attributeName + "' \n");
        String line = br.readLine();

        return selectByAttribute(fileContentsMap, attributeName, line);
    }

    private ArrayList<String> selectByAttribute(Map<String, FileManager> fileContentsMap, String attribute, String line) {

        ArrayList<String> files = new ArrayList<>();
        ArrayList<FileManager> fileContentsList = new ArrayList<>(fileContentsMap.values());

        StringTokenizer st = new StringTokenizer(line);

        while (st.hasMoreTokens()) {
            String attributeValue = st.nextToken();
            for (FileManager file : fileContentsList) {
                String hash = file.searchByAttribute(attribute, attributeValue);
                if (!hash.equals(""))
                    files.add(hash);
            }
        }
        return files;
    }

    @Override
    public void uploadFile(String path) throws IOException, NoSuchAlgorithmException {
        File file = new File(path);
        Path destiny = folder.toPath().resolve(file.getName());
        if (file.exists() &&  file.isFile()) {
            Path source = file.toPath();
            System.out.println("Sourece: "+source);
            System.out.println("Destiny: "+destiny);
            Files.copy(source, destiny, StandardCopyOption.REPLACE_EXISTING);
        } else if (file.exists() && file.isDirectory()) {
            for (File doc: Objects.requireNonNull(file.listFiles())) {
                Path source = doc.toPath();
                Files.copy(source, destiny, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        updateFilesMap();
    }

    @Override
    public void changeFileName(String hash, String name) throws IOException {
        if(filesMap.containsKey(hash)) {
            FileManager fileContents = filesMap.get(hash);
            fileContents.setName(List.of(name));
            Path source = Paths.get(fileContents.getFile().getAbsolutePath());
            Files.move(source, source.resolveSibling(name));
        }
    }

    @Override
    public void changeFileDescription(String hash, String description)  throws RemoteException{
        if(filesMap.containsKey(hash)) {
            FileManager fileContents = filesMap.get(hash);
            fileContents.setDescription(List.of(description));
        }
    }

    @Override
    public void changeFileKeywords(String hash, List<String> keywords)  throws RemoteException{
        if (filesMap.containsKey(hash)) {
            FileManager fileContents = filesMap.get(hash);
            fileContents.setKeywords(keywords);
        }
    }

    @Override
    public void deleteFile(String hash) throws IOException, NoSuchAlgorithmException {
        if (filesMap.containsKey(hash)) {
            FileManager fileContents = filesMap.get(hash);
            if(fileContents.getFile().delete()) {
                updateFilesMap();
                System.out.println("File deleted successfully!");
            } else {
                System.err.println("Error deleting file :(");
            }
        }
    }

    @Override
    public Map<String, FileManager> getOwnFiles() throws IOException, NoSuchAlgorithmException {
        updateFilesMap();
        return this.filesMap;
    }

}
