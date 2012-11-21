/*
 * This file is part of the RUNA WFE project.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation; version 2.1 
 * of the License. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wfe.definition.par;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.runa.wfe.commons.xml.XMLHelper;
import ru.runa.wfe.definition.IFileDataProvider;
import ru.runa.wfe.definition.InvalidDefinitionException;
import ru.runa.wfe.form.Interaction;
import ru.runa.wfe.lang.ProcessDefinition;
import ru.runa.wfe.lang.SwimlaneDefinition;
import ru.runa.wfe.var.VariableDefinition;
import ru.runa.wfe.var.format.ExecutorFormat;

import com.google.common.base.Strings;

/**
 * Created on 17.11.2004
 * 
 */
public class InteractionsParser implements ProcessArchiveParser {

    public static final String NS_URI = "http://runa.ru/xml";

    private final static String FORM_ELEMENT_NAME = "form";

    private final static String STATE_ATTRIBUTE_NAME = "state";

    private final static String FILE_ATTRIBUTE_NAME = "file";

    private static final String VALIDATION_FILE_ATTRIBUTE_NAME = "validationFile";

    private static final String SCRIPT_FILE_ATTRIBUTE_NAME = "scriptFile";

    private static final String JS_VALIDATION_ATTRIBUTE_NAME = "jsValidation";

    private final static String TYPE_ATTRIBUTE_NAME = "type";

    @Override
    public void readFromArchive(ProcessArchive archive, ProcessDefinition processDefinition) {
        try {
            byte[] formsXml = archive.getFileDataNotNull(IFileDataProvider.FORMS_XML_FILE_NAME);
            Document document = XMLHelper.getDocumentWithoutValidation(new ByteArrayInputStream(formsXml));
            NodeList formElementsList = document.getElementsByTagName(FORM_ELEMENT_NAME);
            for (int i = 0; i < formElementsList.getLength(); i++) {
                Element formElement = (Element) formElementsList.item(i);
                String stateId = formElement.getAttribute(STATE_ATTRIBUTE_NAME);
                String fileName = formElement.getAttribute(FILE_ATTRIBUTE_NAME);
                String typeName = formElement.getAttribute(TYPE_ATTRIBUTE_NAME);
                if (typeName == null || typeName.length() == 0) {
                    throw new InvalidDefinitionException("Invalid form type = '" + typeName + "' for state " + stateId);
                }
                String validationFileName = formElement.getAttribute(VALIDATION_FILE_ATTRIBUTE_NAME);
                boolean jsValidationEnabled = Boolean.parseBoolean(formElement.getAttribute(JS_VALIDATION_ATTRIBUTE_NAME));
                String scriptFileName = formElement.getAttribute(SCRIPT_FILE_ATTRIBUTE_NAME);

                byte[] formCode = null;
                if (!Strings.isNullOrEmpty(fileName)) {
                    formCode = archive.getFileDataNotNull(fileName);
                }
                byte[] validationXml = null;
                if (!Strings.isNullOrEmpty(validationFileName)) {
                    validationXml = archive.getFileDataNotNull(validationFileName);
                }
                byte[] scriptJs = null;
                if (!Strings.isNullOrEmpty(scriptFileName)) {
                    scriptJs = archive.getFileDataNotNull(scriptFileName);
                }
                byte[] css = archive.getFileData(IFileDataProvider.FORM_CSS_FILE_NAME);
                Interaction interaction = new Interaction(typeName, formCode, validationXml, jsValidationEnabled, scriptJs, css);
                if (validationXml != null) {
                    List<String> variableNames = ValidationXmlParser.readVariableNames(validationXml);
                    List<String> requiredVarNames = ValidationXmlParser.readRequiredVariableNames(validationXml);
                    for (String varName : requiredVarNames) {
                        interaction.getRequiredVariableNames().add(varName);
                    }
                    for (String name : variableNames) {
                        VariableDefinition variableDefinition = processDefinition.getVariable(name);
                        if (variableDefinition == null) {
                            SwimlaneDefinition swimlaneDefinition = processDefinition.getSwimlane(name);
                            if (swimlaneDefinition == null) {
                                throw new InvalidDefinitionException("Variable '" + name + "' is defined in '" + validationFileName
                                        + "' but not defined in " + processDefinition);
                            }
                            variableDefinition = new VariableDefinition();
                            variableDefinition.setDisplayFormat("displayFormat"); // TODO
                                                                                  // delete
                                                                                  // line
                            variableDefinition.setFormat(ExecutorFormat.class.getName());
                            variableDefinition.setName(name);
                        }
                        interaction.getVariables().put(name, variableDefinition);
                    }
                }
                processDefinition.addInteraction(stateId, interaction);
            }
        } catch (Exception e) {
            throw new InvalidDefinitionException(IFileDataProvider.FORMS_XML_FILE_NAME, e);
        }
    }

}
