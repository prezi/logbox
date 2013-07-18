package com.prezi.logsort.config;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class CategoryConfiguration {
    private String name;
    private ArrayList<Rule> rules;

    @SerializedName("input_glob")
    private String inputGlob;

    public CategoryConfiguration(String name) {
        this.name = name;
        rules = new ArrayList<Rule>();
        inputGlob = "";
    }

    public String getInputGlob() {
        return inputGlob;
    }

    public ArrayList<Rule> getRules() {
        return rules;
    }

    public String getName() {
        return name;
    }

}
