public class Node {
    public static void main(String args[]) {

        switch (args[0]){
            case "isolated":
                System.out.println("Inside Isolated");
//                IsolatedNodeThread server = new IsolatedNodeThread(args);
//                new Thread(server).start();
                break;
            case "connected":
                System.out.println("Inside Connected");
//                IsolatedNodeThread serverThread = new IsolatedNodeThread(args);
//                ConnectedNodeThread clientThread = new ConnectedNodeThread(args);
//                new Thread(serverThread).start();
//                new Thread(clientThread).start();
                break;
            default:
                System.err.println("Parameters needed for execution:");
                System.err.println("- Start as isolated node: isolated [port-to-expose]");
                System.err.println("- Start as connected node: connected host-ip port-host [port-to-expose]");
                System.exit(0);
                break;
        }
    }
}
