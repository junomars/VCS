package Model;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Created by jonat on 4/16/2016.
 */
public class RepositoryManager {
    /**
     * Used to either create a repo that doesn't already exist or to check in an existing repo
     *
     * @param source the project tree to copy from
     * @param target the location to create / update repo
     */
    public void checkIn(Path source, Path target) {
        Path manifestFile;
        Path manifestFolder;
        Path parentFile = null;
        LocalDateTime dt = LocalDateTime.now();

        // Create a folder for the manifests if it doesn't already exist
        manifestFolder = target.resolve("manifests");
        try {
            parentFile = Files.list(manifestFolder).sorted((o1, o2) -> o2.toString().compareTo(o1.toString())).findFirst().get();
        } catch (NoSuchElementException e) {
            // Ignore
        } catch (IOException e) {
            System.err.format("Error reading manifest folder: %s, %s%n", manifestFolder, e);
        }

        // Create our manifest file with time stamp
        manifestFile = manifestFolder.resolve(dt.toString().replace(':', '_'));

        // Create new manifest file with try with resource
        try (OutputStream output = new BufferedOutputStream(
                Files.newOutputStream(manifestFile, CREATE, APPEND))) {
            // Create new project folder
            target = target.resolve(source.getFileName());

            if (Files.notExists(target))
                Files.createDirectory(target);

            // Walk through tree adding files
            output.write("Project File Tree:\n".getBytes());
            Files.walkFileTree(source, new CopyTreeVisitor(source, target, output));

            // Grab Parent
            if (parentFile == null) {
                output.write(String.format("%nParent file: %n%s%n", "null").getBytes());
            } else {
                output.write(String.format("%nParent file: %n%s%n", parentFile.toString()).getBytes());
            }

            // Enter project location
            output.write(String.format("%nProject File Location: %n%s%n", source.toString()).getBytes());

            // Enter time stamp
            output.write(String.format("%nDate and time written:%n%s%n", dt.toString()).getBytes());

            // Close file
            output.close();
        } catch (IOException e) {
            System.err.format("Error writing manifest: %s, %s%n", manifestFile, e);
        }
    }

    public void checkOut(Path manifestFile, Path target) {
        Path fileToCopy;
        Path newManifestFile;
        LocalDateTime dt = LocalDateTime.now();

        fileToCopy = manifestFile.getParent().getParent();

        // Create our manifest file with time stamp
        newManifestFile = manifestFile.getParent().resolve(dt.toString().replace(':', '_'));

        // Create new manifest file with try with resource
        try (OutputStream output = new BufferedOutputStream(
                Files.newOutputStream(newManifestFile, CREATE, APPEND))) {
            // Walk through tree adding files
            int prevDepth = -1;
            int currDepth;
            boolean tree = true;
            for (String line : Files.readAllLines(manifestFile)) {
                if (line.contains("Parent File:")) {
                    break;
                }

                // Copied line
                output.write(String.format("%s%n", line).getBytes());

                if (line.contains("Project File Tree:")) {
                    continue;
                }

                if (line.length() > 0 && tree) {
                    // Copy file into ptree
                    if (line.matches("(.* ){2}[\\d]+")) {
                        // We have a file with an AID
                        // Delete current directory and make it into a file with contents from current path
                        String[] paths = line.trim().split(" ");
                        Files.copy(fileToCopy.resolve(paths[1]).resolve(paths[2]), target.resolve(paths[1]), REPLACE_EXISTING);
                    } else {
                        // We have a directory
                        currDepth = 0;
                        while (line.matches("[ ]{4}.*")) {
                            currDepth++;
                            line = line.replaceFirst("[ ]{4}.*", "");
                        }

                        if (currDepth < prevDepth) {
                            target = target.getParent();
                            fileToCopy = fileToCopy.getParent();
                        } else if (currDepth > prevDepth) {
                            target = target.resolve(line.trim().replace("/", ""));
                            fileToCopy = fileToCopy.resolve(line.trim().replace("/", ""));

                            if (Files.notExists(target)) {
                                Files.createDirectory(target);
                            }
                        } else {
                            target = target.getParent().resolve(line.trim().replace("/", ""));
                            fileToCopy = fileToCopy.getParent().resolve(line.trim().replace("/", ""));

                            if (Files.notExists(target)) {
                                Files.createDirectory(target);
                            }
                        }

                        prevDepth = currDepth;
                    }
                } else {
                    tree = false;
                }
            }

            // Enter parent
            output.write(String.format("Parent File: %n%s%n", manifestFile).getBytes());

            // Enter project location
            output.write(String.format("%nProject File Location: %n%s%n", target).getBytes());

            // Enter time stamp
            output.write(String.format("%nDate and time written:%n%s%n", dt.toString()).getBytes());

            // Close file
            output.close();
        } catch (IOException e) {
            System.err.format("Error writing manifest: %s, %s%n", newManifestFile, e);
            e.printStackTrace();
        }
    }

    public void merge(Path repoManifest, Path target) {
        Path treeManifest;
        Path base = null;

        // Get target manifest
        try {
            // Iterate through all the files in the manifest folder to find possible manifest
            ArrayList<Path> manifests = new ArrayList<>();
            Iterator<Path> iter = Files.list(repoManifest.getParent()).iterator();
            while (iter.hasNext()) {
                treeManifest = iter.next();
                if (Files.lines(treeManifest).anyMatch(target.toString()::equals)) {
                    manifests.add(treeManifest);
                }
            }

            // Find the first check in within the manifests (i.e. the check out manifest that started it all)
            Collections.sort(manifests);
            Iterator<String> lines = Files.readAllLines(manifests.get(0)).iterator();
            String debug;
            while (lines.hasNext()) {
                debug = lines.next();
                if (debug.contains("Parent File:")) {
                    System.out.format("-%s-%n", debug);
                    break;
                }
            }
            base = Paths.get(lines.next());

            // Find the last check in, this is our mergeFrom manifest
            treeManifest = manifests.get(manifests.size() - 1);

            System.out.printf("repo: %s%ntree: %s%ngramps: %s%nat: %s%n", repoManifest, treeManifest, base, target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class CopyTreeVisitor implements FileVisitor<Path> {
        int checksum;
        byte walkData[];
        String depth = "";
        String fileProperties = "";
        String fileFormat = "%s%3s %s %d%n"; // depth, attributes, path, aid
        Path source;
        Path target;
        OutputStream output;

        CopyTreeVisitor(Path source, Path target, OutputStream output) {
            this.source = source;
            this.target = target;
            this.output = output;
        }

        /**
         * Invoked for a directory before entries in the directory are visited.
         *
         * @param dir   a reference to the directory
         * @param attrs the directory's basic attributes
         * @return the visit result
         * @throws IOException if an I/O error occurs
         */
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            // Create the directory if it doesn't already exist
            Path newDir = target.resolve(source.relativize(dir));

            try {
                if (Files.notExists(newDir))
                    Files.createDirectory(newDir);

                // Copy directory to the manifest
                walkData = String.format("%s%s/%n", depth, newDir.getFileName().toString()).getBytes();
                output.write(walkData, 0, walkData.length);
            } catch (IOException e) {
                System.err.format("Unable to copy folder: %s: %s%n", newDir, e);
                return SKIP_SUBTREE;
            }

            depth += "    ";
            return CONTINUE;
        }

        /**
         * Invoked for a file in a directory.
         *
         * @param file  a reference to the file
         * @param attrs the file's basic attributes
         * @return the visit result
         * @throws IOException if an I/O error occurs
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            fileProperties = "";

            // Creates our directory for our file
            Path newDir = target.resolve(source.relativize(file.getParent())).resolve(file.getFileName());

            // Copy file through createAID() and write file name with it's AID in manifest
            try {
                // Creates a directory for the file
                if (Files.notExists(newDir))
                    Files.createDirectory(newDir);

                // Read file contents to create checksum
                checksum = 0;
                int readChar;

                InputStream fileStream = Files.newInputStream(file);
                while ((readChar = fileStream.read()) != -1) {
                    checksum += readChar;
                }

                checksum %= 256;

                // Create our file
                Path aidFile = newDir.resolve(checksum + "");
                Files.copy(file, aidFile, REPLACE_EXISTING);

                // Adding file properties
                fileProperties += Files.isReadable(file) ? 'R' : '_';
                fileProperties += Files.isWritable(file) ? 'W' : '_';
                fileProperties += Files.isExecutable(file) ? 'E' : '_';

                // Create our line and write
                walkData = String.format(fileFormat, depth, fileProperties, file.getFileName().toString(), checksum).getBytes();
                output.write(walkData, 0, walkData.length);
            } catch (IOException e) {
                System.out.format("Error writing to file: %s%n", file);
            }

            return CONTINUE;
        }

        /**
         * Invoked for a file that could not be visited. This method is invoked
         * if the file's attributes could not be read, the file is a directory
         * that could not be opened, and other reasons.
         *
         * @param file a reference to the file
         * @param exc  the I/O exception that prevented the file from being visited
         * @return the visit result
         * @throws IOException if an I/O error occurs
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            if (exc instanceof FileSystemLoopException) {
                System.err.println("cycle detected: " + file);
            } else {
                System.err.format("Unable to copy: %s: %s%n", file, exc);
            }
            return CONTINUE;
        }

        /**
         * Invoked for a directory after entries in the directory, and all of their
         * descendants, have been visited. This method is also invoked when iteration
         * of the directory completes prematurely.
         *
         * @param dir a reference to the directory
         * @param exc {@code null} if the iteration of the directory completes without
         *            an error; otherwise the I/O exception that caused the iteration
         *            of the directory to complete prematurely
         * @return the visit result
         * @throws IOException if an I/O error occurs
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            depth = depth.substring(0, depth.length() - 4);
            return CONTINUE;
        }
    }
}
