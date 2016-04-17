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
    private final Path manifestFolder;

    public CheckManager(Path manifestFolder) {
        this.manifestFolder = manifestFolder;
    }

    /**
     * Used to either create a repo that doesn't already exist or to check in an existing repo
     *
     * @param source
     * @param target
     */
    public void checkIn(Path source, Path target) {
        // Get current datetime
        LocalDateTime dateTime = LocalDateTime.now();

        // Get manifest file path
        Path manifestFile = manifestFolder.resolve(dateTime.toString().replace(':', '.'));

        // Write source tree to file
        // Create new manifest file
        try (OutputStream output = new BufferedOutputStream(
                Files.newOutputStream(manifestFile, CREATE, APPEND))) {
            // Walk through tree adding files

            Files.walkFileTree(source, new FileVisitor<Path>() {
                byte data[];
                long aid;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Create the directory if it doesn't already exist
                    Path newDir = target.resolve(source.relativize(dir));

                    try {
                        // FIXME: prevent recursion
                        System.out.format("Creating dir: %s%n", newDir);
                        Files.createDirectory(newDir);

                        // Copy directory to the manifest
                        data = String.format("%s%n", dir.toString()).getBytes();
                        output.write(data, 0, data.length);
                    } catch (FileAlreadyExistsException e) {
                        // ignore
                        System.err.format("Skipping already existing directory: %s%n", newDir);
                    } catch (IOException e) {
                        System.err.format("Unable to copy folder: %s: %s%n", newDir, e);
                        return SKIP_SUBTREE;
                    }

                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    // Removes extension
                    String nameNoExt = file.getFileName().toString().split("\\.")[0];
                    Path newDir = target.resolve(source.relativize(file.getParent())).resolve(nameNoExt);

                    // Creates a directory for the file
                    try {
                        if (Files.notExists(newDir.getParent()))
                            Files.createDirectory(newDir.getParent());

                        Files.createDirectory(newDir);
                    } catch (FileAlreadyExistsException e) {
                        System.err.format("Skipping already existing file: %s%n", newDir);
                    } catch (IOException e) {
                        System.err.format("Unable to create folder: %s: %s%n", newDir, e);
                    }

                    // Copy file through createAID() and write file name with it's AID in manifest
                    try {
                        aid = Files.size(file) % 255;

                        data = file.toString().getBytes();
                        output.write(data, 0, data.length);

                        Path aidFile = newDir.resolve(aid + "");
                        Files.copy(file, aidFile, REPLACE_EXISTING);

                        data = String.format(" %d%n", aid).getBytes();
                        output.write(data, 0, data.length);
                    } catch (FileAlreadyExistsException e) {
                        System.out.format("File already exists: %s%n", file);
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
