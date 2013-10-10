package com.prezi.hadoop;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

public class IndexFilter implements PathFilter {

    @Override
    public boolean accept(Path path) {
        return !(path.toString().endsWith(".index"));
    }
}
