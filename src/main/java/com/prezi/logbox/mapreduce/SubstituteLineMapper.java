package com.prezi.logbox.mapreduce;

import com.prezi.logbox.utils.FileUtils;
import com.prezi.logbox.executor.HadoopExecutor;
import com.prezi.logbox.config.CategoryConfiguration;
import com.prezi.logbox.config.LogBoxConfiguration;
import com.prezi.logbox.config.Rule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;


import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.UUID;

public class SubstituteLineMapper extends Mapper<LongWritable, Text, Text, NullWritable> {

    private String inputPath;
    private String inputBaseName;
    private LogBoxConfiguration config;
    private HashSet<String> outputPaths;
    private String temporalFilePrefix;
    private static Log log = LogFactory.getLog(HadoopExecutor.class);

    @Override
    protected void setup(Context context)
            throws IOException, InterruptedException {

        outputPaths = new HashSet<String>();
        inputPath = ((FileSplit) context.getInputSplit()).getPath().toString();
        context.getCounter(FileCounter.INPUT_FILES).increment(1);
        String configJSON = context.getConfiguration().get("config.json");
        config = LogBoxConfiguration.fromConfig(configJSON);
        temporalFilePrefix = config.getTemporalFilePrefix();

        try {
            inputBaseName = FileUtils.baseName(inputPath);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        log.info("Processing input path " + inputPath + ", filename " + inputBaseName);
        config.compileInputBaseName(inputBaseName);
    }

    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        if (line.trim().isEmpty()) {
            return;
        }

        for (CategoryConfiguration c : config.getCategoryConfigurations()) {
            if (c.matches(inputPath, context.getConfiguration().get("date_glob"))) {
                for (Rule r : c.getRules()) {
                    if (r.matches(line)) {
                        String locationDirectory = r.getSubstitutedOutputLocation(line);
                        outputPaths.add(locationDirectory);

                        Text lineAndLocation = new Text(r.getSubstitutedLine(line) + "|" + locationDirectory);
                        context.write(lineAndLocation, NullWritable.get());
                        context.getCounter(LineCounter.EMITTED_IN_MAPPER).increment(1);
                    } else {
                        context.getCounter(LineCounter.IGNORED_IN_MAPPER).increment(1);
                    }
                }
            }
        }
    }

    @Override
    protected void cleanup(Context context)
            throws IOException, InterruptedException {
        String filename ="";
        URI uri = URI.create(temporalFilePrefix);
        FileSystem fs = FileSystem.get(uri, context.getConfiguration());
        try {
            String uuid = UUID.randomUUID().toString();
            filename = temporalFilePrefix + "temp-" + uuid;
            FSDataOutputStream out = fs.create(new Path(filename));
            for (String path : outputPaths) {
                out.writeBytes(path + '\n');
            }
            out.close();
        } catch (IOException e) {
            System.out.println("Problem with: " + filename);
            e.printStackTrace();
        }
    }
}
