package com.prezi.dataservice.logsort.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LogSortConfiguration {
    @SerializedName("rule_config")
    private ArrayList<RuleConfiguration> ruleConfigurations;

    private Log log = LogFactory.getLog(LogSortConfiguration.class);

    public static LogSortConfiguration loadConfig(File configFile)
            throws FileNotFoundException, JsonSyntaxException {
        if (!configFile.isFile() || !configFile.canRead()) {
            throw new FileNotFoundException("Config file " + configFile.getAbsoluteFile().toString() + " doesn't exists or not readable");
        }
        Gson gson = new Gson();
        return gson.fromJson(new FileReader(configFile), LogSortConfiguration.class);
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
            for (int i = 0; i < ruleConfigurations.size(); i++) {
                if (ruleConfigurations.get(i).getCategory().equals(category)) {
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
            for (RuleConfiguration category : ruleConfigurations) {
                if (ruleFound) {
                    break;
                }

                String filterCategory = ruleFilter.split("\\.")[0];
                if (!category.getCategory().equals(filterCategory)) {
                    continue;
                }

                for (Rule rule : category.getRules()) {
                    String canonicalRuleName = category.getCategory() + "." + rule.getName();
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

        ArrayList<RuleConfiguration> filteredConfiguration = new ArrayList<RuleConfiguration>();
        for (RuleConfiguration ruleCategories : ruleConfigurations) {

            // if the current category isn't mentioned in the filters, we don't care
            if (filterCategories.contains(ruleCategories.getCategory())) {

                // if there is a category filter, put the corresponding ruleset into the new config
                if (categoryFilters.contains(ruleCategories.getCategory())) {
                    filteredConfiguration.add(ruleCategories);
                } else {
                    // Check and add individual rule filters, if necessary
                    RuleConfiguration newRuleConfiguration = new RuleConfiguration(ruleCategories.getCategory());

                    for (Rule rule : ruleCategories.getRules()) {
                        String canonicalRuleName = ruleCategories.getCategory() + "." + rule.getName();
                        if (filterSet.contains(canonicalRuleName)) {
                            newRuleConfiguration.getRules().add(rule);
                        }
                    }

                    if (newRuleConfiguration.getRules().size() > 0) {
                        filteredConfiguration.add(newRuleConfiguration);
                    }
                }
            }
        }
        ruleConfigurations = filteredConfiguration;

        for ( RuleConfiguration category : ruleConfigurations){
            for (Rule rule : category.getRules() ){
                log.info("Added rule " + category.getCategory() + " -> " + rule.getName());
            }
        }
    }
}
