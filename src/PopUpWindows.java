/**
 * Created by Dave on 3/24/17.
 */

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PopUpWindows {

    private static boolean answer;
    private static Stage window;


    public static boolean quit() {
        window = new Stage();

        // user can only interact with alert box window
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Quit");
        window.setMaxWidth(450);
        window.setMaxHeight(450);

        Label label = new Label();
        label.setText("Are you sure you want to quit?");

        // create two buttons
        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");

        yesButton.setOnAction(e -> {
            answer = true;
            window.close();
        });

        noButton.setOnAction(e -> {
            answer = false;
            window.close();
        });


        VBox layout = new VBox(10);
        layout.getChildren().addAll(label, yesButton, noButton);
        layout.setAlignment(Pos.CENTER);
        //layout.setStyle("-fx-background-color: red");

        Scene scene = new Scene(layout);
        window.setScene(scene);

        window.showAndWait();
        return answer;
    }
}