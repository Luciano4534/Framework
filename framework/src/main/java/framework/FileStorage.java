package framework;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import jakarta.servlet.ServletContext;

public final class FileStorage {
    public static final String DIRECTORY_PARAMETER = "upload-dir";
    public static final String DIRECTORY_ATTRIBUTE = "framework.upload-dir";

    private final Path directory;

    public FileStorage(Path directory) {
        this.directory = directory;
    }

    public static FileStorage from(ServletContext context) {
        String value = context.getInitParameter(DIRECTORY_PARAMETER);
        if (value == null || value.isBlank()) {
            Object attribute = context.getAttribute(DIRECTORY_ATTRIBUTE);
            value = attribute instanceof String string ? string : null;
        }
        return new FileStorage(value == null || value.isBlank() ? null : Path.of(value.trim()));
    }

    public Path getDirectory() {
        return directory;
    }

    public Path save(MaFile file) throws IOException {
        return save(file, file.getFileName());
    }

    public Path save(MaFile file, String fileName) throws IOException {
        if (directory == null) {
            throw new IllegalStateException("Répertoire d'upload non configuré : context-param '"
                    + DIRECTORY_PARAMETER + "'");
        }
        Files.createDirectories(directory);
        Path target = directory.resolve(safeName(fileName));
        Files.write(target, file.getData(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
    }

    private static String safeName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "fichier";
        }
        String name = Path.of(fileName).getFileName().toString();
        if (name.equals(".") || name.equals("..")) {
            return "fichier";
        }
        return name;
    }
}
