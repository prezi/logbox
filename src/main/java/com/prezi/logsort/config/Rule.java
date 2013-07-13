package com.prezi.logsort.config;

import com.google.gson.annotations.SerializedName;

public class Rule {
    private String name;
    private String description;

    private String match;

    public String getMatchRegex() {
        return matchRegex;
    }

    private String matchRegex;

    @SerializedName("output_format")
    private String outputFormat;

    @SerializedName("output_config")
    private String outputConfig;

    @SerializedName("started_at")
    private String startedAt;

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

    public String getMatch() {
        return match;
    }

    public void compile(){

    }

    public boolean matches(String line){
          return true;
    }

    public String getSubstitutedLine(){
        return "";
    }

    public String getOutputLocation(){
        return "";
    }
}
