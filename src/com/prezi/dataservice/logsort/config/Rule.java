package com.prezi.dataservice.logsort.config;

import com.google.gson.annotations.SerializedName;

public class Rule {
    private String name;
    private String description;

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
}
