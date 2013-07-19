package com.prezi.logbox;

public class Executor {

    protected com.prezi.logbox.config.ExecutionConfiguration execConfig;

    public void execute()
    throws Exception {
        throw new Exception("Executor cannot be called. Call one of its derived classes instead");
    }
}
