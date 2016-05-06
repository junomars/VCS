import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ResourceBundle;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load our properties file
        ResourceBundle strings = ResourceBundle.getBundle("res/config/strings");

        // Give access to properties file to loader
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/res/fxml/main.fxml"));
        loader.setResources(strings);

        // Load fxml and create our gui
        Parent root = loader.load();
        primaryStage.setTitle(strings.getString("appTitle"));
        primaryStage.setScene(new Scene(root, 600, 375));
        primaryStage.show();
    }
}
