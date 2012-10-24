package org.jbpm.ui.common.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jbpm.ui.resource.Messages;

public abstract class SwimlanedNode extends DescribableNode implements PropertyChangeListener {
    private Swimlane swimlane;

    @Override
    public void setParent(GraphElement parent) {
        super.setParent(parent);
        getProcessDefinition().addPropertyChangeListener(this);
    }

    public Swimlane getSwimlane() {
        return swimlane;
    }

    public String getSwimlaneName() {
        return swimlane != null ? swimlane.getName() : null;
    }

    public void setSwimlane(Swimlane swimlane) {
        Swimlane old = this.swimlane;
        if (old != null) {
            old.removePropertyChangeListener(this);
        }
        this.swimlane = swimlane;
        if (this.swimlane != null) {
            this.swimlane.addPropertyChangeListener(this);
        }
        firePropertyChange(PROPERTY_SWIMLANE, old, swimlane);
    }

    @Override
    protected void validate() {
        super.validate();
        if (getSwimlane() == null) {
            addError("swimlaneNotSet");
        }
    }

    @Override
    protected List<IPropertyDescriptor> getCustomPropertyDescriptors() {
        List<IPropertyDescriptor> list = super.getCustomPropertyDescriptors();
        List<String> swimlaneNames = getProcessDefinition().getSwimlaneNames();
        String[] arr = swimlaneNames.toArray(new String[swimlaneNames.size()]);
        list.add(new ComboBoxPropertyDescriptor(PROPERTY_SWIMLANE, Messages.getString("SwimlanedNode.property.swimlane"), arr));
        return list;
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (PROPERTY_SWIMLANE.equals(id)) {
            Swimlane swimlane = getSwimlane();
            if (swimlane == null) {
                return -1;
            }
            List<Swimlane> swimlanes = getProcessDefinition().getSwimlanes();
            return swimlanes.indexOf(swimlane);
        }
        return super.getPropertyValue(id);
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        if (PROPERTY_SWIMLANE.equals(id)) {
            int i = ((Integer) value).intValue();
            setSwimlane(getProcessDefinition().getSwimlanes().get(i));
        } else {
            super.setPropertyValue(id, value);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (PROPERTY_NAME.equals(propertyName) && evt.getSource() instanceof Swimlane) {
            setSwimlane((Swimlane) evt.getSource());
        } else if (NODE_REMOVED.equals(propertyName) && evt.getOldValue().equals(getSwimlane())) {
            setSwimlane(null);
        }
    }
}
