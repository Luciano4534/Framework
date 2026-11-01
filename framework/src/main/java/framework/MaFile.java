package framework;

import java.util.Locale;
import java.util.Objects;

public final class MaFile {
    private final String fileName;
    private final String type;
    private final byte[] data;

    public MaFile(String fileName, String type, byte[] data) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.type = type == null ? extensionOf(fileName) : type;
        this.data = Objects.requireNonNull(data, "data").clone();
    }

    public MaFile(String fileName, byte[] data) {
        this(fileName, extensionOf(fileName), data);
    }

    public String getFileName() {
        return fileName;
    }

    public String getType() {
        return type;
    }

    public byte[] getData() {
        return data.clone();
    }

    public int getSize() {
        return data.length;
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
