package com.auction.util.core.datamanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * File-based implementation của DataPersistence.
 * Sử dụng atomic write để đảm bảo data integrity.
 * Tuân thủ Single Responsibility Principle - chỉ làm file I/O.
 * Tuân thủ Open/Closed Principle - có thể extend cho các storage khác.
 */
public class FileDataPersistence<K, V> implements DataPersistence<K, V> {

  private static final String FILE_EXTENSION = ".dat";
  private static final String TEMP_EXTENSION = ".tmp";

  /**
   * Save data với atomic write.
   */
  @Override
  public void save(Map<K, V> data, String identifier) throws DataPersistenceException {
    String fileName = identifier + FILE_EXTENSION;
    String tempFileName = fileName + TEMP_EXTENSION;
    File tempFile = new File(tempFileName);

    // Step 1: Write to temp file
    try (ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(tempFile))) {
      oos.writeObject(data);
      oos.flush();
    } catch (IOException e) {
      cleanup(tempFile);
      throw new DataPersistenceException("Failed to write temp file: " + tempFileName, e);
    }

    // Step 2: Atomic move
    try {
      Path source = Paths.get(tempFileName);
      Path target = Paths.get(fileName);
      Files.move(source, target,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      cleanup(tempFile);
      throw new DataPersistenceException("Failed to move file atomically: " + fileName, e);
    }
  }

  /**
   * Load data từ file.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Map<K, V> load(String identifier) throws DataPersistenceException {
    String fileName = identifier + FILE_EXTENSION;
    File file = new File(fileName);

    if (!file.exists()) {
      return null;
    }

    try (ObjectInputStream ois = new ObjectInputStream(
        new FileInputStream(file))) {
      return (Map<K, V>) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new DataPersistenceException("Failed to load file: " + fileName, e);
    }
  }

  /**
   * Cleanup temp file nếu có lỗi.
   */
  private void cleanup(File tempFile) {
    if (tempFile.exists()) {
      if (!tempFile.delete()) {
        System.err.println("[FileDataPersistence] Warning: Could not delete temp file: "
            + tempFile.getAbsolutePath());
      }
    }
  }
}