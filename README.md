#Logbox

Logbox is an efficient hadoop-based tool for tidying up huge amount of unstructured logs. 
With logbox your logs lines can be reshaped and stored into several buckets in a single execution.

## What, exactly?

Imagine you have a huge unstructured log file at *s3://example-logs/example-2013-10-22_00012.lzo* a chuck of which looks like this:

```
2013-10-22 12:12:12 app321 registration userid=12345 source=direct
2013-10-22 12:12:13 app456 payment_event:321 userid=34567 ENJOY $59.00
2013-10-22 12:12:14 app567 some dummy event we don't care about right now
2013-10-22 12:12:14 app567 so simply leave it here
2013-10-22 12:12:13 app456 payment_event:443 userid=12345 PRO $159.00
2013-10-22 12:12:13 app456 registration userid=23456 source=adwords
```

You want to clean this up and have two separate log files, one for the *payment records*, which will be
stored in *s3://example-logs/logbox/registration*
and the other one for *registrations*, which will be sored at  *s3://example-logs/logbox/payment*. 
Also, you want log lines to be put into a directory nominated by the date the line is associated with.
Finally, you want to get rid of the clutter and transform these lines into a better shape.

Let's look at this configuration file (more explanation below):

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

After executing logbox, you will have 2 new directories with the following content:

 1. ```s3://example-logs/logbox/registration/2013-10-22/example-2013-10-22```

    2013-10-22 12:12:12 12345 direct
    2013-10-22 12:12:13 23456 adwords


 2. ```s3://example-logs/logbox/payment/2013-10-22/example-2013-10-22```

    2013-10-22 12:12:13 34567 ENJOY 59.00
    2013-10-22 12:12:13 12345 PRO 159.00


When you execute logbox, this is what happens:

1. You will have two log 

## Install LogBox

1. Checkout this project
2. Download the dependencies and build the jar file containing all the dependencies:
    
     `mvn clean compile assembly:single`

You will find your jar file here: 

    target/logbox-1.0-SNAPSHOT-jar-with-dependencies.jar

## Run LogBox

Logbox has two execution modes

1. Local test
2. Production execution

### Test mode:
 
*Take a look at the [examples](https://github.com/prezi/logbox/tree/master/example).*

In this execution mode LogBox runs locally (without hadoop) and simulates the execution of the job. It prints to stdout the files and the lines logbox would emit data into. 
This way you can check wether your regexps are correct and you specified the input and output paths correctly.

Assuming that you have the following [input file](https://github.com/prezi/logbox/blob/master/example/example-2013-10-22_00000):


You would like to separate registrations and payments from this file into the `registration` and `payment` folder.

In the [config.json](https://github.com/prezi/logbox/blob/master/example/config.json) file you can specify the rules that will be used to extract the required data:

Execute LogBox in local test mode:


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


|Manual||
|--- | --- |
| -rd,--run                           |Run logbox on hadoop|
| -rt,--local-test                    |Test logbox locally|
| -cf,--config <file>                 |path to the config.json file|
| -s,--start-date <date>              |The date to run logbox from (YYYY-MM-DD)
| 				      |only applies to hadoop execution|
| -e,--end-date <date>                |The date to run logbox until (YYYY-MM-DD) - only applies to hadoop execution|
| 				      |only applies to hadoop execution|
| -f,--rule-filters <filter-list>     |Comma separated list of the rules to be executed, in the form of <logcategory>.<rulename>|
|                                     |By default, each rule is executed|
| -i,--input-location <file>          |Path of input file|
|                                     |Only applies to local test|
| -tc,--test-category <category>      |Log category to test in local test|
|                                     |Only applies to local test|




### Work mode

Usage:

**hadoop jar target/logbox-1.0-SNAPSHOT-jar-with-dependencies.jar com.prezi.logbox.main.App --run -s 2013-07-21 -e 2013-07-23 -cf config.json**

With start and end date you can specify the time range to process. (If start equals end, you only consider logs from that certain day.)
You need a config file of json format describe your job: input and output path, file types and your rules.
For more information, see example_config.json



### Config file


#### Rules

Your rules must be defined in the config file (using json format). Your rules can be grouped into categories: for categries you can specify the input files ("input_glob"). It is useful if your logs are already sorted into folders according some properties.

For each rule you can give a description, a name, a regular expression to be matched ("match"), the output format("output_format"), describing which parts of the original line should be kept, an output location ("output_location") and a date ("started_at") that tells when this rule (or the feature we log about) was intoduced.


#### Input/Output location and properties

Also the input and output folder and the compression types must be defined in the config file (in case of work mode).
Logbox supports [LZO compression](https://github.com/twitter/hadoop-lzo) but it also works with uncompressed text files. 
If you choose lzo as output_compression, the Logbox will index your output files. This can take a while.
Another useful feature that you can specify with a regexp how the filenames produced by LogBox should look like. This can be very useful if you want to aggregate your results, e.g. instead of having files containing logs of a 1-hour timespan, you can store your sorted logs in daily resolution. 
Why might you need this?
As indexing of lzo file scales not very well, it is not practical having hundreds or thousend of small lzo files. 

#### Notes

When running LogBox you may specify an already existing output directory, but be careful: if the job should write into an already existing file LogBox WILL overwrite them without notification (unlike a standard hadoob job). In our case this is the expected behaviour, we use a clever output location naming convention in order that no important files will be lost.
