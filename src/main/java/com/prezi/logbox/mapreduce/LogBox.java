package com.prezi.logbox.mapreduce;

import com.hadoop.compression.lzo.LzopCodec;
import com.hadoop.mapreduce.LzoTextInputFormat;
import com.prezi.hadoop.IndexFilter;
import com.prezi.hadoop.OverwriteOutputDirTextOutputFormat;
import com.prezi.logbox.config.CategoryConfiguration;
import com.prezi.logbox.config.ExecutionContext;
import com.prezi.logbox.executor.HadoopExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.util.Tool;

import java.io.IOException;


public class LogBox extends Configured implements Tool {
    private static Log log = LogFactory.getLog(LogBox.class);
    private ExecutionContext executionContext;
    final private int DEFAULT_NUMBER_OF_REDUCERS = 20;

    public LogBox(ExecutionContext c) {
        this.executionContext = c;
    }

    private Job createJob(Configuration conf) throws IOException {
        Job job = new Job(conf, "logbox");
        job.setJarByClass(HadoopExecutor.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        conf.setBoolean("mapreduce.map.tasks.speculative.execution",false);
        conf.setBoolean("mapreduce.reduce.tasks.speculative.execution",false);

        // Reducers might take a long time to run
        conf.setInt("mapreduce.task.timeout",60*60*1000);

        job.setMapperClass(SubstituteLineMapper.class);
        job.setReducerClass(SubstituteLineReducer.class);

        job.setJarByClass(HadoopExecutor.class);

        if (executionContext.getConfig().getInputCompression().equals("lzo")) {
            job.setInputFormatClass(LzoTextInputFormat.class);
        } else {
            job.setInputFormatClass(TextInputFormat.class);
        }
        job.setOutputFormatClass(OverwriteOutputDirTextOutputFormat.class);


        int reducerNum = Integer.parseInt(executionContext.getConfig().getReducerNumberStr());

        if (reducerNum != 0) {
            job.setNumReduceTasks(reducerNum);
        } else {
            job.setNumReduceTasks(DEFAULT_NUMBER_OF_REDUCERS);
        }
        FileOutputFormat.setCompressOutput(job, false);

        String inputLocationPrefix = executionContext.getConfig().getInputLocationPrefix();

        for (CategoryConfiguration c : executionContext.getConfig().getCategoryConfigurations()) {
            String inputLocation = inputLocationPrefix + c.getInputGlob();
            log.info("Adding input glob: " + inputLocation);
            FileInputFormat.setInputPathFilter(job, IndexFilter.class);
            FileInputFormat.addInputPath(job, new Path(inputLocation));
        }

        if ( executionContext.getConfig().getOutputCompression().equals("lzo")){
            FileOutputFormat.setCompressOutput(job, true);
            FileOutputFormat.setOutputCompressorClass(job, LzopCodec.class);
        }

        FileOutputFormat.setOutputPath(job, new Path(executionContext.getConfig().getOutputLocationBase()));
        LazyOutputFormat.setOutputFormatClass(job, OverwriteOutputDirTextOutputFormat.class);

        return job;
    }

    @Override
    public int run(String[] strings) throws Exception {

        Configuration conf = new Configuration();
        conf.setStrings("config.json", executionContext.getConfig().toJSON());
        conf.set("date_glob", executionContext.getDateGlob());

        Job job = createJob(conf);
        Boolean executionSuccess = false;
        try {
            executionSuccess = job.waitForCompletion(true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return (executionSuccess ? 0 : 1);
    }
}
