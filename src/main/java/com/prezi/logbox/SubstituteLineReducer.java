package com.prezi.logbox;

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
        if (fields.length == 3) {
            String substituted_line = fields[0];
            String location = fields[1];
            String rule_type = fields[2];
            multipleOutputs.write(new Text(substituted_line), NullWritable.get(), location);
            context.getCounter("RuleTypesInMapper", rule_type).increment(1);
            context.getCounter(LineCounter.SUBSTITUTED).increment(1);
        } else {
            context.getCounter(MalformedRecord.FROM_MAPPER).increment(1);
        }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        multipleOutputs.close();
    }
}
