package com.prezi.logbox.config;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


public class CommandLineOptions {

    Options _options;
    CommandLineOptions() {
        _options = new Options();
        create();
    }

    private void create() {
        _options.addOption("rd", "run", false, "Run logbox on hadoop");
        _options.addOption("rt", "local-test", false, "Test logbox locally");

        _options.addOption(
                OptionBuilder
                        .withLongOpt("start-date")
                        .withDescription("The date to run logbox from (YYYY-MM-DD)")
                        .hasArg()
                        .withArgName("date")
                        .create("s")
        );

        _options.addOption(
                OptionBuilder
                        .withLongOpt("end-date")
                        .withDescription("The date to run logbox until (YYYY-MM-DD)")
                        .hasArg()
                        .withArgName("date")
                        .create("e")
        );

        _options.addOption(
                OptionBuilder
                        .withLongOpt("config")
                        .withDescription("path to the config.json file")
                        .hasArg()
                        .withArgName("file")
                        .create("cf")
        );

        _options.addOption(
                OptionBuilder
                        .withLongOpt("input-location")
                        .withDescription("path to the input file in local modes")
                        .hasArg()
                        .withArgName("file")
                        .create("i")
        );

        _options.addOption(
                OptionBuilder
                        .withLongOpt("test-category")
                        .withDescription("log category to test the rules from in local-test-mode")
                        .hasArg()
                        .withArgName("category")
                        .create("tc")
        );

        _options.addOption(
                OptionBuilder
                        .withLongOpt("rule-filters")
                        .withDescription("Comma separated list of the rules to be executed, in the form of [logcategory].[rulename]")
                        .hasArg()
                        .withValueSeparator(',')
                        .withArgName("filter-list")
                        .create("f")
        );
    }

    Options getOptions() {
        return _options;
    }

    public void printCommandLineHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar logbox.jar [OPTION]", _options);
    }
}
