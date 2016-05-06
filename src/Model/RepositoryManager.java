package Model;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 *
 */
public class RepositoryManager {
    private static ArrayList<Path> getFileLineage(Path manifest) {
        ArrayList<Path> manifestLineage = new ArrayList<>();
        List<String> lines = null;
        String parentFile;

        try {
            while (manifest != null) {
                manifestLineage.add(manifest);

                // Open manifest
                lines = Files.readAllLines(manifest);

                // Get the parent file
                if (!lines.get(lines.indexOf("Parent File: ") + 1).equals("null")) {
                    parentFile = lines.get(lines.indexOf("Parent File: ") + 1);
                } else break;

                // Update manifest
                manifest = Paths.get(parentFile);
            }

            return manifestLineage;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static int getAIDFrom(Path file) throws IOException {
        int checksum = 0;
        int readChar;

        InputStream fileStream = Files.newInputStream(file);
        while ((readChar = fileStream.read()) != -1) {
            checksum += readChar;
        }

        return checksum % 256;
    }

    /**
     * Used to either create a repo that doesn't already exist or to check in an existing repo
     *
     * @param source the project tree to copy from
     * @param target the location to create / update repo
     */
    public void checkIn(Path source, Path target) {
        Path manifestFile;
        Path manifestFolder;
        final Path[] parentFile = {null};
        LocalDateTime dt = LocalDateTime.now();

        // Create a folder for the manifests if it doesn't already exist
        manifestFolder = target.resolve("manifests");
        try {
            Files.list(manifestFolder).sorted().forEach(mani -> {
                try {
                    if (Files.readAllLines(mani).contains(source.toString())) {
                        parentFile[0] = mani;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
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

            // List files
            output.write("\nProject Files:\n".getBytes());
            Files.list(source).forEach(filePath -> {
                try {
                    output.write(String.format("%s %d%n", source.getParent().relativize(filePath), getAIDFrom(filePath)).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Grab Parent
            if (parentFile[0] == null) {
                output.write(String.format("%nParent File: %n%s%n", "null").getBytes());
            } else {
                output.write(String.format("%nParent File: %n%s%n", parentFile[0].toString()).getBytes());
            }

            // Enter project location
            output.write(String.format("%nProject File Location: %n%s%n", source.toString()).getBytes());

            // Enter time stamp
            output.write(String.format("%nDate and Time Written:%n%s%n", dt.toString()).getBytes());
        } catch (IOException e) {
            System.err.format("Error writing manifest: %s, %s%n", manifestFile, e);
        }
    }

    public void checkOut(Path manifestFile, Path target) {
        Path fileToCopy;
        Path newManifestFile;
        Path targetCopy = target;
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
            output.write(String.format("%nDate and Time Written%n%s%n", dt.toString()).getBytes());

            // Close file
            output.close();
        } catch (IOException e) {
            System.err.format("Error writing manifest: %s, %s%n", newManifestFile, e);
            e.printStackTrace();
        }
    }

    public void merge(Path repoManifest, Path target) {
        Path treeManifest;
        Path mergeLocation;
        Path baseManifest;
        Path newManifestFile;
        Path logFile;
        Path repoLocation = repoManifest.getParent().getParent();
        LocalDateTime dt = LocalDateTime.now();

        // Create our manifest file with time stamp
        newManifestFile = repoManifest.getParent().resolve(dt.toString().replace(':', '_'));
        logFile = repoManifest.getParent().resolve(dt.toString().replace(':', '_').concat("_log"));

        try (OutputStream output = new BufferedOutputStream(
                Files.newOutputStream(newManifestFile, CREATE, APPEND));
             OutputStream log = new BufferedOutputStream(
                     Files.newOutputStream(logFile, CREATE, APPEND))
        ) {
            // Get the merge location from the repo manifest file
            Iterator<String> repoLines = Files.readAllLines(repoManifest).iterator();
            while (repoLines.hasNext()) {
                if (repoLines.next().contains("Project File Location:")) break;
            }
            mergeLocation = Paths.get(repoLines.next());

            // Get target manifest
            // Iterate through all the files in the manifest folder to find possible manifest
            ArrayList<Path> manifests = new ArrayList<>();
            Iterator<Path> iter = Files.list(repoManifest.getParent()).iterator();
            while (iter.hasNext()) {
                treeManifest = iter.next();
                if (Files.lines(treeManifest).anyMatch(target.toString()::equals)) {
                    manifests.add(treeManifest);
                }
            }

            // Find the last check in, this is our mergeFrom manifest
            Collections.sort(manifests);
            treeManifest = manifests.get(manifests.size() - 1);

            // Find the grandpa or base file
            ArrayList<Path> targetLineage = getFileLineage(treeManifest);
            ArrayList<Path> repoLineage = getFileLineage(repoManifest);
            targetLineage.retainAll(repoLineage);
            baseManifest = targetLineage.get(0);

            // Find our files from our trees
            List<String> treeHierarchy = Files.readAllLines(treeManifest);
            treeHierarchy = treeHierarchy.subList(treeHierarchy.indexOf("Project Files:") + 1, treeHierarchy.indexOf("Parent File: ") - 1);

            List<String> mergeHierarchy = Files.readAllLines(repoManifest);
            mergeHierarchy = mergeHierarchy.subList(mergeHierarchy.indexOf("Project Files:") + 1, mergeHierarchy.indexOf("Parent File: ") - 1);

            List<String> baseHierarchy = Files.readAllLines(baseManifest);
            baseHierarchy = baseHierarchy.subList(baseHierarchy.indexOf("Project Files:") + 1, baseHierarchy.indexOf("Parent File: ") - 1);

            ArrayList<Path> conflicts = new ArrayList<>();

            for (String path : mergeHierarchy) {
                // Split to get path - aid
                String[] tokens = path.split(" ");

                // Check for conflict in files
                if (tokens.length == 2 && treeHierarchy.stream().anyMatch(line -> line.contains(tokens[0]))) {
                    int treeAID = -1;
                    int baseAID = -1;

                    // Get other AIDs
                    for (String treeLine : treeHierarchy) {
                        if (treeLine.contains(tokens[0])) {
                            treeAID = Integer.parseInt(treeLine.split(" ")[1]);
                        }
                    }

                    for (String baseLine : baseHierarchy) {
                        if (baseLine.contains(tokens[0])) {
                            baseAID = Integer.parseInt(baseLine.split(" ")[1]);
                        }
                    }

                    // Prepare our files - we need our file location and the copies
                    String extension = tokens[0].substring(tokens[0].lastIndexOf("."));
                    conflicts.add(mergeLocation.getParent().resolve(path.substring(0, path.lastIndexOf(".")) + extension));
                    Path mt = mergeLocation.getParent().resolve(path.substring(0, path.lastIndexOf(".")) + "_MT" + extension);
                    Path mr = mergeLocation.getParent().resolve(path.substring(0, path.lastIndexOf(".")) + "_MR" + extension);
                    Path mg = mergeLocation.getParent().resolve(path.substring(0, path.lastIndexOf(".")) + "_MG" + extension);
                    Path mt_copy = repoLocation.resolve(tokens[0]).resolve("" + treeAID);
                    Path mr_copy = repoLocation.resolve(tokens[0]).resolve("" + Integer.parseInt(tokens[1]));
                    Path mg_copy = repoLocation.resolve(tokens[0]).resolve("" + baseAID);

                    // Write to log file
                    log.write(String.format("%s:%n%s %n%s %n%s %n%n", tokens[0], mt_copy, mr_copy, mg_copy).getBytes());

                    // Create new files
                    Files.createDirectories(mt.getParent());
                    Files.copy(mt_copy, mt, REPLACE_EXISTING);
                    Files.copy(mr_copy, mr, REPLACE_EXISTING);
                    Files.copy(mg_copy, mg, REPLACE_EXISTING);
                } else if (tokens.length == 2) {
                    // Add non conflicting files
                    Path newFile = mergeLocation.getParent().resolve(tokens[0]);
                    Files.createDirectories(newFile.getParent());
                    Files.createFile(newFile);
                    Files.copy(repoLocation.resolve(tokens[0]).resolve(tokens[1]), newFile, REPLACE_EXISTING);
                } else {
                    // Add folders
                    Path newDir = mergeLocation.getParent().resolve(tokens[0]);
                    Files.createDirectories(newDir);
                }
            }

            // Create our project hierarchy
            output.write("Project File Tree:\n".getBytes());
            Files.walkFileTree(mergeLocation, new FileVisitor<Path>() {
                final String fileFormat = "%s%3s %s %d%n"; // depth, attributes, path, aid
                int checksum;
                byte walkData[];
                String depth = "";
                String fileProperties = "";

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    try {
                        // Copy directory to the manifest
                        walkData = String.format("%s%s/%n", depth, dir.getFileName()).getBytes();
                        output.write(walkData, 0, walkData.length);
                    } catch (IOException e) {
                        System.err.format("Unable to copy folder: %s: %s%n", dir, e);
                        return SKIP_SUBTREE;
                    }

                    depth += "    ";
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    fileProperties = "";

                    if (file.toString().contains("_MG.") || file.toString().contains("_MR.") || file.toString().contains("_MT.")) {
                        return CONTINUE;
                    }

                    // Copy file through createAID() and write file name with it's AID in manifest
                    try {
                        // Read file contents to create checksum
                        checksum = getAIDFrom(file);

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

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    if (exc instanceof FileSystemLoopException) {
                        System.err.println("cycle detected: " + file);
                    } else {
                        System.err.format("Unable to copy: %s: %s%n", file, exc);
                    }
                    return CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    depth = depth.substring(0, depth.length() - 4);
                    return CONTINUE;
                }
            });

            // List files
            output.write("\nProject Files:\n".getBytes());
            Files.list(mergeLocation).forEach(filePath -> {
                try {
                    if (filePath.toString().contains("_MG.") || filePath.toString().contains("_MR.") || filePath.toString().contains("_MT.")) {
                        // do nothing
                    } else {
                        output.write(String.format("%s %d%n", mergeLocation.getParent().relativize(filePath), getAIDFrom(filePath)).getBytes());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            conflicts.stream().forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


            // Grab Parents
            output.write(String.format("%nParent File: %nTo Be: %s%nBefore: %s%n", repoManifest, treeManifest).getBytes());

            // Enter project location
            output.write(String.format("%nProject File Location: %n%s%n", mergeLocation).getBytes());

            // Enter time stamp
            output.write(String.format("%nDate and Time Written:%n%s%n", dt.toString()).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class CopyTreeVisitor implements FileVisitor<Path> {
        final String fileFormat = "%s%3s %s %d%n"; // depth, attributes, path, aid
        int checksum;
        byte walkData[];
        String depth = "";
        String fileProperties = "";
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
                checksum = getAIDFrom(file);

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
