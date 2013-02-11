package ru.runa.wf.web.ftl.method;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ru.runa.service.ExecutorService;
import ru.runa.service.delegate.Delegates;
import ru.runa.wfe.commons.ftl.AjaxFreemarkerTag;
import ru.runa.wfe.presentation.BatchPresentation;
import ru.runa.wfe.presentation.BatchPresentationFactory;
import ru.runa.wfe.presentation.filter.FilterCriteria;
import ru.runa.wfe.presentation.filter.FilterCriteriaFactory;
import ru.runa.wfe.presentation.filter.StringFilterCriteria;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.user.Group;
import ru.runa.wfe.user.User;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import freemarker.template.TemplateModelException;

public class LegacyActorsMultiSelectTag extends AjaxFreemarkerTag {
    private static final long serialVersionUID = 1L;

    @Override
    protected String renderRequest() throws TemplateModelException {
        try {
            String variableName = getParameterAs(String.class, 0);
            Map<String, String> substitutions = new HashMap<String, String>();
            substitutions.put("VARIABLE", variableName);
            StringBuffer html = new StringBuffer();
            html.append(exportScript("scripts/LegacyActorsMultiSelectTag.js", substitutions, true));

            html.append("<div id=\"actorsMultiSelect").append(variableName).append("\"><div id=\"actorsMultiSelectCnt").append(variableName)
                    .append("\"></div><div id=\"actorsMultiSelectAddButton\"><a href=\"javascript:{}\" id=\"btnAdd").append(variableName)
                    .append("\">[ + ]</a></div></div>");
            return html.toString();
        } catch (Exception e) {
            throw new TemplateModelException(e);
        }
    }

    @Override
    public void processAjaxRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String displayFormat = getParameterAs(String.class, 1);
        String resultFormat = getParameterAs(String.class, 2);
        String groupName = getParameterAs(String.class, 3);
        boolean byLogin = "login".equals(displayFormat);
        StringBuffer json = new StringBuffer("[");
        String hint = request.getParameter("hint");
        List<Actor> actors = getActors(user, groupName, byLogin, hint);
        if (actors.size() == 0) {
            json.append("{\"code\": \"\", \"name\": \"\"}");
        }
        for (Actor actor : actors) {
            if (json.length() > 10) {
                json.append(", ");
            }
            Object data;
            if ("login".equals(resultFormat)) {
                data = actor.getName();
            } else if ("email".equals(resultFormat)) {
                data = actor.getEmail();
            } else if ("fio".equals(resultFormat)) {
                data = actor.getFullName();
            } else {
                data = actor.getCode();
            }
            json.append("{\"code\": \"").append(data).append("\", \"name\": \"");
            if (byLogin) {
                json.append(actor.getName());
            } else {
                json.append(actor.getFullName());
            }
            json.append("\"}");
        }
        json.append("]");
        response.getOutputStream().write(json.toString().getBytes(Charsets.UTF_8));
    }

    private List<Actor> getActors(User user, String groupName, boolean byLogin, String hint) throws TemplateModelException {
        try {
            int rangeSize = 50;
            List<Actor> actors = Lists.newArrayListWithExpectedSize(rangeSize);
            ExecutorService executorService = Delegates.getExecutorService();
            if (groupName != null && groupName.length() > 0) {
                Group group = executorService.getExecutorByName(user, groupName);
                List<Actor> groupActors = executorService.getGroupActors(user, group);
                for (Actor actor : groupActors) {
                    if (byLogin) {
                        if (actor.getName().startsWith(hint)) {
                            actors.add(actor);
                        }
                    } else {
                        if (actor.getFullName().startsWith(hint)) {
                            actors.add(actor);
                        }
                    }
                }
            } else {
                BatchPresentation batchPresentation = BatchPresentationFactory.ACTORS.createDefault();
                batchPresentation.setRangeSize(rangeSize);
                batchPresentation.setFieldsToSort(new int[] { 1 }, new boolean[] { true });
                if (hint.length() > 0) {
                    int filterIndex = byLogin ? 0 : 1;
                    Map<Integer, FilterCriteria> filterFieldsMap = batchPresentation.getFilteredFields();
                    StringFilterCriteria filterCriteriaEnd = (StringFilterCriteria) FilterCriteriaFactory.getFilterCriteria(batchPresentation,
                            filterIndex);
                    filterCriteriaEnd.applyFilterTemplates(new String[] { hint + "%" });
                    filterFieldsMap.put(filterIndex, filterCriteriaEnd);
                }
                // thid method used instead of getActors due to lack paging in
                // that
                // method
                for (Executor executor : executorService.getAll(user, batchPresentation)) {
                    if (executor instanceof Actor) {
                        actors.add((Actor) executor);
                    }
                }
            }
            return actors;
        } catch (Exception e) {
            throw new TemplateModelException(e);
        }
    }

}
