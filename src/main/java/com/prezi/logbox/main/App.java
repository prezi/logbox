package com.prezi.logbox.main;

import com.prezi.logbox.executor.Executor;
import com.prezi.logbox.executor.HadoopExecutor;
import com.prezi.logbox.executor.LocalExecutor;
import com.prezi.logbox.config.ExecutionContext;
import com.prezi.logbox.config.ExecutionMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class App {
    private static Log log = LogFactory.getLog(App.class);

    public static void main(String[] args) throws Exception {

        ExecutionContext context = new ExecutionContext();
        context.setupFromCommandLineArgs(args);

        Executor executor;
        if (context.getExecutionMode() == ExecutionMode.LOCAL_TEST) {
            executor = new LocalExecutor(context);
        }
        else {
            executor = new HadoopExecutor(context);
        }
        int exitCode = executor.execute(args);
        System.exit(exitCode);
    }
}
