#Logbox

Logbox is an efficient Hadoop-based tool for tidying up huge amounts of unstructured logs. 
With Logbox your logs lines can be reshaped and stored into several output locations within a single execution.

## What, exactly?

Imagine you have a huge unstructured log file at *s3://example-logs/example-2013-10-22_00000.lzo* a chunck of which looks like this:

```
2013-10-22 12:12:12 app321 registration userid=12345 source=direct
2013-10-22 12:12:13 app456 payment_event:321 userid=34567 ENJOY $59.00
2013-10-22 12:12:14 app567 some dummy event we don't care about right now
2013-10-22 12:12:14 app567 so simply leave it here
2013-10-22 12:12:13 app456 payment_event:443 userid=12345 PRO $159.00
2013-10-22 12:12:13 app456 registration userid=23456 source=adwords
```

You want to clean this up and have two separate log files: 
 
 1. one for the *regsistrations*, which will be stored in *s3://example-logs/logbox/registration*
 2. an other one for *payment records*, which will be stored at  *s3://example-logs/logbox/payment*. 

Also, you want log lines to be put into a directory nominated by the date the line is associated with.

Finally, you want to get rid of the clutter and transform these lines into better shape.


Let's look at this configuration file (there's more explanation below):

```javascript
{
    "categories": [
        {
            "input_glob": "example-${date_glob}_*",
            "name": "example_category",
            "rules": [
                {
                    "name": "registration",
                    "description": "Separate registrations by source",

                    "match": "^(?<date>20[0-9\\-]{8}) (?<time>[0-9:]{8}) (?<appserver>[a-z\\-0-9]+) registration userid=(?<userid>[0-9]+) source=(?<source>\\w+)$",
                    "output_format": "${date} ${time} ${userid} ${source}",
                    "output_location": "registration/${date}/${input_basename}"
                },
                {
                    "name": "payment",
                    "description": "Separate payment events",

                    "match": "^(?<date>20[0-9\\-]{8}) (?<time>[0-9:]{8}) (?<appserver>[a-z\\-0-9]+) payment_event:[0-9]+ userid=(?<userid>[0-9]+) (?<license>\\w+) \\$(?<amount>[0-9\\.]+)$",
                    "output_format": "${date} ${time} ${userid} ${license} ${amount}",
                    "output_location": "payment/${date}/${input_basename}"
                }
            ]
        }
    ],
    "max_line_length" : "100000",
    "reducer_number": "40",
 
    "input_filename" : "^(?<name>.*)\\-(?<date>20[0-9\\-]{8})_(?<hour>000[0-2][0-9])$",
    "output_name_template" : "${name}-${date}",

    "input_compression": "lzo",
    "input_location_prefix": "s3://example-logs/",
 
    "output_compression": "lzo",
    "output_location": "s3://example-logs/logbox/"
}
```

After executing Logbox, you will have two new directories with the following content:

```
s3://example-logs/logbox/registration/2013-10-22/example-2013-10-22:

    2013-10-22 12:12:12 12345 direct
    2013-10-22 12:12:13 23456 adwords


s3://example-logs/logbox/payment/2013-10-22/example-2013-10-22

    2013-10-22 12:12:13 34567 ENJOY 59.00
    2013-10-22 12:12:13 12345 PRO 159.00
```

It's really that simple.


## Installation

1. Checkout this project
2. Download the dependencies and then build the jar file containing all the dependencies:
    
     `mvn clean compile assembly:single`

You will find your jar file here: 

    target/logbox-1.0-SNAPSHOT-jar-with-dependencies.jar


## Running LogBox

### Logbox has two execution modes

1. Local test execution
2. Production execution

### Command line parameters

| Parameter     | Description |
| ------------- | ------------- |
| -rd,--run                           |Run Logbox on Hadoop|
| -rt,--local-test                    |Test Logbox locally|
| -cf,--config <file>                 |path to the config.json file|
| -s,--start-date <date>              |The date to run Logbox from (YYYY-MM-DD)
| 				      |only applies to hadoop execution|
| -e,--end-date <date>                |The date to run Logbox until (YYYY-MM-DD) - only applies to Hadoop execution|
| 				      |only applies to hadoop execution|
| -f,--rule-filters <filter-list>     |Comma separated list of the rules to be executed, in the form of <logcategory>.<rulename>|
|                                     |By default, each rule is executed|
| -i,--input-location <file>          |Path of input file|
|                                     |Only applies to local test|
| -tc,--test-category <category>      |Log category to test in local test|
|                                     |Only applies to local test|


### Local test:
 
*Take a look at the [examples](https://github.com/prezi/logbox/tree/master/example).*

In this execution mode Logbox runs locally (without Hadoop) and simulates the execution of the job. 
It prints the names of the files and the lines  within those files to *stdout*. 
This way you can check whether your regexps are correct and if you specified the input and output paths correctly.

Executing Logbox in local test mode:

    java -jar target/logbox-1.0-SNAPSHOT-jar-with-dependencies.jar --local-test \
       --input-location example/example-2013-10-22_00000 \
       --test-category example_category \ 
       --config example/config.json


Check out the results:

```
INFO: Added rule example_category -> registration
INFO: Added rule example_category -> payment
INFO: Using basename: example-2013-10-22

registration/2013-10-22/example-2013-10-22 <- 2013-10-22 12:12:12 12345 direct
payment/2013-10-22/example-2013-10-22      <- 2013-10-22 12:12:13 34567 ENJOY 59.00
payment/2013-10-22/example-2013-10-22      <- 2013-10-22 12:12:13 12345 PRO 159.00
registration/2013-10-22/example-2013-10-22 <- 2013-10-22 12:12:13 23456 adwords
```


### Production mode

Usage:

    hadoop jar target/logbox-1.0-SNAPSHOT-jar-with-dependencies.jar com.prezi.logbox.main.App 
        --run 
        -s 2013-07-21
        -e 2013-07-23 
        -cf config.json

The start and end dates specify the time range to process. (If start equals end, you only consider logs from that certain day.)


### The config file

Create a config file in JSON format. This is the hardest part of working with Logbox :)

The config file should contain the following information:

 * ```input_location_prefix```: Where can the input logs be found? 
 * ```output_location```: Where do you want the derived files to be stored?
 * ```input_compression``` and ```output_compression```: What is your input format and what is your desired output format? Logbox supports (splittable) lzo compression, so here you can write either "lzo" or "none". In case you want lzo files as your output, Logbox will create index files as well.
 * ```input_filename``` and ```output_name_template```: The naming convention of your filtered logfiles. You can specify with a regexp how the directories produced by LogBox should look. This can be very useful if you want to aggregate your results, e.g. instead of having files containing logs of a 1-hour timespan, you can store your sorted logs in daily resolution.  Why might you need this? As the indexing of lzo files doesn't scale very well, it is not practical having hundreds or thousands of small lzo files.
 * ```max_line_length```: Maximum line length, all files longer than this number will be omitted 
 * ```reducer_number```: Number of reducers for the MapReduce job
 * Filtering/Transformation rules: You can define categories. Each category has its own input path, described by the ```input_glob``` property. In each category, rules can be defined; these filters and transformations will be applied to the files that belong to the category. For each filtering rule you must provide the following properties:
  * a ```name``` and a ```description``` of your rule. This will help your co-workers to understand your intentions.
  * ```match```: a regular expression, possibly defining named groups, to identify fields of interest in the processed logline
  * ```output_format```: How should the processed lines look?
  * ```output_location```: The output directory of this rule. 

*Take a look at the [examples](https://github.com/prezi/logbox/tree/master/example)* which show you how to set up a simple config.

#### Notes

1. When running Logbox you may specify an already existing output directory, but be careful: 
if the job should write into an already existing file Logbox will overwrite them without notification 
(unlike a standard hadoob job).
2. Logbox will eliminate duplicate log lines from the input files.

**Have fun using Logbox!**


