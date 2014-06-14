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
package ru.runa.wfe.graph.history;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.runa.wfe.audit.NodeEnterLog;
import ru.runa.wfe.audit.NodeLeaveLog;
import ru.runa.wfe.audit.NodeLog;
import ru.runa.wfe.audit.ProcessLog;
import ru.runa.wfe.audit.ReceiveMessageLog;
import ru.runa.wfe.audit.SendMessageLog;
import ru.runa.wfe.audit.SubprocessEndLog;
import ru.runa.wfe.audit.SubprocessStartLog;
import ru.runa.wfe.audit.TaskAssignLog;
import ru.runa.wfe.audit.TaskCreateLog;
import ru.runa.wfe.audit.TaskEndLog;
import ru.runa.wfe.audit.TaskLog;
import ru.runa.wfe.audit.TransitionLog;
import ru.runa.wfe.commons.CalendarUtil;
import ru.runa.wfe.execution.Process;
import ru.runa.wfe.graph.DrawProperties;
import ru.runa.wfe.graph.history.GraphImage.RenderHits;
import ru.runa.wfe.graph.history.figure.AbstractFigure;
import ru.runa.wfe.graph.history.figure.AbstractFigureFactory;
import ru.runa.wfe.graph.history.figure.TransitionFigureBase;
import ru.runa.wfe.graph.history.figure.uml.UMLFigureFactory;
import ru.runa.wfe.graph.history.model.BendpointModel;
import ru.runa.wfe.graph.history.model.DiagramModel;
import ru.runa.wfe.graph.history.model.NodeModel;
import ru.runa.wfe.graph.history.model.TransitionModel;
import ru.runa.wfe.graph.view.GraphElementPresentation;
import ru.runa.wfe.graph.view.MultiinstanceGraphElementPresentation;
import ru.runa.wfe.graph.view.SubprocessGraphElementPresentation;
import ru.runa.wfe.graph.view.TaskGraphElementPresentation;
import ru.runa.wfe.lang.EmbeddedSubprocessStartNode;
import ru.runa.wfe.lang.Node;
import ru.runa.wfe.lang.NodeType;
import ru.runa.wfe.lang.ProcessDefinition;
import ru.runa.wfe.lang.SubProcessState;
import ru.runa.wfe.lang.SubprocessDefinition;
import ru.runa.wfe.lang.Transition;
import ru.runa.wfe.task.dto.WfTaskFactory;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class GraphHistoryBuilder {
    private static final int heightBetweenNode = 40;
    private static final int heightForkJoinNode = 4;
    private final List<Executor> executors;
    private final ProcessDefinition processDefinition;
    private final Map<String, NodeModel> allNodes = Maps.newHashMap();
    private final Map<String, AbstractFigure> allNodeFigures = Maps.newHashMap();
    private final Map<TransitionFigureBase, RenderHits> transitionFigureBases = Maps.newHashMap();
    private final Map<AbstractFigure, RenderHits> nodeFigures = Maps.newHashMap();

    private final List<ProcessLog> processLogs = Lists.newArrayList();
    private final List<TransitionLog> transitionLogs = Lists.newArrayList();
    private final List<NodeLog> nodeLogs = Lists.newArrayList();
    private final List<TaskLog> taskLogs = Lists.newArrayList();
    private final DiagramModel diagramModel;
    private final AbstractFigureFactory factory;

    private final List<GraphElementPresentation> logElements = new ArrayList<GraphElementPresentation>();

    // PARALLEL_GATEWAY
    private final Set<String> parallelGatewayIsForkNodes = new HashSet<String>();

    public GraphHistoryBuilder(List<Executor> executors, WfTaskFactory taskObjectFactory, ProcessDefinition processDefinition,
            List<ProcessLog> fullProcessLogs, String subProcessId) {

        this.executors = executors;
        this.processDefinition = processDefinition;

        List<ProcessLog> processLogsExceptComposition = new ArrayList<ProcessLog>();
        Map<String, List<ProcessLog>> processLogsComposition = new HashMap<String, List<ProcessLog>>();

        Iterator<ProcessLog> processLogIterator = fullProcessLogs.iterator();
        while (processLogIterator.hasNext()) {
            ProcessLog processLog = processLogIterator.next();

            if (processLog instanceof NodeEnterLog && NodeType.SUBPROCESS == ((NodeEnterLog) processLog).getNodeType()) {

                for (Node node : processDefinition.getNodes(true)) {
                    if (node.getNodeId().equals(((NodeEnterLog) processLog).getNodeId())) {

                        if (node instanceof SubProcessState && ((SubProcessState) node).isEmbedded()) {
                            processLogsExceptComposition.add(processLog);

                            SubprocessDefinition subprocessDefinition = processDefinition.getEmbeddedSubprocessByName(((SubProcessState) node)
                                    .getSubProcessName());
                            String subProcessName = subprocessDefinition.getNodeId();

                            ProcessLog processLogFinish = parseEmbeddedLogs(processLogIterator, processLogsComposition, subProcessName);
                            if (processLogFinish != null) {
                                processLogsExceptComposition.add(processLogFinish);
                            }

                            break;
                        }
                    }
                }
            } else {
                processLogsExceptComposition.add(processLog);
            }
        }

        List<ProcessLog> processLogForProcessing = (subProcessId != null && !"null".equals(subProcessId)) ? processLogsComposition.get(subProcessId)
                : processLogsExceptComposition;
        for (ProcessLog processLog : processLogForProcessing) {
            if (processLog instanceof TransitionLog) {
                transitionLogs.add((TransitionLog) processLog);
            } else if (processLog instanceof NodeLog) {
                if (subProcessId != null && !"null".equals(subProcessId) && processLog instanceof NodeEnterLog
                        && NodeType.START_EVENT == ((NodeLog) processLog).getNodeType()) {
                    continue;
                }
                nodeLogs.add((NodeLog) processLog);
            } else if (processLog instanceof TaskLog) {
                taskLogs.add((TaskLog) processLog);
            }
        }

        String forkNodeId = null;
        int countLeaveLog = 0;
        for (ProcessLog processLog : processLogForProcessing) {
            if (processLog instanceof NodeEnterLog && ((NodeEnterLog) processLog).getNodeType() == NodeType.PARALLEL_GATEWAY) {
                countLeaveLog = 0;
                forkNodeId = processLog.getNodeId();
            }
            if (forkNodeId != null) {
                if (processLog instanceof NodeLeaveLog && processLog.getNodeId().equals(forkNodeId)) {
                    countLeaveLog++;
                } else if (processLog instanceof NodeLeaveLog && !processLog.getNodeId().equals(forkNodeId)) {
                    if (countLeaveLog > 1) {
                        parallelGatewayIsForkNodes.add(forkNodeId);
                    }
                    forkNodeId = null;
                    countLeaveLog = 0;
                }
            }
        }

        this.processLogs.addAll(processLogForProcessing);
        diagramModel = (subProcessId != null && !"null".equals(subProcessId)) ? DiagramModel.load(processDefinition
                .getEmbeddedSubprocessById(subProcessId)) : DiagramModel.load(processDefinition);
        factory = new UMLFigureFactory();
    }

    private ProcessLog parseEmbeddedLogs(Iterator<ProcessLog> processLogIterator, Map<String, List<ProcessLog>> processLogsComposition,
            String subProcessName) {
        List<ProcessLog> subProcessLogs = processLogsComposition.get(subProcessName);
        if (subProcessLogs == null) {
            subProcessLogs = new ArrayList<ProcessLog>();
            processLogsComposition.put(subProcessName, subProcessLogs);
        }

        while (processLogIterator.hasNext()) {
            ProcessLog processLog = processLogIterator.next();

            if (processLog instanceof NodeEnterLog && NodeType.SUBPROCESS == ((NodeEnterLog) processLog).getNodeType()) {

                for (Node node : processDefinition.getNodes(true)) {
                    if (node.getNodeId().equals(((NodeEnterLog) processLog).getNodeId())) {

                        if (node instanceof SubProcessState && ((SubProcessState) node).isEmbedded()) {
                            subProcessLogs.add(processLog);

                            SubprocessDefinition subprocessDefinition = processDefinition.getEmbeddedSubprocessByName(((SubProcessState) node)
                                    .getSubProcessName());
                            String subProcessNameEmb = subprocessDefinition.getNodeId();

                            ProcessLog processLogFinish = parseEmbeddedLogs(processLogIterator, processLogsComposition, subProcessNameEmb);
                            if (processLogFinish != null) {
                                subProcessLogs.add(processLogFinish);
                            }

                            break;
                        }
                    }
                }
            } else if (processLog instanceof NodeLeaveLog && NodeType.SUBPROCESS == ((NodeLeaveLog) processLog).getNodeType()) {
                return processLog;
            } else {
                subProcessLogs.add(processLog);
            }
        }

        return null;
    }

    public byte[] createDiagram(Process process, List<Transition> passedTransitions) throws Exception {
        String startNodeId = null;
        Map<String, Integer> widthTokens = new HashMap<String, Integer>();
        Map<String, Integer> heightTokens = new HashMap<String, Integer>();
        Map<String, String> parentNodeInTokenForNodeMap = new HashMap<String, String>();
        Map<String, Long> nodeRepetitionCount = new HashMap<String, Long>();

        for (NodeLog log : nodeLogs) {
            if (log instanceof NodeLeaveLog && NodeType.START_EVENT == log.getNodeType()) {
                startNodeId = log.getNodeId() + ":" + log.getTokenId();
                parentNodeInTokenForNodeMap.put(startNodeId, startNodeId);
                calculateWidthTokensInGraph(startNodeId, log.getId(), widthTokens, parentNodeInTokenForNodeMap);
                break;
            }
        }

        calculateCoordinatesForNodes(widthTokens, heightTokens, parentNodeInTokenForNodeMap);

        // render transition
        for (NodeLog log : nodeLogs) {
            if (log instanceof NodeEnterLog || log instanceof NodeLeaveLog) {
                String nodeId = log.getNodeId();

                for (Node node : processDefinition.getNodes(true)) {
                    if (node.getNodeId().equals(nodeId)) {
                        if (isNodePresentInGraph(log)) {
                            String correctNodeId = getNodeIdIncludeRepetition(nodeId + ":" + log.getTokenId(), nodeRepetitionCount);
                            NodeModel nodeModel = allNodes.get(correctNodeId);
                            Preconditions.checkNotNull(nodeModel, "Node model not found by id " + nodeId);

                            AbstractFigure nodeFigure = allNodeFigures.get(correctNodeId);
                            nodeFigures.put(nodeFigure, new RenderHits(DrawProperties.getBaseColor()));
                        }

                        if (log instanceof NodeLeaveLog && !(log instanceof SubprocessEndLog && (NodeType.MULTI_SUBPROCESS == log.getNodeType()))) {
                            Long tokenId = log.getTokenId();

                            if (isForkNode(log.getNodeId(), log.getTokenId(), log.getNodeType())) {
                                for (ProcessLog correctTokenLog : processLogs) {
                                    if (correctTokenLog.getId().equals(log.getId())) {
                                        break;
                                    }
                                    if (correctTokenLog instanceof NodeEnterLog
                                            && ((NodeEnterLog) correctTokenLog).getNodeId().equals(log.getNodeId())) {
                                        tokenId = correctTokenLog.getTokenId();
                                    }
                                }
                            }

                            Long duplicateCount = nodeRepetitionCount.get(nodeId + ":" + tokenId);
                            String correctNodeId = nodeId + ":" + tokenId + ":" + duplicateCount;
                            NodeModel nodeModel = allNodes.get(correctNodeId);
                            Preconditions.checkNotNull(nodeModel, "Node model not found by id " + nodeId);

                            AbstractFigure nodeFigure = allNodeFigures.get(correctNodeId);
                            for (Transition transition : node.getLeavingTransitions()) {
                                boolean correctTransition = false;
                                for (ProcessLog findTransitionLog : processLogs) {
                                    if (findTransitionLog.getId() > log.getId() && findTransitionLog instanceof TransitionLog) {
                                        correctTransition = ((TransitionLog) findTransitionLog).getTransitionId().equals(transition.getName());
                                        break;
                                    }
                                }
                                if (!correctTransition) {
                                    continue;
                                }

                                TransitionModel transitionModel = nodeModel.getTransition(transition.getName());
                                transitionModel.getBendpoints().clear();

                                if (findNextTransitionLog(log, correctNodeId) != null) {
                                    Date transitionLeaveDate = findNextTransitionLog(log, correctNodeId).getCreateDate();
                                    transitionModel.setName(CalendarUtil.formatDateTime(transitionLeaveDate));
                                }

                                if (diagramModel.isShowActions()) {
                                    transitionModel.setActionsCount(GraphImageHelper.getTransitionActionsCount(transition));
                                }

                                String toNodeId = null;
                                Node toNode = transition.getTo();
                                if (toNode instanceof EmbeddedSubprocessStartNode) {
                                    toNodeId = ((EmbeddedSubprocessStartNode) toNode).getTransitionNodeId(true);
                                } else {
                                    toNodeId = toNode.getNodeId();
                                }
                                Long duplicateTransitionCount = nodeRepetitionCount.get(toNodeId + ":" + log.getTokenId());
                                if (duplicateTransitionCount == null) {
                                    duplicateTransitionCount = new Long(0);
                                }
                                duplicateTransitionCount = duplicateTransitionCount + 1;

                                AbstractFigure figureTo = allNodeFigures.get(toNodeId + ":" + log.getTokenId() + ":" + duplicateTransitionCount);

                                if (isJoinNode(toNodeId, log.getTokenId(), diagramModel.getNodeNotNull(toNodeId).getType())) {
                                    for (ProcessLog tempLog : processLogs) {
                                        if (tempLog.getId() > log.getId() && tempLog instanceof NodeLeaveLog
                                                && ((NodeLeaveLog) tempLog).getNodeId().equals(toNodeId)) {
                                            duplicateTransitionCount = nodeRepetitionCount.get(toNodeId + ":" + tempLog.getTokenId());
                                            if (duplicateTransitionCount == null) {
                                                duplicateTransitionCount = new Long(0);
                                            }
                                            duplicateTransitionCount = duplicateTransitionCount + 1;
                                            figureTo = allNodeFigures.get(toNodeId + ":" + tempLog.getTokenId() + ":" + duplicateTransitionCount);
                                            break;
                                        }
                                    }
                                }
                                if (figureTo != null) {
                                    if (isForkNode(nodeModel.getNodeId(), log.getTokenId(), nodeModel.getType())) {
                                        BendpointModel bendpointModel = new BendpointModel();
                                        bendpointModel.setX(figureTo.getCoords()[0] + figureTo.getCoords()[2] / 2);
                                        bendpointModel.setY(nodeModel.getY() + nodeModel.getHeight() / 2);
                                        transitionModel.addBendpoint(bendpointModel);
                                    }
                                    if (isJoinNode(nodeModel.getNodeId(), log.getTokenId(), nodeModel.getType())) {
                                        BendpointModel bendpointModel = new BendpointModel();
                                        bendpointModel.setX(nodeModel.getX() + nodeModel.getWidth() / 2);
                                        bendpointModel.setY(figureTo.getCoords()[1]);
                                        transitionModel.addBendpoint(bendpointModel);
                                    }
                                    TransitionFigureBase transitionFigureBase = factory.createTransitionFigure(transitionModel, nodeFigure, figureTo);
                                    transitionFigureBase.init(transitionModel, nodeFigure, figureTo);
                                    if (Objects.equal(nodeModel.getTimerTransitionName(), transitionModel.getName())) {
                                        transitionFigureBase.setTimerInfo(GraphImageHelper.getTimerInfo(node));
                                    }
                                    nodeFigure.addTransition(transition.getName(), transitionFigureBase);
                                    transitionFigureBases.put(transitionFigureBase, new RenderHits(DrawProperties.getTransitionColor()));
                                }
                            }
                        }

                        break;
                    }
                }
            }
        }

        // find max height
        int height = 0;
        for (String nodeId : heightTokens.keySet()) {
            if (heightTokens.get(nodeId) != null && heightTokens.get(nodeId) > height) {
                height = heightTokens.get(nodeId);
            }
        }

        diagramModel.setHeight(height + 100);
        diagramModel.setWidth(widthTokens.get(startNodeId) + 20);
        GraphImage graphImage = new GraphImage(null, diagramModel, transitionFigureBases, nodeFigures, false);
        return graphImage.getImageBytes();
    }

    private boolean isForkNode(String nodeId, Long tokenId, NodeType nodeType) {
        return NodeType.FORK == nodeType || (NodeType.PARALLEL_GATEWAY == nodeType && parallelGatewayIsForkNodes.contains(nodeId));
    }

    private boolean isJoinNode(String nodeId, Long tokenId, NodeType nodeType) {
        return NodeType.JOIN == nodeType || (NodeType.PARALLEL_GATEWAY == nodeType && !parallelGatewayIsForkNodes.contains(nodeId));
    }

    private String getNodeIdIncludeRepetition(String nodeId, Map<String, Long> nodeRepetitionCount) {
        Long duplicateCount = nodeRepetitionCount.get(nodeId);
        if (duplicateCount == null) {
            duplicateCount = new Long(0);
        }

        duplicateCount = duplicateCount + 1;
        nodeRepetitionCount.put(nodeId, duplicateCount);
        return nodeId + ":" + duplicateCount;
    }

    /**
     * Method calculates the width of tokens. Token is line that contains nodes and transitions. Fork node creates subtokens. Join node finishes all
     * subtokens node and continue the main token.
     * 
     * @param startTokenNodeId
     *            - the first node in token.
     * @param logId
     * @param tokenWidth
     *            - object contains start node in token and the width of token.
     * @param parentNodeInTokenForNodeMap
     *            - object populate parent node id for all nodes.
     * @return last nodeId in graph
     */
    private String calculateWidthTokensInGraph(String startTokenNodeId, Long logId, Map<String, Integer> tokenWidth,
            Map<String, String> parentNodeInTokenForNodeMap) {
        String nodeId = startTokenNodeId;

        boolean firstNodeInit = true;

        while (nodeId != null) {
            NodeModel nodeModel = diagramModel.getNodeNotNull(nodeId.split(":")[0]);
            initNodeModel(nodeModel, nodeId.split(":")[0]);

            if (!firstNodeInit) {
                parentNodeInTokenForNodeMap.put(nodeId, startTokenNodeId);
                if (isJoinNode(nodeId.split(":")[0], Long.valueOf(nodeId.split(":")[1]), nodeModel.getType())) {
                    boolean startFind = false;
                    for (NodeLog joinEnter : nodeLogs) {
                        if (joinEnter instanceof NodeEnterLog && ((NodeEnterLog) joinEnter).getNodeId().equals(nodeId.split(":")[0])
                                && ((NodeEnterLog) joinEnter).getTokenId().toString().equals(nodeId.split(":")[1])) {
                            startFind = true;
                            continue;
                        } else if (joinEnter instanceof NodeLeaveLog && ((NodeLeaveLog) joinEnter).getNodeId().equals(nodeId.split(":")[0])
                                && startFind) {
                            parentNodeInTokenForNodeMap.put(((NodeLeaveLog) joinEnter).getNodeId() + ":" + ((NodeLeaveLog) joinEnter).getTokenId(),
                                    startTokenNodeId);
                            break;
                        }
                    }
                }
            }
            firstNodeInit = false;

            setCurrentTokenWidth(tokenWidth, startTokenNodeId, nodeModel.getWidth());

            if (isForkNode(nodeId.split(":")[0], Long.valueOf(nodeId.split(":")[1]), nodeModel.getType())) {
                List<String> nextNodeIds = getNextNodesInGraphForFork(logId, nodeId);

                for (String nextNodeId : nextNodeIds) {
                    parentNodeInTokenForNodeMap.put(nextNodeId, nextNodeId);
                }

                // calculate the width of token for fork tokens
                int resultWidth = 0;
                String joinNodeId = null;
                Long forkStartLog = logId;
                for (String nextNodeId : nextNodeIds) {
                    logId = getNodeLeaveLog(forkStartLog, nextNodeId);
                    if (logId == null) {
                        continue;
                    }

                    String retVal = calculateWidthTokensInGraph(nextNodeId, logId, tokenWidth, parentNodeInTokenForNodeMap);
                    resultWidth += tokenWidth.get(nextNodeId);
                    if (retVal != null) {
                        joinNodeId = retVal;
                    }
                }

                setCurrentTokenWidth(tokenWidth, startTokenNodeId, resultWidth);

                if (joinNodeId == null) {
                    return nodeId;
                } else {
                    nodeId = joinNodeId;
                }

                // return joinNodeId;
            } else if (isJoinNode(nodeId.split(":")[0], Long.valueOf(nodeId.split(":")[1]), nodeModel.getType())) {
                // parentNodeInTokenForNodeMap.put(nodeId, startTokenNodeId);
                return nodeId;
            }

            // get next node
            logId = getNodeLeaveLog(logId, nodeId);
            if (logId == null) {
                return nodeId;
            }

            List<String> nextNodeIds = getNextNodesInGraph(logId, nodeId);

            nodeId = nextNodeIds != null && nextNodeIds.size() > 0 ? nextNodeIds.get(0) : null;
        }

        return nodeId;
    }

    private void initNodeModel(NodeModel nodeModel, String nodeId) {
        for (Node node : processDefinition.getNodes(true)) {
            if (node.getNodeId().equals(nodeId)) {
                GraphImageHelper.initNodeModel(node, nodeModel);

                if (nodeModel.getType() == NodeType.START_EVENT) {
                    if (nodeModel.getWidth() < 100) {
                        nodeModel.setWidth(100);
                    }

                    nodeModel.setHeight(nodeModel.getHeight() + 10);
                }

                break;
            }
        }
    }

    private void setCurrentTokenWidth(Map<String, Integer> tokenWidth, String nodeId, int value) {
        Integer width = tokenWidth.get(nodeId);
        if (width == null || (width != null && width.intValue() < (value + 10))) {
            tokenWidth.put(nodeId, value + 10);
        }
    }

    private Long getNodeLeaveLog(Long currentLogId, String nodeId) {
        boolean isJoin = isJoinNode(nodeId.split(":")[0], Long.valueOf(nodeId.split(":")[1]), diagramModel.getNodeNotNull(nodeId.split(":")[0])
                .getType());

        if (isJoin) {
            for (ProcessLog log : nodeLogs) {
                if (log.getId() >= currentLogId && log instanceof NodeLeaveLog) {
                    if (((NodeLog) log).getNodeId().equals(nodeId.split(":")[0])) {
                        return log.getId();
                    }
                }
            }
        } else {
            for (ProcessLog log : nodeLogs) {
                if (log.getId() >= currentLogId && log instanceof NodeLeaveLog) {
                    if (((NodeLog) log).getNodeId().equals(nodeId.split(":")[0])
                            && ((NodeLog) log).getTokenId().toString().equals(nodeId.split(":")[1])) {
                        return log.getId();
                    }
                }
            }
        }

        return currentLogId;
    }

    private List<String> getNextNodesInGraphForFork(Long currentLogId, String nodeId) {
        Set<String> returnNodes = new HashSet<String>();

        for (ProcessLog log : processLogs) {
            if (log.getId() > currentLogId && log instanceof TransitionLog) {
                TransitionLog transitionLog = (TransitionLog) log;
                if (transitionLog.getFromNodeId() != null && transitionLog.getToNodeId() != null
                        && nodeId.split(":")[0].equals(transitionLog.getFromNodeId())) {
                    for (ProcessLog fullLog : processLogs) {
                        if (fullLog.getId() > transitionLog.getId() && fullLog instanceof NodeEnterLog
                                && ((NodeEnterLog) fullLog).getNodeId().equals(transitionLog.getToNodeId())
                                && transitionLog.getTokenId().toString().equals(((NodeEnterLog) fullLog).getTokenId().toString())) {
                            returnNodes.add(transitionLog.getToNodeId() + ":" + fullLog.getTokenId());
                            break;
                        }
                    }
                }
            } else if (log.getId() > currentLogId && log instanceof NodeLeaveLog && !((NodeLeaveLog) log).getNodeId().equals(nodeId.split(":")[0])) {
                break;
            }
        }

        return new ArrayList<String>(returnNodes);
    }

    private List<String> getNextNodesInGraph(Long currentLogId, String nodeId) {
        Set<String> returnNodes = new HashSet<String>();

        boolean isJoin = isJoinNode(nodeId.split(":")[0], Long.valueOf(nodeId.split(":")[1]), diagramModel.getNodeNotNull(nodeId.split(":")[0])
                .getType());

        if (isJoin) {
            // transition only in period leave and enter
            for (ProcessLog log : processLogs) {
                if (log.getId() >= currentLogId) {
                    if (log instanceof TransitionLog) {
                        TransitionLog transitionLog = (TransitionLog) log;
                        if (transitionLog.getFromNodeId() != null && transitionLog.getToNodeId() != null
                                && nodeId.split(":")[0].equals(transitionLog.getFromNodeId())) {
                            // &&
                            // nodeId.split(":")[1].equals(transitionLog.getTokenId().toString()))
                            // {
                            returnNodes.add(transitionLog.getToNodeId() + ":" + transitionLog.getTokenId());
                        }
                    } else if (log instanceof NodeEnterLog) {
                        break;
                    }
                }
            }
        } else {
            // transition only in period leave and enter
            for (ProcessLog log : processLogs) {
                if (log.getId() >= currentLogId) {
                    if (log instanceof TransitionLog) {
                        TransitionLog transitionLog = (TransitionLog) log;
                        if (transitionLog.getFromNodeId() != null && transitionLog.getToNodeId() != null
                                && nodeId.split(":")[0].equals(transitionLog.getFromNodeId())
                                && nodeId.split(":")[1].equals(transitionLog.getTokenId().toString())) {
                            returnNodes.add(transitionLog.getToNodeId() + ":" + transitionLog.getTokenId());
                        }
                    } else if (log instanceof NodeEnterLog) {
                        break;
                    }
                }
            }
        }

        return new ArrayList<String>(returnNodes);
    }

    private boolean isNodePresentInGraph(NodeLog log) {
        return !(log instanceof ReceiveMessageLog || log instanceof SendMessageLog)
                && !((log instanceof SubprocessStartLog || log instanceof SubprocessEndLog) && (NodeType.MULTI_SUBPROCESS == log.getNodeType() || NodeType.SUBPROCESS == log
                        .getNodeType()))
                && ((log instanceof NodeEnterLog && !isJoinNode(log.getNodeId(), log.getTokenId(), log.getNodeType())) || (log instanceof NodeLeaveLog && (NodeType.START_EVENT == log
                        .getNodeType() || isJoinNode(log.getNodeId(), log.getTokenId(), log.getNodeType()))));
    }

    /**
     * Method calculates X and Y coordinates for figures which present nodes in the graph.
     * 
     * @param widthTokens
     *            - object contains start node in token and the width of token.
     * @param heightTokens
     *            - object contains start node in token and the height of token
     * @param rootNodeForNodeMap
     *            - object contains parent node id for all nodes.
     */
    private void calculateCoordinatesForNodes(Map<String, Integer> widthTokens, Map<String, Integer> heightTokens,
            Map<String, String> rootNodeForNodeMap) {
        int startY = 10;
        Map<String, List<String>> forkNodes = new HashMap<String, List<String>>();
        Map<String, Long> nodeRepetitionCount = new HashMap<String, Long>();

        for (NodeLog log : nodeLogs) {
            if (isNodePresentInGraph(log)) {
                String nodeId = log.getNodeId();
                String correctNodeId = nodeId + ":" + log.getTokenId();
                String rootNodeId = rootNodeForNodeMap.get(correctNodeId);
                Integer height = heightTokens.get(rootNodeId);
                int x = 0;
                if (height == null) {
                    height = startY;

                    for (String forkRootNodeId : forkNodes.keySet()) {
                        List<String> nodes = forkNodes.get(forkRootNodeId);
                        if (nodes != null && nodes.contains(rootNodeId)) {
                            height = heightTokens.get(forkRootNodeId);
                        }
                    }
                }

                for (Node node : processDefinition.getNodes(true)) {
                    if (node.getNodeId().equals(nodeId)) {
                        NodeModel nodeModelForClone = diagramModel.getNodeNotNull(node.getNodeId());
                        NodeModel nodeModel = new NodeModel();
                        nodeModel.setNodeId(nodeModelForClone.getNodeId());
                        nodeModel.setMinimizedView(nodeModelForClone.isMinimizedView());
                        nodeModel.setTimerTransitionName(nodeModelForClone.getTimerTransitionName());
                        nodeModel.setAsync(nodeModelForClone.isAsync());
                        nodeModel.setSwimlane(nodeModelForClone.getSwimlane());
                        nodeModel.setX(nodeModelForClone.getX());
                        nodeModel.setY(nodeModelForClone.getY());
                        nodeModel.setWidth(nodeModelForClone.getWidth());
                        nodeModel.setHeight(nodeModelForClone.getHeight());
                        for (TransitionModel transitionModelForClone : nodeModelForClone.getTransitions().values()) {
                            TransitionModel transitionModel = new TransitionModel();
                            transitionModel.setName(transitionModelForClone.getName());
                            transitionModel.setActionsCount(transitionModelForClone.getActionsCount());
                            transitionModel.setNodeFrom(transitionModelForClone.getNodeFrom());
                            transitionModel.setNodeTo(transitionModelForClone.getNodeTo());
                            nodeModel.addTransition(transitionModel);
                        }

                        if (diagramModel.isShowActions()) {
                            nodeModel.setActionsCount(GraphImageHelper.getNodeActionsCount(node));
                        }
                        GraphImageHelper.initNodeModel(node, nodeModel);

                        Integer width = widthTokens.get(rootNodeId);

                        if (width == null) {
                            width = 10;
                        }

                        x = addedLeftTokenWidthIfExist(width / 2, forkNodes, widthTokens, rootNodeId);

                        if (isJoinNode(log.getNodeId(), log.getTokenId(), log.getNodeType())) {
                            for (String forkRootNodeId : forkNodes.keySet()) {
                                List<String> nodes = forkNodes.get(forkRootNodeId);
                                if (nodes != null && nodes.contains(rootNodeId)) {
                                    width = widthTokens.get(forkRootNodeId);

                                    if (width == null) {
                                        width = 10;
                                    }

                                    x = addedLeftTokenWidthIfExist(width / 2, forkNodes, widthTokens, forkRootNodeId);
                                }
                            }

                        }

                        if (isForkNode(log.getNodeId(), log.getTokenId(), log.getNodeType())) {
                            List<String> nodes = getNextNodesInGraphForFork(log.getId(), correctNodeId);
                            forkNodes.put(rootNodeId, nodes);

                            nodeModel.setWidth(width);
                            nodeModel.setHeight(heightForkJoinNode);
                        }

                        if (isJoinNode(log.getNodeId(), log.getTokenId(), log.getNodeType())) {
                            for (String forkRootNodeId : forkNodes.keySet()) {
                                List<String> nodes = forkNodes.get(forkRootNodeId);
                                if (nodes != null && nodes.contains(rootNodeId)) {

                                    nodeModel.setWidth(widthTokens.get(forkRootNodeId));
                                    nodeModel.setHeight(heightForkJoinNode);
                                }
                            }

                            height = updateAllRootTokenHeight(height, nodeModel, forkNodes, heightTokens, rootNodeId);
                        }

                        nodeModel.setY(height);
                        nodeModel.setX(x - nodeModel.getWidth() / 2);

                        if (!isJoinNode(log.getNodeId(), log.getTokenId(), log.getNodeType())) {
                            if (NodeType.SUBPROCESS == log.getNodeType()) {
                                if (isLastSubprocessLogForMultiinstanceOnly(log)) {
                                    height += (nodeModel.getHeight() + heightBetweenNode);
                                    heightTokens.put(rootNodeId, height);
                                }
                            } else {
                                height += (nodeModel.getHeight() + heightBetweenNode);
                                heightTokens.put(rootNodeId, height);
                            }
                        }

                        correctNodeId = getNodeIdIncludeRepetition(correctNodeId, nodeRepetitionCount);
                        allNodes.put(correctNodeId, nodeModel);
                        if (NodeType.PARALLEL_GATEWAY == nodeModel.getType()) {
                            if (!parallelGatewayIsForkNodes.contains(nodeId)) {
                                nodeModel.setType(NodeType.JOIN);
                            }
                            if (parallelGatewayIsForkNodes.contains(nodeId)) {
                                nodeModel.setType(NodeType.FORK);
                            }
                        }
                        if (NodeType.EXCLUSIVE_GATEWAY == nodeModel.getType()) {
                            nodeModel.setType(NodeType.DECISION);
                        }

                        AbstractFigure nodeFigure = factory.createFigure(nodeModel, false);
                        allNodeFigures.put(correctNodeId, nodeFigure);
                        if (log instanceof NodeEnterLog) {
                            addedTooltipOnGraph(node, nodeFigure, nodeModel, (NodeEnterLog) log);
                        }

                        break;
                    }
                }
            }
        }
    }

    private int addedLeftTokenWidthIfExist(int x, Map<String, List<String>> forkNodes, Map<String, Integer> tokenWidths, String rootNodeId) {
        for (String forkRootNodeId : forkNodes.keySet()) {
            List<String> nodes = forkNodes.get(forkRootNodeId);
            if (nodes != null && nodes.contains(rootNodeId)) {
                int leftTokenWidth = addedLeftTokenWidthIfExist(0, forkNodes, tokenWidths, forkRootNodeId);
                for (String forkNode : nodes) {
                    if (forkNode.equals(rootNodeId)) {
                        break;
                    }

                    leftTokenWidth += tokenWidths.get(forkNode);
                }

                x += leftTokenWidth;
            }
        }

        return x;
    }

    private int updateAllRootTokenHeight(int height, NodeModel nodeModel, Map<String, List<String>> forkNodes, Map<String, Integer> tokenHieght,
            String rootNodeId) {
        for (String tempForkRootNodeId : forkNodes.keySet()) {
            List<String> nodes = forkNodes.get(tempForkRootNodeId);
            if (nodes != null && nodes.contains(rootNodeId)) {
                for (String forkNode : nodes) {
                    if (tokenHieght.get(forkNode) != null && tokenHieght.get(tempForkRootNodeId) != null
                            && tokenHieght.get(forkNode) > tokenHieght.get(tempForkRootNodeId)) {
                        height = tokenHieght.get(forkNode);
                        tokenHieght.put(tempForkRootNodeId, height + nodeModel.getHeight() + heightBetweenNode);
                    }
                }
            }
        }

        return height;
    }

    private TransitionLog findNextTransitionLog(NodeLog log, String nodeId) {
        for (TransitionLog tempLog : transitionLogs) {
            if (tempLog.getId() > log.getId() && tempLog.getFromNodeId().equals(nodeId.split(":")[0])) {
                return tempLog;
            }
        }

        return null;
    }

    private boolean isLastSubprocessLogForMultiinstanceOnly(NodeLog log) {
        boolean multiinstance = false;
        for (ProcessLog processLog : processLogs) {
            if (processLog instanceof NodeEnterLog && ((NodeEnterLog) processLog).getNodeType() == NodeType.MULTI_SUBPROCESS) {
                multiinstance = true;
            } else if (processLog instanceof NodeEnterLog) {
                multiinstance = false;
            }
            if (processLog.getId() >= log.getId()) {
                if (processLog instanceof NodeEnterLog && ((NodeEnterLog) processLog).getNodeType() == NodeType.SUBPROCESS && multiinstance) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private void addedTooltipOnGraph(Node node, AbstractFigure figure, NodeModel nodeModel, NodeEnterLog nodeEnterlog) {
        // find node leave log and taskEnterLog
        NodeLeaveLog nodeLeaveLog = null;
        for (NodeLog nodeLog : nodeLogs) {
            if (nodeLog.getId() > nodeEnterlog.getId() && nodeLog instanceof NodeLeaveLog
                    && nodeEnterlog.getNodeId().equals(((NodeLeaveLog) nodeLog).getNodeId())) {
                nodeLeaveLog = (NodeLeaveLog) nodeLog;
                break;
            }
        }

        if (nodeLeaveLog == null) {
            return;
        }

        GraphElementPresentation presentation;
        switch (nodeModel.getType()) {
        case SUBPROCESS:
            presentation = new SubprocessGraphElementPresentation();
            ((SubprocessGraphElementPresentation) presentation).setSubprocessAccessible(true);
            for (NodeLog nodeLog : nodeLogs) {
                if (nodeLog instanceof SubprocessStartLog && nodeEnterlog.getNodeId().equals(((SubprocessStartLog) nodeLog).getNodeId())) {
                    ((SubprocessGraphElementPresentation) presentation).setSubprocessId(((SubprocessStartLog) nodeLog).getSubprocessId());
                    break;
                }

                if (nodeLog instanceof NodeEnterLog && nodeEnterlog.getNodeId().equals(((NodeEnterLog) nodeLog).getNodeId())
                        && ((NodeEnterLog) nodeLog).getNodeType() == NodeType.SUBPROCESS && node instanceof SubProcessState) {
                    //
                    // find first SubprocessStartLog
                    for (NodeLog nodeLog2 : nodeLogs) {
                        if (nodeLog2.getId() >= nodeLog.getId() && nodeLog2 instanceof SubprocessStartLog) {
                            ((SubprocessGraphElementPresentation) presentation).setSubprocessId(((SubprocessStartLog) nodeLog2).getSubprocessId());
                            break;
                        }
                    }

                    if (((SubProcessState) node).isEmbedded()) {
                        ((SubprocessGraphElementPresentation) presentation).setSubprocessId(((NodeEnterLog) nodeLog).getProcessId());
                        SubprocessDefinition subprocessDefinition = processDefinition.getEmbeddedSubprocessByName(((SubProcessState) node)
                                .getSubProcessName());
                        ((SubprocessGraphElementPresentation) presentation).setEmbeddedSubprocessId(subprocessDefinition.getNodeId());
                        ((SubprocessGraphElementPresentation) presentation).setEmbeddedSubprocessGraphWidth(subprocessDefinition
                                .getGraphConstraints()[2]);
                        ((SubprocessGraphElementPresentation) presentation).setEmbeddedSubprocessGraphHeight(subprocessDefinition
                                .getGraphConstraints()[3]);
                    }

                    break;
                }
            }

            break;
        case MULTI_SUBPROCESS:
            presentation = new MultiinstanceGraphElementPresentation();
            Iterator<ProcessLog> logIterator = processLogs.iterator();
            boolean subProcessStartedLog = false;
            while (logIterator.hasNext()) {
                ProcessLog processLog = logIterator.next();
                if (processLog.getId() > nodeEnterlog.getId() && processLog instanceof SubprocessStartLog) {
                    ((MultiinstanceGraphElementPresentation) presentation).addSubprocessInfo(((SubprocessStartLog) processLog).getSubprocessId(),
                            true, false);
                    subProcessStartedLog = true;
                } else if (processLog.getId() > nodeEnterlog.getId() && !(processLog instanceof SubprocessStartLog) && subProcessStartedLog) {
                    subProcessStartedLog = false;
                    break;
                }
            }
            break;
        case TASK_STATE:
            presentation = new TaskGraphElementPresentation();
            break;
        default:
            presentation = new GraphElementPresentation();
        }
        presentation.initialize(node, nodeModel.getConstraints());

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(nodeEnterlog.getCreateDate());
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(nodeLeaveLog.getCreateDate());
        Long period = endCal.getTimeInMillis() - startCal.getTimeInMillis();
        Calendar periodCal = Calendar.getInstance();
        periodCal.setTimeInMillis(period);
        String date = getPeriodDateString(startCal, endCal);

        if (nodeModel.getType().equals(NodeType.SUBPROCESS) || nodeModel.getType().equals(NodeType.MULTI_SUBPROCESS)) {
            presentation.setData("Time period is " + date);
        } else if (nodeModel.getType().equals(NodeType.TASK_STATE)) {
            StringBuffer str = new StringBuffer();

            TaskCreateLog taskCreateLog = null;
            TaskEndLog taskEndLog = null;
            for (ProcessLog processLog : processLogs) {
                if (processLog.getId() > nodeEnterlog.getId()) {
                    if (processLog instanceof TaskCreateLog) {
                        taskCreateLog = (TaskCreateLog) processLog;
                        continue;
                    } else if (processLog instanceof TaskEndLog && taskCreateLog != null
                            && taskCreateLog.getTaskName().equals(((TaskEndLog) processLog).getTaskName())) {
                        taskEndLog = (TaskEndLog) processLog;
                        break;
                    }
                }
            }

            if (taskEndLog != null) {
                String actor = taskEndLog.getActorName();

                TaskAssignLog prev = null;
                for (TaskLog tempLog : taskLogs) {
                    if (tempLog instanceof TaskAssignLog) {
                        prev = (TaskAssignLog) tempLog;
                    } else if (tempLog.equals(taskEndLog)) {
                        break;
                    }
                }

                if (prev != null) {
                    if (prev.getOldExecutorName() != null && !prev.getOldExecutorName().equals(actor)) {
                        actor = prev.getOldExecutorName();
                    }
                }

                for (Executor executor : executors) {
                    if (executor.getName().equals(actor)) {
                        if (executor instanceof Actor && ((Actor) executor).getFullName() != null) {
                            str.append("Full Name is " + ((Actor) executor).getFullName() + ".</br>");
                        }

                        str.append("Login is " + executor.getName() + ".</br>");
                    }
                }
            }

            str.append("Time period is " + date + ".");
            presentation.setData(str.toString());
        }

        logElements.add(presentation);
    }

    private String getPeriodDateString(Calendar startCal, Calendar endCal) {
        long period = endCal.getTimeInMillis() - startCal.getTimeInMillis();
        Calendar periodCal = Calendar.getInstance();
        periodCal.setTimeInMillis(period);
        periodCal.setTimeInMillis(period - periodCal.getTimeZone().getOffset(period));

        String result = "";
        long days = period / (24 * 60 * 60 * 1000);

        if (days > 0) {
            result = (days == 1) ? "1 day " : (String.valueOf(days) + " days ");
        }

        result = result + CalendarUtil.formatTime(periodCal.getTime());

        return result;
    }

    public List<GraphElementPresentation> getLogElements() {
        return logElements;
    }
}
