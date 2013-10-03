package com.prezi.logbox;

import com.hadoop.compression.lzo.LzopCodec;
import com.prezi.FileUtils;
import com.prezi.hadoop.OverwriteOutputDirTextOutputFormat;
import com.prezi.logbox.config.CategoryConfiguration;
import com.prezi.logbox.config.ExecutionContext;
import com.prezi.logbox.config.LogBoxConfiguration;
import com.prezi.logbox.config.Rule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
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
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;


public class HadoopExecutor extends Configured implements Tool, Executor
{
    private static Log log = LogFactory.getLog(HadoopExecutor.class);
    private ExecutionContext context;

    public HadoopExecutor(ExecutionContext c) {
        this.context = c;
    }

    public void execute(String[] cliArgs) throws Exception {
        int res = ToolRunner.run(new Configuration(), this, cliArgs);
    }

    @Override
    public int run(String[] strings) throws Exception {

        Configuration conf = new Configuration();
        conf.setStrings("config.json", context.getConfig().toJSON());

        Job job = new Job(conf, "logbox");
        job.setJarByClass(HadoopExecutor.class);

        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(Map.class);
        job.setJarByClass(HadoopExecutor.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(OverwriteOutputDirTextOutputFormat.class);
        FileOutputFormat.setCompressOutput(job, false);

        String inputLocationPrefix = context.getConfig().getInputLocationPrefix();
        log.debug("inputLocationPrefix set to: " + inputLocationPrefix);

        context.compileDateGlob();
        log.debug("Date glob set to:" + context.getDateGlob());

        for (CategoryConfiguration c : context.getConfig().getCategoryConfigurations()) {
            String inputLocation = inputLocationPrefix + c.getInputGlob();
            log.info("Adding input glob: " + inputLocation);
            FileInputFormat.setInputPathFilter(job, IndexFilter);
            FileInputFormat.addInputPath(job, new Path(inputLocation));
        }

        if ( context.getConfig().getOutputCompression().equals("lzo")){
            FileOutputFormat.setCompressOutput(job, true);
            FileOutputFormat.setOutputCompressorClass(job, LzopCodec.class);

            //TODO: Add dependent job for indexing
        }


        FileOutputFormat.setOutputPath(job, new Path(context.getConfig().getOutputLocationBase()));

        try {
            job.waitForCompletion(true);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return 0;
    }

    public static class Map extends Mapper<LongWritable, Text, NullWritable, Text> {
        private MultipleOutputs<NullWritable, Text> multipleOutputs;
        private String inputPath;
        private String inputBaseName;
        private LogBoxConfiguration config;
        // private ArrayList<Rule> applicableRules = new ArrayList<Rule>();

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException {
            inputPath = ((FileSplit) context.getInputSplit()).getPath().toString();
            /*
            if (inputPath.endsWith(".index")) {
                log.info("Ignoring *.index files: " + inputPath);
                return;
            }
            */
            multipleOutputs = new MultipleOutputs<NullWritable, Text>(context);

            String configJSON = context.getConfiguration().get("config.json");
            config = LogBoxConfiguration.fromConfig(configJSON);
            try {
                inputBaseName = FileUtils.baseName(inputPath);
            }
            catch (Exception e){
                throw new IOException(e.getMessage());
            }
            log.info("Processing input path " + inputPath + ", using basename " + inputBaseName);
            config.compileInputBaseName(inputBaseName);
        }

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            if (line.trim().isEmpty()) {
                return;
            }

            //TODO: Regex match on input location, regex -> glob
            for (CategoryConfiguration c : config.getCategoryConfigurations()) {
                for (Rule r : c.getRules()) {
                    if (r.matches(line)) {
                        multipleOutputs.write(NullWritable.get(), new Text(r.getSubstitutedLine(line)), r.getSubstitutedOutputLocation(line) + "/part");
                    }
                }
            }
        }

        @Override
        protected void cleanup(Context context)
                throws IOException, InterruptedException {
            multipleOutputs.close();
        }
    }

}
