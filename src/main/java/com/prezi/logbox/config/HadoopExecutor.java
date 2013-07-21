package com.prezi.logbox.config;

import com.hadoop.compression.lzo.DistributedLzoIndexer;
import com.hadoop.compression.lzo.LzoCodec;
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
    private ExecutionContext context;

    public static class Map extends Mapper<LongWritable, Text, NullWritable, Text> {
        private Text word = new Text();
        private MultipleOutputs<NullWritable, Text> multipleOutputs;

        private String inputPath;
        private String inputBaseName;
        private LogBoxConfiguration config;

        public String getBaseName(String location){
            String[] parts = location.split("/(?=[^/\\.]+(\\.*)$)");
            if ( config.getInputCompression().equals("lzo") && parts.length == 3 && parts[2].equals("lzo") ){
                return parts[1] + "." + parts[2];
            } else{
                return parts[1];
            }
        }

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException {
            inputPath = ((FileSplit)context.getInputSplit()).getPath().toString();
            multipleOutputs = new MultipleOutputs<NullWritable, Text>(context);

            String configJSON = (String)context.getConfiguration().get("config.json");
            config = LogBoxConfiguration.fromConfig(configJSON);
            inputBaseName = getBaseName(inputPath);
        }

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            if (line.trim().isEmpty()) {
                return;
            }

            multipleOutputs.write(NullWritable.get(), new Text(inputBaseName + " " + line), "sample-path/part");
        }

        @Override
        protected void cleanup(Context context)
                throws IOException, InterruptedException {
            multipleOutputs.close();
        }
    }

    public HadoopExecutor(ExecutionContext c) {
        this.context = c;
    }

    public void execute() throws IOException {

        Configuration conf = new Configuration();
        conf.setStrings("config.json",context.getRuleConfig().toJSON());

        Job job = new Job(conf, "logbox");

        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(Map.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path("/Users/zoltanctoth/src/logbox/input_sample"));


        if ( context.getRuleConfig().getOutputCompression() == "lzo"){
            FileOutputFormat.setCompressOutput(job, true);
            FileOutputFormat.setOutputCompressorClass(job, LzoCodec.class);
        }



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
