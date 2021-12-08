import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AJUNTAR ELS IMPLEMENTATIONS VELLS EN AQUESTA CLASSE I FER LA RECURSIVITAT
 */
public class NodeImplementation extends UnicastRemoteObject implements NodeInter {
    private Map<String, NodeInter> nodes = new HashMap<>();
    private Map<String, FileContents> filesMap = new HashMap<>();
    private String lastAddedNodeId;
    private File folder;


    protected NodeImplementation(File folder) throws IOException, NoSuchAlgorithmException {

    }


    // SERVER methods
    @Override
    public void registerNode(NodeInter node, String id) throws RemoteException {
    }

    @Override
    public Map<String, FileContents> getListOfFiles() throws IOException, NoSuchAlgorithmException {
        return null;
    }

    @Override
    public byte[] downloadFileFromServer(String fileHash) throws RemoteException {
        return new byte[0];
    }



    // CLIENT methods

    @Override
    public void notifyRegistered(String id) throws RemoteException {

    }

    @Override
    public List<String> selectFiles(Map<String, FileContents> fileContentsMap) throws RemoteException {
        return null;
    }
}
