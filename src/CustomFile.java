import java.util.ArrayList;

/**
 * Created by Dave on 3/15/17.
 */
public class CustomFile {
    ArrayList<String> text;
    String fileName;

    public CustomFile(ArrayList<String> text, String fileName) {
        this.text = text;
        this.fileName = fileName;
    }

    public ArrayList<String> getText() {
        return text;
    }

    public void setText(ArrayList<String> text) {
        this.text = text;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
