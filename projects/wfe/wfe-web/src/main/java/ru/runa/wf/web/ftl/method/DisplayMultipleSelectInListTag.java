package ru.runa.wf.web.ftl.method;

import java.util.List;

import ru.runa.wfe.commons.ftl.FreemarkerTag;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.var.ISelectable;

import com.google.common.collect.Lists;

@SuppressWarnings("unchecked")
public class DisplayMultipleSelectInListTag extends FreemarkerTag {
    private static final long serialVersionUID = 1L;

    @Override
    protected Object executeTag() {
        String variableName = getParameterAsString(0);
        List<Object> list = getParameterVariableValue(List.class, 1, null);
        if (list == null) {
            list = Lists.newArrayList();
        }
        List<Object> selectedValues = variableProvider.getValue(List.class, variableName);
        StringBuffer html = new StringBuffer();
        html.append("<span class=\"multipleSelectFromList\">");
        for (Object option : list) {
            String optionValue;
            String optionLabel;
            if (option instanceof ISelectable) {
                ISelectable selectable = (ISelectable) option;
                optionValue = selectable.getValue();
                optionLabel = selectable.getLabel();
            } else if (option instanceof Executor) {
                Executor executor = (Executor) option;
                optionValue = "ID" + executor.getId();
                optionLabel = executor.getLabel();
            } else {
                optionValue = String.valueOf(option);
                optionLabel = String.valueOf(option);
            }
            String id = variableName + "_" + optionValue;
            html.append("<input id=\"").append(id).append("\"");
            html.append(" type=\"checkbox\" value=\"").append(optionValue).append("\"");
            html.append(" name=\"").append(variableName).append("\"");
            if (selectedValues != null && selectedValues.contains(option)) {
                html.append(" checked=\"true\"");
            }
            html.append("style=\"width: 30px;\" disabled=\"true\">");
            html.append("<label for=\"").append(id).append("\">");
            html.append(optionLabel);
            html.append("</label><br>");
        }
        html.append("</span>");
        return html;
    }

}
