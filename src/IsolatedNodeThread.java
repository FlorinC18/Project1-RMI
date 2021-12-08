import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class IsolatedNodeThread implements Runnable{

    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_RESET = "\u001B[0m";

    private int exposedPort;
    private File folder;

    public IsolatedNodeThread(int exposedPort, File folder) {
        this.exposedPort = exposedPort;
        this.folder = folder;
    }

    @Override
    public void run() {
        try {
            System.setProperty("java.rmi.server.hostname", "192.168.1.131");
            Registry registry = startRegistry(exposedPort);
            ServerNodeImplementation obj = new ServerNodeImplementation(folder);
            registry.rebind("Node", obj);

            System.out.println(ANSI_PURPLE + "Server ready. Start registering clients ..." + ANSI_RESET);
            while (true) {
                synchronized (obj) {
                    obj.wait();
                    obj.notifyCorrectRegistration();
                    System.out.println(ANSI_PURPLE + "Node " + obj.getLastAddedNodeId() + " has been notified!" + ANSI_RESET);
                    obj.wait();
                    System.out.println(ANSI_PURPLE + "Displaying files to client..." + ANSI_RESET);
                    obj.wait();
                    System.out.println(ANSI_PURPLE + "Client downloaded file :)" + ANSI_RESET);
                }
            }
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString()); e.printStackTrace();
        }
    }

    private Registry startRegistry(Integer port) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(port);
            registry.list( );
            // The above call will throw an exception
            // if the registry does not already exist
            return registry;
        }
        catch (RemoteException ex) {
            // No valid registry at that port.
            System.out.println(ANSI_PURPLE + "RMI registry cannot be located " + ANSI_RESET);
            Registry registry= LocateRegistry.createRegistry(port);
            System.out.println(ANSI_PURPLE + "RMI registry created at port " + port + ANSI_RESET);
            return registry;
        }
    }

}

