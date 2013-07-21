package ru.runa.gpd.lang.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import ru.runa.gpd.Localization;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.extension.LocalizationRegistry;
import ru.runa.gpd.extension.VariableFormatRegistry;
import ru.runa.gpd.util.VariableUtils;

public class Variable extends NamedGraphElement {
    public static final String FORMAT_COMPONENT_TYPE_START = "(";
    public static final String FORMAT_COMPONENT_TYPE_END = ")";
    public static final String FORMAT_COMPONENT_TYPE_CONCAT = ", ";
    private String scriptingName;
    private String format;
    private boolean publicVisibility;
    private String defaultValue;

    protected Variable(String format, boolean publicVisibility, String defaultValue) {
        this(null, null, format, publicVisibility, defaultValue);
    }

    public Variable(String name, String scriptingName, String format, boolean publicVisibility, String defaultValue) {
        super(name);
        setScriptingName(scriptingName);
        setFormat(format);
        this.publicVisibility = publicVisibility;
        this.defaultValue = defaultValue;
    }

    public Variable(Variable variable) {
        this(variable.getName(), variable.getScriptingName(), variable.getFormat(), variable.isPublicVisibility(), variable.getDefaultValue());
    }

    @Override
    protected boolean canNameBeSetFromProperties() {
        return false;
    }

    public String getScriptingName() {
        return scriptingName;
    }

    public void setScriptingName(String nameForScripting) {
        this.scriptingName = nameForScripting;
    }

    @Override
    public void setName(String name) {
        if (name.trim().length() == 0 || getProcessDefinition().getVariableNames(true).contains(name)) {
            return;
        }
        super.setName(name);
        setScriptingName(VariableUtils.generateNameForScripting(getProcessDefinition(), name, null));
    }

    public String getFormat() {
        return format;
    }

    public String getFormatClassName() {
        if (format.contains(FORMAT_COMPONENT_TYPE_START)) {
            int index = format.indexOf(FORMAT_COMPONENT_TYPE_START);
            return format.substring(0, index);
        }
        return format;
    }

    public String[] getFormatComponentClassNames() {
        if (format.contains(FORMAT_COMPONENT_TYPE_START)) {
            int index = format.indexOf(FORMAT_COMPONENT_TYPE_START);
            String raw = format.substring(index + 1, format.length() - 1);
            return raw.split(FORMAT_COMPONENT_TYPE_CONCAT, -1);
        }
        return new String[0];
    }

    public String getFormatLabel() {
        if (format.contains(FORMAT_COMPONENT_TYPE_START)) {
            String label = LocalizationRegistry.getLabel(getFormatClassName()) + FORMAT_COMPONENT_TYPE_START;
            String[] componentClassNames = getFormatComponentClassNames();
            for (int i = 0; i < componentClassNames.length; i++) {
                if (i != 0) {
                    label += FORMAT_COMPONENT_TYPE_CONCAT;
                }
                label += LocalizationRegistry.getLabel(componentClassNames[i]);
            }
            return label + FORMAT_COMPONENT_TYPE_END;
        }
        return LocalizationRegistry.getLabel(format);
    }

    public String getJavaClassName() {
        return VariableFormatRegistry.getInstance().getArtifactNotNull(getFormatClassName()).getJavaClassName();
    }

    public void setFormat(String format) {
        String old = this.format;
        this.format = format;
        firePropertyChange(PROPERTY_FORMAT, old, this.format);
    }

    public boolean isPublicVisibility() {
        return publicVisibility;
    }

    public void setPublicVisibility(boolean publicVisibility) {
        boolean old = this.publicVisibility;
        this.publicVisibility = publicVisibility;
        firePropertyChange(PROPERTY_PUBLIC_VISIBILITY, old, this.publicVisibility);
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        String old = this.defaultValue;
        this.defaultValue = defaultValue;
        firePropertyChange(PROPERTY_DEFAULT_VALUE, old, this.defaultValue);
    }

    @Override
    public List<IPropertyDescriptor> getCustomPropertyDescriptors() {
        List<IPropertyDescriptor> list = new ArrayList<IPropertyDescriptor>();
        list.add(new PropertyDescriptor(PROPERTY_FORMAT, Localization.getString("Variable.property.format")));
        list.add(new PropertyDescriptor(PROPERTY_PUBLIC_VISIBILITY, Localization.getString("Variable.property.publicVisibility")));
        list.add(new PropertyDescriptor(PROPERTY_DEFAULT_VALUE, Localization.getString("Variable.property.defaultValue")));
        return list;
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (PROPERTY_FORMAT.equals(id)) {
            return getFormatLabel();
        }
        if (PROPERTY_PUBLIC_VISIBILITY.equals(id)) {
            return publicVisibility ? Localization.getString("yes") : Localization.getString("false");
        }
        if (PROPERTY_DEFAULT_VALUE.equals(id)) {
            return defaultValue == null ? "" : defaultValue;
        }
        return super.getPropertyValue(id);
    }

    @Override
    public Image getEntryImage() {
        return SharedImages.getImage("icons/obj/variable.gif");
    }
}
