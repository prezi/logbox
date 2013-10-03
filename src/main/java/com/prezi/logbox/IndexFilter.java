package com.prezi.logbox;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

/**
 * Created with IntelliJ IDEA.
 * User: julcsi
 * Date: 10/3/13
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class IndexFilter implements PathFilter {

    @Override
    public boolean accept(Path path) {
        return !(path.toString().endsWith(".index"));
    }
}
