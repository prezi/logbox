package com.prezi.logbox.config;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedList;

public class CategoryConfiguration {
    private String name;
    private LinkedList<Rule> rules;

    public void setInputGlob(String inputGlob) {
        this.inputGlob = inputGlob;
    }

    @SerializedName("input_glob")
    private String inputGlob;

    public CategoryConfiguration() {
        this.name = "";
        rules = new LinkedList<Rule>();
        inputGlob = "";
    }

    public CategoryConfiguration(String name) {
        this.name = name;
        rules = new LinkedList<Rule>();
        inputGlob = "";
    }

    public String getInputGlob() {
        return inputGlob;
    }

    public LinkedList<Rule> getRules() {
        return rules;
    }

    public String getName() {
        return name;
    }

}
