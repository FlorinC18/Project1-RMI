import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface Client extends Remote {
    void notifyRegistered(String id) throws RemoteException;
    List<String> selectFiles(Map<String, FileContents> fileContentsMap) throws RemoteException;
    void saveFile(FileContents file) throws RemoteException;
}
