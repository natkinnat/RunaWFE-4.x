package ru.runa.wfe.var;

import java.util.Map;

import ru.runa.wfe.var.dto.WfVariable;

public class MapDelegableVariableProvider extends AbstractVariableProvider {
    private final Map<String, ? extends Object> variables;
    private final IVariableProvider delegate;

    public MapDelegableVariableProvider(Map<String, ? extends Object> variables, IVariableProvider delegate) {
        this.variables = variables;
        this.delegate = delegate;
    }

    @Override
    public Long getProcessId() {
        if (delegate != null) {
            return delegate.getProcessId();
        }
        return null;
    }

    @Override
    public Object getValue(String variableName) {
        Object object = variables.get(variableName);
        if (object != null) {
            return object;
        }
        if (delegate != null) {
            return delegate.getValue(variableName);
        }
        return null;
    }

    @Override
    public WfVariable getVariable(String variableName) {
        if (delegate != null) {
            return delegate.getVariable(variableName);
        }
        return null;
    }
}
