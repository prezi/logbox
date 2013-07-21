package com.prezi.logbox;

import com.prezi.logbox.config.ExecutionContext;

public class Executor {

    protected ExecutionContext context;

    public void execute()
    throws Exception {
        throw new Exception("Executor cannot be called. Call one of its derived classes instead");
    }
}
