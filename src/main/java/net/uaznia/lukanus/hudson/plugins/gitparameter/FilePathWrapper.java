package net.uaznia.lukanus.hudson.plugins.gitparameter;

import java.io.IOException;
import java.util.logging.Logger;

import hudson.FilePath;

public class FilePathWrapper {
    private final FilePath filePath;
    private Boolean isTemporary = false;

    public FilePathWrapper(FilePath filePath) {
        this.filePath = filePath;
    }

    public void setThatTemporary() {
        isTemporary = true;
    }

    public FilePath getFilePath() {
        return filePath;
    }

    public void delete() throws IOException, InterruptedException {
        if (isTemporary) {
            filePath.deleteRecursive();
        }
    }
}
