package com.prezi.logbox;

import com.hadoop.compression.lzo.DistributedLzoIndexer;
import com.hadoop.compression.lzo.LzopCodec;
import com.prezi.hadoop.OverwriteOutputDirTextOutputFormat;
import com.prezi.logbox.config.CategoryConfiguration;
import com.prezi.logbox.config.ExecutionContext;
import com.prezi.logbox.config.LogBoxConfiguration;
import com.prezi.logbox.config.Rule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;

enum LineCounter {
    SUBSTITUTED,
    EMITTED_IN_MAPPER,
    OMITTED_IN_MAPPER
}

enum MalformedRecord {
    FROM_MAPPER
}
enum FileCounter {
    INPUT_FILES
}
public class HadoopExecutor extends Configured implements Tool, Executor
{
    private static Log log = LogFactory.getLog(HadoopExecutor.class);
    private ExecutionContext context;

    private String[] outdirList;
    private Boolean indexing;

    ArrayList<String> ruleNames;
    public HadoopExecutor(ExecutionContext c) {
        this.context = c;
    }

    private void ruleNames() {
        ruleNames = new ArrayList<String>();
        LogBoxConfiguration logBoxConfiguration = this.context.getConfig();
        for (CategoryConfiguration c : logBoxConfiguration.getCategoryConfigurations()) {
          for (Rule r : c.getRules()) {
            ruleNames.add(r.getName());
          }
        }
    }
    public void execute(String[] cliArgs) throws Exception {

        Configuration conf = new Configuration();
        String temporalFilePrefix = context.getConfig().getTemporalFilePrefix();
        URI uri = URI.create(temporalFilePrefix);
        FileSystem fs = FileSystem.get(uri, conf);

        HashSet<String> directories = new HashSet<String>();

        int res = ToolRunner.run(conf, this, cliArgs);
        try{
            FileStatus[] fss = fs.listStatus(new Path(temporalFilePrefix));
            for (FileStatus status : fss) {
                Path path = status.getPath();
                BufferedReader br=new BufferedReader(new InputStreamReader(fs.open(path)));

                String line;
                line = br.readLine();
                while (line != null){
                    directories.add(context.getConfig().getOutputLocationBase() + line);
                    line=br.readLine();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            log.error("Temporary file not found. Indexing failed.");
        }
        log.info("Number of subdirectories: " + directories.size());
        int counter = 0;
        outdirList = new String[10];
        for (String dir : directories) {
            outdirList[counter%10] = dir;
            if (counter%10 == 9) {
                if (indexing) {
                    int exitCode = ToolRunner.run(new DistributedLzoIndexer(), outdirList);
                    outdirList = new String[10];
                } else {
                    for (String s : outdirList){
                        System.out.println(s);
                    }
                }

            }

            counter++;
        }
        int r = counter%10;
        String[] rest = new String[r];
        for (int i = 0; i< r; i++) {
            rest[i] = outdirList[i];
        }
        if (indexing) {

            int exitCode = ToolRunner.run(new DistributedLzoIndexer(), rest);
        } else {
            for (String s : rest){
                System.out.println(s);
            }
        }
        fs.delete(new Path(context.getConfig().getTemporalFilePrefix()), true);
    }

    public static Log getLog() {
        return log;
    }

    @Override
    public int run(String[] strings) throws Exception {

        Configuration conf = new Configuration();
        conf.setStrings("config.json", context.getConfig().toJSON());

        Job job = new Job(conf, "logbox");
        job.setJarByClass(HadoopExecutor.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        job.setMapperClass(SubstituteLineMapper.class);
        job.setReducerClass(SubstituteLineReducer.class);

        job.setJarByClass(HadoopExecutor.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(OverwriteOutputDirTextOutputFormat.class);
        LazyOutputFormat.setOutputFormatClass(job, OverwriteOutputDirTextOutputFormat.class);
        job.setNumReduceTasks(39);

        FileOutputFormat.setCompressOutput(job, false);
        indexing = false;

        String inputLocationPrefix = context.getConfig().getInputLocationPrefix();
        log.debug("inputLocationPrefix set to: " + inputLocationPrefix);

        context.compileDateGlob();
        log.debug("Date glob set to:" + context.getDateGlob());

        for (CategoryConfiguration c : context.getConfig().getCategoryConfigurations()) {
            String inputLocation = inputLocationPrefix + c.getInputGlob();
            log.info("Adding input glob: " + inputLocation);
            FileInputFormat.setInputPathFilter(job, IndexFilter.class);
            FileInputFormat.addInputPath(job, new Path(inputLocation));
        }

        if ( context.getConfig().getOutputCompression().equals("lzo")){
            FileOutputFormat.setCompressOutput(job, true);
            indexing = true;
            FileOutputFormat.setOutputCompressorClass(job, LzopCodec.class);

            //TODO: Add dependent job for indexing
        }


        outdirList = new String[10];

        FileOutputFormat.setOutputPath(job, new Path(context.getConfig().getOutputLocationBase()));

        try {
            job.waitForCompletion(true);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        Counters counters = job.getCounters();
        long duplicates = counters.findCounter(LineCounter.EMITTED_IN_MAPPER).getValue()
                - counters.findCounter(LineCounter.SUBSTITUTED).getValue();
/*
        System.out.println("Number of lines read by mappers: " + counters.findCounter(Task.Counter.MAP_INPUT_RECORDS).getValue() );
        System.out.println("Number of unchanged lines: " + counters.findCounter(LineCounter.OMITTED_IN_MAPPER).getValue());
        System.out.println("Number of lines written by reducers: " + counters.findCounter(LineCounter.SUBSTITUTED).getValue());
        System.out.println("Number of duplicates (filtered out by reducer) : " + duplicates);
        System.out.println("Number of malformed records from mapper: " + counters.findCounter(MalformedRecord.FROM_MAPPER).getValue());
        System.out.println("Number of malformed records from mapper: " + counters.findCounter(MalformedRecord.FROM_MAPPER).getValue());
        System.out.println("Number of input files: " + counters.findCounter(FileCounter.INPUT_FILES).getValue());
        
        ruleNames();
        for (String ruleName : ruleNames) {
            System.out.println("Number of records from rule type " + ruleName + ": " + counters.findCounter("RuleTypesInMapper", ruleName).getValue());
        }
*/
        return 0;
    }

}
