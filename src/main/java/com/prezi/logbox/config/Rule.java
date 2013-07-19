package com.prezi.logbox.config;

import com.google.code.regexp.Pattern;
import com.google.gson.annotations.SerializedName;

import java.util.regex.PatternSyntaxException;

public class Rule {
    private String name;
    private String description;
    @SerializedName("match")
    private String regexStr;
    @SerializedName("output_format")
    private String outputFormat;
    @SerializedName("output_config")
    private String outputConfig;
    @SerializedName("started_at")
    private String startedAt;
    private Pattern regexPattern;

    public Pattern getMatcherPattern() {
        return regexPattern;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getOutputConfig() {
        return outputConfig;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getRegexStr() {
        return regexStr;
    }

    public void compile() throws PatternSyntaxException {
        regexPattern = Pattern.compile(regexStr);
    }

    public boolean matches(String line) {
        return regexPattern.matcher(line).find();
    }

    public String getSubstitutedLine(String line) {
        return regexPattern.matcher(line).replaceAll(outputFormat);
    }

    public String getOutputLocation() {
        return "";
    }
}
