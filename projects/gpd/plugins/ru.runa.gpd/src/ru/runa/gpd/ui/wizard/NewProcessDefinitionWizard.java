package ru.runa.gpd.ui.wizard;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.dom4j.Document;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.lang.BpmnSerializer;
import ru.runa.gpd.lang.Language;
import ru.runa.gpd.lang.par.GpdXmlContentProvider;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.gpd.util.XmlUtil;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

public class NewProcessDefinitionWizard extends Wizard implements INewWizard {
    private IStructuredSelection selection;
    private IWorkbench workbench;
    private NewProcessDefinitionWizardPage page;

    public NewProcessDefinitionWizard() {
        setWindowTitle(Localization.getString("NewProcessDefinitionWizard.wizard.title"));
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        this.workbench = workbench;
        this.selection = currentSelection;
    }

    @Override
    public void addPages() {
        page = new NewProcessDefinitionWizardPage(selection);
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        try {
            getContainer().run(false, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException {
                    try {
                        // TODO move to par provider
                        monitor.beginTask(Localization.getString("NewProcessDefinitionWizard.monitor.title"), 4);
                        IFolder folder = page.getProcessFolder();
                        folder.create(true, true, null);
                        monitor.worked(1);
                        IFile definitionFile = IOUtils.getProcessDefinitionFile(folder);
                        String processName = folder.getName();
                        Language language = page.getLanguage();
                        Map<String, String> properties = Maps.newHashMap();
                        if (language == Language.BPMN) {
                            properties.put(BpmnSerializer.SWIMLANE_DISPLAY_MODE, page.getSwimlaneDisplayMode().name());
                        }
                        Document document = language.getSerializer().getInitialProcessDefinitionDocument(processName, properties);
                        byte[] bytes = XmlUtil.writeXml(document);
                        definitionFile.create(new ByteArrayInputStream(bytes), true, null);
                        monitor.worked(1);
                        IFile gpdFile = folder.getFile(GpdXmlContentProvider.GPD_FILE_NAME);
                        gpdFile.create(createInitialGpdInfo(page.getLanguage().getNotation()), true, null);
                        monitor.worked(1);
                        ProcessCache.newProcessDefinitionWasCreated(definitionFile);
                        WorkspaceOperations.openProcessDefinition(definitionFile);
                        monitor.worked(1);
                        BasicNewResourceWizard.selectAndReveal(gpdFile, getActiveWorkbenchWindow());
                        monitor.done();
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            PluginLogger.logError(Localization.getString("NewProcessDefinitionWizard.error.creation"), e.getTargetException());
            return false;
        } catch (InterruptedException e) {
        }
        return true;
    }

    private IWorkbenchWindow getActiveWorkbenchWindow() {
        return workbench.getActiveWorkbenchWindow();
    }

    private InputStream createInitialGpdInfo(String notation) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffer.append("\n");
        buffer.append("\n");
        buffer.append("<process-diagram notation=\"").append(notation).append("\" showActions=\"true\"></process-diagram>");
        return new ByteArrayInputStream(buffer.toString().getBytes(Charsets.UTF_8));
    }
}
