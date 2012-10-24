package org.jbpm.ui.common.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jbpm.ui.SharedImages;
import org.jbpm.ui.orgfunctions.OrgFunctionDefinition;
import org.jbpm.ui.orgfunctions.OrgFunctionsRegistry;
import org.jbpm.ui.resource.Messages;

public class Swimlane extends NamedGraphElement implements Delegable {
    public static final String DELEGATION_CLASS_NAME = "ru.runa.wf.jbpm.delegation.assignment.AssignmentHandler";
    private boolean publicVisibility;

    public String getDelegationType() {
        return ASSIGNMENT_HANDLER;
    }

    @Override
    protected boolean canSetNameTo(String name) {
        ProcessDefinition definition = getProcessDefinition();
        if (definition == null) {
            return false;
        }
        Swimlane swimlane = definition.getSwimlaneByName(name);
        return swimlane == null;
    }

    @Override
    protected boolean canNameBeSetFromProperties() {
        return false;
    }

    public boolean isPublicVisibility() {
        return publicVisibility;
    }

    public void setPublicVisibility(boolean publicVisibility) {
        boolean old = this.publicVisibility;
        this.publicVisibility = publicVisibility;
        firePropertyChange(PROPERTY_PUBLIC_VISIBILITY, old, this.publicVisibility);
    }

    @Override
    public List<IPropertyDescriptor> getCustomPropertyDescriptors() {
        List<IPropertyDescriptor> list = new ArrayList<IPropertyDescriptor>();
        list.add(new PropertyDescriptor(PROPERTY_PUBLIC_VISIBILITY, Messages.getString("Variable.property.publicVisibility")));
        return list;
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (PROPERTY_PUBLIC_VISIBILITY.equals(id)) {
            return publicVisibility ? Messages.getString("message.yes") : Messages.getString("message.no");
        }
        return super.getPropertyValue(id);
    }

    @Override
    protected void validate() {
        super.validate();
        try {
            OrgFunctionDefinition definition = OrgFunctionsRegistry.parseSwimlaneConfiguration(getDelegationConfiguration());
            if (definition != null) {
                List<String> errors = definition.getErrors(getProcessDefinition());
                for (String errorKey : errors) {
                    addError(errorKey);
                }
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith(OrgFunctionDefinition.MISSED_DEFINITION)) {
                addWarning("orgfunction.missed");
            } else {
                addError("orgfunction.broken");
            }
        }
    }

    @Override
    public Image getEntryImage() {
        return SharedImages.getImage("icons/obj/swimlane.gif");
    }

}
