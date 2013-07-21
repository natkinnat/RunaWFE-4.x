package ru.runa.gpd.extension.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ru.runa.gpd.Localization;
import ru.runa.gpd.extension.VariableFormatRegistry;
import ru.runa.gpd.extension.handler.ParamDef.Presentation;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.ui.custom.InsertVariableTextMenuDetectListener;
import ru.runa.gpd.ui.custom.SWTUtils;
import ru.runa.gpd.ui.custom.TypedUserInputCombo;

import com.google.common.base.Objects;

public class ParamDefComposite extends Composite {
    protected final ParamDefConfig config;
    private final Map<String, List<String>> comboItems = new HashMap<String, List<String>>();
    private final Map<String, String> properties;
    private final List<Variable> variables;
    private MessageDisplay messageDisplay;
    private boolean helpInlined = false;
    private boolean menuForSettingVariable = false;
    private String[] booleanValues = { Localization.getString("yes"), Localization.getString("no") };

    public ParamDefComposite(Composite parent, ParamDefConfig config, Map<String, String> properties, List<Variable> variables) {
        super(parent, SWT.NONE);
        this.config = config;
        this.properties = properties != null ? properties : new HashMap<String, String>();
        this.variables = variables;
        GridLayout layout = new GridLayout(2, false);
        setLayout(layout);
    }

    public void createUI() {
        if (properties.size() == 0) {
            setMessages(null, Localization.getString("ParamBasedProvider.parseError"));
        }
        for (ParamDefGroup group : config.getGroups()) {
            GridData strokeData = new GridData(GridData.FILL_HORIZONTAL);
            strokeData.horizontalSpan = 2;
            SWTUtils.createStrokeComposite(this, strokeData, Localization.getString("ParamDefGroup.group." + group.getLabel()), 3);
            for (ParamDef param : group.getParameters()) {
                if (helpInlined) {
                    Label helpLabel = new Label(this, SWT.WRAP);
                    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                    gridData.horizontalSpan = 2;
                    helpLabel.setLayoutData(gridData);
                    helpLabel.setText(param.getHelp());
                }
                Presentation presentation = param.getPresentation();
                if (presentation == Presentation.combo || presentation == Presentation.richcombo) {
                    addComboField(param, presentation == Presentation.richcombo);
                } else if (presentation == Presentation.checkbox) {
                    addCheckboxField(param);
                } else {
                    addTextField(param);
                }
            }
        }
    }

    public void setMessageDisplay(MessageDisplay messageDisplay) {
        this.messageDisplay = messageDisplay;
    }

    public void setHelpInlined(boolean helpInlined) {
        this.helpInlined = helpInlined;
    }

    public void setMenuForSettingVariable(boolean menuForSettingVariable) {
        this.menuForSettingVariable = menuForSettingVariable;
    }

    private List<String> getVariableNames(Collection<String> typeFilters) {
        List<String> result = new ArrayList<String>();
        for (Variable variable : variables) {
            boolean applicable = typeFilters.size() == 0;
            if (!applicable) {
                for (String typeFilter : typeFilters) {
                    if (VariableFormatRegistry.isApplicable(variable, typeFilter)) {
                        applicable = true;
                        break;
                    }
                }
            }
            if (applicable) {
                result.add(variable.getName());
            }
        }
        return result;
    }

    private Text addTextField(final ParamDef paramDef) {
        Label label = new Label(this, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.minimumWidth = 200;
        label.setLayoutData(gridData);
        label.setText(getLabelText(paramDef));
        final Text textInput = new Text(this, SWT.BORDER);
        textInput.setData(paramDef.getName());
        if (!helpInlined) {
            textInput.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    setMessages(paramDef.getHelp(), null);
                }
            });
        }
        GridData typeComboData = new GridData(GridData.FILL_HORIZONTAL);
        typeComboData.minimumWidth = 200;
        textInput.setLayoutData(typeComboData);
        String selectedValue = properties.get(paramDef.getName());
        if (selectedValue == null) {
            selectedValue = paramDef.getDefaultValue();
        }
        textInput.setText(selectedValue != null ? selectedValue : "");
        if (menuForSettingVariable) {
            Set<String> formatFilters = new HashSet<String>();
            formatFilters.add(String.class.getName());
            new InsertVariableTextMenuDetectListener(textInput, getVariableNames(formatFilters));
        }
        return textInput;
    }

    private Combo addComboField(final ParamDef paramDef, boolean editable) {
        String selectedValue = properties.get(paramDef.getName());
        if (selectedValue == null) {
            selectedValue = paramDef.getDefaultValue();
        }
        List<String> variableNames = new ArrayList<String>();
        if (paramDef.isUseVariable()) {
            variableNames.addAll(getVariableNames(paramDef.getFormatFilters()));
        }
        boolean localizeTextValue = false;
        for (String option : paramDef.getComboItems()) {
            variableNames.add(Localization.getString(option));
            if (Objects.equal(option, selectedValue)) {
                localizeTextValue = true;
            }
        }
        Collections.sort(variableNames);
        if (paramDef.isOptional()) {
            variableNames.add(0, "");
        }
        comboItems.put(paramDef.getName(), variableNames);
        Label label = new Label(this, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.minimumWidth = 200;
        label.setLayoutData(gridData);
        label.setText(getLabelText(paramDef));
        Combo combo;
        if (editable) {
            String lastUserInputValue = variableNames.contains(selectedValue) ? null : selectedValue;
            TypedUserInputCombo userInputCombo = new TypedUserInputCombo(this, lastUserInputValue);
            userInputCombo.setShowEmptyValue(false);
            combo = userInputCombo;
        } else {
            combo = new Combo(this, SWT.BORDER | SWT.READ_ONLY);
        }
        combo.setData(paramDef.getName());
        combo.setVisibleItemCount(10);
        for (String item : variableNames) {
            combo.add(item);
        }
        if (editable) {
            String typeClassName = paramDef.getFormatFilters().size() > 0 ? paramDef.getFormatFilters().get(0) : String.class.getName();
            if (Boolean.class.getName().equals(typeClassName)) {
                ((TypedUserInputCombo) combo).setBooleanValues(booleanValues);
                localizeTextValue = true;
            }
            ((TypedUserInputCombo) combo).setTypeClassName(typeClassName);
        }
        if (!helpInlined) {
            combo.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    setMessages(paramDef.getHelp(), null);
                }
            });
        }
        GridData typeComboData = new GridData(GridData.FILL_HORIZONTAL);
        typeComboData.minimumWidth = 200;
        combo.setLayoutData(typeComboData);
        if (selectedValue != null) {
            combo.setText(localizeTextValue ? Localization.getString(selectedValue) : selectedValue);
        }
        return combo;
    }

    private Button addCheckboxField(final ParamDef paramDef) {
        Label label = new Label(this, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.minimumWidth = 200;
        label.setLayoutData(gridData);
        label.setText(getLabelText(paramDef));
        Button button = new Button(this, SWT.CHECK);
        button.setData(paramDef.getName());
        if (!helpInlined) {
            button.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    setMessages(paramDef.getHelp(), null);
                }
            });
        }
        GridData gridData2 = new GridData(GridData.FILL_HORIZONTAL);
        gridData2.minimumWidth = 200;
        button.setLayoutData(gridData2);
        String selectedValue = properties.get(paramDef.getName());
        if (selectedValue == null) {
            selectedValue = paramDef.getDefaultValue();
        }
        button.setSelection("true".equals(selectedValue));
        return button;
    }

    private String getLabelText(ParamDef aParam) {
        String labelText = aParam.getLabel();
        if (labelText == null) {
            labelText = "";
        }
        if (!aParam.isOptional()) {
            labelText += " *";
        }
        return labelText;
    }

    public Map<String, String> readUserInput() {
        Map<String, String> properties = new HashMap<String, String>();
        Control[] controls = this.getChildren();
        for (Control control : controls) {
            if (control.getData() != null) {
                String propertyName = (String) control.getData();
                String propertyValue = null;
                if (control instanceof Text) {
                    propertyValue = ((Text) control).getText();
                } else if (control instanceof Button) { // Checkbox
                    boolean checked = ((Button) control).getSelection();
                    ParamDef paramDef = config.getParamDef(propertyName);
                    if (paramDef.getDefaultValue() != null) {
                        if (checked) {
                            propertyValue = paramDef.getDefaultValue();
                        } else if (paramDef.isOptional()) {
                        } else if ("true".equals(paramDef.getDefaultValue())) {
                            propertyValue = "false";
                        }
                    } else {
                        if (checked) {
                            propertyValue = "true";
                        } else if (paramDef.isOptional()) {
                        } else {
                            propertyValue = "false";
                        }
                    }
                } else { // Combo
                    propertyValue = ((Combo) control).getText();
                    String[] comboItems = config.getParamDef(propertyName).getComboItems();
                    for (String comboValue : comboItems) {
                        // TODO ILocalization from plugin
                        if (Objects.equal(propertyValue, Localization.getString(comboValue))) {
                            propertyValue = comboValue;
                            break;
                        }
                    }
                    if (Objects.equal(propertyValue, booleanValues[0])) {
                        propertyValue = "true";
                    }
                    if (Objects.equal(propertyValue, booleanValues[1])) {
                        propertyValue = "false";
                    }
                }
                if (propertyValue != null && propertyValue.trim().length() > 0) {
                    properties.put(propertyName, propertyValue);
                }
            }
        }
        return properties;
    }

    public void setMessages(String message, String errorMessage) {
        if (messageDisplay != null) {
            messageDisplay.setMessages(message, errorMessage);
        }
    }

    public interface MessageDisplay {
        void setMessages(String message, String errorMessage);
    }
}
