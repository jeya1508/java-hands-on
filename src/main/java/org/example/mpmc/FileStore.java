package org.example.mpmc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

class FileStore<T> {
     static final Path FILE_PATH = Paths.get("queue_data.ser");

    public void serialize(T[] data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            oos.writeObject(data);
        } catch (IOException e) {
            System.out.println("Error during serialization: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public T[] deserialize() throws IOException {
        if (!Files.exists(FILE_PATH) || Files.size(FILE_PATH) == 0) return null;

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(FILE_PATH))) {
            return (T[]) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error during deserialization: " + e.getMessage());
            return null;
        }
    }
}
