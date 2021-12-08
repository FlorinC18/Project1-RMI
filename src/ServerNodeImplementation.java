import java.io.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServerNodeImplementation extends UnicastRemoteObject implements Server, Remote {
    private Map<String, Client> nodes = new HashMap<>();
    private Map<String, FileContents> filesMap = new HashMap<>();
    private String lastAddedNodeId;
    private File folder;


    protected ServerNodeImplementation(File folder) throws IOException, NoSuchAlgorithmException {
        this.folder = folder;
        updateFilesMap();
    }

    protected ServerNodeImplementation(int port) throws RemoteException {
        super(port);
    }


    @Override
    public void registerNode(Client node, String id) throws RemoteException {
        synchronized (this) {
            System.out.println("Registering node " + id + " ...");
            nodes.put(id, node);
            lastAddedNodeId = id;
            this.notify();
        }
    }

    public void notifyCorrectRegistration() throws RemoteException {
        try {
            System.out.println("Calling client " + lastAddedNodeId + " ...");
            nodes.get(lastAddedNodeId).notifyRegistered(lastAddedNodeId);


        } catch (RemoteException e) {
            System.err.println("error in notifying correct registration: " + e.toString()); e.printStackTrace();
            System.err.println();
        }
    }

    @Override
    public Map<String, FileContents> getListOfFiles() throws IOException, NoSuchAlgorithmException {
        synchronized (this) {
            updateFilesMap();
            this.notify();
            return filesMap;
        }
    }

    @Override
    public void uploadFileToServer(byte[] mybyte, String serverpath, int length) throws RemoteException {
        try {
            File serverpathfile = new File(serverpath);
            FileOutputStream out=new FileOutputStream(serverpathfile);
            byte [] data=mybyte;

            out.write(data);
            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Done writing data...");
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
                FileContents fileContents = new FileContents(fileEntry);
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



































    // GETTERS


    public String getLastAddedNodeId() {
        return lastAddedNodeId;
    }

    public Map<String, Client> getNodes() {
        return nodes;
    }

    public Map<String, FileContents> getFilesMap() {
        return filesMap;
    }

    public File getFolder() {
        return folder;
    }
}
