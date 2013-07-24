package com.prezi.logbox;

import com.prezi.FileUtils;
import com.prezi.logbox.config.ExecutionContext;
import com.prezi.logbox.config.ExecutionMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class App {
    private static Log log = LogFactory.getLog(App.class);

    public static void main(String[] args) throws Exception {

        ExecutionContext context = ExecutionContext.setupFromCLArgs(args);

        Executor executor = new Executor();
        if (context.getExecutionMode() == ExecutionMode.LOCAL_TEST) {
            executor = new LocalExecutor(context);
        }
        else {
            executor = new HadoopExecutor(context);
        }

        executor.execute();

    }
}
