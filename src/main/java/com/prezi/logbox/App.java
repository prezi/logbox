package com.prezi.logbox;

import com.prezi.logbox.config.ExecutionContext;
import com.prezi.logbox.config.ExecutionMode;
import com.prezi.logbox.config.HadoopExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;


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
