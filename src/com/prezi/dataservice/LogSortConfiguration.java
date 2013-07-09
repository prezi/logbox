package com.prezi.dataservice;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class LogSortConfiguration {
    public RuleConfiguration[] getRuleConfiguration() {
        return ruleConfiguration;
    }

    @SerializedName("rule_config")
    private RuleConfiguration[] ruleConfiguration;

    public static LogSortConfiguration loadConfig(File configFile)
            throws FileNotFoundException, JsonSyntaxException {
        Gson gson = new Gson();
        return gson.fromJson(new FileReader(configFile), LogSortConfiguration.class);
    }
}
