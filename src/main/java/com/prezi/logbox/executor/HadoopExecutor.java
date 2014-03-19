package com.prezi.logbox.executor;

import com.hadoop.compression.lzo.DistributedLzoIndexer;
import com.prezi.logbox.config.ExecutionContext;
import com.prezi.logbox.mapreduce.LogBox;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashSet;


public class HadoopExecutor extends Configured implements Executor
{
    private static Log log = LogFactory.getLog(HadoopExecutor.class);
    private ExecutionContext executionContext;

    ;
    private Boolean indexing;

    FileSystem fileSystem;


    public HadoopExecutor(ExecutionContext c) {
        this.executionContext = c;
        indexing = this.executionContext.getConfig().getOutputCompression().equals("lzo");
    }


    public int execute(String[] cliArgs) throws Exception {
        Configuration conf = new Configuration();

        String temporalFilePrefix = executionContext.getTemporalDirectory();
        URI uri = URI.create(temporalFilePrefix);
        fileSystem = FileSystem.get(uri, conf);
        executionContext.compileDateGlob();
        executionContext.compileHourGlob();

        int exitCode = ToolRunner.run(conf, new LogBox(executionContext), cliArgs);

        if (exitCode == 0){
            String[] directoryList = readTemporalFiles(temporalFilePrefix);
            if (indexing) {
                exitCode = ToolRunner.run(new DistributedLzoIndexer(), directoryList);
            } else {
                for (String d : directoryList) {
                    System.out.println(d);
                }
            }
        }

        fileSystem.delete(new Path(executionContext.getTemporalDirectory()), true);

        return exitCode;
    }

    private String[] readTemporalFiles(String temporalFilePrefix) {
        HashSet<String> directories = new HashSet<String>();
        try{
            FileStatus[] fss = fileSystem.listStatus(new Path(temporalFilePrefix));
            for (FileStatus status : fss) {
                BufferedReader br=new BufferedReader(new InputStreamReader(fileSystem.open( status.getPath())));

                String line = br.readLine();
                while (line != null){
                    directories.add(executionContext.getConfig().getOutputLocationBase() + line);
                    line=br.readLine();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            log.error("Temporary file not found. Indexing failed.");
        }
        log.info("Number of subdirectories: " + directories.size());

        String[] directoryList = new String[directories.size()];
        int i = 0;
        for (String d : directories) {
            directoryList[i] = d;
            i++;
        }

        return directoryList;
    }
}
