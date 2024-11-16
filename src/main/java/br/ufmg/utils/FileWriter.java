package br.ufmg.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

public class FileWriter {
    private FileOutputStream fos;

    public FileWriter(Path filePath, boolean autoFlush) throws FileNotFoundException, UnsupportedEncodingException {
        fos = new FileOutputStream(filePath.toFile(), true);
    }

    public void write(String text) {
        byte[] strToBytes = text.getBytes();
        try {
            fos.write(strToBytes);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        fos.close();
    }
}
