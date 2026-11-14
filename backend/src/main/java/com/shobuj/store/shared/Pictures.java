package com.shobuj.store.shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Stores uploaded pictures under the storage folder and returns their /media address. */
@Component
public class Pictures {

    public static final long MAX_BYTES = 5 * 1024 * 1024;

    private final StoreProperties properties;

    public Pictures(StoreProperties properties) {
        this.properties = properties;
    }

    /** Keeps a JPEG, PNG or WebP picture (checked by its bytes, not its name) in {@code folder}. */
    public String store(String folder, byte[] bytes) {
        if (bytes.length > MAX_BYTES) {
            throw ApiException.badRequest("image.size", "Pictures can be up to 5 MB.");
        }
        String ext = type(bytes);
        if (ext == null) {
            throw ApiException.badRequest("image.type", "Upload a JPEG, PNG or WebP picture.");
        }
        String name = UUID.randomUUID() + "." + ext;
        Path dir = properties.storageRoot().resolve(folder);
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(name), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties.mediaUrl() + "/" + folder + "/" + name;
    }

    static String type(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B'
                && b[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
