package com.prezi.logbox;

import com.prezi.logbox.config.ExecutionContext;

public interface Executor {

    public void execute(String[] cliArgs)
    throws Exception;
}
