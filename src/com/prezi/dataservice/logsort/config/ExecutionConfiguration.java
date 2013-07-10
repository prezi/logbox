package com.prezi.dataservice.logsort.config;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExecutionConfiguration {

    private static Log log = LogFactory.getLog(ExecutionConfiguration.class);
    private static Options cliOptions;

    private ExecutionMode executionMode;
    private LogSortConfiguration ruleConfig;
    private Date startDate;
    private Date endDate;
    private String localTestInputFile;

    public String getLocalTestInputFile() {
        return localTestInputFile;
    }

    public void setLocalTestInputFile(String localTestInputFile) {
        this.localTestInputFile = localTestInputFile;
    }

    public String getLocalTestCategory() {
        return localTestCategory;
    }

    public void setLocalTestCategory(String localTestCategory) {
        this.localTestCategory = localTestCategory;
    }

    private String localTestCategory;


    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
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

    public LogSortConfiguration getRuleConfig() {
        return ruleConfig;
    }

    public void setRuleConfig(LogSortConfiguration ruleConfig) {
        this.ruleConfig = ruleConfig;
    }

    private static void printCommandLineHelp(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar logsort.jar [OPTION]", options);
    }

    private static void failWithCliParamError(final String error) {
        printCommandLineHelp(cliOptions);
        System.err.println("ERROR: " + (error));
        System.exit(101);

    }

    private static Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption("r", "run", false, "Run logsort on hadoop");
        options.addOption("l", "local-test", false, "Run logsort locally");

        options.addOption(
                OptionBuilder
                        .withLongOpt("start-date")
                        .withDescription("The date to run logsort from (YYYY-MM-DD)")
                        .hasArg()
                        .withArgName("date")
                        .create("s")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("end-date")
                        .withDescription("The date to run logsort until (YYYY-MM-DD)")
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
                        .withLongOpt("input-file")
                        .withDescription("path to the input file in local-test mode")
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
                        .create("c")
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

        return options;
    }

    public static ExecutionConfiguration setupFromCLArgs(String[] args) {

        ExecutionConfiguration execConfig = new ExecutionConfiguration();

        cliOptions = createCommandLineOptions();

        CommandLineParser parser = new BasicParser();

        try {
            CommandLine cli = parser.parse(cliOptions, args);


            if (!cli.hasOption("run") && !cli.hasOption("local-test"))
                failWithCliParamError("Please choose from --run or --local execution modes");

            if (cli.hasOption("run") && cli.hasOption("local-test"))
                failWithCliParamError("Please choose only one from --run or --local execution modes");

            if (cli.hasOption("run")) {
                execConfig.setExecutionMode(ExecutionMode.HADOOP);
                if (!cli.hasOption("start-date")) failWithCliParamError("Please specify a start-date");
                if (!cli.hasOption("end-date")) failWithCliParamError("Please specify an end-date");

                String startDateStr = cli.getOptionValue("start-date");
                String endDateStr = cli.getOptionValue("end-date");
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

                try {
                    execConfig.setStartDate(dateFormat.parse(startDateStr));
                } catch (java.text.ParseException e) {
                    failWithCliParamError("Invalid date for start date.\n" + endDateStr.toString());
                }

                try {
                    execConfig.setEndDate(dateFormat.parse(endDateStr));
                } catch (java.text.ParseException e) {
                    failWithCliParamError("Invalid date for end date.\n" + endDateStr.toString());
                }
            } else {
                execConfig.setExecutionMode(ExecutionMode.LOCAL);


                if (!cli.hasOption("input-file")) {
                    failWithCliParamError("Running in local mode: Please specify an input file");
                }

                execConfig.setLocalTestInputFile(cli.getOptionValue("input-file"));

                if (!new File(execConfig.getLocalTestInputFile()).isFile()) {
                    log.error("Cannot read input file " + execConfig.getLocalTestInputFile());
                    System.exit(104);
                }


                if (!cli.hasOption("test-category")) {
                    failWithCliParamError("Running in local mode: Please specify a test category");
                }

                execConfig.setLocalTestCategory(cli.getOptionValue("test-category"));
            }

            String configFileName = "config.json";
            File configFile;

            if (cli.hasOption("config")) {
                configFileName = cli.getOptionValue("config");
            }
            configFile = new File(configFileName);

            try {
                execConfig.setRuleConfig(LogSortConfiguration.loadConfig(configFile));
                if ( execConfig.getExecutionMode() == ExecutionMode.LOCAL){
                    execConfig.getRuleConfig().applyFilters(new String[]{execConfig.getLocalTestCategory()});
                }

                if (cli.hasOption("rule-filters")) {
                    String[] filters = cli.getOptionValue("rule-filters").split(",");
                    execConfig.getRuleConfig().applyFilters(filters);
                }
            } catch (FileNotFoundException e) {
                failWithCliParamError("Config file " + configFileName + " doesn't exists or not readable");
            } catch (JsonSyntaxException e) {
                log.error("Syntax error in the JSON file " + configFile + ": " + e);
                System.exit(102);
            }
        } catch (InvalidParameterException e) {
            log.error(e.getMessage());
            System.exit(103);
        } catch (ParseException e) {
            failWithCliParamError("Cannot parse parameters: " + e.getMessage());
        }

        return null;
    }
}
