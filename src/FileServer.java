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
import java.nio.charset.StandardCharsets;
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
import sun.misc.IOUtils;

public class FileServer extends Application {

    // Text area to display content
    private TextArea textArea = new TextArea();
    private ArrayList<String> fileNames = new ArrayList<>();
    private String CWD;
    private boolean directorySet = false;
    private File directory;
    private String userCommand;


    // number the clients
    private int clientNo = 0;



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

                textArea.appendText("Current working directory: " + CWD +"\n");
                // function call to get files
                getFiles(CWD);

                // FOREVER!! Listen for connections
                while (true) {
                    Socket socket = serverSocket.accept();
                    clientNo++; // we added a client
                    Platform.runLater(() -> {
                        // Display the number of clients
                        textArea.appendText("Starting thread for client: " + clientNo +
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

    public File[] getFiles(String CWD) {
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
                DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
                DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());

                // FOREVER serve the client
                while (true) {

                    // receive client commands
                    userCommand = inputFromClient.readLine();
                    System.out.print("User command: " + userCommand + "\n");
                    textArea.appendText("*&*^&%^&$&^&%" + userCommand);
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



}
