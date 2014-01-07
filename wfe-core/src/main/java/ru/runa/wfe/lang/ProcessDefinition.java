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

import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.definition.DefinitionFileDoesNotExistException;
import ru.runa.wfe.definition.Deployment;
import ru.runa.wfe.definition.IFileDataProvider;
import ru.runa.wfe.definition.InvalidDefinitionException;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;
import ru.runa.wfe.form.Interaction;
import ru.runa.wfe.task.Task;
import ru.runa.wfe.var.VariableDefinition;
import ru.runa.wfe.var.VariableUserType;
import ru.runa.wfe.var.format.FormatCommons;
import ru.runa.wfe.var.format.VariableFormat;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ProcessDefinition extends GraphElement implements IFileDataProvider {
    private static final long serialVersionUID = 1L;

    protected Deployment deployment;
    protected Map<String, byte[]> processFiles = Maps.newHashMap();
    protected StartState startState;
    protected final List<Node> nodes = Lists.newArrayList();
    protected final Map<String, SwimlaneDefinition> swimlaneDefinitions = Maps.newHashMap();
    protected final Map<String, Interaction> interactions = Maps.newHashMap();
    protected final List<VariableDefinition> variables = Lists.newArrayList();
    protected final Map<String, VariableDefinition> variablesMap = Maps.newHashMap();
    protected ProcessDefinitionAccessType accessType = ProcessDefinitionAccessType.Process;
    protected Map<String, SubprocessDefinition> embeddedSubprocesses = Maps.newHashMap();

    private boolean graphActionsEnabled;

    protected ProcessDefinition() {
    }

    public ProcessDefinition(Deployment deployment) {
        this.deployment = deployment;
        processDefinition = this;
    }

    public Long getId() {
        return deployment.getId();
    }

    @Override
    public String getName() {
        return deployment.getName();
    }

    @Override
    public void setName(String name) {
        deployment.setName(name);
    }

    @Override
    public String getDescription() {
        return deployment.getDescription();
    }

    @Override
    public void setDescription(String description) {
        deployment.setDescription(description);
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public ProcessDefinitionAccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(ProcessDefinitionAccessType accessType) {
        this.accessType = accessType;
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
        variablesMap.put(name, variableDefinition);
        variables.add(variableDefinition);
    }

    public VariableDefinition getVariable(String name, boolean searchInSwimlanes) {
        VariableDefinition variableDefinition = variablesMap.get(name);
        if (variableDefinition != null) {
            return variableDefinition;
        }
        if (searchInSwimlanes) {
            SwimlaneDefinition swimlaneDefinition = getSwimlane(name);
            if (swimlaneDefinition != null) {
                return swimlaneDefinition.toVariableDefinition();
            }
        }
        return buildVariable(name);
    }
    
    private VariableDefinition buildVariable(String name) {
        int dotIndex = name.indexOf(VariableUserType.DELIM);
        if (dotIndex != -1) {
            String parentName = name.substring(0, dotIndex);
            String attributeName = name.substring(dotIndex + 1);
            VariableDefinition parentDefinition = variablesMap.get(parentName);
            if (parentDefinition == null) {
                throw new InternalApplicationException("No variable found by name '" + parentName + 
                        "' when building user type attribute descriptor");
            }
            if (!parentDefinition.isComplex()) {
                throw new InternalApplicationException(parentDefinition + " is not user defined type");
            }
            VariableDefinition attributeDefinition = parentDefinition.getUserType().getAttributeNotNull(attributeName);
            return new VariableDefinition(name, attributeDefinition);
        }
        return null;
    }

    public VariableDefinition getVariableNotNull(String name, boolean searchInSwimlanes) {
        VariableDefinition variableDefinition = getVariable(name, searchInSwimlanes);
        if (variableDefinition == null) {
            throw new InternalApplicationException("variable '" + name + "' not found in " + this);
        }
        return variableDefinition;
    }

    public List<VariableDefinition> getVariables() {
        return variables;
    }

    public Interaction getInteractionNotNull(String nodeId) {
        Interaction interaction = interactions.get(nodeId);
        if (interaction == null) {
            InteractionNode node = (InteractionNode) getNodeNotNull(nodeId);
            interaction = new Interaction(node.getName(), node.getDescription(), null, null, null, false, null, null);
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

    public byte[] getGraphImageBytesNotNull() {
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
                    VariableFormat variableFormat = FormatCommons.create(variableDefinition);
                    Object value = variableFormat.parse(variableDefinition.getDefaultValue());
                    result.put(variableDefinition.getName(), value);
                } catch (Exception e) {
                    log.warn("Unable to get default value '" + variableDefinition.getDefaultValue() + "' in " + variableDefinition, e);
                }
            }
        }
        return result;
    }

    public StartState getStartStateNotNull() {
        Preconditions.checkNotNull(startState, "startState");
        return startState;
    }

    public List<Node> getNodes(boolean withEmbeddedSubprocesses) {
        List<Node> result = Lists.newArrayList(nodes);
        if (withEmbeddedSubprocesses) {
            for (SubprocessDefinition subprocessDefinition : embeddedSubprocesses.values()) {
                result.addAll(subprocessDefinition.getNodes(withEmbeddedSubprocesses));
            }
        }
        return result;
    }

    public Node addNode(Node node) {
        Preconditions.checkArgument(node != null, "can't add a null node to a processdefinition");
        nodes.add(node);
        node.processDefinition = this;
        if (node instanceof StartState) {
            if (startState != null) {
                throw new InvalidDefinitionException(getName(), "only one start-state allowed in a process");
            }
            startState = (StartState) node;
        }
        return node;
    }

    public void removeNode(Node node) {
        nodes.remove(node);
    }

    public Node getNode(String id) {
        Preconditions.checkNotNull(id);
        for (Node node : nodes) {
            if (id.equals(node.getNodeId())) {
                return node;
            }
        }
        for (SubprocessDefinition subprocessDefinition : embeddedSubprocesses.values()) {
            Node node = subprocessDefinition.getNode(id);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    public Node getNodeNotNull(String id) {
        Preconditions.checkNotNull(id);
        Node node = getNode(id);
        if (node != null) {
            return node;
        }
        throw new InternalApplicationException("node '" + id + "' not found");
    }

    public GraphElement getGraphElement(String id) {
        for (Node node : nodes) {
            if (id.equals(node.getNodeId())) {
                return node;
            }
            Action action = node.getAction(id);
            if (action != null) {
                return action;
            }
        }
        for (SwimlaneDefinition swimlaneDefinition : swimlaneDefinitions.values()) {
            if (id.equals(swimlaneDefinition.getNodeId())) {
                return swimlaneDefinition;
            }
        }
        for (SubprocessDefinition subprocessDefinition : embeddedSubprocesses.values()) {
            GraphElement graphElement = subprocessDefinition.getGraphElement(id);
            if (graphElement != null) {
                return graphElement;
            }
        }
        return null;
    }

    public GraphElement getGraphElementNotNull(String id) {
        GraphElement graphElement = getGraphElement(id);
        if (graphElement == null) {
            throw new InternalApplicationException("element '" + id + "' not found");
        }
        return graphElement;
    }

    @Override
    public GraphElement getParent() {
        return null;
    }

    public void addSwimlane(SwimlaneDefinition swimlaneDefinition) {
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

    public boolean ignoreSubsitutionRulesForTask(Task task) {
        InteractionNode interactionNode = (InteractionNode) getNodeNotNull(task.getNodeId());
        return interactionNode.getFirstTaskNotNull().isIgnoreSubsitutionRules();
    }

    public boolean isGraphActionsEnabled() {
        return graphActionsEnabled;
    }

    public void setGraphActionsEnabled(boolean graphActionsEnabled) {
        this.graphActionsEnabled = graphActionsEnabled;
    }

    public void addEmbeddedSubprocess(SubprocessDefinition subprocessDefinition) {
        embeddedSubprocesses.put(subprocessDefinition.getNodeId(), subprocessDefinition);
    }

    public List<String> getEmbeddedSubprocessNodeIds() {
        List<String> result = Lists.newArrayList();
        for (Node node : nodes) {
            if (node instanceof SubProcessState && ((SubProcessState) node).isEmbedded()) {
                result.add(node.getNodeId());
            }
        }
        return result;
    }

    public String getEmbeddedSubprocessNodeId(String subprocessName) {
        for (Node node : nodes) {
            if (node instanceof SubProcessState) {
                SubProcessState subProcessState = (SubProcessState) node;
                if (subProcessState.isEmbedded() && Objects.equal(subprocessName, subProcessState.getSubProcessName())) {
                    return node.getNodeId();
                }
            }
        }
        for (SubprocessDefinition subprocessDefinition : embeddedSubprocesses.values()) {
            String nodeId = subprocessDefinition.getEmbeddedSubprocessNodeId(subprocessName);
            if (nodeId != null) {
                return nodeId;
            }
        }
        return null;
    }

    public Map<String, SubprocessDefinition> getEmbeddedSubprocesses() {
        return embeddedSubprocesses;
    }

    public SubprocessDefinition getEmbeddedSubprocessById(String id) {
        return getEmbeddedSubprocesses().get(id);
    }

    public SubprocessDefinition getEmbeddedSubprocessByName(String name) {
        for (SubprocessDefinition subprocessDefinition : getEmbeddedSubprocesses().values()) {
            if (Objects.equal(name, subprocessDefinition.getName())) {
                return subprocessDefinition;
            }
        }
        return null;
    }

    public void mergeWithEmbeddedSubprocesses() {
        for (Node node : Lists.newArrayList(nodes)) {
            if (node instanceof SubProcessState) {
                SubProcessState subProcessState = (SubProcessState) node;
                if (subProcessState.isEmbedded()) {
                    SubprocessDefinition subprocessDefinition = getEmbeddedSubprocessByName(subProcessState.getSubProcessName());
                    Preconditions.checkNotNull(subprocessDefinition, "subprocessDefinition");
                    EmbeddedSubprocessStartNode startNode = subprocessDefinition.getStartStateNotNull();
                    startNode.addArrivingTransition(subProcessState.getArrivingTransitions().get(0));
                    startNode.setSubProcessState(subProcessState);
                    for (EmbeddedSubprocessEndNode endNode : subprocessDefinition.getEndNodes()) {
                        endNode.addLeavingTransition(subProcessState.getLeavingTransitions().get(0));
                        endNode.setSubProcessState(subProcessState);
                    }
                    subprocessDefinition.mergeWithEmbeddedSubprocesses();
                }
            }
        }
    }

    @Override
    public String toString() {
        if (deployment != null) {
            return deployment.toString();
        }
        return name;
    }
}