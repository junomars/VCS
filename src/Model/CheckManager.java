package Model;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import static java.nio.file.FileVisitResult.CONTINUE;
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

    public void checkIn(Path source) {
        // Get current datetime
        LocalDateTime dateTime = LocalDateTime.now();

        // Get manifest file path
        Path manifestFile = manifestFolder.resolve(dateTime.toString());

        // Write source tree to file
        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(manifestFile, CREATE, APPEND))) {
            // Create new manifest file
            Files.createFile(manifestFile);

            // Walk through tree adding files
            Files.walkFileTree(source, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    out.write(file.toString().getBytes());
                    // TODO: calculate AID and add to end of the line
                    // TODO: create an artifact within the folder in the repo for that file
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.err.println(exc);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    System.out.format("Directory: %s%n", dir);
                    return CONTINUE;
                }
            });
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
     * Build manifest file to save the history of work
     *
     * @param manifestFolder
     * @throws IOException
     */
    public void buildManifest(File manifestFolder) throws IOException {

        // get current datetime
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd.hh.mm.ss");
        String timestand = ft.format(dNow);

        // create manifest file with its name is timestand
        File manifest = new File(manifestFolder, timestand);

        // write arraylist to manifest file(named by timestand)
        FileWriter writer = new FileWriter(manifest);

        /**
         Iterator itr = al_File.iterator();
         while (itr.hasNext()) {
         writer.write(itr.next() + System.getProperty("line.separator"));
         }

         writer.close();

         // clear Arraylist to make it empty for the next check in
         al_File.clear();
         al_OriginalFile.clear();
         */
    }


    /**
     * build manifest file for checking in
     *
     * @param srcFolderPath
     * @param CheckInFolder
     * @throws IOException
     */
    public void buildCheckInManifest(String srcFolderPath, File CheckInFolder) throws IOException {

        // get current datetime
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd.hh.mm.ss");
        String timestand = ft.format(dNow);

        // create manifest file inside checkin folder with its name is timestand
        File manifestFile = new File(CheckInFolder, timestand);

        // FileWriter object to write man-line to file
        FileWriter writer = new FileWriter(manifestFile);

        // write parent folder to file
        writer.write(srcFolderPath + System.getProperty("line.separator"));
        /**
         al_OriginalFile.add(0, srcFolderPath);

         // write arraylist of original files to manifest file(named by timestand)
         Iterator itr1 = al_OriginalFile.iterator();

         while (itr1.hasNext()) {
         // write line by line
         writer.write(itr1.next() + System.getProperty("line.separator"));
         }

         // write arraylist to manifest file(named by timestand)
         Iterator itr = al_File.iterator();
         while (itr.hasNext()) {
         writer.write(itr.next() + System.getProperty("line.separator"));
         }

         writer.close();

         // clear Arraylist to make it empty for the next check in
         al_File.clear();
         al_OriginalFile.clear();
         */
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
