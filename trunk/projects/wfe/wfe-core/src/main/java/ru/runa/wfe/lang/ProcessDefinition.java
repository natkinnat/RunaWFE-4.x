/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package ru.runa.wfe.lang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.definition.DefinitionFileDoesNotExistException;
import ru.runa.wfe.definition.Deployment;
import ru.runa.wfe.definition.IFileDataProvider;
import ru.runa.wfe.definition.InvalidDefinitionException;
import ru.runa.wfe.form.Interaction;
import ru.runa.wfe.security.Identifiable;
import ru.runa.wfe.security.SecuredObjectType;
import ru.runa.wfe.task.Task;
import ru.runa.wfe.var.VariableDefinition;
import ru.runa.wfe.var.format.FormatCommons;
import ru.runa.wfe.var.format.VariableFormat;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ProcessDefinition extends GraphElement implements Identifiable, IFileDataProvider {
    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(ProcessDefinition.class);

    private final Deployment dBImpl;
    private Map<String, byte[]> processFiles = Maps.newHashMap();
    private StartState startState;
    private final List<Node> nodes = Lists.newArrayList();
    private final Map<String, SwimlaneDefinition> swimlaneDefinitions = Maps.newHashMap();
    private final Map<String, Interaction> interactions = Maps.newHashMap();
    private final List<VariableDefinition> variables = Lists.newArrayList();
    private final Map<String, VariableDefinition> variablesMap = Maps.newHashMap();
    private final Set<String> taskNamesToignoreSubstitutionRules = Sets.newHashSet();
    private Map<String, String> displayMappings = Maps.newHashMap();

    private static final String[] supportedEventTypes = new String[] { Event.EVENTTYPE_PROCESS_START, Event.EVENTTYPE_PROCESS_END,
            Event.EVENTTYPE_NODE_ENTER, Event.EVENTTYPE_NODE_LEAVE, Event.EVENTTYPE_TASK_CREATE, Event.EVENTTYPE_TASK_ASSIGN,
            Event.EVENTTYPE_TASK_START, Event.EVENTTYPE_TASK_END, Event.EVENTTYPE_TRANSITION, Event.EVENTTYPE_BEFORE_SIGNAL,
            Event.EVENTTYPE_AFTER_SIGNAL, Event.EVENTTYPE_SUPERSTATE_ENTER, Event.EVENTTYPE_SUPERSTATE_LEAVE, Event.EVENTTYPE_SUBPROCESS_CREATED,
            Event.EVENTTYPE_SUBPROCESS_END, Event.EVENTTYPE_TIMER };

    public ProcessDefinition(Deployment processDeploymentDBImpl) {
        dBImpl = processDeploymentDBImpl;
        processDefinition = this;
    }

    @Override
    public Long getId() {
        return dBImpl.getId();
    }

    @Override
    public SecuredObjectType getSecuredObjectType() {
        return SecuredObjectType.DEFINITION;
    }

    @Override
    public String getName() {
        return dBImpl.getName();
    }

    @Override
    public void setName(String name) {
        dBImpl.setName(name);
    }

    @Override
    public String getDescription() {
        return dBImpl.getDescription();
    }

    @Override
    public void setDescription(String description) {
        dBImpl.setDescription(description);
    }

    @Override
    public String[] getSupportedEventTypes() {
        return supportedEventTypes;
    }

    public Deployment getDBImpl() {
        return dBImpl;
    }

    /**
     * add a file to this definition.
     */
    public void addFile(String name, byte[] bytes) {
        processFiles.put(name, bytes);
    }

    public void addInteraction(String name, Interaction interaction) {
        interactions.put(name, interaction);
    }

    public void addVariable(String name, VariableDefinition variableDefinition) {
        variableDefinition.setDisplayFormat(displayMappings.get(variableDefinition.getFormat()));
        variablesMap.put(name, variableDefinition);
        variables.add(variableDefinition);
    }

    public VariableDefinition getVariable(String name) {
        return variablesMap.get(name);
    }

    public boolean isVariablePublic(String name) {
        VariableDefinition variableDefinition = getVariable(name);
        if (variableDefinition == null) {
            return false;
        }
        return variableDefinition.isPublicAccess();
    }

    public List<VariableDefinition> getVariables() {
        return variables;
    }

    public Interaction getInteractionNotNull(String nodeId) {
        Interaction interaction = interactions.get(nodeId);
        if (interaction == null) {
            interaction = new Interaction(null, null, null, false, null, null);
        }
        return interaction;
    }

    @Override
    public byte[] getFileData(String fileName) {
        Preconditions.checkNotNull(fileName, "fileName");
        return processFiles.get(fileName);
    }

    @Override
    public byte[] getFileDataNotNull(String fileName) {
        byte[] bytes = getFileData(fileName);
        if (bytes == null) {
            throw new DefinitionFileDoesNotExistException(fileName);
        }
        return bytes;
    }

    public byte[] getGraphImageBytes() {
        byte[] graphBytes = processDefinition.getFileData(IFileDataProvider.GRAPH_IMAGE_NEW_FILE_NAME);
        if (graphBytes == null) {
            graphBytes = processDefinition.getFileData(IFileDataProvider.GRAPH_IMAGE_OLD_FILE_NAME);
        }
        if (graphBytes == null) {
            throw new InternalApplicationException("Neither " + IFileDataProvider.GRAPH_IMAGE_NEW_FILE_NAME + " and "
                    + IFileDataProvider.GRAPH_IMAGE_OLD_FILE_NAME + " not found in process");
        }
        return graphBytes;
    }

    public Map<String, Object> getDefaultVariableValues() {
        Map<String, Object> result = new HashMap<String, Object>();
        for (VariableDefinition variableDefinition : variables) {
            if (variableDefinition.getDefaultValue() != null) {
                try {
                    VariableFormat variableFormat = FormatCommons.create(variableDefinition.getFormat());
                    Object value = variableFormat.parse(new String[] { variableDefinition.getDefaultValue() });
                    result.put(variableDefinition.getName(), value);
                } catch (Exception e) {
                    log.warn("Unable to get default value '" + variableDefinition.getDefaultValue() + "' of type " + variableDefinition.getFormat()
                            + " to " + variableDefinition.getName(), e);
                }
            }
        }
        return result;
    }

    public StartState getStartStateNotNull() {
        Preconditions.checkNotNull(startState, "startState");
        return startState;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public Node addNode(Node node) {
        Preconditions.checkArgument(node != null, "can't add a null node to a processdefinition");
        nodes.add(node);
        node.processDefinition = this;
        if (node instanceof StartState) {
            if (startState != null) {
                throw new InvalidDefinitionException("only one start-state allowed in a process");
            }
            startState = (StartState) node;
        }
        return node;
    }

    public Node getNodeNotNull(String id) {
        for (Node node : nodes) {
            if (id.equals(node.getNodeId())) {
                return node;
            }
        }
        throw new InternalApplicationException("node '" + id + "' not found");
    }

    /**
     * Unused
     * 
     * @param id
     * @return
     */
    public Action getActionNotNull(String id) {
        for (Node node : nodes) {
            if (id.startsWith(node.getNodeId() + "/")) {
                for (Entry<String, Event> entry : node.getEvents().entrySet()) {
                    for (Action action : entry.getValue().getActions()) {
                        if (id.equals(action.getName())) {
                            return action;
                        }
                    }
                }
            }
        }
        throw new InternalApplicationException("action '" + id + "' not found");
    }

    @Override
    public GraphElement getParent() {
        return null;
    }

    public void addSwimlane(SwimlaneDefinition swimlaneDefinition) {
        if (swimlaneDefinition.getDelegation() != null && swimlaneDefinition.getDelegation().getConfiguration() != null) {
            String conf = swimlaneDefinition.getDelegation().getConfiguration();
            swimlaneDefinition.setDisplayOrgFunction(conf);
            String[] orgFunctionParts = conf.split("\\(");
            if (orgFunctionParts.length == 2) {
                String mapping = displayMappings.get(orgFunctionParts[0].trim());
                if (mapping != null) {
                    swimlaneDefinition.setDisplayOrgFunction(mapping + " (" + orgFunctionParts[1]);
                }
            }
        }
        swimlaneDefinitions.put(swimlaneDefinition.getName(), swimlaneDefinition);
    }

    public Map<String, SwimlaneDefinition> getSwimlanes() {
        return swimlaneDefinitions;
    }

    public SwimlaneDefinition getSwimlane(String swimlaneName) {
        return swimlaneDefinitions.get(swimlaneName);
    }

    public SwimlaneDefinition getSwimlaneNotNull(String swimlaneName) {
        SwimlaneDefinition swimlaneDefinition = getSwimlane(swimlaneName);
        if (swimlaneDefinition == null) {
            throw new InternalApplicationException("swimlane '" + swimlaneName + "' not found in " + this);
        }
        return swimlaneDefinition;
    }

    public TaskDefinition getTaskNotNull(String nodeId) {
        for (Node node : getNodes()) {
            if (node instanceof InteractionNode) {
                for (TaskDefinition taskDefinition : ((InteractionNode) node).getTasks()) {
                    if (Objects.equal(nodeId, taskDefinition.getNodeId())) {
                        return taskDefinition;
                    }
                }
            }
        }
        throw new InternalApplicationException("task '" + nodeId + "' not found in " + this);
    }

    public Transition getTransitionNotNull(String fromNodeId, String transitionName) {
        Node node = getNodeNotNull(fromNodeId);
        return node.getLeavingTransitionNotNull(transitionName);
    }

    public boolean isSubsitutionIgnoredFor(Task task) {
        return taskNamesToignoreSubstitutionRules.contains(task.getNodeId());
    }

    public void addTaskNameToignoreSubstitutionRules(String nodeId) {
        taskNamesToignoreSubstitutionRules.add(nodeId);
    }

    public Map<String, String> getDisplayMappings() {
        return displayMappings;
    }

    @Override
    public String toString() {
        return name;
    }

}
