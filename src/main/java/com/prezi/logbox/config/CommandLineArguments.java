package com.prezi.logbox.config;

import com.prezi.IgnoreUnknownParametersParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: julcsi
 * Date: 10/8/13
 * Time: 5:32 PM
 */
public class CommandLineArguments {
    private Date startDate;
    private Date endDate;
    private String dateGlob;
    private boolean cleanUpOutputDir = false;
    private String localTestCategory;
    private CommandLineOptions commandLineOptions;
    private ExecutionMode executionMode;

    private File localTestInputFile;
    private File localOutputDirectory;
    private File configFile;
    private String[] filters;
    private String configFileName;

    public CommandLineArguments() {
        commandLineOptions = new CommandLineOptions();
    }

    public void setCleanUpOutputDir(boolean cleanUpOutputDir) {
        this.cleanUpOutputDir = cleanUpOutputDir;
    }

    public boolean isCleanUpOutputDir() {
        return cleanUpOutputDir;
    }

    public String getLocalTestCategory() {
        return localTestCategory;
    }

    public void setLocalTestCategory(String localTestCategory) {
        this.localTestCategory = localTestCategory;
    }

    public void calculateDateGlob() {
        if (startDate == null){
            throw new IllegalArgumentException("No start date specified.");
        }
        if (endDate == null){
            throw new IllegalArgumentException("No end date specified.");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        if (startDate == endDate){
            dateGlob = dateFormat.format(startDate);
        } else {
            ArrayList<String> dates = new ArrayList<String>();

            Calendar c = Calendar.getInstance();
            c.setTime(startDate);
            while (c.getTime().compareTo(endDate) < 1){
                dates.add(dateFormat.format(c.getTime()));
                c.add(Calendar.DATE,1);
            }
            dateGlob = "{" + StringUtils.join(dates.toArray(), ',') + "}";
        }

    }

    private void failWithCliParamError(final String error) {
        commandLineOptions.printCommandLineHelp();
        System.err.println("ERROR: " + (error));
        System.exit(101);
    }

    private void checkModeParameter(CommandLine commandLine){
        if (!commandLine.hasOption("run") && !commandLine.hasOption("local-test"))
            failWithCliParamError("Please choose from --run or --local execution modes");

        if (commandLine.hasOption("run") && commandLine.hasOption("local-test"))
            failWithCliParamError("Please choose only one from --run or --local execution modes");
    }

    private Date parseDate(CommandLine commandLine, String option) {
        String dateString = commandLine.getOptionValue(option);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date = new Date();
        try {
            date = dateFormat.parse(dateString);
        } catch (java.text.ParseException e) {
            failWithCliParamError("Invalid date for start date.\n" + dateString.toString());
        }
        return date;
    }

    private void processDates(CommandLine commandLine) {
        if (!commandLine.hasOption("start-date")) failWithCliParamError("Please specify a start-date");
        if (!commandLine.hasOption("end-date")) failWithCliParamError("Please specify an end-date");

        startDate = parseDate(commandLine, "start-date");
        endDate =parseDate(commandLine, "end-date");

        calculateDateGlob();
    }

    private void processLocalInput(CommandLine commandLine){
        if (!commandLine.hasOption("input-location")) {
            failWithCliParamError("Running in local mode: Please specify an input file");
        }

        File localInputFile = new File(commandLine.getOptionValue("input-location"));

        if (!localInputFile.canRead()) {
            failWithCliParamError("Local input file " + commandLine.getOptionValue("input-location") + " doesn't exists of not readable");
        }

        setLocalTestInputFile(localInputFile);
    }

    private void processLocalTestCategory(CommandLine commandLine){
        if (!commandLine.hasOption("test-category")) {
            failWithCliParamError("Running in local mode: Please specify a test category");
        }

        setLocalTestCategory(commandLine.getOptionValue("test-category"));

        if (!commandLine.hasOption("local-output-dir")) {
            failWithCliParamError("Running in local mode: Please specify an output directory");
        }
    }

    private void processLocalOutput(CommandLine commandLine) {
        localOutputDirectory = new File(commandLine.getOptionValue("local-output-dir"));
    }

    private void processConfigOption(CommandLine commandLine) {

        if (commandLine.hasOption("config")) {
            configFileName = commandLine.getOptionValue("config");
        } else {
            // default value
            configFileName = "config.json";
        }
        configFile = new File(configFileName);
    }

    public void parseArguments(String[] args) {
        CommandLineParser parser = new IgnoreUnknownParametersParser();

        try {
            CommandLine commandLine = parser.parse(commandLineOptions.getOptions(), args);
            checkModeParameter(commandLine);

            if (commandLine.hasOption("run")) {
                setExecutionMode(com.prezi.logbox.config.ExecutionMode.HADOOP);
                processDates(commandLine);
            } else {
                setExecutionMode(com.prezi.logbox.config.ExecutionMode.LOCAL_TEST);
                processLocalInput(commandLine);
                processLocalTestCategory(commandLine);
                processLocalOutput(commandLine);
            }
            processConfigOption(commandLine);

            if (commandLine.hasOption("cleanup")) {
                setCleanUpOutputDir(true);
            }
            if (commandLine.hasOption("rule-filters")) {
                filters = commandLine.getOptionValue("rule-filters").split(",");
            } else {
                filters = null;
            }
        } catch (InvalidParameterException e) {
            System.err.println("ERROR " + e.getMessage());
            System.exit(103);
        } catch (ParseException e) {
            failWithCliParamError("Cannot parse parameters: " + e.getMessage());
        }
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public void setLocalTestInputFile(File localTestInputFile) {
        this.localTestInputFile = localTestInputFile;
    }

    public File getConfigFile() {
        return configFile;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public String[] getFilters() {
        return filters;
    }

    public void configFileNotFound() {
        failWithCliParamError("Config file " + configFileName + " doesn't exists or not readable");
    }

    public boolean hasFilters() {
        return filters != null;
    }

    public String getDateGlob() {
        return dateGlob;
    }

    public File getLocalTestInputFile() {
        return localTestInputFile;
    }

    public File getLocalOutputDirectory() {
        return localOutputDirectory;
    }
}