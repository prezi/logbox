package com.prezi.logbox.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.regex.PatternSyntaxException;

public class LogBoxConfiguration implements Serializable {

    public String getInputCompression() {
        return inputCompression;
    }

    @SerializedName("input_compression")
    String inputCompression = null;

    public String getOutputCompression() {
        return outputCompression;
    }

    public void setOutputCompression(String outputCompression) {
        this.outputCompression = outputCompression;
    }

    @SerializedName("output_compression")
    String outputCompression = null;

    public ArrayList<CategoryConfiguration> getCategoryConfigurations() {
        return categoryConfigurations;
    }

    @SerializedName("categories")
    private ArrayList<CategoryConfiguration> categoryConfigurations;

    @SerializedName("output_location")
    private String outputLocationBase;

    public String getInputLocationPrefix() {
        return inputLocationPrefix;
    }

    @SerializedName("input_location_prefix")
    private String inputLocationPrefix;

    public String getOutputLocationBase() {
        return outputLocationBase;
    }

    private transient Log log = LogFactory.getLog(LogBoxConfiguration.class);

    public static LogBoxConfiguration fromConfig(String configJSON){
        Gson gson = new Gson();
        LogBoxConfiguration conf = gson.fromJson(configJSON, LogBoxConfiguration.class);

        conf.setup();
        return conf;
    }

    public static LogBoxConfiguration loadConfig(File configFile)
            throws FileNotFoundException, JsonSyntaxException, PatternSyntaxException  {
        if (!configFile.isFile() || !configFile.canRead()) {
            throw new FileNotFoundException(
                    "Config file " + configFile.getAbsoluteFile().toString() + " doesn't exists or not readable"
            );
        }
        Gson gson = new Gson();
        LogBoxConfiguration conf = gson.fromJson(new FileReader(configFile), LogBoxConfiguration.class);

        conf.setup();
        return conf;
    }

    public void setup(){
        compileRules();
    }

    public void compileInputBaseName(String basename){
        for (CategoryConfiguration c : categoryConfigurations){
            for (Rule r : c.getRules()){
                r.setOutputLocationTemplate(r.getOutputLocationTemplate().replace("${input_basename}",basename));
            }
        }
    }

    public String toJSON(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public void compileRules() throws PatternSyntaxException {
        for (CategoryConfiguration category : categoryConfigurations) {
            for (Rule rule : category.getRules()) {
                try {
                    rule.compile();
                }
                catch (PatternSyntaxException e){
                    throw new PatternSyntaxException(
                            "Invalid regex pattern in rule " + rule.getName() + ": " + e.getDescription(),
                            e.getPattern(),
                            e.getIndex()
                    );
                }
            }
        }
    }

    public void applyFilters(String[] filters)
            throws InvalidParameterException {

        Set<String> filterSet = new HashSet<String>(Arrays.asList(filters));

        Set<String> filterCategories = new HashSet<String>();
        for (String f : filters) {
            filterCategories.add(f.split("\\.")[0]);
        }

        // -- Input Error Discovery

        // Look for category-filters that reference non-existing categories
        for (String category : filterCategories) {
            boolean categoryFoundInFilters = false;
            for (int i = 0; i < categoryConfigurations.size(); i++) {
                if (categoryConfigurations.get(i).getName().equals(category)) {
                    categoryFoundInFilters = true;
                    break;
                }
            }
            if (!categoryFoundInFilters) {
                throw new InvalidParameterException("Can't find rules for the category called " + category + " in the configuration");

            }
        }

        // Look for rule-filters that reference non-existing rules
        for (String ruleFilter : filters) {
            if (!ruleFilter.contains(".")) {
                continue;
            }

            boolean ruleFound = false;
            for (CategoryConfiguration category : categoryConfigurations) {
                if (ruleFound) {
                    break;
                }

                String filterCategory = ruleFilter.split("\\.")[0];
                if (!category.getName().equals(filterCategory)) {
                    continue;
                }

                for (Rule rule : category.getRules()) {
                    String canonicalRuleName = category.getName() + "." + rule.getName();
                    if (ruleFilter.equals(canonicalRuleName)) {
                        ruleFound = true;
                        break;
                    }

                }

            }
            if (!ruleFound) {
                throw new InvalidParameterException("Can't find rule for the filter called " + ruleFilter);
            }
        }

        // Eliminate rule filters if there is a corresponding category filter
        Set<String> categoryFilters = new HashSet<String>();
        for (String filter : filterSet) {
            if (!filter.contains(".")) {
                categoryFilters.add(filter);
            }
        }

        if (categoryFilters.size() > 0) {
            Set<String> normalizedFilters = new HashSet<String>();
            for (String filter : filterSet) {
                if ((!filter.contains(".")) || (!categoryFilters.contains(filter.split("\\.")[0]))) {
                    normalizedFilters.add(filter);
                }
            }

            filterSet = normalizedFilters;
        }

        // -- Eliminate unnecessary rules from the configuration

        ArrayList<CategoryConfiguration> filteredConfiguration = new ArrayList<CategoryConfiguration>();
        for (CategoryConfiguration ruleCategories : categoryConfigurations) {

            // if the current category isn't mentioned in the filters, we don't care
            if (filterCategories.contains(ruleCategories.getName())) {

                // if there is a category filter, put the corresponding ruleset into the new config
                if (categoryFilters.contains(ruleCategories.getName())) {
                    filteredConfiguration.add(ruleCategories);
                } else {
                    // Check and add individual rule filters, if necessary
                    CategoryConfiguration newCategoryConfiguration = new CategoryConfiguration(ruleCategories.getName());

                    for (Rule rule : ruleCategories.getRules()) {
                        String canonicalRuleName = ruleCategories.getName() + "." + rule.getName();
                        if (filterSet.contains(canonicalRuleName)) {
                            newCategoryConfiguration.getRules().add(rule);
                        }
                    }

                    if (newCategoryConfiguration.getRules().size() > 0) {
                        filteredConfiguration.add(newCategoryConfiguration);
                    }
                }
            }
        }
        categoryConfigurations = filteredConfiguration;

        for (CategoryConfiguration category : categoryConfigurations) {
            for (Rule rule : category.getRules()) {
                log.info("Added rule " + category.getName() + " -> " + rule.getName());
            }
        }
    }

    public CategoryConfiguration getCategoryConfigByName(String name){
        for (CategoryConfiguration conf : categoryConfigurations){
            if (conf.getName().equals(name)){
                return conf;
            }
        }
        return null;
    }
}
