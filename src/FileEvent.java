/*
 * Created by Dave on 3/12/17.
 *
 * This class will take the file we want to send it and wrap it as an object.
 * The object will then we unwrapped client side.
 * Serializable is used to write the state of an object into a byte stream (marshaling)
 *
 */


import java.io.Serializable;

public class FileEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    private String destinationDirectory;
    private String sourceDirectory;
    private String filename;
    private long fileSize;
    private byte[] filedata;
    private String status;

    public FileEvent() {
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getDestinationDirectory() {
        return destinationDirectory;
    }

    public void setDestinationDirectory(String destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getFiledata() {
        return filedata;
    }

    public void setFiledata(byte[] filedata) {
        this.filedata = filedata;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
