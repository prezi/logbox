package com.prezi.logsort.config;

import java.util.ArrayList;

public class RuleConfiguration {
    private String category;
    private ArrayList<Rule> rules;

    public RuleConfiguration(String category) {
        this.category = category;
        rules = new ArrayList<Rule>();
    }

    public ArrayList<Rule> getRules() {
        return rules;
    }

    public String getCategory() {
        return category;
    }
}
