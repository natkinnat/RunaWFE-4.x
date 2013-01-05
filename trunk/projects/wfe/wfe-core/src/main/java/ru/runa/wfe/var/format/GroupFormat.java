package ru.runa.wfe.var.format;

import java.util.HashMap;

import javax.security.auth.Subject;

import org.springframework.beans.factory.annotation.Autowired;

import ru.runa.wfe.commons.TypeConversionUtil;
import ru.runa.wfe.commons.web.WebHelper;
import ru.runa.wfe.security.Permission;
import ru.runa.wfe.security.auth.SubjectPrincipalsHelper;
import ru.runa.wfe.security.dao.PermissionDAO;
import ru.runa.wfe.user.Group;

import com.google.common.collect.Maps;

public class GroupFormat implements VariableFormat<Group>, VariableDisplaySupport<Group> {
    @Autowired
    private PermissionDAO permissionDAO;

    @Override
    public Group parse(String[] source) throws Exception {
        return TypeConversionUtil.convertTo(source[0], Group.class);
    }

    @Override
    public String format(Group object) {
        return object.getName();
    }

    @Override
    public String getHtml(Subject subject, WebHelper webHelper, Long processId, String name, Group value) {
        if (permissionDAO.isAllowed(SubjectPrincipalsHelper.getActor(subject), Permission.READ, value)) {
            HashMap<String, Object> params = Maps.newHashMap();
            params.put("id", value.getId());
            String href = webHelper.getActionUrl("/manage_executor", params);
            return "<a href=\"" + href + "\">" + value.getName() + "</>";
        } else {
            return value.getName();
        }
    }

}
