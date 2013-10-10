package com.prezi.logbox.config;

import com.google.gson.annotations.SerializedName;
import com.prezi.logbox.utils.FileUtils;

import java.util.LinkedList;
import java.util.regex.Pattern;

public class CategoryConfiguration {
    private String name;
    private LinkedList<Rule> rules;
    private transient Pattern regexPattern;

    public void setInputGlob(String inputGlob) {
        this.inputGlob = inputGlob;
        regexPattern = Pattern.compile(FileUtils.globToRegex(this.inputGlob));
    }

    @SerializedName("input_glob")
    private String inputGlob;

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

    public boolean matches(String filename, String globDate) {
        String replacedGlob = inputGlob.replaceAll("\\$\\{date_glob\\}", globDate);
        if (regexPattern == null) {
            regexPattern = Pattern.compile(".*" + FileUtils.globToRegex(replacedGlob));
        }
        return regexPattern.matcher(filename).find();
    }

}
