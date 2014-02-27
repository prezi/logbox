package com.prezi.logbox.config;

import com.google.gson.JsonSyntaxException;
import com.prezi.logbox.utils.FileUtils;
import com.prezi.logbox.utils.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;

public class ExecutionContext {

    private static Log log = LogFactory.getLog(ExecutionContext.class);
    private LogBoxConfiguration ruleConfig;
    private CommandLineArguments commandLineArguments;

    public ExecutionMode getExecutionMode() {
        return commandLineArguments.getExecutionMode();
    }

    public LogBoxConfiguration getConfig() {
        return ruleConfig;
    }

    public void setRuleConfig(LogBoxConfiguration config) {
        this.ruleConfig = config;
    }


    public  void setupFromCommandLineArgs(String[] args) {
        commandLineArguments = new CommandLineArguments();
        commandLineArguments.parseArguments(args);

        try {
            setRuleConfig(LogBoxConfiguration.loadConfig(commandLineArguments.getConfigFile()));
            if (commandLineArguments.getExecutionMode() == com.prezi.logbox.config.ExecutionMode.LOCAL_TEST) {
                getConfig().applyFilters(new String[]{commandLineArguments.getLocalTestCategory()});
            }
            if (commandLineArguments.hasFilters()){
                getConfig().applyFilters(commandLineArguments.getFilters());
            }
        } catch (FileNotFoundException e) {
            commandLineArguments.configFileNotFound();
        } catch (JsonSyntaxException e) {
            log.error("Syntax error in the JSON file " + commandLineArguments.getConfigFile() + ": " + e);
            System.exit(102);
        }
        try {
            if (FileUtils.protocolFromURI(getConfig().getInputLocationPrefix()) == Protocol.S3){
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void compileDateGlob(){
        for (CategoryConfiguration c : getConfig().getCategoryConfigurations()){
            c.setInputGlob(c.getInputGlob().replace("${date_glob}", commandLineArguments.getDateGlob()));
        }
    }
    public void compileHourGlob(){
        for (CategoryConfiguration c : getConfig().getCategoryConfigurations()){

            c.setInputGlob(c.getInputGlob().replace("${hours_glob}", commandLineArguments.getHourGlob()));
        }
    }
    public String getLocalTestCategory() {
        return commandLineArguments.getLocalTestCategory();
    }

    public File getLocalTestInputFile() {
        return commandLineArguments.getLocalTestInputFile();
    }

    public String getDateGlob() {
        return commandLineArguments.getDateGlob();
    }

    public String getLocalTestInputFileName() {
        return commandLineArguments.getLocalTestInputFileName();
    }
}
