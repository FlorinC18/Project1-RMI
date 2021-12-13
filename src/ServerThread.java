import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;

public class ServerThread implements Runnable{
    private static int exposedPort;
    private static NodeImplementation node;

    public ServerThread(int exposedPort, NodeImplementation node) {
        ServerThread.exposedPort = exposedPort;
        ServerThread.node = node;
    }


    @Override
    public void run() {
        try {
            System.setProperty("java.rmi.server.hostname", "192.168.1.131");
            Registry registry = startRegistry(exposedPort);
            registry.rebind("Node", node);
            while (true){

            }
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString()); e.printStackTrace();
        }
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
