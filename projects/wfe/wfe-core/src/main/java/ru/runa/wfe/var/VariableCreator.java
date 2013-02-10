package ru.runa.wfe.var;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

import ru.runa.wfe.WfException;
import ru.runa.wfe.execution.ExecutionContext;
import ru.runa.wfe.var.impl.NullVariable;

public class VariableCreator {
    private static final Log log = LogFactory.getLog(VariableCreator.class);

    private List<VariableType> types;

    @Required
    public void setTypes(List<VariableType> types) {
        this.types = types;
    }

    /**
     * Creates new variable of the corresponding type.
     * 
     * @param value
     *            initial value
     * @return variable
     */
    private Variable<?> create(Object value) {
        for (VariableType type : types) {
            if (type.getMatcher().matches(value)) {
                try {
                    Variable<?> variable = type.getVariableClass().newInstance();
                    variable.setConverter(type.getConverter());
                    return variable;
                } catch (Exception e) {
                    throw new WfException("Unable to create variable " + type.getVariableClass(), e);
                }
            }
        }
        throw new WfException("No variable found for value " + value);
    }

    /**
     * Creates new variable of the corresponding type. This method does not
     * persisit it.
     * 
     * @param value
     *            initial value
     * @return variable
     */
    public Variable<?> create(ExecutionContext executionContext, String name, Object value) {
        log.debug("create variable '" + name + "' in '" + executionContext + "' with value '" + value + "'");
        Variable<?> variable;
        if (value == null) {
            variable = new NullVariable();
        } else {
            variable = create(value);
        }
        variable.setName(name);
        variable.setProcess(executionContext.getProcess());
        variable.setValue(executionContext, value);
        return variable;
    }

}
