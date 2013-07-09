package com.prezi.dataservice;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.gson.Gson;
import org.apache.commons.cli.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import static org.apache.commons.cli.OptionBuilder.*;


public class LogSort {

    public static class Map extends Mapper<LongWritable, Text, NullWritable, Text> {
        private Text word = new Text();
        private MultipleOutputs<NullWritable, Text> multipleOutputs;

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException {
            multipleOutputs = new MultipleOutputs<NullWritable, Text>(context);

        }

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            if (line.trim().isEmpty()) {
                return;
            }

            word.set(line);
            String path = line.substring(0, 1);
            multipleOutputs.write(NullWritable.get(), word, path + "/part");
        }

        @Override
        protected void cleanup(Context context)
                throws IOException, InterruptedException {
            multipleOutputs.close();
        }
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
                        .create("c")
        );

        options.addOption(
                OptionBuilder.withValueSeparator(',')
                        .withLongOpt("rule-filters")
                        .withDescription("Comma separated list of the rules to be executed, in the form of [logcategory].[rulename]")
                        .hasArg()
                        .withArgName("filters")
                        .create("f")
        );

        return options;
    }

    private static void printCommandLineHelp(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar logsort.jar [OPTION]", options);
    }

    private static Options cliOptions;

    private static void failWithCliParamError(final String error) {
        printCommandLineHelp(cliOptions);
        System.err.println("ERROR: " + (error));
        System.exit(101);

    }

    private static void parseRuleConfig(File configFile){
        Gson gson = new Gson();

    }


    public static void main(String[] args) throws Exception {

        cliOptions = createCommandLineOptions();

        CommandLineParser parser = new BasicParser();

        try {
            CommandLine cli = parser.parse(cliOptions, args);

            if (!cli.hasOption("run") && !cli.hasOption("local-test"))
                failWithCliParamError("Please choose from --run or --local execution modes");

            if (cli.hasOption("run")) {
                if (!cli.hasOption("start-date")) failWithCliParamError("Please specify a start-date");
                if (!cli.hasOption("end-date")) failWithCliParamError("Please specify an end-date");

                String startDateStr = cli.getOptionValue("start-date");
                String endDateStr = cli.getOptionValue("end-date");
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

                Date startDate;
                Date endDate;

                try {
                    startDate = dateFormat.parse(startDateStr);
                } catch (java.text.ParseException e) {
                    failWithCliParamError("Invalid date for start date.\n" + endDateStr.toString());
                }

                try {
                    endDate = dateFormat.parse(endDateStr);
                } catch (java.text.ParseException e) {
                    failWithCliParamError("Invalid date for end date.\n" + endDateStr.toString());
                }

                String configFileName = "config.json";
                File configFile;

                if (cli.hasOption("config")){
                    configFileName = cli.getOptionValue("config");
                }
                configFile = new File(configFileName);
                if ( !configFile.isFile() || !configFile.canRead()){
                    failWithCliParamError("Config file " + configFileName + " doesn't exists or not readable");
                }

                parseRuleConfig( configFile );







            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(101);
        }

        System.exit(0);
        Configuration conf = new Configuration();

        Job job = new Job(conf, "logsort");

        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(Map.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path("/Users/zoltanctoth/system.gems"));
        FileOutputFormat.setOutputPath(job, new Path("/Users/zoltanctoth/src/logsort/hadoop.sample.out"));

        job.waitForCompletion(true);
    }
}