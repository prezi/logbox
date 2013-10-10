package com.prezi.logbox.config;

import com.google.gson.JsonSyntaxException;
import com.prezi.FileUtils;
import com.prezi.IgnoreUnknownParametersParser;
import com.prezi.Protocol;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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


    //public ExecutionContext setupFromCommandLineArgs(String[] args) {
public  void setupFromCommandLineArgs(String[] args) {
        //ExecutionContext context = new ExecutionContext();
        // read arguments
        commandLineArguments = new CommandLineArguments();
        commandLineArguments.parseArguments(args);
        // LogBoxConfiguration.setDateGlob(commandLineArguments.getDateGlob());
        // then getting information from config.json
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
            //TODO: Handle me
        }

    }


    public void compileDateGlob(){
        for (CategoryConfiguration c : getConfig().getCategoryConfigurations()){
            c.setInputGlob(c.getInputGlob().replace("${date_glob}", commandLineArguments.getDateGlob()));
        }
    }

    public boolean isCleanUpOutputDir() {
        return commandLineArguments.isCleanUpOutputDir();
    }

    public String getLocalTestCategory() {
        return commandLineArguments.getLocalTestCategory();
    }

    public File getLocalTestInputFile() {
        return commandLineArguments.getLocalTestInputFile();
    }

    public File getLocalOutputDirectory() {
        return commandLineArguments.getLocalOutputDirectory();
    }

    public String getDateGlob() {
        return commandLineArguments.getDateGlob();
    }
}
