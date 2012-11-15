package ru.runa.gpd.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.eclipse.core.resources.IFile;

import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginConstants;
import ru.runa.gpd.lang.model.Action;
import ru.runa.gpd.lang.model.ActionImpl;
import ru.runa.gpd.lang.model.ActionNode;
import ru.runa.gpd.lang.model.Decision;
import ru.runa.gpd.lang.model.Delegable;
import ru.runa.gpd.lang.model.Describable;
import ru.runa.gpd.lang.model.EndState;
import ru.runa.gpd.lang.model.Event;
import ru.runa.gpd.lang.model.Fork;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.ITimed;
import ru.runa.gpd.lang.model.Join;
import ru.runa.gpd.lang.model.MailNode;
import ru.runa.gpd.lang.model.MultiInstance;
import ru.runa.gpd.lang.model.NamedGraphElement;
import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.ReceiveMessageNode;
import ru.runa.gpd.lang.model.SendMessageNode;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.lang.model.State;
import ru.runa.gpd.lang.model.Subprocess;
import ru.runa.gpd.lang.model.Swimlane;
import ru.runa.gpd.lang.model.SwimlanedNode;
import ru.runa.gpd.lang.model.TaskState;
import ru.runa.gpd.lang.model.TimerAction;
import ru.runa.gpd.lang.model.Transition;
import ru.runa.gpd.lang.model.WaitState;
import ru.runa.gpd.ui.dialog.ErrorDialog;
import ru.runa.gpd.util.TimerDuration;
import ru.runa.gpd.util.VariableMapping;
import ru.runa.gpd.util.XmlUtil;

@SuppressWarnings("unchecked")
public class JpdlSerializer extends ProcessSerializer {
    private static final String TIMER_GLOBAL_NAME = "__GLOBAL";
    private static final String TIMER_ESCALATION = "__ESCALATION";
    protected static final String ROOT_ELEMENT = "process-definition";
    private static boolean validationEnabled = true;

    public static void setValidationEnabled(boolean validationEnabled) {
        JpdlSerializer.validationEnabled = validationEnabled;
    }

    @Override
    public Document getInitialProcessDefinitionDocument(String processName) {
        Document document = XmlUtil.createDocument(ROOT_ELEMENT);
        document.getRootElement().addAttribute("name", processName);
        document.getRootElement().addAttribute("lang", Language.JPDL.name());
        return document;
    }

    @Override
    public void validateProcessDefinitionXML(IFile file) {
        if (!validationEnabled) {
            return;
        }
        //        try {
        //            XmlUtil.parseWithXSDValidation(getClass().getResourceAsStream("/schema/" + XSD_FILE_NAME));
        //        } catch (Exception e) {
        //            throw new RuntimeException(e);
        //        } TODO
    }

    @Override
    public boolean isSupported(Document document) {
        if (ROOT_ELEMENT.equals(document.getRootElement().getName())) {
            return Language.JPDL.name().equals(document.getRootElement().attributeValue("lang"));
        }
        return false;
    }

    private static final String ACCESS_ATTR = "access";
    private static final String END_STATE_NODE = "end-state";
    private static final String VARIABLE_NODE = "variable";
    private static final String SUB_PROCESS_NODE = "sub-process";
    private static final String MAPPED_NAME_ATTR = "mapped-name";
    private static final String PROCESS_STATE_NODE = "process-state";
    private static final String MULTI_INSTANCE_STATE_NODE = "multiinstance-state";
    private static final String DECISION_NODE = "decision";
    private static final String JOIN_NODE = "join";
    private static final String FORK_NODE = "fork";
    private static final String DUEDATE_ATTR = "duedate";
    private static final String DEFAULT_DUEDATE_ATTR = "default-task-duedate";
    private static final String REPEAT_ATTR = "repeat";
    private static final String TIMER_NODE = "timer";
    private static final String ASSIGNMENT_NODE = "assignment";
    private static final String TASK_STATE_NODE = "task-node";
    private static final String TASK_NODE = "task";
    private static final String ACTION_NODE_NODE = "node";
    private static final String MAIL_NODE = "mail-node";
    private static final String START_STATE_NODE = "start-state";
    private static final String SWIMLANE_NODE = "swimlane";
    private static final String REASSIGN_ATTR = "reassign";
    private static final String END_TASKS_ATTR = "end-tasks";
    private static final String TO_ATTR = "to";
    private static final String CLASS_ATTR = "class";
    private static final String ACTION_NODE = "action";
    private static final String EVENT_NODE = "event";
    private static final String TRANSITION_NODE = "transition";
    private static final String HANDLER_NODE = "handler";
    private static final String DESCRIPTION_NODE = "description";
    private static final String NAME_ATTR = "name";
    private static final String SEND_MESSAGE_NODE = "send-message";
    private static final String RECEIVE_MESSAGE_NODE = "receive-message";

    private <T extends GraphElement> T create(Element node, GraphElement parent) {
        return create(node, parent, node.getName());
    }

    private <T extends GraphElement> T create(Element node, GraphElement parent, String typeName) {
        GraphElement element = NodeRegistry.getNodeTypeDefinition(typeName).createElement();
        if (parent != null) {
            parent.addChild(element);
        }
        if (element instanceof NamedGraphElement) {
            ((NamedGraphElement) element).setName(node.attributeValue(NAME_ATTR));
        }
        List<Element> nodeList = node.elements();
        for (Element childNode : nodeList) {
            if (DESCRIPTION_NODE.equals(childNode.getName())) {
                ((Describable) element).setDescription(childNode.getTextTrim());
            }
            if (HANDLER_NODE.equals(childNode.getName()) || ASSIGNMENT_NODE.equals(childNode.getName())) {
                ((Delegable) element).setDelegationClassName(childNode.attributeValue(CLASS_ATTR));
                element.setDelegationConfiguration(childNode.getTextTrim());
            }
            if (ACTION_NODE.equals(childNode.getName())) {
                // only transition actions loaded here
                String eventType;
                if (element instanceof Transition) {
                    eventType = Event.TRANSITION;
                } else if (element instanceof ActionNode) {
                    eventType = Event.NODE_ACTION;
                } else {
                    throw new RuntimeException("Unexpected action in XML, context of " + element);
                }
                parseAction(childNode, element, eventType);
            }
            if (TRANSITION_NODE.equals(childNode.getName())) {
                parseTransition(childNode, element);
            }
        }
        return (T) element;
    }

    private void parseTransition(Element node, GraphElement parent) {
        Transition transition = create(node, parent);
        String targetName = node.attributeValue(TO_ATTR);
        TRANSITION_TARGETS.put(transition, targetName);
    }

    private void parseAction(Element node, GraphElement parent, String eventType) {
        ActionImpl action = NodeRegistry.getNodeTypeDefinition(ACTION_NODE).createElement();
        action.setDelegationClassName(node.attributeValue(CLASS_ATTR));
        action.setDelegationConfiguration(node.getTextTrim());
        parent.addAction(action, -1);
        action.setEventType(eventType);
    }

    private static Map<Transition, String> TRANSITION_TARGETS = new HashMap<Transition, String>();

    @Override
    public ProcessDefinition parseXML(Document document) {
        TRANSITION_TARGETS.clear();
        Element root = document.getRootElement();
        ProcessDefinition definition = create(root, null);
        definition.setDefaultTaskDuedate(root.attributeValue(DEFAULT_DUEDATE_ATTR));
        List<Element> swimlanes = root.elements(SWIMLANE_NODE);
        for (Element node : swimlanes) {
            create(node, definition);
        }
        List<Element> startStates = root.elements(START_STATE_NODE);
        if (startStates.size() > 0) {
            if (startStates.size() > 1) {
                ErrorDialog.open(Localization.getString("model.validation.multipleStartStatesNotAllowed"));
            }
            Element node = startStates.get(0);
            StartState startState = create(node, definition);
            List<Element> stateChilds = node.elements();
            for (Element stateNodeChild : stateChilds) {
                if (TASK_NODE.equals(stateNodeChild.getName())) {
                    String swimlaneName = stateNodeChild.attributeValue(SWIMLANE_NODE);
                    Swimlane swimlane = definition.getSwimlaneByName(swimlaneName);
                    startState.setSwimlane(swimlane);
                }
            }
        }
        List<Element> actionNodeNodes = root.elements(ACTION_NODE_NODE);
        for (Element node : actionNodeNodes) {
            ActionNode actionNode = create(node, definition);
            List<Element> aaa = node.elements();
            for (Element a : aaa) {
                if (EVENT_NODE.equals(a.getName())) {
                    String eventType = a.attributeValue("type");
                    List<Element> actionNodes = a.elements();
                    for (Element aa : actionNodes) {
                        if (ACTION_NODE.equals(aa.getName())) {
                            parseAction(aa, actionNode, eventType);
                        }
                    }
                }
            }
        }
        List<Element> states = root.elements(TASK_STATE_NODE);
        for (Element node : states) {
            List<Element> nodeList = node.elements();
            int transitionsCount = 0;
            boolean hasTimeOutTransition = false;
            for (Element childNode : nodeList) {
                if (TRANSITION_NODE.equals(childNode.getName())) {
                    String transitionName = childNode.attributeValue(NAME_ATTR);
                    if (PluginConstants.TIMER_TRANSITION_NAME.equals(transitionName)) {
                        hasTimeOutTransition = true;
                    }
                    transitionsCount++;
                }
            }
            GraphElement state;
            if (transitionsCount == 1 && hasTimeOutTransition) {
                state = create(node, definition, "waitState");
            } else {
                state = create(node, definition);
            }
            List<Element> stateChilds = node.elements();
            for (Element stateNodeChild : stateChilds) {
                if (TASK_NODE.equals(stateNodeChild.getName())) {
                    String swimlaneName = stateNodeChild.attributeValue(SWIMLANE_NODE);
                    if (swimlaneName != null && state instanceof SwimlanedNode) {
                        Swimlane swimlane = definition.getSwimlaneByName(swimlaneName);
                        ((SwimlanedNode) state).setSwimlane(swimlane);
                        String reassign = stateNodeChild.attributeValue(REASSIGN_ATTR);
                        if (reassign != null) {
                            boolean forceReassign = Boolean.parseBoolean(reassign);
                            ((State) state).setReassignmentEnabled(forceReassign);
                        }
                    }
                    String duedate_attr = stateNodeChild.attributeValue(DUEDATE_ATTR);
                    if (duedate_attr != null) {
                        ((State) state).setTimeOutDueDate(duedate_attr);
                    }
                    List<Element> aaa = stateNodeChild.elements();
                    for (Element a : aaa) {
                        if (EVENT_NODE.equals(a.getName())) {
                            String eventType = a.attributeValue("type");
                            List<Element> actionNodes = a.elements();
                            for (Element aa : actionNodes) {
                                if (ACTION_NODE.equals(aa.getName())) {
                                    parseAction(aa, state, eventType);
                                }
                            }
                        }
                    }
                }
                if (TIMER_NODE.equals(stateNodeChild.getName())) {
                    String nameTimer = stateNodeChild.attributeValue(NAME_ATTR);
                    String dueDate = stateNodeChild.attributeValue(DUEDATE_ATTR);
                    if (TIMER_ESCALATION.equals(nameTimer)) {
                        ((TaskState) state).setUseEscalation(true);
                        if (dueDate != null) {
                            ((TaskState) state).setEscalationTime(new TimerDuration(dueDate));
                        }
                    } else if (TIMER_GLOBAL_NAME.equals(nameTimer)) {
                        definition.setTimeOutDueDate(dueDate);
                    } else {
                        if (State.class.isInstance(state)) {
                            ((State) state).setHasTimer(true);
                            if (dueDate != null) {
                                ((State) state).setDueDate(dueDate);
                            }
                        } else if (WaitState.class.isInstance(state)) {
                            if (dueDate != null) {
                                ((WaitState) state).setDueDate(dueDate);
                            } else {
                                ((WaitState) state).setDueDate(TimerDuration.EMPTY);
                            }
                        }
                    }
                    List<Element> actionNodes = stateNodeChild.elements();
                    for (Element aa : actionNodes) {
                        if (ACTION_NODE.equals(aa.getName())) {
                            TimerAction timerAction = new TimerAction(null);
                            timerAction.setDelegationClassName(aa.attributeValue(CLASS_ATTR));
                            timerAction.setDelegationConfiguration(aa.getTextTrim());
                            timerAction.setRepeat(stateNodeChild.attributeValue(REPEAT_ATTR));
                            if (TIMER_GLOBAL_NAME.equals(nameTimer)) {
                                definition.setTimeOutAction(timerAction);
                            } else if (TIMER_ESCALATION.equals(nameTimer)) {
                                ((TaskState) state).setEscalationAction(timerAction);
                            } else {
                                ((ITimed) state).setTimerAction(timerAction);
                            }
                        }
                    }
                }
            }
        }
        List<Element> mailNodes = root.elements(MAIL_NODE);
        for (Element node : mailNodes) {
            MailNode mailNode = create(node, definition);
            mailNode.setRecipient(node.attributeValue("to"));
            List<Element> mailNodeChilds = node.elements();
            for (Element mailNodeChild : mailNodeChilds) {
                if ("body".equals(mailNodeChild.getName())) {
                    mailNode.setMailBody(mailNodeChild.getTextTrim());
                }
                if ("subject".equals(mailNodeChild.getName())) {
                    mailNode.setSubject(mailNodeChild.getTextTrim());
                }
            }
        }
        List<Element> forks = root.elements(FORK_NODE);
        for (Element node : forks) {
            create(node, definition);
        }
        List<Element> joins = root.elements(JOIN_NODE);
        for (Element node : joins) {
            create(node, definition);
        }
        List<Element> decisions = root.elements(DECISION_NODE);
        for (Element node : decisions) {
            create(node, definition);
        }
        List<Element> processStates = root.elements(PROCESS_STATE_NODE);
        for (Element node : processStates) {
            Subprocess subprocess = create(node, definition);
            List<VariableMapping> variablesList = new ArrayList<VariableMapping>();
            List<Element> nodeList = node.elements();
            for (Element childNode : nodeList) {
                if (SUB_PROCESS_NODE.equals(childNode.getName())) {
                    subprocess.setSubProcessName(childNode.attributeValue(NAME_ATTR));
                }
                if (VARIABLE_NODE.equals(childNode.getName())) {
                    VariableMapping variable = new VariableMapping();
                    variable.setProcessVariable(childNode.attributeValue(NAME_ATTR));
                    variable.setSubprocessVariable(childNode.attributeValue(MAPPED_NAME_ATTR));
                    variable.setUsage(childNode.attributeValue(ACCESS_ATTR));
                    variablesList.add(variable);
                }
            }
            subprocess.setVariablesList(variablesList);
        }
        List<Element> multiInstanceStates = root.elements(MULTI_INSTANCE_STATE_NODE);
        for (Element node : multiInstanceStates) {
            MultiInstance multiInstance = create(node, definition);
            List<VariableMapping> variablesList = new ArrayList<VariableMapping>();
            List<Element> nodeList = node.elements();
            for (Element childNode : nodeList) {
                if (SUB_PROCESS_NODE.equals(childNode.getName())) {
                    multiInstance.setSubProcessName(childNode.attributeValue(NAME_ATTR));
                }
                if (VARIABLE_NODE.equals(childNode.getName())) {
                    VariableMapping variable = new VariableMapping();
                    variable.setProcessVariable(childNode.attributeValue(NAME_ATTR));
                    variable.setSubprocessVariable(childNode.attributeValue(MAPPED_NAME_ATTR));
                    variable.setUsage(childNode.attributeValue(ACCESS_ATTR));
                    variablesList.add(variable);
                }
            }
            multiInstance.setVariablesList(variablesList);
        }
        List<Element> sendMessageNodes = root.elements(SEND_MESSAGE_NODE);
        for (Element node : sendMessageNodes) {
            SendMessageNode messageNode = create(node, definition);
            List<VariableMapping> variablesList = new ArrayList<VariableMapping>();
            List<Element> nodeList = node.elements();
            for (Element childNode : nodeList) {
                if (VARIABLE_NODE.equals(childNode.getName())) {
                    VariableMapping variable = new VariableMapping();
                    variable.setProcessVariable(childNode.attributeValue(NAME_ATTR));
                    variable.setSubprocessVariable(childNode.attributeValue(MAPPED_NAME_ATTR));
                    variable.setUsage(childNode.attributeValue(ACCESS_ATTR));
                    variablesList.add(variable);
                }
            }
            messageNode.setVariablesList(variablesList);
        }
        List<Element> receiveMessageNodes = root.elements(RECEIVE_MESSAGE_NODE);
        for (Element node : receiveMessageNodes) {
            ReceiveMessageNode messageNode = create(node, definition);
            List<VariableMapping> variablesList = new ArrayList<VariableMapping>();
            List<Element> nodeList = node.elements();
            for (Element childNode : nodeList) {
                if (VARIABLE_NODE.equals(childNode.getName())) {
                    VariableMapping variable = new VariableMapping();
                    variable.setProcessVariable(childNode.attributeValue(NAME_ATTR));
                    variable.setSubprocessVariable(childNode.attributeValue(MAPPED_NAME_ATTR));
                    variable.setUsage(childNode.attributeValue(ACCESS_ATTR));
                    variablesList.add(variable);
                }
                if (TIMER_NODE.equals(childNode.getName())) {
                    String dueDate = childNode.attributeValue(DUEDATE_ATTR);
                    messageNode.setDueDate(dueDate);
                    List<Element> actionNodes = childNode.elements();
                    for (Element aa : actionNodes) {
                        if (ACTION_NODE.equals(aa.getName())) {
                            TimerAction timerAction = new TimerAction(null);
                            timerAction.setDelegationClassName(aa.attributeValue(CLASS_ATTR));
                            timerAction.setDelegationConfiguration(aa.getTextTrim());
                            timerAction.setRepeat(childNode.attributeValue(REPEAT_ATTR));
                            messageNode.setTimerAction(timerAction);
                        }
                    }
                }
            }
            messageNode.setVariablesList(variablesList);
        }
        List<Element> endStates = root.elements(END_STATE_NODE);
        for (Element node : endStates) {
            create(node, definition);
        }
        List<Transition> tmpTransitions = new ArrayList<Transition>(TRANSITION_TARGETS.keySet());
        for (Transition transition : tmpTransitions) {
            String targetName = TRANSITION_TARGETS.remove(transition);
            ru.runa.gpd.lang.model.Node target = definition.getNodeByNameNotNull(targetName);
            transition.setTarget(target);
        }
        return definition;
    }

    @Override
    public void saveToXML(ProcessDefinition definition, Document document) {
        Element root = document.getRootElement();
        root.addAttribute(NAME_ATTR, definition.getName());
        if (!definition.getDefaultTaskDuedate().startsWith("0 ")) {
            root.addAttribute(DEFAULT_DUEDATE_ATTR, definition.getDefaultTaskDuedate());
        }
        if (definition.isInvalid()) {
            root.addAttribute("invalid", String.valueOf(definition.isInvalid()));
        }
        if (definition.getDescription() != null && definition.getDescription().length() > 0) {
            Element desc = root.addElement(DESCRIPTION_NODE);
            setNodeValue(desc, definition.getDescription());
        }
        List<Swimlane> swimlanes = definition.getSwimlanes();
        for (Swimlane swimlane : swimlanes) {
            Element swimlaneElement = writeElement(root, swimlane);
            writeDelegation(swimlaneElement, ASSIGNMENT_NODE, swimlane);
        }
        StartState startState = definition.getFirstChild(StartState.class);
        if (startState != null) {
            Element startStateElement = writeTaskState(root, startState);
            writeTransitions(startStateElement, startState);
        }
        List<ActionNode> actionNodeNodes = definition.getChildren(ActionNode.class);
        for (ActionNode actionNode : actionNodeNodes) {
            Element actionNodeElement = writeNode(root, actionNode, null);
            for (Action action : actionNode.getActions()) {
                ActionImpl actionImpl = (ActionImpl) action;
                if (!Event.NODE_ACTION.equals(actionImpl.getEventType())) {
                    writeEvent(actionNodeElement, new Event(actionImpl.getEventType()), actionImpl);
                }
            }
        }
        List<Decision> decisions = definition.getChildren(Decision.class);
        for (Decision decision : decisions) {
            writeNode(root, decision, HANDLER_NODE);
        }
        List<TaskState> states = definition.getChildren(TaskState.class);
        for (TaskState state : states) {
            Element stateElement = writeTaskStateWithDuedate(root, state);
            if (state.timerExist()) {
                Element timerElement = stateElement.addElement(TIMER_NODE);
                if (state.getDuration() != null && state.getDuration().hasDuration()) {
                    setAttribute(timerElement, DUEDATE_ATTR, state.getDuration().getDuration());
                }
                if (!state.hasTimeoutTransition() && state.getTimerAction() != null) {
                    if (state.getTimerAction().getRepeat().hasDuration()) {
                        setAttribute(timerElement, REPEAT_ATTR, state.getTimerAction().getRepeat().getDuration());
                    }
                    writeDelegation(timerElement, ACTION_NODE, state.getTimerAction());
                } else {
                    setAttribute(timerElement, TRANSITION_NODE, PluginConstants.TIMER_TRANSITION_NAME);
                }
            }
            if (state.isUseEscalation()) {
                //boolean escalationEnabled = DesignerPlugin.getPrefBoolean(PrefConstants.P_TASKS_TIMEOUT_ENABLED);
                //if (escalationEnabled) {
                String timerName = TIMER_ESCALATION;
                TimerDuration escalationDuration = state.getEscalationTime();
                Element timerElement = stateElement.addElement(TIMER_NODE);
                setAttribute(timerElement, NAME_ATTR, timerName);
                if (escalationDuration != null && escalationDuration.hasDuration()) {
                    setAttribute(timerElement, DUEDATE_ATTR, escalationDuration.getDuration());
                }
                TimerAction escalationAction = state.getEscalationAction();
                if (escalationAction != null) {
                    if (escalationAction.getRepeat().hasDuration()) {
                        setAttribute(timerElement, REPEAT_ATTR, escalationAction.getRepeat().getDuration());
                    }
                    writeDelegation(timerElement, ACTION_NODE, escalationAction);
                }
                //}
            }
            writeTransitions(stateElement, state);
        }
        List<WaitState> waitStates = definition.getChildren(WaitState.class);
        for (WaitState waitState : waitStates) {
            Element stateElement = writeWaitState(root, waitState);
            writeTransitions(stateElement, waitState);
        }
        List<MailNode> mailNodes = definition.getChildren(MailNode.class);
        for (MailNode mailNode : mailNodes) {
            Element nodeElement = writeNode(root, mailNode, null);
            setAttribute(nodeElement, "to", mailNode.getRecipient());
            writeTransitions(nodeElement, mailNode);
            Element subject = nodeElement.addElement("subject");
            setNodeValue(subject, mailNode.getSubject());
            Element body = nodeElement.addElement("body");
            setNodeValue(body, mailNode.getMailBody());
        }
        List<Fork> forks = definition.getChildren(Fork.class);
        for (ru.runa.gpd.lang.model.Node node : forks) {
            writeNode(root, node, null);
        }
        List<Join> joins = definition.getChildren(Join.class);
        for (ru.runa.gpd.lang.model.Node node : joins) {
            writeNode(root, node, null);
        }
        List<Subprocess> subprocesses = definition.getChildren(Subprocess.class);
        for (Subprocess subprocess : subprocesses) {
            Element processStateElement = writeNode(root, subprocess, null);
            Element subProcessElement = processStateElement.addElement(SUB_PROCESS_NODE);
            setAttribute(subProcessElement, NAME_ATTR, subprocess.getSubProcessName());
            setAttribute(subProcessElement, "binding", "late");
            for (VariableMapping variable : subprocess.getVariablesList()) {
                Element variableElement = processStateElement.addElement(VARIABLE_NODE);
                setAttribute(variableElement, NAME_ATTR, variable.getProcessVariable());
                setAttribute(variableElement, MAPPED_NAME_ATTR, variable.getSubprocessVariable());
                setAttribute(variableElement, ACCESS_ATTR, variable.getUsage());
            }
        }
        List<SendMessageNode> sendMessageNodes = definition.getChildren(SendMessageNode.class);
        for (SendMessageNode messageNode : sendMessageNodes) {
            Element messageElement = writeNode(root, messageNode, null);
            for (VariableMapping variable : messageNode.getVariablesList()) {
                Element variableElement = messageElement.addElement(VARIABLE_NODE);
                setAttribute(variableElement, NAME_ATTR, variable.getProcessVariable());
                setAttribute(variableElement, MAPPED_NAME_ATTR, variable.getSubprocessVariable());
                setAttribute(variableElement, ACCESS_ATTR, variable.getUsage());
            }
        }
        List<ReceiveMessageNode> receiveMessageNodes = definition.getChildren(ReceiveMessageNode.class);
        for (ReceiveMessageNode messageNode : receiveMessageNodes) {
            Element messageElement = writeNode(root, messageNode, null);
            for (VariableMapping variable : messageNode.getVariablesList()) {
                Element variableElement = messageElement.addElement(VARIABLE_NODE);
                setAttribute(variableElement, NAME_ATTR, variable.getProcessVariable());
                setAttribute(variableElement, MAPPED_NAME_ATTR, variable.getSubprocessVariable());
                setAttribute(variableElement, ACCESS_ATTR, variable.getUsage());
            }
            if (messageNode.timerExist()) {
                Element timerElement = messageElement.addElement(TIMER_NODE);
                setAttribute(timerElement, DUEDATE_ATTR, messageNode.getDuration().getDuration());
                if (messageNode.getTimerAction() != null) {
                    if (messageNode.getTimerAction().getRepeat().hasDuration()) {
                        setAttribute(timerElement, REPEAT_ATTR, messageNode.getTimerAction().getRepeat().getDuration());
                    }
                    writeDelegation(timerElement, ACTION_NODE, messageNode.getTimerAction());
                } else {
                    setAttribute(timerElement, TRANSITION_NODE, PluginConstants.TIMER_TRANSITION_NAME);
                }
            }
        }
        EndState endState = definition.getFirstChild(EndState.class);
        if (endState != null) {
            writeElement(root, endState);
        }
    }

    private Element writeNode(Element parent, Node node, String delegationNodeName) {
        Element nodeElement = writeElement(parent, node);
        if (delegationNodeName != null) {
            writeDelegation(nodeElement, delegationNodeName, (Delegable) node);
        }
        writeTransitions(nodeElement, node);
        return nodeElement;
    }

    private Element writeTaskStateWithDuedate(Element parent, TaskState state) {
        Element nodeElement = writeElement(parent, state);
        Element taskElement = nodeElement.addElement(TASK_NODE);
        setAttribute(taskElement, DUEDATE_ATTR, state.getTimeOutDueDate());
        setAttribute(taskElement, NAME_ATTR, state.getName());
        setAttribute(taskElement, SWIMLANE_NODE, state.getSwimlaneName());
        if (state instanceof State && ((State) state).isReassignmentEnabled()) {
            setAttribute(taskElement, REASSIGN_ATTR, "true");
        }
        for (Action action : state.getActions()) {
            ActionImpl actionImpl = (ActionImpl) action;
            writeEvent(taskElement, new Event(actionImpl.getEventType()), actionImpl);
        }
        if (state instanceof ITimed && ((ITimed) state).timerExist()) {
            setAttribute(nodeElement, END_TASKS_ATTR, "true");
        }
        return nodeElement;
    }

    private Element writeTaskState(Element parent, SwimlanedNode state) {
        Element nodeElement = writeElement(parent, state);
        Element taskElement = nodeElement.addElement(TASK_NODE);
        setAttribute(taskElement, NAME_ATTR, state.getName());
        setAttribute(taskElement, SWIMLANE_NODE, state.getSwimlaneName());
        if (state instanceof State && ((State) state).isReassignmentEnabled()) {
            setAttribute(taskElement, REASSIGN_ATTR, "true");
        }
        for (Action action : state.getActions()) {
            ActionImpl actionImpl = (ActionImpl) action;
            writeEvent(taskElement, new Event(actionImpl.getEventType()), actionImpl);
        }
        if (state instanceof ITimed && ((ITimed) state).timerExist()) {
            setAttribute(nodeElement, END_TASKS_ATTR, "true");
        }
        return nodeElement;
    }

    private Element writeWaitState(Element parent, WaitState state) {
        Element nodeElement = writeElement(parent, state, "task-node");
        Element taskElement = nodeElement.addElement(TASK_NODE);
        setAttribute(taskElement, NAME_ATTR, state.getName());
        setAttribute(nodeElement, END_TASKS_ATTR, "true");
        Element timerElement = nodeElement.addElement(TIMER_NODE);
        setAttribute(timerElement, DUEDATE_ATTR, state.getDueDate());
        if (state.getTimerAction() != null) {
            if (state.getTimerAction().getRepeat().hasDuration()) {
                setAttribute(timerElement, REPEAT_ATTR, state.getTimerAction().getRepeat().getDuration());
            }
            writeDelegation(timerElement, ACTION_NODE, state.getTimerAction());
        }
        setAttribute(timerElement, TRANSITION_NODE, PluginConstants.TIMER_TRANSITION_NAME);
        return nodeElement;
    }

    private Element writeElement(Element parent, GraphElement element) {
        return writeElement(parent, element, element.getTypeName());
    }

    private Element writeElement(Element parent, GraphElement element, String typeName) {
        Element result = parent.addElement(typeName);
        if (element instanceof NamedGraphElement) {
            setAttribute(result, NAME_ATTR, ((NamedGraphElement) element).getName());
        }
        if (element instanceof ActionNode) {
            List<Action> nodeActions = ((ActionNode) element).getNodeActions();
            for (Action nodeAction : nodeActions) {
                writeDelegation(result, ACTION_NODE, nodeAction);
            }
        }
        if (element instanceof Describable) {
            String description = ((Describable) element).getDescription();
            if (description != null && description.length() > 0) {
                Element desc = result.addElement(DESCRIPTION_NODE);
                setNodeValue(desc, description);
            }
        }
        return result;
    }

    private void writeTransitions(Element parent, Node node) {
        List<Transition> transitions = node.getLeavingTransitions();
        for (Transition transition : transitions) {
            Element transitionElement = writeElement(parent, transition);
            transitionElement.addAttribute(TO_ATTR, transition.getTargetName());
            for (Action action : transition.getActions()) {
                writeDelegation(transitionElement, ACTION_NODE, action);
            }
        }
    }

    private void writeEvent(Element parent, Event event, ActionImpl action) {
        Element eventElement = writeElement(parent, event, EVENT_NODE);
        setAttribute(eventElement, "type", event.getType());
        writeDelegation(eventElement, ACTION_NODE, action);
    }

    private void writeDelegation(Element parent, String elementName, Delegable delegable) {
        Element delegationElement = parent.addElement(elementName);
        setAttribute(delegationElement, CLASS_ATTR, delegable.getDelegationClassName());
        setAttribute(delegationElement, "config-type", "configuration-property");
        setNodeValue(delegationElement, delegable.getDelegationConfiguration());
    }
}
