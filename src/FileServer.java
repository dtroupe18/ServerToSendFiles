/*
 * Created by Dave on 3/12/17.
 *
 * This class will start with an initial current working directory
 * which will be set by whoever starts the server
 *
 * The client will be able to connect, change the CDW, and request any file
 *
 * The server packages the file into a "FileEvent" and sends it to the client
 *
 * This server will also be multithreaded
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;
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
    private String userCommand;
    private int clientNo = 0;
    private ArrayList<String> serverLog;

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

        submit.setOnAction( (ActionEvent e) -> {
            String tmp = textField.getText().trim();
            directory = new File(tmp);

            if (!directory.exists()) {
                // not a valid directory
                textArea.appendText("Not a valid directory please try again\n");
            }

            else if (directorySet) {
                CWD = tmp;
                textArea.appendText("Current working directory updated to: " + CWD + "\n");
                File[] files = getFiles(CWD);
                for (int i = 0; i < files.length; i++) {
                    textArea.appendText(printFile(files, i));
                }
            }

            else {
                CWD = tmp;
                directorySet = true;
                File[] files = getFiles(CWD);
                textArea.appendText("Current working directory: " + CWD +"\n");
                for (int i = 0; i < files.length; i++) {
                    textArea.appendText(printFile(files, i));
                }
            }
        });
        // END OF GUI

        // MULTI-THREADING
        new Thread( () -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8675);
                textArea.appendText("Multi-Threaded Server Started at " + new Date() + "\n");
                try {
                    while(!directorySet)
                    {
                        Thread.sleep(1000);
                        // Wait for the user to set an initial working directory
                    }

                }
                catch (InterruptedException e) {
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
                        textArea.appendText("Client " + clientNo +"'s host name is " +
                                inetAddress.getHostName() +"\n");
                        textArea.appendText("Client " + clientNo +"'s IP Address is " +
                                inetAddress.getHostAddress() + "\n");
                    });

                    // Create a new thread for each connection
                    new Thread(new HandleAClient(socket)).start();
                }
            }
            catch (IOException exception) {
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
        public  void run() {
            try {
                // create input and output data streams
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());


                // FOREVER serve the client
                while (true) {

                    try {
                        // user can only send strings
                        userCommand = (String) objectInputStream.readObject();
                        System.out.print(userCommand + "\n");
                    }
                    catch (ClassNotFoundException noClass) {
                        noClass.printStackTrace();
                    }

                    if (userCommand.startsWith("/") && isValidDirectory(userCommand)) {
                        // return list of files in the directory the user requested
                        File[] files = getFiles(userCommand);
                        objectOutputStream.writeObject(files);
                        System.out.print("CWD updated and list of files sent to client\n");
                    }

                    else if (userCommand.equals("index")) {
                        // return list of files in the CWD
                        File[] indexFiles = getFiles(CWD);
                        objectOutputStream.writeObject(indexFiles);
                        System.out.print("Index of files in" + CWD + " sent to client\n");
                    }

                    else if (isInteger(userCommand)) {
                        // construct array of files
                        int fileNumber = Integer.parseInt(userCommand);
                        File[] files = getFiles(CWD);
                        // figure out if the user wants a directory or a file
                        File requested = files[fileNumber];
                        if (requested.isDirectory()) {
                            // send files in that directory
                            System.out.print("User requested a directory\n");
                            CWD = requested.getPath();
                            File[] newDirectory = getFiles(requested.getPath());
                            objectOutputStream.writeObject(newDirectory);
                        }

                        else {
                            // user requested a file
                            objectOutputStream.writeObject(files[fileNumber]);
                            System.out.print("File: " + files[fileNumber].getName()  + " in " + CWD + " send to client\n");
                        }
                    }

                    else if (userCommand.endsWith("$")) {
                        // create directory
                        System.out.print("if check works $\n");
                        String directoryName = userCommand.substring(0, userCommand.length() - 1);
                        File newDirectory = new File(directoryName);
                        boolean created = false;
                        // if the directory doesn't exist create it

                        System.out.print("Creating directory " + directoryName + " in " + CWD + "\n");

                        try {
                            created = new File(CWD + "/" + directoryName).mkdirs();
                        }
                        catch (SecurityException se) {
                            System.out.print(se.getLocalizedMessage() +"\n");
                        }

                        if (created) {
                            try {
                                String result = ("Directory " + newDirectory + " created");
                                System.out.println(CWD + "/" + directoryName + " created");
                                objectOutputStream.writeObject(result);
                            }
                            catch (IOException ioe){
                                System.out.print(ioe.getLocalizedMessage() +"\n");
                            }
                        }
                        else {
                            try {
                                String result = ("Failed to create Directory " + newDirectory);
                                objectOutputStream.writeObject(result);
                            }
                            catch (IOException ioe){
                                System.out.print(ioe.getLocalizedMessage() +"\n");
                            }
                        }
                    }

                    else  {
                        objectOutputStream.writeObject("Not a valid command");
                    }

                    Platform.runLater(() -> {
                        textArea.appendText("Command received from client: " +
                                userCommand + "\n");
                    });
                }
            }
            catch (IOException ex) {
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
            file = ("File(" + n +"): " + f[n].getName() + "\n");
        }
        else if (f[n].isDirectory()) {
            file = ("Directory(" + n + "): " + f[n].getName() + "\n");
        }

        return file;
    }

    private boolean isValidDirectory(String path) {
        File tmpDirectory = new File(path);

        if (!tmpDirectory.exists()) {
            // not a valid directory
            return false;
        }

        else {
            CWD = path;
            directory = tmpDirectory;
            return true;
        }
    }

    private boolean isInteger(String str) {
        try {
            int n = Integer.parseInt(str);
        }
        catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
