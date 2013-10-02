package com.prezi.logbox.config;

import com.google.gson.JsonSyntaxException;
import com.prezi.FileUtils;
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
    private static Options cliOptions;

    private com.prezi.logbox.config.ExecutionMode executionMode;
    private LogBoxConfiguration ruleConfig;
    private Date startDate;
    private Date endDate;
    private File localTestInputFile;
    private File localOutputDirectory;

    public String getDateGlob() {
        return dateGlob;
    }

    private String dateGlob;

    public File getLocalOutputDirectory() {
        return localOutputDirectory;
    }

    public void setLocalOutputDirectory(File localOutputDirectory) {
        this.localOutputDirectory = localOutputDirectory;
    }

    private boolean cleanUpOutputDir = false;

    public void setCleanUpOutputDir(boolean cleanUpOutputDir) {
        this.cleanUpOutputDir = cleanUpOutputDir;
    }


    public boolean isCleanUpOutputDir() {
        return cleanUpOutputDir;
    }

    public File getLocalTestInputFile() {
        return localTestInputFile;
    }

    public void setLocalTestInputFile(File localTestInputFile) {
        this.localTestInputFile = localTestInputFile;
    }

    public String getLocalTestCategory() {
        return localTestCategory;
    }

    public void setLocalTestCategory(String localTestCategory) {
        this.localTestCategory = localTestCategory;
    }

    private String localTestCategory;


    public com.prezi.logbox.config.ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(com.prezi.logbox.config.ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }


    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public LogBoxConfiguration getConfig() {
        return ruleConfig;
    }

    public void setRuleConfig(LogBoxConfiguration config) {
        this.ruleConfig = config;
    }

    private static void printCommandLineHelp(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar logbox.jar [OPTION]", options);
    }

    private static void failWithCliParamError(final String error) {
        printCommandLineHelp(cliOptions);
        System.err.println("ERROR: " + (error));
        System.exit(101);

    }

    private static Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption("rd", "run", false, "Run logbox on hadoop");
        options.addOption("rt", "local-test", false, "Test logbox locally");
        options.addOption("c", "cleanup", false, "Remove output dir before running");

        options.addOption(
                OptionBuilder
                        .withLongOpt("start-date")
                        .withDescription("The date to run logbox from (YYYY-MM-DD)")
                        .hasArg()
                        .withArgName("date")
                        .create("s")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("end-date")
                        .withDescription("The date to run logbox until (YYYY-MM-DD)")
                        .hasArg()
                        .withArgName("date")
                        .create("e")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("config")
                        .withDescription("path to the config.json file")
                        .hasArg()
                        .withArgName("file")
                        .create("cf")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("input-location")
                        .withDescription("path to the input file in local modes")
                        .hasArg()
                        .withArgName("file")
                        .create("i")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("test-category")
                        .withDescription("log category to test the rules from in local-test-mode")
                        .hasArg()
                        .withArgName("category")
                        .create("tc")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("rule-filters")
                        .withDescription("Comma separated list of the rules to be executed, in the form of [logcategory].[rulename]")
                        .hasArg()
                        .withValueSeparator(',')
                        .withArgName("filter-list")
                        .create("f")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("local-output-dir")
                        .withDescription("Output directory for local test")
                        .hasArg()
                        .withArgName("directory")
                        .create("d")
        );

        return options;
    }

    public static ExecutionContext setupFromCLArgs(String[] args) {

        ExecutionContext context = new ExecutionContext();

        cliOptions = createCommandLineOptions();

        CommandLineParser parser = new BasicParser();

        try {
            CommandLine cli = parser.parse(cliOptions, args);


            if (!cli.hasOption("run") && !cli.hasOption("local-test"))
                failWithCliParamError("Please choose from --run or --local execution modes");

            if (cli.hasOption("run") && cli.hasOption("local-test"))
                failWithCliParamError("Please choose only one from --run or --local execution modes");

            if (cli.hasOption("run")) {
                context.setExecutionMode(com.prezi.logbox.config.ExecutionMode.HADOOP);
                if (!cli.hasOption("start-date")) failWithCliParamError("Please specify a start-date");
                if (!cli.hasOption("end-date")) failWithCliParamError("Please specify an end-date");

                String startDateStr = cli.getOptionValue("start-date");
                String endDateStr = cli.getOptionValue("end-date");
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

                try {
                    context.setStartDate(dateFormat.parse(startDateStr));
                } catch (java.text.ParseException e) {
                    failWithCliParamError("Invalid date for start date.\n" + endDateStr.toString());
                }

                try {
                    context.setEndDate(dateFormat.parse(endDateStr));
                } catch (java.text.ParseException e) {
                    failWithCliParamError("Invalid date for end date.\n" + endDateStr.toString());
                }

                if (context.getStartDate().compareTo(context.getEndDate()) > 0){
                    failWithCliParamError("Start date cannot precede end date.\n");
                }


                context.calculateDateGlob();
            } else {
                context.setExecutionMode(com.prezi.logbox.config.ExecutionMode.LOCAL_TEST);


                if (!cli.hasOption("input-location")) {
                    failWithCliParamError("Running in local mode: Please specify an input file");
                }

                File localInputFile = new File(cli.getOptionValue("input-location"));
                if (!localInputFile.canRead()) {
                    failWithCliParamError("Local input file " + cli.getOptionValue("input-location") + " doesn't exists of not readable");
                }

                context.setLocalTestInputFile(localInputFile);

                if (!cli.hasOption("test-category")) {
                    failWithCliParamError("Running in local mode: Please specify a test category");
                }

                context.setLocalTestCategory(cli.getOptionValue("test-category"));

                if (!cli.hasOption("local-output-dir")) {
                    failWithCliParamError("Running in local mode: Please specify an output directory");
                }

                File localOutputDirectory = new File(cli.getOptionValue("local-output-dir"));
                context.setLocalOutputDirectory(localOutputDirectory);
            }

            String configFileName = "config.json";
            File configFile;

            if (cli.hasOption("config")) {
                configFileName = cli.getOptionValue("config");
            }
            configFile = new File(configFileName);

            try {
                context.setRuleConfig(LogBoxConfiguration.loadConfig(configFile));
                if (context.getExecutionMode() == com.prezi.logbox.config.ExecutionMode.LOCAL_TEST) {
                    context.getConfig().applyFilters(new String[]{context.getLocalTestCategory()});
                }

                if (cli.hasOption("rule-filters")) {
                    String[] filters = cli.getOptionValue("rule-filters").split(",");
                    context.getConfig().applyFilters(filters);
                }
            } catch (FileNotFoundException e) {
                failWithCliParamError("Config file " + configFileName + " doesn't exists or not readable");
            } catch (JsonSyntaxException e) {
                log.error("Syntax error in the JSON file " + configFile + ": " + e);
                System.exit(102);
            }

            if (cli.hasOption("cleanup")) {
                context.setCleanUpOutputDir(true);
            }

        } catch (InvalidParameterException e) {
            log.error(e.getMessage());
            System.exit(103);
        } catch (ParseException e) {
            failWithCliParamError("Cannot parse parameters: " + e.getMessage());
        }

        try {
            if (FileUtils.protocolFromURI(context.getConfig().getInputLocationPrefix()) == Protocol.S3){

            }
        }
        catch (Exception e){
            //TODO: Handle me
        }

        return context;
    }

    public void calculateDateGlob(){
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
            dateGlob = "{" + StringUtils.join(dates.toArray(),',') + "}";
        }
    }

    public void compileDateGlob(){
        calculateDateGlob();
        for (CategoryConfiguration c : getConfig().getCategoryConfigurations()){
            c.setInputGlob(c.getInputGlob().replace("${date_glob}", dateGlob));
        }
    }
}
