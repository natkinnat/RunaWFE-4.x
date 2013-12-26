package ru.runa.wfe.var.dto;

import ru.runa.wfe.commons.ftl.FreemarkerTagHelper;

public class QuickFormVariable {
    private String tagName;
    private String name;
    private String[] params;
    private String format;
    private String description;

    public QuickFormVariable() {
    }

    public QuickFormVariable(String tagName, String name, String... params) {
        this.tagName = tagName;
        this.name = name;
        this.params = params;
    }

    public String getTag() {
        return FreemarkerTagHelper.build(tagName, name, params);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}