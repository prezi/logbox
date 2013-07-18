package com.prezi.logsort;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import com.prezi.logsort.config.CategoryConfiguration;
import com.prezi.logsort.config.ExecutionConfiguration;
import com.prezi.logsort.config.LogSortConfiguration;
import com.prezi.logsort.config.Rule;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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
        CategoryConfiguration ruleConfig = execConfig.getRuleConfig().getCategoryConfigByName(
                execConfig.getLocalTestCategory()
        );

        while ((line = br.readLine()) != null) {
            for (Rule r : ruleConfig.getRules()){
                if ( r.matches(line)){
                    System.out.println(r.getSubstitutedLine(line));
                }
            }
        }
        br.close();
    }

}
