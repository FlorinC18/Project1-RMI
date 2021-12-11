import java.io.File;
import java.io.Serializable;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;

public class FileContents implements Serializable, Remote {
    private File file;
    private String hash;
    private String name;
    private String description;
    private List<String> keywords;

    public FileContents() {
    }

    public FileContents(File file) {
        this.file = file;
        name = file.getName();
        keywords = new ArrayList<>();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", keywords=" + keywords;
    }
}
