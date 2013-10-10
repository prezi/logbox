package com.prezi.logbox.utils;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.ParseException;

import java.util.ListIterator;

public class IgnoreUnknownParametersParser extends BasicParser {

    private boolean ignoreUnrecognizedOption;

    public IgnoreUnknownParametersParser () {
    }

    @Override
    protected void processOption(final String arg, final ListIterator iter) throws ParseException {
        boolean hasOption = getOptions().hasOption(arg);

        if (hasOption) {
            super.processOption(arg, iter);
        }
    }

}
