package Model;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Created by jonat on 4/16/2016.
 */
public class CheckManager {
    /**
     * Used to either create a repo that doesn't already exist or to check in an existing repo
     *
     * @param source
     * @param target
     */
    public void checkIn(Path source, Path target) {
        Path manifestFolder;
        Path manifestFile;
        LocalDateTime dateTime;
        final String fileFormat = "%s%3s %s %d%n"; // depth, attributes, path, aid

        // Get current datetime
        dateTime = LocalDateTime.now();

        // Create a folder for the manifests if it doesn't already exist
        manifestFolder = target.resolve("manifests");
        if (Files.notExists(manifestFolder))
            try {
                Files.createDirectory(manifestFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

        // Create our manifest file
        manifestFile = manifestFolder.resolve(dateTime.toString().replace(':', '.'));

        // Create new manifest file with try with resource
        try (OutputStream output = new BufferedOutputStream(
                Files.newOutputStream(manifestFile, CREATE, APPEND))) {
            // Walk through tree adding files
            Files.walkFileTree(source, new FileVisitor<Path>() {
                int checksum;
                byte walkData[];
                String depth = "";
                String fileProperties = "";

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Create the directory if it doesn't already exist
                    Path newDir = target.resolve(source.relativize(dir));

                    try {
                        // FIXME: prevent recursion
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

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    fileProperties = "";
                    // Removes extension
                    String nameNoExt = file.getFileName().toString().split("\\.")[0];
                    String ext = file.getFileName().toString().replaceAll(nameNoExt, "");
                    Path newDir = target.resolve(source.relativize(file.getParent())).resolve(nameNoExt);

                    // Creates a directory for the file
                    try {
                        if (Files.notExists(newDir))
                            Files.createDirectory(newDir);
                    } catch (IOException e) {
                        System.err.format("Unable to create folder: %s: %s%n", newDir, e);
                    }

                    // Copy file through createAID() and write file name with it's AID in manifest
                    try {
                        // Read file contents to create checksum
                        checksum = 0;
                        int readChar;

                        InputStream fileStream = Files.newInputStream(file);
                        while ((readChar = fileStream.read()) != -1) {
                            checksum += readChar;
                        }

                        // Create our file
                        Path aidFile = newDir.resolve(checksum + ext);
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

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    if (exc instanceof FileSystemLoopException) {
                        System.err.println("cycle detected: " + file);
                    } else {
                        System.err.format("Unable to copy: %s: %s%n", file, exc);
                    }
                    return CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    depth = depth.substring(0, depth.length() - 4);
                    return CONTINUE;
                }
            });

            output.close();
        } catch (IOException e) {
            System.err.format("Error writing manifest: %s, %s%n", manifestFile, e);
        }
    }

    /**
     * Copy directory tree from source to destination in Check out
     *
     * @param sourceLocation
     * @param desLocation
     * @throws IOException
     */
    public void copyDirectoryCheckOut(File sourceLocation, File desLocation) throws IOException {

        if (sourceLocation.isDirectory()) {
            // if desLocation does not exist, it will be created.
            if (!desLocation.exists()) {
                desLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                // createFolder(desLocation.toString());
                copyDirectoryCheckOut(new File(sourceLocation, children[i]), new File(desLocation, children[i]));
            }
        }
    }

    /**
     * Copy file from source file in repo to destination folder in check out 	 * Check out
     *
     * @param sourceLocation
     * @param desLocation
     * @throws IOException
     */
    public void copyFileCheckOut(File sourceLocation, File desLocation) throws IOException {

        InputStream in = new FileInputStream(sourceLocation);
        OutputStream out = new FileOutputStream(desLocation);

        // Copy the bits from instream to outstream
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();

        // add man-line to arraylist
        //al_File.add(desLocation.getPath());

        // save path of each file into arraylist al_OriginalFile
        // al_OriginalFile.add(sourceLocation.getPath());
    }

    /**
     * build manifest file for checking out
     *
     * @param srcCheckInVersion
     * @param desFolder
     * @param CheckOutFolder
     * @throws IOException
     */
    public void buildCheckOutManifest(String srcCheckInVersion, String desFolder, File CheckOutFolder) throws IOException {

        // get current datetime
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd.hh.mm.ss");
        String timestand = ft.format(dNow);

        // Create manifest file inside checkin folder with its name is timestand
        File manifestFile = new File(CheckOutFolder, timestand);

        // FileWriter object to write man-line to file
        FileWriter writer = new FileWriter(manifestFile);

        // write check in version into manifest file
        writer.write(srcCheckInVersion + System.getProperty("line.separator"));
/**
 // write destination file path
 Iterator itr = al_File.iterator();
 while (itr.hasNext()) {
 writer.write(itr.next() + System.getProperty("line.separator"));
 }

 // clear Arraylist to make it empty
 al_File.clear();
 */
        writer.close();
    }
}
