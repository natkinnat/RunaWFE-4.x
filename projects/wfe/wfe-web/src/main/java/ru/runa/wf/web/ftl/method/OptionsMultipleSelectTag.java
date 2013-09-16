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
package ru.runa.wf.web.ftl.method;

import java.util.ArrayList;
import java.util.List;

import ru.runa.wfe.commons.ftl.FreemarkerTag;
import ru.runa.wfe.commons.ftl.FtlTagVariableHandler;
import ru.runa.wfe.var.ISelectable;

import com.google.common.collect.Lists;

import freemarker.template.TemplateModelException;

@SuppressWarnings("unchecked")
public class OptionsMultipleSelectTag extends FreemarkerTag implements FtlTagVariableHandler {
    private static final long serialVersionUID = 1L;

    @Override
    protected Object executeTag() throws TemplateModelException {
        String outputVarName = getParameterAs(String.class, 0);
        registerVariableHandler(outputVarName);
        String inputVarName = getParameterAs(String.class, 1);
        List<Object> selectedValues = variableProvider.getValue(List.class, outputVarName);
        List<ISelectable> options = variableProvider.getValue(List.class, inputVarName);
        if (options == null) {
            options = Lists.newArrayList();
        }
        StringBuffer html = new StringBuffer();
        for (ISelectable option : options) {
            String id = outputVarName + "_" + option.getValue();
            html.append("<input id=\"").append(id).append("\"");
            html.append(" type=\"checkbox\" value=\"").append(option.getValue()).append("\"");
            html.append(" name=\"").append(outputVarName).append("\"");
            if (selectedValues != null && (selectedValues.contains(option.getValue()) || selectedValues.contains(option))) {
                html.append(" checked=\"true\"");
            }
            html.append(">");
            html.append("<label for=\"").append(id).append("\">");
            html.append(option.getLabel());
            html.append("</label><br>");
        }
        return html;
    }

    @Override
    public Object handle(Object source) throws TemplateModelException {
        if (source instanceof List) {
            List<String> valuesList = (List<String>) source;
            String inputVarName = getParameterAs(String.class, 1);
            List<ISelectable> options = variableProvider.getValueNotNull(List.class, inputVarName);
            List<ISelectable> selectedOptions = new ArrayList<ISelectable>();
            for (String selectedValue : valuesList) {
                for (ISelectable option : options) {
                    if (selectedValue.equals(option.getValue())) {
                        selectedOptions.add(option);
                        break;
                    }
                }
            }
            return selectedOptions;
        }
        return source;
    }

}
