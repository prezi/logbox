package com.prezi.logbox;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LocalExecutor extends Executor {
    public LocalExecutor(com.prezi.logbox.config.ExecutionConfiguration c) {
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
        com.prezi.logbox.config.CategoryConfiguration ruleConfig = execConfig.getRuleConfig().getCategoryConfigByName(
                execConfig.getLocalTestCategory()
        );

        while ((line = br.readLine()) != null) {
            for (com.prezi.logbox.config.Rule r : ruleConfig.getRules()){
                if ( r.matches(line)){
                    System.out.println(r.getSubstitutedLine(line));
                }
            }
        }
        br.close();
    }

}
