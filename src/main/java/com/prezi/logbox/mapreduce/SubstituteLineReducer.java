package com.prezi.logbox.mapreduce;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import java.io.IOException;

public class SubstituteLineReducer  extends Reducer<Text, NullWritable, Text, NullWritable> {
    private MultipleOutputs<Text, NullWritable> multipleOutputs;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        multipleOutputs = new MultipleOutputs<Text, NullWritable>(context);
    }

    @Override
    protected void reduce(Text key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
        String[] fields = key.toString().split("\\|");
        int lengthOfField = fields.length;
        StringBuffer stringBuffer = new StringBuffer();
        if (lengthOfField > 1 ) {
            stringBuffer.append(fields[0]);
            for (int i = 1; i < lengthOfField - 1; i++) {
                stringBuffer.append("|");
                stringBuffer.append(fields[i]);
            }
        } else {
            context.getCounter(MalformedRecord.FROM_MAPPER);
        }
        String substituted_line = stringBuffer.toString();
        String locationDirectory = fields[lengthOfField - 1];

        multipleOutputs.write(new Text(substituted_line), NullWritable.get(), locationDirectory + "/part");
        // context.getCounter("Locations", locationDirectory).increment(1);
        // context.getCounter(LineCounter.SUBSTITUTED).increment(1);

    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        multipleOutputs.close();
    }
}
