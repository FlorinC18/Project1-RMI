import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public interface NodeInter extends Remote {
    // SERVER
    void registerNode(NodeInter node, String id) throws RemoteException;
    Map<String, NewFileContents> getListOfFiles(Map<String, NewFileContents> currentList, List<String> alreadyAskedNodes) throws IOException, NoSuchAlgorithmException;
    byte[] downloadFileFromServer(String fileHash) throws RemoteException;
    String getId() throws RemoteException;


    // CLIENT
    void notifyRegistered(String id) throws RemoteException;
    List<String> selectFiles(Map<String, NewFileContents> fileContentsMap) throws RemoteException;
}
