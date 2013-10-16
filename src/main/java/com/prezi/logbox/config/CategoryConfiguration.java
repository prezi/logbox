package com.prezi.logbox.config;

import com.google.gson.annotations.SerializedName;
import com.prezi.logbox.utils.FileUtils;
import java.util.LinkedList;
import java.util.regex.Pattern;

public class CategoryConfiguration {
    private String name;
    private LinkedList<Rule> rules;
    private transient Pattern regexPattern;


    @SerializedName("input_glob")
    private String inputGlob;
    @SerializedName("input_filename_template")
    private String inputFilenameTemplateStr;
    @SerializedName("input_basename")
    private String inputBasename;

    public void setInputGlob(String inputGlob) {
        this.inputGlob = inputGlob;

        regexPattern = Pattern.compile(FileUtils.globToRegex(this.inputGlob));
    }

    public CategoryConfiguration(){}

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
        String regex = FileUtils.globToRegex(replacedGlob);
        if (regexPattern == null) {
            regexPattern = Pattern.compile(".*" + regex);
        }

        return  regexPattern.matcher(filename).find();
    }

    public String getInputFilenameTemplateStr() {
        return inputFilenameTemplateStr;
    }

    public void setInputFilenameTemplateStr(String inputFilenameTemplateStr) {
        this.inputFilenameTemplateStr = inputFilenameTemplateStr;
    }

    public String getInputBasename() {
        return inputBasename;
    }

    public void setInputBasename(String inputBasename) {
        this.inputBasename = inputBasename;
    }
}