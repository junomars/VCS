import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ResourceBundle;

/**
 * TODO: Tasks
 * Implement localization further
 * Implement auto merge option
 * <p>
 * TODO: Extra Features
 * Browse opens at current edit box location
 * Update program to keep a list of repositories
 * <p>
 * TODO: Bugs
 * Project files do not get written on deeper file levels on checkin
 * Hint: Access denied exception when running the program and a
 * new folder is created at the same time
 * Fix recursion
 * Old files aren't deleted in merge unless program closes
 */
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
