package com.prezi.logbox.executor;

import com.prezi.logbox.config.ExecutionContext;
import com.prezi.logbox.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LocalExecutor implements Executor {

    private ExecutionContext context;

    public LocalExecutor(ExecutionContext c) {
        this.context = c;
    }

    public void execute(String[] cliArgs) throws IOException {
        String inputBaseName;
        try {
            inputBaseName = FileUtils.baseName(context.getLocalTestInputFileName());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        context.getConfig().compileInputBaseName(inputBaseName);

        BufferedReader br = new BufferedReader(new FileReader(context.getLocalTestInputFile()));
        String line;
        com.prezi.logbox.config.CategoryConfiguration ruleConfig = context.getConfig().getCategoryConfigByName(
                context.getLocalTestCategory()
        );

        while ((line = br.readLine()) != null) {
            for (com.prezi.logbox.config.Rule r : ruleConfig.getRules()){
                if ( r.matches(line)){
                    System.out.println(r.getSubstitutedOutputLocation(line) +"\t<-\t" + r.getSubstitutedLine(line));
                }
            }
        }
        br.close();
    }

}
