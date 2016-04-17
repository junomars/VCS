package Controller;

import Model.CheckManager;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainController {
    /**
     * Other needed variables
     */
    private static DirectoryChooser directoryChooser;
    private static CheckManager checkManager;

    static {
        directoryChooser = new DirectoryChooser();
    }

    /**
     * FXML Objects
     */
    @FXML
    private TextField sourceField;
    @FXML
    private TextField targetField;
    @FXML
    private TextField repoNameField;
    @FXML
    private Button selectSource;
    @FXML
    private Button selectTarget;
    @FXML
    private Button createRepo;

    /**
     * Opens a directory chooser and sets the corresponding field to the path
     *
     * @param event the event identifies the source object
     */
    public void chooseDirectory(Event event) {
        if (event.getSource().equals(selectTarget)) {
            directoryChooser.setTitle("Select target directory");
        } else if (event.getSource().equals(selectSource)) {
            directoryChooser.setTitle("Select source directory");
        }

        File directory = directoryChooser.showDialog(new Stage());

        if (directory != null) {
            if (event.getSource().equals(selectTarget)) {
                targetField.setText(directory.getPath());
            } else if (event.getSource().equals(selectSource)) {
                sourceField.setText(directory.getPath());
            }
        }
    }

    /**
     * Creates a repository of the source folder in the target folder
     *
     * @param event the event identifies the source object
     */
    public void createRepo(Event event) {
        /**
         * FIXME: Don't assume folders exist and there is adequate diskspace
         * This can be done by adding a listener to the fields and
         *      updating a label when the field contains an invalid
         *      directory
         * FIXME: Don't allow recursion
         */
        Path manifest;
        Path repoPath;
        String repoName = repoNameField.getText();
        Path source = Paths.get(sourceField.getText());
        Path target = Paths.get(targetField.getText());

        if (repoName.isEmpty()) {
            repoPath = target.resolve(source.getFileName() + "_repo343");
        } else {
            repoPath = target.resolve(repoName);
        }

        // Create our directory if it doesn't already exist
        // consider: prompt user if the folder already exists?
        try {
            if (Files.notExists(repoPath)) {
                Files.createDirectory(repoPath);
                repoPath = repoPath.resolve(source.getFileName());
                Files.createDirectory(repoPath);
            }

            // Create a folder for the manifests
            manifest = repoPath.resolve("manifests");
            if (Files.notExists(manifest))
                Files.createDirectory(manifest);

            // Check in (by creating repo in this case)
            CheckManager cm = new CheckManager(manifest);
            cm.checkIn(source, repoPath);

            System.out.println("Repository successfully created.");
        } catch (IOException e) {
            System.err.format("Error creating repository: %s, %sn", repoPath, e);
        }
    }
}
