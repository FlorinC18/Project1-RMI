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
    private Map<String, Collection<Integer>> containingNodes = new HashMap<>();


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

    public Map<String, Collection<Integer>> getContainingNodes() {
        return containingNodes;
    }

    public void setContainingNodes(Map<String, Collection<Integer>> containingNodes) {
        this.containingNodes = containingNodes;
    }

    public void addContainingNode(String ip, int port) {
        this.containingNodes.put(ip, List.of(port));
    }

    /*
        AIxo no va ni pa atras
        Nose com tenint 2 mapes amb <key = string ip, value = collection/list/array de int>
        combinar els 2 value sota una mateixa key
     */
    public void addAllContainingNode(Map<String, Collection<Integer>> containingNodes) {
        for (String ip: containingNodes.keySet()) {
            if (this.containingNodes.containsKey(ip)) {
                Collection<Integer> ports = containingNodes.get(ip);
                this.containingNodes.get(ip).addAll(ports);
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
                if (getName().contains(attributeValue))
                    return getHash();
                break;
            case "description": // description
                if (getDescription().contains(attributeValue))
                    return getHash();
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
        return " hash='" + hash + '\'' +
                ", name='" + name.toString() + '\'' +
                ", description='" + description.toString() + '\'' +
                ", keywords=" + keywords.toString() + '\'' +
                ", containingNodesAdresses=" + containingNodes.toString();
    }
}
