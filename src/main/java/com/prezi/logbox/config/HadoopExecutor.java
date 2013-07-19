package com.prezi.logbox.config;

import com.prezi.logbox.Executor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;

public class HadoopExecutor extends Executor {
    private ExecutionConfiguration execConfig;

    public static class Map extends Mapper<LongWritable, Text, NullWritable, Text> {
        private Text word = new Text();
        private MultipleOutputs<NullWritable, Text> multipleOutputs;

        private String inputPath;
        private String configJson;

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException {
            inputPath = ((FileSplit)context.getInputSplit()).getPath().toString();
            multipleOutputs = new MultipleOutputs<NullWritable, Text>(context);
            configJson = (String)context.getConfiguration().get("config.json");

        }

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            if (line.trim().isEmpty()) {
                return;
            }

            String path = line.substring(0, 1);
            multipleOutputs.write(NullWritable.get(), new Text(configJson + " " + line), path + "/part");
        }

        @Override
        protected void cleanup(Context context)
                throws IOException, InterruptedException {
            multipleOutputs.close();
        }
    }

    public HadoopExecutor(com.prezi.logbox.config.ExecutionConfiguration c) {
        this.execConfig = c;
    }

    public void execute() throws IOException {

        Configuration conf = new Configuration();
        conf.setStrings("config.json","hello_config");

        Job job = new Job(conf, "logbox");

        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(Map.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path("/Users/zoltanctoth/system.gems"));
        FileOutputFormat.setOutputPath(job, new Path("/Users/zoltanctoth/src/logbox/hadoop.sample.out"));

        try {
            job.waitForCompletion(true);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

}
