package com.prezi.logbox;

import com.prezi.FileUtils;
import com.prezi.logbox.config.ExecutionContext;
import com.prezi.logbox.config.ExecutionMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



// 1. s3 reading/writing - 1
// 2. mukodik-e etl-en? - ...

// clean-up -> ??? - hetfo
// lzo index -

// distinct - reducer



public class App {
    private static Log log = LogFactory.getLog(App.class);

    public static void main(String[] args) throws Exception {

        ExecutionContext context = ExecutionContext.setupFromCLArgs(args);

        Executor executor;
        if (context.getExecutionMode() == ExecutionMode.LOCAL_TEST) {
            executor = new LocalExecutor(context);
        }
        else {
            executor = new HadoopExecutor(context);
        }

        executor.execute(args);

    }
}
