package ru.runa.gpd.extension;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.window.Window;
import org.osgi.framework.Bundle;

import ru.runa.gpd.lang.ValidationError;
import ru.runa.gpd.lang.model.Delegable;
import ru.runa.gpd.lang.model.Variable;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class DelegableProvider {
    protected Bundle bundle;

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    protected DelegableConfigurationDialog createConfigurationDialog(Delegable delegable) {
        return new DelegableConfigurationDialog(delegable.getDelegationConfiguration());
    }

    public String showConfigurationDialog(Delegable delegable) {
        DelegableConfigurationDialog dialog = createConfigurationDialog(delegable);
        if (dialog.open() == Window.OK) {
            return dialog.getResult();
        }
        return null;
    }

    /**
     * Validates configuration. Implementors can return <code>false</code> to
     * raise default invalid configuration message. Or can invoke
     * delegable.addError.
     * 
     * @return <code>false</code> for raising default invalid configuration
     *         message
     */
    public boolean validateValue(Delegable delegable, List<ValidationError> errors) {
        return true;
    }

    /**
     * Callback is invoked when delegable is deleted from process definition.
     * 
     * @param delegable
     */
    public void onDelete(Delegable delegable) {
    }

    public List<String> getUsedVariableNames(Delegable delegable) throws Exception {
        String configuration = delegable.getDelegationConfiguration();
        if (Strings.isNullOrEmpty(configuration)) {
            return Lists.newArrayList();
        }
        List<String> result = Lists.newArrayList();
        for (String variableName : delegable.getVariableNames(true)) {
            if (configuration.contains(variableName)) {
                result.add(variableName);
            }
        }
        return result;
    }

    public String getConfigurationOnVariableRename(Delegable delegable, Variable currentVariable, Variable previewVariable) {
        return delegable.getDelegationConfiguration().replaceAll(Pattern.quote(currentVariable.getName()), Matcher.quoteReplacement(previewVariable.getName()));
    }
}