package ru.runa.gpd.lang.par;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;

import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Swimlane;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.util.XmlUtil;
import ru.runa.wfe.commons.BackCompatibilityClassNames;
import ru.runa.wfe.var.format.UserTypeFormat;

import com.google.common.base.Strings;

public class VariablesXmlContentProvider extends AuxContentProvider {
    private static final String XML_FILE_NAME = "variables.xml";
    private static final String FORMAT = "format";
    private static final String SWIMLANE = "swimlane";
    private static final String DESCRIPTION = "description";
    private static final String VARIABLE = "variable";
    private static final String VARIABLES = "variables";
    private static final String PUBLIC = "public";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String SCRIPTING_NAME = "scriptingName";
    private static final String USER_TYPE = "usertype";
    private static final String EDITOR = "editor";

    @Override
    public boolean isSupportedForEmbeddedSubprocess() {
        return false;
    }

    @Override
    public String getFileName() {
        return XML_FILE_NAME;
    }

    @Override
    public void read(Document document, ProcessDefinition definition) throws Exception {
        List<Element> typeElements = document.getRootElement().elements(USER_TYPE);
        for (Element typeElement : typeElements) {
            VariableUserType type = new VariableUserType();
            type.setName(typeElement.attributeValue(NAME));
            definition.addVariableUserType(type);
        }
        for (Element typeElement : typeElements) {
            VariableUserType type = definition.getVariableUserTypeNotNull(typeElement.attributeValue(NAME));
            List<Element> attributeElements = typeElement.elements(VARIABLE);
            for (Element attributeElement : attributeElements) {
                Variable variable = parse(attributeElement, definition);
                type.addAttribute(variable);
            }
        }
        List<Element> elementsList = document.getRootElement().elements(VARIABLE);
        for (Element element : elementsList) {
            if ("true".equals(element.attributeValue(SWIMLANE, "false"))) {
                String variableName = element.attributeValue(NAME);
                String scriptingName = element.attributeValue(SCRIPTING_NAME, variableName);
                try {
                    String publicVisibilityStr = element.attributeValue(PUBLIC);
                    boolean publicVisibility = "true".equals(publicVisibilityStr);
                    String description = element.attributeValue(DESCRIPTION);
                    if ("false".equals(description)) {
                        // remove old comments due to some bug
                        description = null;
                    }
                    Swimlane swimlane = definition.getSwimlaneByName(variableName);
                    swimlane.setScriptingName(scriptingName);
                    swimlane.setDescription(description);
                    swimlane.setPublicVisibility(publicVisibility);
                    swimlane.setEditorPath(element.attributeValue(EDITOR));
                } catch (Exception e) {
                    PluginLogger.logErrorWithoutDialog("No swimlane found for " + variableName, e);
                }
                continue;
            }
            Variable variable = parse(element, definition);
            definition.addChild(variable);
        }
    }

    private Variable parse(Element element, ProcessDefinition processDefinition) {
        String variableName = element.attributeValue(NAME);
        String format;
        String userTypeName = element.attributeValue(USER_TYPE);
        VariableUserType userType = null;
        if (userTypeName != null) {
            format = UserTypeFormat.class.getName();
            userType = processDefinition.getVariableUserTypeNotNull(userTypeName);
        } else {
            format = element.attributeValue(FORMAT);
            format = BackCompatibilityClassNames.getClassName(format);
        }
        boolean publicVisibility = "true".equals(element.attributeValue(PUBLIC));
        String defaultValue = element.attributeValue(DEFAULT_VALUE);
        String scriptingName = element.attributeValue(SCRIPTING_NAME, variableName);
        String description = element.attributeValue(DESCRIPTION);
        if ("false".equals(description)) {
            // remove old comments due to some bug
            description = null;
        }
        Variable variable = new Variable(variableName, scriptingName, format, userType, publicVisibility, defaultValue);
        variable.setDescription(description);
        return variable;
    }

    @Override
    public Document save(ProcessDefinition definition) throws Exception {
        Document document = XmlUtil.createDocument(VARIABLES);
        Element root = document.getRootElement();
        for (VariableUserType type : definition.getVariableUserTypes()) {
            Element typeElement = root.addElement(USER_TYPE);
            typeElement.addAttribute(NAME, type.getName());
            for (Variable variable : type.getAttributes()) {
                writeVariable(typeElement, variable);
            }
        }
        for (Variable variable : definition.getVariables(false, true)) {
            writeVariable(root, variable);
        }
        return document;
    }

    private Element writeVariable(Element root, Variable variable) {
        Element element = root.addElement(VARIABLE);
        element.addAttribute(NAME, variable.getName());
        element.addAttribute(SCRIPTING_NAME, variable.getScriptingName());
        if (variable.getUserType() != null) {
            element.addAttribute(USER_TYPE, variable.getUserType().getName());
        } else {
            element.addAttribute(FORMAT, variable.getFormat());
        }
        if (variable.isPublicVisibility()) {
            element.addAttribute(PUBLIC, "true");
        }
        if (!Strings.isNullOrEmpty(variable.getDescription())) {
            element.addAttribute(DESCRIPTION, variable.getDescription());
        }
        if (!Strings.isNullOrEmpty(variable.getDefaultValue())) {
            element.addAttribute(DEFAULT_VALUE, variable.getDefaultValue());
        }
        if (variable instanceof Swimlane) {
            Swimlane swimlane = (Swimlane) variable;
            element.addAttribute(SWIMLANE, Boolean.TRUE.toString());
            if (!Strings.isNullOrEmpty(swimlane.getEditorPath())) {
                element.addAttribute(EDITOR, swimlane.getEditorPath());
            }
        }
        return element;
    }

}
