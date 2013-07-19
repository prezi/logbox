package com.prezi.logbox;

import com.prezi.logbox.config.ExecutionConfiguration;
import com.prezi.logbox.config.ExecutionMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;


public class App {
    private static Log log = LogFactory.getLog(App.class);

    public static void main(String[] args) throws Exception {


        ExecutionConfiguration execConfig = ExecutionConfiguration.setupFromCLArgs(args);

        ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream());
        out.writeObject(execConfig.getRuleConfig());
        out.close();

        System.out.println(out.toString());
        System.exit(0);

        Executor executor = new Executor();
        if (execConfig.getExecutionMode() == ExecutionMode.LOCAL) {
            executor = new LocalExecutor(execConfig);
        }
        executor.execute();

    }
}
