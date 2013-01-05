package ru.runa.gpd.formeditor.wysiwyg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.UIPlugin;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.formeditor.WYSIWYGPlugin;
import ru.runa.gpd.formeditor.ftl.FreemarkerUtil;
import ru.runa.gpd.formeditor.vartag.VarTagUtil;
import ru.runa.gpd.lang.model.FormNode;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.util.EditorUtils;
import tk.eclipse.plugin.htmleditor.HTMLPlugin;
import tk.eclipse.plugin.htmleditor.editors.HTMLConfiguration;
import tk.eclipse.plugin.htmleditor.editors.HTMLSourceEditor;

/**
 * The WYSIWYG HTML editor using <a
 * href="http://www.fckeditor.net/">FCKeditor</a>.
 * <p>
 * org.eclipse.ui.texteditor.BasicTextEditorActionContributor
 * </p>
 */
public class WYSIWYGHTMLEditor extends MultiPageEditorPart implements StatusTextListener, IResourceChangeListener {
    private static final String PREFIX_SYNCHTML = "SYNCHTML:";
    private static final String PREFIX_HTML = "HTML:";
    private static final String PREFIX_STARTSYNC = "STARTSYNC";
    public static final int CLOSED = 197;
    private HTMLSourceEditor sourceEditor;
    private Browser browser;
    private boolean ftlFormat = true;
    private FormNode formNode;
    private List<String> lazyVariableNameList;
    private boolean dirty = false;
    private boolean browserLoaded = false;
    private static final Pattern pattern = Pattern.compile("^(.*?<(body|BODY).*?>)(.*?)(</(body|BODY)>.*?)$", Pattern.DOTALL);
    private Timer updateSaveButtonTimer = new Timer("updateSaveButtonTimer");
    private String savedHTML = "";
    private static WYSIWYGHTMLEditor lastInitializedInstance;

    private void syncBrowser2Editor() {
        if (browser != null) {
            browser.execute("getHTML(false)");
        }
    }

    protected synchronized boolean isBrowserLoaded() {
        return browserLoaded;
    }

    protected synchronized void setBrowserLoaded(boolean browserLoaded) {
        this.browserLoaded = browserLoaded;
    }

    public void setFormNode(FormNode formNode) {
        this.formNode = formNode;
    }

    public synchronized List<String> getLazyVariableNameList() {
        if (lazyVariableNameList == null) {
            lazyVariableNameList = new ArrayList<String>();
            for (Variable variable : getVariablesList(false)) {
                lazyVariableNameList.add(variable.getName());
            }
        }
        return lazyVariableNameList;
    }

    public List<Variable> getVariablesList(boolean onlyVariablesWithSpace) {
        if (formNode == null) {
            // This is because earlier access from web page (not user request)
            return new ArrayList<Variable>();
        }
        List<Variable> variableWithSwimlanes = new ArrayList<Variable>();
        for (Variable variable : formNode.getProcessDefinition().getVariables()) {
            if (onlyVariablesWithSpace || variable.getName().indexOf(" ") == -1) {
                variableWithSwimlanes.add(variable);
            }
        }
        for (String swimlaneName : formNode.getProcessDefinition().getSwimlaneNames()) {
            if (onlyVariablesWithSpace || swimlaneName.indexOf(" ") == -1) {
                variableWithSwimlanes.add(Variable.createForSwimlane(swimlaneName));
            }
        }
        return variableWithSwimlanes;
    }

    public Map<String, Variable> getVariablesMap(boolean includeVariablesWithSpace) {
        Map<String, Variable> variableWithSwimlanes = new HashMap<String, Variable>();
        // This is because earlier access from web page (not user request)
        if (formNode != null) {
            for (Variable variable : formNode.getProcessDefinition().getVariables()) {
                if (includeVariablesWithSpace || variable.getName().indexOf(" ") == -1) {
                    variableWithSwimlanes.put(variable.getName(), variable);
                }
            }
            for (String swimlaneName : formNode.getProcessDefinition().getSwimlaneNames()) {
                if (includeVariablesWithSpace || swimlaneName.indexOf(" ") == -1) {
                    variableWithSwimlanes.put(swimlaneName, Variable.createForSwimlane(swimlaneName));
                }
            }
        }
        return variableWithSwimlanes;
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == ITextEditor.class) {
            return sourceEditor;
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (sourceEditor != null && sourceEditor.getEditorInput() != null) {
            EditorUtils.closeEditorIfRequired(event, ((IFileEditorInput) sourceEditor.getEditorInput()).getFile(), this);
        }
    }

    private void syncEditor2Browser() {
        String html = getSourceDocumentHTML();
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            html = matcher.group(3);
        }
        if (isFtlFormat()) {
            try {
                html = FreemarkerUtil.transformToHtml(getVariablesMap(true).keySet(), html);
            } catch (Exception e) {
                WYSIWYGPlugin.logError("ftl WYSIWYGHTMLEditor.syncEditor2Browser()", e);
            }
        }
        html = html.replaceAll("\r\n", "\n");
        html = html.replaceAll("\r", "\n");
        html = html.replaceAll("\n", "\\\\n");
        html = html.replaceAll("'", "\\\\'");
        if (browser != null) {
            browser.execute("setHTML('" + html + "')");
        }
    }

    @Override
    protected void pageChange(int newPageIndex) {
        if (newPageIndex == 1) {
            syncBrowser2Editor();
        } else if (newPageIndex == 0) {
            ConnectorServletHelper.sync();
            syncEditor2Browser();
        }
        super.pageChange(newPageIndex);
    }

    @Override
    protected void createPages() {
        sourceEditor = new HTMLSourceEditor(new HTMLConfiguration(HTMLPlugin.getDefault().getColorProvider()));
        int pageNumber = 0;
        try {
            browser = new Browser(getContainer(), SWT.NULL);
            browser.addStatusTextListener(this);
            browser.addOpenWindowListener(new BrowserWindowHelper(getContainer().getDisplay()));
            addPage(browser);
            setPageText(pageNumber++, WYSIWYGPlugin.getResourceString("wysiwyg.design.tab_name"));
        } catch (Throwable th) {
            PluginLogger.logError(WYSIWYGPlugin.getResourceString("wysiwyg.design.create_error"), th);
        }
        try {
            addPage(sourceEditor, getEditorInput());
            setPageText(pageNumber++, WYSIWYGPlugin.getResourceString("wysiwyg.source.tab_name"));
        } catch (Exception ex) {
            PluginLogger.logError(WYSIWYGPlugin.getResourceString("wysiwyg.source.create_error"), ex);
        }
        if (browser == null) {
            return;
        }
        ConnectorServletHelper.setBaseDir(sourceEditor.getFile().getParent());
        try {
            final Display display = Display.getCurrent();
            final ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(getSite().getShell());
            final IRunnableWithProgress runnable = new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask(WYSIWYGPlugin.getResourceString("editor.task.init_wysiwyg"), 10);
                        WYSIWYGPlugin.getDefault().startWebServer(monitor, 9);
                        monitor.subTask(WYSIWYGPlugin.getResourceString("editor.subtask.waiting_init"));
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                monitorDialog.setCancelable(true);
                                browser.setUrl(WYSIWYGPlugin.getDefault().getEditorURL());
                            }
                        });
                        monitorDialog.setCancelable(true);
                        while (!isBrowserLoaded() && !monitor.isCanceled()) {
                            Thread.sleep(1000);
                        }
                        monitor.worked(1);
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                setActivePage(0);
                            }
                        });
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            };
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        monitorDialog.run(true, false, runnable);
                    } catch (InvocationTargetException e) {
                        PluginLogger.logError(WYSIWYGPlugin.getResourceString("wysiwyg.design.create_error"), e.getTargetException());
                    } catch (InterruptedException e) {
                        WYSIWYGPlugin.logError("Web editor page", e);
                    }
                }
            });
            savedHTML = getSourceDocumentHTML();
            savedHTML = removeRN(savedHTML);
        } catch (Exception e) {
            MessageDialog.openError(getContainer().getShell(), WYSIWYGPlugin.getResourceString("wysiwyg.design.create_error"), e.getCause().getMessage());
            WYSIWYGPlugin.logError("Web editor page", e);
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        super.init(site, editorInput);
        setPartName(editorInput.getName());
        ftlFormat = editorInput.getName().endsWith("ftl");
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
        lastInitializedInstance = this;
    }

    // Used from servlets
    public static WYSIWYGHTMLEditor getCurrent() {
        IEditorPart editor = UIPlugin.getDefault().getWorkbench().getWorkbenchWindows()[0].getActivePage().getActiveEditor();
        if (editor instanceof WYSIWYGHTMLEditor) {
            return (WYSIWYGHTMLEditor) editor;
        }
        if (lastInitializedInstance != null) {
            return lastInitializedInstance;
        }
        throw new RuntimeException("No editor instance initialized");
    }

    private void setDirty(boolean dirty) {
        boolean changedDirtyState = this.dirty != dirty;
        this.dirty = dirty;
        if (changedDirtyState) {
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    @Override
    public boolean isDirty() {
        return dirty || sourceEditor.isDirty();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        if (getActivePage() == 0) {
            syncBrowser2Editor();
        }
        sourceEditor.doSave(monitor);
        savedHTML = getSourceDocumentHTML();
        savedHTML = removeRN(savedHTML);
        if (formNode != null) {
            formNode.setDirty();
        }
        setDirty(false);
    }

    public boolean isFtlFormat() {
        return ftlFormat;
    }

    @Override
    public void changed(StatusTextEvent event) {
        String text = event.text;
        if (text.startsWith(PREFIX_STARTSYNC)) {
            updateSaveButtonTimer.schedule(new UpdateSaveButtonTimerTask(), 5000, 5000);
        } else if (text.startsWith(PREFIX_HTML) && text.length() > PREFIX_HTML.length()) {
            String html = getDesignDocumentHTML(PREFIX_HTML, text);
            String oldContent = getSourceDocumentHTML();
            if (!oldContent.equals(html)) {
                sourceEditor.getDocumentProvider().getDocument(sourceEditor.getEditorInput()).set(html);
                setDirty(true);
            }
        } else if (text.startsWith(PREFIX_SYNCHTML) && text.length() > PREFIX_SYNCHTML.length()) {
            String html = getDesignDocumentHTML(PREFIX_SYNCHTML, text);
            html = removeRN(html);
            String diff = StringUtils.difference(savedHTML, html);
            boolean setDirty = (diff.length() != 0);
            if (setDirty != isDirty()) {
                setDirty(setDirty);
            }
        }
    }

    private String removeRN(String string) {
        string = string.replaceAll("\\n", "");
        string = string.replaceAll("\\r", "");
        return string;
    }

    private String getDesignDocumentHTML(String prefix, String statusText) {
        String html = statusText.substring(prefix.length());
        if (isFtlFormat()) {
            try {
                html = FreemarkerUtil.transformFromHtml(html, getLazyVariableNameList());
                Matcher matcher = pattern.matcher(html);
                if (matcher.find()) {
                    html = matcher.group(3);
                }
            } catch (Exception e) {
                PluginLogger.logErrorWithoutDialog("freemarker html transform", e);
            }
        } else {
            // bug in closing customtag tag
            html = VarTagUtil.normalizeVarTags(html);
        }
        return html;
    }

    private String getSourceDocumentHTML() {
        return sourceEditor.getDocumentProvider().getDocument(sourceEditor.getEditorInput()).get();
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        if (getActivePage() == 0) {
            syncBrowser2Editor();
            return true;
        }
        return super.isSaveOnCloseNeeded();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void dispose() {
        updateSaveButtonTimer.cancel();
        firePropertyChange(CLOSED);
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
    }

    private class UpdateSaveButtonTimerTask extends TimerTask {
        @Override
        public void run() {
            try {
                if (getActivePage() == 0 && browser != null) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                browser.execute("getHTML(true)");
                            } catch (Throwable e) {
                                // ignore
                            }
                        }
                    });
                }
            } catch (Throwable e) {
                // ignore SWTException: Widget is disposed
            }
        }
    }
}
