package com.prezi.dataservice;

import com.prezi.dataservice.logsort.config.ExecutionConfiguration;

import java.util.MissingResourceException;

public class Executor {

    protected ExecutionConfiguration execConfig;

    public void execute()
    throws Exception {
        throw new Exception("Executor cannot be called. Call one of its derived classes instead");
    }
}
