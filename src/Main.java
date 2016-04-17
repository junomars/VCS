import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/res/fxml/main.fxml"));
        primaryStage.setTitle("343-JNN");
        primaryStage.setScene(new Scene(root, 600, 375));
        primaryStage.show();
    }
}
