package ru.runa.wfe.definition.par;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;

import ru.runa.wfe.commons.xml.XmlUtils;
import ru.runa.wfe.definition.IFileDataProvider;
import ru.runa.wfe.definition.InvalidDefinitionException;
import ru.runa.wfe.lang.GraphElement;
import ru.runa.wfe.lang.Node;
import ru.runa.wfe.lang.ProcessDefinition;
import ru.runa.wfe.lang.SubprocessDefinition;
import ru.runa.wfe.lang.Transition;
import ru.runa.wfe.lang.Transition.Bendpoint;

import com.google.common.base.Throwables;

public class GraphXmlParser implements ProcessArchiveParser {
    private static final String NODE_ELEMENT = "node";
    private static final String TRANSITION_ELEMENT = "transition";
    private static final String BENDPOINT_ELEMENT = "bendpoint";

    @Override
    public boolean isApplicableToEmbeddedSubprocess() {
        return true;
    }

    @Override
    public void readFromArchive(ProcessArchive archive, ProcessDefinition processDefinition) {
        try {
            String fileName = IFileDataProvider.GPD_XML_FILE_NAME;
            if (processDefinition instanceof SubprocessDefinition) {
                fileName = processDefinition.getNodeId() + "." + fileName;
            }
            byte[] gpdBytes = processDefinition.getFileDataNotNull(fileName);
            Document document = XmlUtils.parseWithoutValidation(gpdBytes);
            Element root = document.getRootElement();
            processDefinition.setGraphConstraints(0, 0,
                    Integer.parseInt(root.attributeValue("width")), 
                    Integer.parseInt(root.attributeValue("height")));
            processDefinition.setGraphActionsEnabled(Boolean.parseBoolean(root.attributeValue("showActions", "true")));
            List<Element> nodeElements = root.elements(NODE_ELEMENT);
            for (Element nodeElement : nodeElements) {
                String nodeId = nodeElement.attributeValue("name");
                GraphElement node = processDefinition.getGraphElementNotNull(nodeId);
                node.setGraphConstraints(
                        Integer.parseInt(nodeElement.attributeValue("x")), 
                        Integer.parseInt(nodeElement.attributeValue("y")),
                        Integer.parseInt(nodeElement.attributeValue("width")), 
                        Integer.parseInt(nodeElement.attributeValue("height")));
                if (node instanceof Node) {
                    boolean minimizedView = Boolean.parseBoolean(nodeElement.attributeValue("minimizedView", "false"));
                    ((Node) node).setGraphMinimazedView(minimizedView);
                }
                List<Element> transitionElements = nodeElement.elements(TRANSITION_ELEMENT);
                // TODO check without this
//              NodeModel transitionSource = nodeModel;
//                GraphElement graphElement = definition.getGraphElement(nodeModel.getNodeId());
//                if (graphElement instanceof Action) {
//                    GraphElement parent = graphElement.getParent();
//                    transitionSource = diagramModel.nodes.get(parent.getNodeId());
//                }
                for (Element transitionElement : transitionElements) {
                    String transitionName = transitionElement.attributeValue("name");
                    Transition transition = ((Node) node).getLeavingTransitionNotNull(transitionName);
                    List<Element> bendpointElements = transitionElement.elements(BENDPOINT_ELEMENT);
                    for (Element bendpointElement : bendpointElements) {
                        Bendpoint bendpoint = new Bendpoint(
                                Integer.parseInt(bendpointElement.attributeValue("x")), 
                                Integer.parseInt(bendpointElement.attributeValue("y")));
                        transition.getBendpoints().add(bendpoint);
                    }
                }
            }
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidDefinitionException.class);
            throw new InvalidDefinitionException(processDefinition.getName(), e);
        }
    }
}