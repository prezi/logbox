package com.prezi.logsort;

import com.prezi.logsort.config.ExecutionConfiguration;

public class Executor {

    protected ExecutionConfiguration execConfig;

    public void execute()
    throws Exception {
        throw new Exception("Executor cannot be called. Call one of its derived classes instead");
    }
}
