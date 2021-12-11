import java.io.File;
import java.io.FileOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class ConnectedNodeThread implements Runnable{

    private String hostIP;
    private int hostPort;
    private File folder;
    private String id;

    public ConnectedNodeThread(String hostIP, int hostPort, File folder, String id) {
        this.hostIP = hostIP;
        this.hostPort = hostPort;
        this.folder = folder;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            Registry registry = LocateRegistry.getRegistry(hostIP, hostPort);
            ClientNodeImplementation node = new ClientNodeImplementation();
//            NodeInter stub = (NodeInter) registry.lookup("Node");
            Server stub = (Server) registry.lookup("Node");
            System.out.println("Connected to Node " + hostIP + ": " + hostPort);
//            NodeImplementation node = new NodeImplementation(folder, id, stub);
            stub.registerNode(node, id);
            System.out.println("Client registered, waiting for notification");
            synchronized (node) {
//                node.wait();
                Map<String, NewFileContents> contentsOnTheNetwork = new HashMap<>();
                List<String> nodeIds = new ArrayList<>();
//                contentsOnTheNetwork = stub.getListOfFiles(contentsOnTheNetwork, nodeIds);
                if (contentsOnTheNetwork.isEmpty()) {
                    System.out.println("esta feo q no trobis res imbesil");
                }
                for (NewFileContents file: contentsOnTheNetwork.values()) {
                    System.out.println(file.toString());
                }

//                List<String> fileHashes = node.selectFiles(contentsOnTheNetwork);
//                for (String hash: fileHashes) {
//                    String fileName = contentsOnTheNetwork.get(hash).getName();
//                    byte [] mydata = stub.downloadFileFromServer(hash);
//                    System.out.println("downloading file '" + fileName +"' ...");
//                    File clientpathfile = new File(folder.getAbsolutePath()+ "\\" + fileName);
//                    FileOutputStream out = new FileOutputStream(clientpathfile);
//                    out.write(mydata);
//                    out.flush();
//                    out.close();
//                    System.out.println("Completed!");
//                }

            }


        } catch (RemoteException e) {
            System.err.println("remote exception: " + e.toString()); e.printStackTrace();
        } catch (Exception e) {
            System.err.println("client exception: " + e.toString()); e.printStackTrace();
        }
    }


}
