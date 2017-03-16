/*
 * Created by Dave on 3/12/17.
 *
 * This class will start with an initial current working directory
 * which will be set by whoever starts the server
 *
 * The client will be able to connect, change the CDW, and request any file
 *
 *
 * This server will also be multithreaded
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class FileServer extends Application {

    // Text area to display content
    private TextArea textArea = new TextArea();
    private String CWD;
    private boolean directorySet = false;
    private File directory;
    private Object clientCommand;
    private String clientCommandString;
    private int clientNo = 0;
    private ArrayList<String> updatedTextFile;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;

    @Override
    public void start(Stage primaryStage) throws Exception {

        // GUI FOR SERVER
        BorderPane paneForTextField = new BorderPane();
        paneForTextField.setPadding(new Insets(5, 5, 15, 5));
        paneForTextField.setStyle("-fx-border-color: red");
        paneForTextField.setLeft(new Label("Enter path for CWD: "));
        Button submit = new Button("Submit");
        paneForTextField.setBottom(submit);
        TextField textField = new TextField();
        textField.setAlignment(Pos.BOTTOM_RIGHT);
        paneForTextField.setCenter(textField);

        BorderPane mainPane = new BorderPane();
        // display contents
        TextArea textArea = new TextArea();
        mainPane.setCenter(new ScrollPane(textArea));
        mainPane.setTop(paneForTextField);

        // create scene and place it on the stage
        Scene scene = new Scene(mainPane, 450, 300);

        //Scene scene = new Scene(new ScrollPane(textArea), 450, 450);
        primaryStage.setTitle("MultiThreaded File Server");
        primaryStage.setScene(scene);
        primaryStage.show();

        submit.setOnAction((ActionEvent e) -> {
            String tmp = textField.getText().trim();
            directory = new File(tmp);

            if (!directory.exists()) {
                // not a valid directory
                textArea.appendText("Not a valid directory please try again\n");
            } else if (directorySet) {
                CWD = tmp;
                textArea.appendText("Current working directory updated to: " + CWD + "\n");
                File[] files = getFiles(CWD);
                for (int i = 0; i < files.length; i++) {
                    textArea.appendText(printFile(files, i));
                }
            } else {
                CWD = tmp;
                directorySet = true;
                File[] files = getFiles(CWD);
                textArea.appendText("Current working directory: " + CWD + "\n");
                for (int i = 0; i < files.length; i++) {
                    textArea.appendText(printFile(files, i));
                }
            }
        });
        // END OF GUI

        // MULTI-THREADING
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8675);
                textArea.appendText("Multi-Threaded Server Started at " + new Date() + "\n");
                try {
                    while (!directorySet) {
                        Thread.sleep(1000);
                        // Wait for the user to set an initial working directory
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();  // set interrupt flag
                    System.out.println("Failed to hold thread for user input");
                }

                // FOREVER!! Listen for connections
                while (true) {
                    Socket socket = serverSocket.accept();
                    clientNo++; // we added a client
                    Platform.runLater(() -> {
                        // Display the number of clients
                        textArea.appendText("Starting thread for client(" + clientNo + ")" +
                                " at " + new Date() + "\n");
                        // Find the client's host name, and IP address
                        InetAddress inetAddress = socket.getInetAddress();
                        textArea.appendText("Client " + clientNo + "'s host name is " +
                                inetAddress.getHostName() + "\n");
                        textArea.appendText("Client " + clientNo + "'s IP Address is " +
                                inetAddress.getHostAddress() + "\n");
                    });

                    // Create a new thread for each connection
                    new Thread(new HandleAClient(socket)).start();
                }
            } catch (IOException exception) {
                System.err.println(exception);
            }
        }).start();

    } // end of start

    // Thread class for handling new client connections
    class HandleAClient implements Runnable {
        private Socket socket;

        // construct a thread
        public HandleAClient(Socket socket) {
            this.socket = socket;
        }

        // run a thread
        public void run() {
            try {
                // create input and output data streams
                objectInputStream = new ObjectInputStream(socket.getInputStream());
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

                // FOREVER serve the client
                while (true) {
                    readFromClient();
                    String clientWants = whatDoesTheClientWant();
                    System.out.println("Client wants " + clientWants);

                    switch (clientWants) {
                        case "updateCWD":
                            File[] files = getFiles(clientCommandString);
                            writeObjectToClient(files);
                            System.out.print("CWD updated and list of files sent to client\n");
                            break;

                        case "showFiles":
                            File[] indexFiles = getFiles(CWD);
                            writeObjectToClient(indexFiles);
                            System.out.print("Index of files in" + CWD + " sent to client\n");
                            break;

                        case "sendDirectory":
                            sendDirectory();
                            break;

                        case "createDirectory":
                            createDirectory();
                            break;

                        case "removeDirectory":
                            String directoryName = clientCommandString.substring(0, clientCommandString.length() - 1);
                            File toRemove = new File(CWD + "/" + directoryName);
                            removeDirectory(toRemove);
                            break;

                        case "overwriteFile":
                            overWriteFile();
                            break;

                        default:
                            objectOutputStream.writeObject("Not a valid command");
                            break;
                    }

                    // some sort of thread issue the textArea cannot be updated ??
                    Platform.runLater(() -> {
                        textArea.appendText("Command received from client: " +
                                clientCommand + "\n");
                    });
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private File[] getFiles(String CWD) {
        File folder = new File(CWD);
        File[] listOfFiles = folder.listFiles();

        return listOfFiles;
    }

    private String printFile(File[] f, int n) {
        String file = "";

        if (f[n].isFile()) {
            file = ("File(" + n + "): " + f[n].getName() + "\n");
        } else if (f[n].isDirectory()) {
            file = ("Directory(" + n + "): " + f[n].getName() + "\n");
        }

        return file;
    }

    private boolean isValidDirectory(String path) {
        File tmpDirectory = new File(path);

        if (!tmpDirectory.exists()) {
            // not a valid directory
            return false;
        } else {
            CWD = path;
            directory = tmpDirectory;
            return true;
        }
    }

    private boolean isInteger(String str) {
        try {
            int n = Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private void removeDirectory(File file) {
        boolean result = false;
        String directoryName = clientCommandString.substring(0, clientCommandString.length() - 1);
        /* java can't delete a directory if it has files inside it
           so we will remove all inner files first then
           delete the directory */
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                f.delete(); // remove file
            }
        }
        result = file.delete(); // remove directory
        if (result) {
            writeObjectToClient("Successfully removed " + directoryName);
        } else {
            writeObjectToClient("Failed to remove " + directoryName);
        }
    }

    private void writeObjectToClient(String s) {
        try {
            objectOutputStream.writeObject(s);
        }
        catch (IOException ioe) {
            textArea.appendText(ioe.getLocalizedMessage());
        }
    }

    private void writeObjectToClient(Object obj) {
        try {
            objectOutputStream.writeObject(obj);
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void readFromClient() {
        try {
            clientCommand = objectInputStream.readObject();

            if (clientCommand instanceof String) {
                clientCommandString = (String) clientCommand;
            }

        } catch (ClassNotFoundException cnf) {
            textArea.appendText(cnf.getLocalizedMessage());
        } catch (IOException ioe2) {
            textArea.appendText(ioe2.getMessage());
        }
    }

    private void sendDirectory() {
        System.out.println(clientCommand);
        // construct array of files
        int fileNumber = Integer.parseInt(clientCommandString);
        File[] files = getFiles(CWD);
        System.out.println("CWD: " + CWD);
        // figure out if the user wants a directory or a file
        File requested = files[fileNumber];
        if (requested.isDirectory()) {
            // send files in that directory
            System.out.print("User requested a directory\n");
            CWD = requested.getPath();
            File[] newDirectory = getFiles(requested.getPath());
            writeObjectToClient(newDirectory);
        }
        else {
            // user requested a file
            writeObjectToClient(files[fileNumber]);
            System.out.print("File: " + files[fileNumber].getName()  + " in " + CWD + " sent to client\n");
        }
    }

    private void createDirectory() {
        // create directory
        String directoryName = clientCommandString.substring(0, clientCommandString.length() - 1);
        File newDirectory = new File(directoryName);
        boolean created = false;

        System.out.print("Creating directory " + directoryName + " in " + CWD + "\n");

        try {
            created = new File(CWD + "/" + directoryName).mkdirs();
        } catch (SecurityException se) {
            System.out.print(se.getLocalizedMessage() + "\n");
        }

        if (created) {
            String result = ("Directory " + newDirectory + " created");
            System.out.println(CWD + "/" + directoryName + " created");
            writeObjectToClient(result);
        } else {
            String result = ("Failed to create Directory " + newDirectory);
            writeObjectToClient(result);
        }
    }

    private void overWriteFile() {
        ArrayList<String> temp = (ArrayList<String>) clientCommand;
        String filepath = CWD + "/" + temp.get(0);
        System.out.println("Size " + temp.size());
        System.out.println("File path = " + filepath);
        temp.remove(0);

        try {
            // Write new text plus all old text to the file
            FileWriter fileWriter = new FileWriter(filepath);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (String s : temp) {
                bufferedWriter.write(s);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();

        }
        catch (IOException ioe) {
            System.out.println("Error occurred " + ioe.getMessage());
            writeObjectToClient("Server had an error");
        }

        writeObjectToClient("Successfully overwrote file ");
    }


    private String whatDoesTheClientWant() {
        String actionToPerform = "";

        if (clientCommand instanceof String && clientCommandString.startsWith("/") && isValidDirectory(clientCommandString))
            actionToPerform = "updateCWD";
        else if (clientCommand instanceof String && clientCommand.equals("index"))
            actionToPerform = "showFiles";
        else if (clientCommand instanceof String && isInteger(clientCommandString))
            actionToPerform = "sendDirectory";
        else if (clientCommand instanceof String && clientCommandString.endsWith("$"))
            actionToPerform = "createDirectory";
        else if (clientCommand instanceof String && clientCommandString.endsWith("#"))
            actionToPerform = "removeDirectory";
        else if (!(clientCommand instanceof String))
            actionToPerform = "overwriteFile";

        return actionToPerform;
    }
}
