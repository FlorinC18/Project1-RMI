import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ClientNodeImplementation extends UnicastRemoteObject implements Client {
    protected ClientNodeImplementation() throws RemoteException {
    }

    protected ClientNodeImplementation(int port) throws RemoteException {
        super(port);
    }

    @Override
    public void notifyRegistered(String id) throws RemoteException {
        System.out.println("Notified that node " + id + " was registered");
        synchronized (this) {
            this.notify();
        }
    }

    @Override
    public List<String> selectFiles(Map<String, FileContents> fileContentsMap) throws RemoteException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter attribute to search by:");
        System.out.println("1 - Hash code of the desired file");
        System.out.println("2 - Name");
        System.out.println("3 - Description");
        System.out.println("4 - Keywords");

        int attribute = scanner.nextInt();
        while (attribute < 0 || attribute > 4) {
            System.out.println("Please select a valid number\n");
            attribute = scanner.nextInt();
        }

        String attributeName = switch (attribute) {
            case 1 -> "hash";
            case 2 -> "name";
            case 3 -> "description";
            case 4 -> "keywords";
            default -> "";
        };

        System.out.println("Enter string to search in '" + attributeName + "' \n");
        String line = scanner.next();
        scanner.close();
        return selectByAttribute(fileContentsMap, attributeName, line);
    }

    private ArrayList<String> selectByAttribute(Map<String, FileContents> fileContentsMap, String attribute, String line) {

        ArrayList<String> files = new ArrayList<>();
        ArrayList<FileContents> fileContentsList = new ArrayList<>(fileContentsMap.values());

        StringTokenizer st = new StringTokenizer(line);

        while (st.hasMoreTokens()) {
            String attributeValue = st.nextToken();
            for (FileContents file : fileContentsList) {
                    String hash = file.searchByAttribute(attribute, attributeValue);
                    if (!hash.equals(""))
                        files.add(hash);
            }
        }
        return files;
    }

    @Override
    public void saveFile(FileContents file) throws RemoteException {
        try {
            final Path path = Paths.get("./files/" + file.getName());
            final BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        } catch (Exception e) {
            System.err.println("Error writing file: " + file.getName());
            e.printStackTrace();
        }
    }
}
