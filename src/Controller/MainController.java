package Controller;

import Model.RepositoryManager;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    /**
     * Other needed variables
     */
    private static final FileChooser fileChooser;
    private static final DirectoryChooser directoryChooser;
    private static final RepositoryManager repositoryManager;
    private static String hierarchy;

    static {
        fileChooser = new FileChooser();
        directoryChooser = new DirectoryChooser();
        repositoryManager = new RepositoryManager();
    }

    /**
     * FXML Objects
     */
    @FXML
    private TextField createFrom;
    @FXML
    private TextField createTo;
    @FXML
    private TextField repoNameField;
    @FXML
    private TextField checkinFrom;
    @FXML
    private TextField checkinTo;
    @FXML
    private TextField checkoutFrom;
    @FXML
    private TextField checkoutTo;
    @FXML
    private TextField mergeFrom;
    @FXML
    private TextField mergeTo;
    @FXML
    private Button selectCreateFrom;
    @FXML
    private Button selectCreateTo;
    @FXML
    private Button selectCheckinFrom;
    @FXML
    private Button selectCheckinTo;
    @FXML
    private Button selectCheckoutFrom;
    @FXML
    private Button selectCheckoutTo;
    @FXML
    private Button selectMergeFrom;
    @FXML
    private Button selectMergeTo;

    public void initialize(URL url, ResourceBundle res) {
        hierarchy = res.getString("hierarchy");
    }

    public void chooseFile(Event event) {
        if (event.getSource().equals(selectCheckoutFrom) || event.getSource().equals(selectMergeFrom)) {
            fileChooser.setTitle("Select manifest file");
        }

        File file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            if (event.getSource().equals(selectCheckoutFrom)) {
                checkoutFrom.setText(file.getPath());
            } else if (event.getSource().equals(selectMergeFrom)) {
                mergeFrom.setText(file.getPath());
            }
        }
    }

    /**
     * Opens a directory chooser and sets the corresponding field to the path
     *
     * @param event the event identifies the source object
     */
    public void chooseDirectory(Event event) {
        if (event.getSource().equals(selectCreateTo) ||
                event.getSource().equals(selectCheckinTo) ||
                event.getSource().equals(selectCheckoutTo) ||
                event.getSource().equals(selectMergeTo)) {
            directoryChooser.setTitle("Select target directory");
        } else if (event.getSource().equals(selectCreateFrom) || event.getSource().equals(selectCheckinFrom)) {
            directoryChooser.setTitle("Select source directory");
        }

        File directory = directoryChooser.showDialog(new Stage());

        if (directory != null) {
            if (event.getSource().equals(selectCreateTo)) {
                createTo.setText(directory.getPath());
            } else if (event.getSource().equals(selectCreateFrom)) {
                createFrom.setText(directory.getPath());
            } else if (event.getSource().equals(selectCheckinTo)) {
                checkinTo.setText(directory.getPath());
            } else if (event.getSource().equals(selectCheckinFrom)) {
                checkinFrom.setText(directory.getPath());
            } else if (event.getSource().equals(selectCheckoutTo)) {
                checkoutTo.setText(directory.getPath());
            } else if (event.getSource().equals(selectMergeTo)) {
                mergeTo.setText(directory.getPath());
            }
        }
    }

    /**
     * Creates a repository of the source folder in the target folder
     *
     */
    public void createRepo() {
        /**
         * FIXME: Don't assume folders exist and there is adequate diskspace
         * This can be done by adding a listener to the fields and
         *      updating a label when the field contains an invalid
         *      directory
         * FIXME: Don't allow recursion
         */
        Path manifestFolder;
        Path repoPath;
        String repoName = repoNameField.getText();
        Path source = Paths.get(createFrom.getText());
        Path target = Paths.get(createTo.getText());

        if (repoName.isEmpty()) {
            repoPath = target.resolve(source.getFileName() + "_repo343");
        } else {
            repoPath = target.resolve(repoName);
        }

        // Create our directory if it doesn't already exist
        // consider: prompt user if the folder already exists?
        try {
            Files.createDirectory(repoPath);

            // Create manifest
            manifestFolder = repoPath.resolve("manifests");
            Files.createDirectory(manifestFolder);

            // Check in (by creating repo in this case)
            repositoryManager.checkIn(source, repoPath);

            System.out.println("Repository successfully created.");
        } catch (FileAlreadyExistsException e) {
            System.err.println("Repository already created here. Check in instead?");
        } catch (IOException e) {
            System.err.format("Error creating repository: %s, %sn", repoPath, e);
        }
    }

    public void checkinRepo() {
        Path source = Paths.get(checkinFrom.getText());
        Path repo = Paths.get(checkinTo.getText());

        repositoryManager.checkIn(source, repo);
        System.out.println("Repository successfully checked in.");
    }

    public void checkoutRepo() {
        Path target = Paths.get(checkoutTo.getText());
        Path manifest = Paths.get(checkoutFrom.getText());

        repositoryManager.checkOut(manifest, target);
        System.out.println("Repository successfully checked out.");
    }

    public void merge() {
        Path target = Paths.get(mergeTo.getText());
        Path manifest = Paths.get(mergeFrom.getText());

        repositoryManager.merge(manifest, target);
        System.out.println("Repository successfully merged.");
    }
}
