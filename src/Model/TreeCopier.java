package Model;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

/**
 * Created by jonat on 4/16/2016.
 */
public class TreeCopier implements FileVisitor<Path> {
    private final Path source;
    private final Path target;

    public TreeCopier(Path source, Path target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        // Removes extension
        String nameNoExt = file.getFileName().toString().split("\\.")[0];
        Path newDir = target.resolve(source.relativize(file.getParent())).resolve(nameNoExt);

        try {
            if (Files.notExists(newDir.getParent()))
                Files.createDirectory(newDir.getParent());

            Files.createDirectory(newDir);
        } catch (IOException e) {
            System.err.format("Unable to create folder: %s: %s%n", newDir, e);
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
        Path newDir = target.resolve(source.relativize(dir));

        try {
            Files.createDirectory(newDir);
        } catch (FileAlreadyExistsException e) {
            System.out.format("Skipping existing directory: %s%n", newDir);
        } catch (IOException e) {
            System.err.format("Unable to create: %s: %s%n", newDir, e);
            return SKIP_SUBTREE;
        }

        return CONTINUE;
    }
}
