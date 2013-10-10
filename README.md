Logbox is an efficient hadoop-based tool for processing huge amount of unstructured logs. 
With logbox your logs can be sorted into several buckets, each described with a regural expression.

=== Install LogBox ===

1. Checkout this project
2. Download the dependencies and build the jar file containing all the dependencies:
		mvn clean compile assembly:single

You will find your jar file here: $LOGBOX_HOME/target/logbox-1.0-SNAPSHOT-jar-with-dependencies.jar 


=== Run LogBox ===

hadoop jar $LOGBOX_HOME/target/logbox-1.0-SNAPSHOT-jar-with-dependencies.jar com.prezi.logbox.main.App
Running the program without parameters, you get the manual:
 -cf,--config <file>                 path to the config.json file
 -d,--local-output-dir <directory>   Output directory for local test
 -e,--end-date <date>                The date to run logbox until
                                     (YYYY-MM-DD)
 -f,--rule-filters <filter-list>     Comma separated list of the rules to
                                     be executed, in the form of
                                     [logcategory].[rulename]
 -i,--input-location <file>          path to the input file in local modes
 -rd,--run                           Run logbox on hadoop
 -rt,--local-test                    Test logbox locally
 -s,--start-date <date>              The date to run logbox from
                                     (YYYY-MM-DD)
 -tc,--test-category <category>      log category to test the rules from
                                     in local-test-mode

Logbox has two modes:
1. "Test mode"
2. "Work mode"


=== Test mode: ===


it runs locally (without hadoop) and simulates the run of the job, and it prints to stdout what would logbox write to whick files. So you can check if regexps are correct and you specified the input and output paths correctly.

java -jar target/logbox-1.0-SNAPSHOT-jar-with-dependencies.jar -i sample_input/input_file.txt -s 2013-07-21 -e 2013-07-21 -rt -tc my_category -f my_category.rule_1,my_category.rule_2 -cf config.json

This will only sort those lines that match to the rules you specified after -f, processes these lines ant then prints to stdout in the following format:
result_file_name 	<- 		line_after_processing

=== Work mode ===
/Users/julcsi/hadoop/hadoop-1.0.3/bin/hadoop jar target/logbox-1.0-SNAPSHOT-jar-with-dependencies.jar com.prezi.logbox.App --run -s 2013-07-21 -e 2013-07-23 -cf config-dev.json

With start and end date you can specify the time range to process. (If start equals end, you only consider logs from that certain day.)
You need a config file of json format describe your job: input and output path, file types and your rules.
For more information, see example_config.json


=== Notes ===
Logbox supports LZO compression (https://github.com/twitter/hadoop-lzo) but it also works with uncompressed text files.

You may specify an already existing output directory when running on hadoop. In this case files might be overwritten (if the program has to write into a file that has already been existed).