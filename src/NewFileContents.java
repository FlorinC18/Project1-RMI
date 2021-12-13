import java.io.File;
import java.io.Serializable;
import java.rmi.Remote;
import java.util.*;

public class NewFileContents implements Serializable, Remote {
    private File file;
    private String hash = "";
    private List<String> name = new ArrayList<>();
    private List<String> description = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private Map<String, List<Integer>> containingNodes = new HashMap<>();


    public NewFileContents(File file, String ip, int rmiPort) {
        this.file = file;
        name.add(file.getName());
        containingNodes.put(ip, List.of(rmiPort));
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public void addName(String name) {
        this.name.add(name);
    }

    public void addAllNames(Collection<String> name) {
        this.name.addAll(name);
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public void addDescription(String desc) {
        this.description.add(desc);
    }

    public void addAllDescriptions(Collection<String> desc) {
        this.description.addAll(desc);
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void addKeyword(String keyword) {
        keywords.add(keyword);
    }

    public void addAllKeywords(Collection<String> keyword) {
        keywords.addAll(keyword);
    }

    public Map<String, List<Integer>> getContainingNodes() {
        return containingNodes;
    }

    public void setContainingNodes(Map<String, List<Integer>> containingNodes) {
        this.containingNodes = containingNodes;
    }

    public void addContainingNode(String ip, int port) {
        this.containingNodes.put(ip, List.of(port));
    }


    public void addAllContainingNode(Map<String, List<Integer>> containingNodes) {
        for (String ip: containingNodes.keySet()) {
            if (this.containingNodes.containsKey(ip)) {
                List<Integer> result = new ArrayList<>();
                List<Integer> portsList1 = this.containingNodes.get(ip);
                List<Integer> portsList2 = containingNodes.get(ip);
                result.addAll(portsList1);
                result.addAll(portsList2);
                this.containingNodes.put(ip, result);

            } else {
                this.containingNodes.put(ip , (List<Integer>) containingNodes.get(ip));
            }
        }
    }

    public String searchByAttribute(String attribute, String attributeValue) {
        switch (attribute) {
            case "hash": // hash
                if (getHash().contains(attributeValue))
                    return getHash();
                break;
            case "name": // name
                for (String name : getName()) {
                    if (name.contains(attributeValue))
                        return getHash();
                }
                break;
            case "description": // description
                for (String desc : getDescription()) {
                    if (desc.contains(attributeValue))
                        return getHash();
                }
                break;
            case "keywords": // keywords
                for (String kw : getKeywords()) {
                    if (kw.contains(attributeValue))
                        return getHash();
                }
                break;
        }
        return "";
    }

    @Override
    public String toString() {
        return "\n------------------------------------------------------------------------------\n" +
                "Hash: " + hash + "\n" +
                "Name: " + name + "\n" +
                "Description: " + description.toString() +"\n" +
                "Keywords: " + keywords.toString() +"\n" +
                "Owner Node IPs and ports: " + containingNodes.toString() +"\n" +
                "------------------------------------------------------------------------------\n";
    }
}
