package com.prezi.dataservice;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;


public class LogSort {

    public static class Map extends Mapper<LongWritable, Text, NullWritable, Text> {
        private Text word = new Text();
        private MultipleOutputs<NullWritable,Text> multipleOutputs;

        @Override
        protected void setup(Context context)
            throws IOException, InterruptedException
        {
            multipleOutputs = new MultipleOutputs<NullWritable, Text>(context);

        }

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            if (line.trim().isEmpty()){
                return;
            }

            word.set(line);
            String path = line.substring(0,1);
            multipleOutputs.write(NullWritable.get(), word, path + "/part");
        }

        @Override
        protected void cleanup(Context context)
            throws IOException, InterruptedException
        {
            multipleOutputs.close();
        }
    }

    public static void main(String[] args) throws Exception {
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
