package com.prezi.logsort;

import com.prezi.logsort.config.ExecutionConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LocalExecutor extends Executor {
    public LocalExecutor(ExecutionConfiguration c) {
        this.execConfig = c;
    }

    public void execute() throws IOException {
        File dir = execConfig.getLocalOutputDirectory();
        if (execConfig.isCleanUpOutputDir()) {
            if (dir.isDirectory()) {
                FileUtils.deleteDirectory(dir);
            } else {
                dir.delete();
            }
        }

        if (dir.exists()) {
            throw new IOException("Output directory " + dir.getName() + " already exists.");
        }

        dir.mkdir();

        BufferedReader br = new BufferedReader(new FileReader(execConfig.getLocalTestInputFile()));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        br.close();
    }

}
