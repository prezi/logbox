package com.prezi.hadoop;

import org.apache.hadoop.fs.FileAlreadyExistsException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;

/*
  Idea from: http://ssklogs.blogspot.hu/2012/08/hadoop-outputformat-overwrite-output.html
 */

public class OverwriteOutputDirTextOutputFormat extends TextOutputFormat {
    @Override
    public void checkOutputSpecs(JobContext job)
            throws FileAlreadyExistsException,
            IOException {

        // bypass the output directory check.

    }
}