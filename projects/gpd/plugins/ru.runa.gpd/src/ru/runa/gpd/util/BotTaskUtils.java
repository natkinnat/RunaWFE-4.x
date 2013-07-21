package ru.runa.gpd.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;

import ru.runa.gpd.BotCache;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.extension.DelegableProvider;
import ru.runa.gpd.extension.HandlerArtifact;
import ru.runa.gpd.extension.HandlerRegistry;
import ru.runa.gpd.extension.handler.ConfigBasedProvider;
import ru.runa.gpd.extension.handler.ParamBasedProvider;
import ru.runa.gpd.extension.handler.ParamDefConfig;
import ru.runa.gpd.extension.handler.ParamDefGroup;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.lang.model.BotTaskLink;
import ru.runa.gpd.lang.model.BotTaskType;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Swimlane;
import ru.runa.gpd.lang.model.TaskState;
import ru.runa.gpd.swimlane.OrgFunctionSwimlaneInitializer;
import ru.runa.gpd.swimlane.SwimlaneInitializer;
import ru.runa.gpd.swimlane.SwimlaneInitializerParser;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * The class provide methods for perform operation with bot task config.
 * 
 * @author rivenforce
 * @since 3.6
 */
public class BotTaskUtils {
    private static final String EXTENDED_ELEMENT = "extended";
    private static final String BOTCONFIG_ELEMENT = "botconfig";
    private static final String PARAMETERS_ELEMENT = "parameters";

    public static ParamDefConfig createEmptyParamDefConfig() {
        ParamDefConfig paramDefConfig = new ParamDefConfig();
        paramDefConfig.getGroups().add(new ParamDefGroup(ParamDefGroup.NAME_INPUT));
        paramDefConfig.getGroups().add(new ParamDefGroup(ParamDefGroup.NAME_OUTPUT));
        return paramDefConfig;
    }

    public static String createBotTaskConfiguration(BotTask botTask) {
        if (botTask.getType() == BotTaskType.EXTENDED) {
            Document document = DocumentHelper.createDocument();
            Element root = document.addElement(EXTENDED_ELEMENT);
            Element parametersElement = root.addElement(PARAMETERS_ELEMENT);
            botTask.getParamDefConfig().writeXml(parametersElement);
            Element botconfigElement = root.addElement(BOTCONFIG_ELEMENT);
            botconfigElement.addCDATA(botTask.getDelegationConfiguration());
            return XmlUtil.toString(document);
        } else if (botTask.getType() == BotTaskType.PARAMETERIZED) {
            if (!Strings.isNullOrEmpty(botTask.getDelegationConfiguration())) {
                // http://sourceforge.net/p/runawfe/bugs/317/
                return botTask.getDelegationConfiguration();
            }
            Document document = DocumentHelper.createDocument();
            botTask.getParamDefConfig().writeXml(document);
            return XmlUtil.toString(document);
        } else {
            return botTask.getDelegationConfiguration();
        }
    }

    private static boolean isBotTaskExtendedConfiguration(String config) {
        try {
            Document document = XmlUtil.parseWithoutValidation(config);
            Element el = document.getRootElement();
            return el.getName().equals(EXTENDED_ELEMENT) && el.element(PARAMETERS_ELEMENT) != null && el.element(BOTCONFIG_ELEMENT) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isTaskHandlerParameterized(String className) {
        HandlerArtifact artifact = HandlerRegistry.getInstance().getArtifact(className);
        if (artifact != null) {
            DelegableProvider provider = HandlerRegistry.getProvider(className);
            return provider instanceof ConfigBasedProvider;
        }
        return false;
    }

    public static InputStream createBotStationInfo(String botStationName, String rmiAddress) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(botStationName);
        buffer.append("\n");
        buffer.append(rmiAddress);
        buffer.append("\n");
        return new ByteArrayInputStream(buffer.toString().getBytes(Charsets.UTF_8));
    }

    public static InputStream createBotTaskInfo(IFolder botFolder, BotTask botTask) throws CoreException {
        StringBuffer buffer = new StringBuffer();
        buffer.append(botTask.getDelegationClassName());
        buffer.append("\n");
        String configuration = createBotTaskConfiguration(botTask);
        if (!Strings.isNullOrEmpty(configuration)) {
            String configurationFileName = botTask.getName() + ".conf";
            buffer.append(configurationFileName);
            IFile configurationFile = botFolder.getFile(configurationFileName);
            ByteArrayInputStream stream = new ByteArrayInputStream(configuration.getBytes(Charsets.UTF_8));
            if (configurationFile.exists()) {
                configurationFile.setContents(stream, true, true, null);
            } else {
                configurationFile.create(stream, true, null);
            }
        }
        buffer.append("\n");
        return new ByteArrayInputStream(buffer.toString().getBytes(Charsets.UTF_8));
    }

    public static BotTask createBotTask(String botTaskName, String handlerClassName, String configuration) {
        BotTask botTask = new BotTask();
        botTask.setName(botTaskName);
        botTask.setDelegationClassName(handlerClassName);
        if (isTaskHandlerParameterized(botTask.getDelegationClassName())) {
            botTask.setType(BotTaskType.PARAMETERIZED);
            Document document = XmlUtil.parseWithoutValidation(configuration);
            botTask.setParamDefConfig(ParamDefConfig.parse(document));
            botTask.setDelegationConfiguration(configuration);
        } else if (isBotTaskExtendedConfiguration(configuration)) {
            botTask.setType(BotTaskType.EXTENDED);
            Document document = XmlUtil.parseWithoutValidation(configuration);
            Element botElement = document.getRootElement();
            Element element = botElement.element(PARAMETERS_ELEMENT).element(ParamDefConfig.NAME_CONFIG);
            Preconditions.checkNotNull(element);
            botTask.setParamDefConfig(ParamDefConfig.parse(element));
            botTask.setDelegationConfiguration(botElement.elementText(BOTCONFIG_ELEMENT));
        } else {
            botTask.setType(BotTaskType.SIMPLE);
            botTask.setDelegationConfiguration(configuration);
        }
        return botTask;
    }

    /**
     * Gets associated with this swimlane bot name.
     * @param swimlane any swimlane, can be <code>null</code>
     * @return bot name or <code>null</code>.
     */
    public static String getBotName(Swimlane swimlane) {
        if (swimlane != null && swimlane.getDelegationConfiguration() != null) {
            SwimlaneInitializer swimlaneInitializer = SwimlaneInitializerParser.parse(swimlane.getDelegationConfiguration());
            if (swimlaneInitializer instanceof OrgFunctionSwimlaneInitializer) {
                OrgFunctionSwimlaneInitializer orgFunctionSwimlaneInitializer = (OrgFunctionSwimlaneInitializer) swimlaneInitializer;
                if (BotTask.SWIMLANE_DEFINITION_NAME.equals(orgFunctionSwimlaneInitializer.getDefinition().getName())) {
                    if (orgFunctionSwimlaneInitializer.getParameters().size() > 0) {
                        return orgFunctionSwimlaneInitializer.getParameters().get(0).getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Opens dialog with formal parameters mapping for bounded to task state bot task.
     * @param taskState task state with valid bot task link and swimlane
     */
    public static void editBotTaskLinkConfiguration(TaskState taskState) {
        ProcessDefinition processDefinition = taskState.getProcessDefinition();
        BotTaskLink botTaskLink = taskState.getBotTaskLink();
        BotTask botTask = BotCache.getBotTaskNotNull(taskState.getSwimlaneBotName(), botTaskLink.getBotTaskName());
        botTaskLink.setDelegationClassName(botTask.getDelegationClassName());
        ParamDefConfig config = botTask.getParamDefConfig();
        String newConfiguration = null;
        if (BotTaskUtils.isTaskHandlerParameterized(botTaskLink.getDelegationClassName())) {
            // this is the case of ru.runa.gpd.lang.model.BotTaskType.PARAMETERIZED
            ParamBasedProvider provider = (ParamBasedProvider) HandlerRegistry.getProvider(botTaskLink.getDelegationClassName());
            newConfiguration = provider.showConfigurationDialog(processDefinition, botTaskLink);
        } else {
            // this is the case of ru.runa.gpd.lang.model.BotTaskType.EXTENDED
            ImageDescriptor logo = SharedImages.getImageDescriptor("/icons/bottasklink.png");
            newConfiguration = ParamBasedProvider.showConfigurationDialog(processDefinition, botTaskLink, config, logo);
        }
        if (newConfiguration != null) {
            botTaskLink.setDelegationConfiguration(newConfiguration);
            taskState.setDirty();
        }
    }
}
