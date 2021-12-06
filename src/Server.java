import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public interface Server extends Remote {
    void registerNode(Client node, String id) throws RemoteException;
    Map<String, FileContents> getListOfFiles() throws IOException, NoSuchAlgorithmException;
    void uploadFileToServer(byte[] mybyte, String serverpath, int length) throws RemoteException;
    byte[] downloadFileFromServer(String fileHash) throws RemoteException;

}
