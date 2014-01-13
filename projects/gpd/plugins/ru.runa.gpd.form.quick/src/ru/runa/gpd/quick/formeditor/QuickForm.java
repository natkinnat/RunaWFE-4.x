package ru.runa.gpd.quick.formeditor;

import java.util.ArrayList;
import java.util.List;

public class QuickForm {
    private String name;
    private String delegationConfiguration = "";
    private final List<QuickFormGpdVariable> variables = new ArrayList<QuickFormGpdVariable>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDelegationConfiguration() {
        return delegationConfiguration;
    }

    public void setDelegationConfiguration(String delegationConfiguration) {
        this.delegationConfiguration = delegationConfiguration;
    }

    public List<QuickFormGpdVariable> getVariables() {
        return variables;
    }

    public void changeChildIndex(QuickFormGpdVariable child, QuickFormGpdVariable insertBefore) {
        if (insertBefore != null && child != null) {
        	variables.remove(child);
        	variables.add(variables.indexOf(insertBefore), child);
        }
    }
}
