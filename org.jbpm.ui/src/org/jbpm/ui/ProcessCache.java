package org.jbpm.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.jbpm.ui.common.model.ProcessDefinition;
import org.jbpm.ui.util.ProjectFinder;

public class ProcessCache {
    private static Map<IFile, ProcessDefinition> CACHE_BY_FILE = new HashMap<IFile, ProcessDefinition>();
    private static Map<String, ProcessDefinition> CACHE_BY_NAME = new HashMap<String, ProcessDefinition>();

    static {
        try {
            for (IFile file : ProjectFinder.getAllProcessDefinitionFiles()) {
                try {
                    ProcessDefinition definition = JpdlVersionRegistry.parseProcessDefinition(file);
                    cacheProcessDefinition(file, definition);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    private static void cacheProcessDefinition(IFile file, ProcessDefinition definition) throws Exception {
        ParContentProvider.readAuxInfo(file, definition);
        CACHE_BY_FILE.put(file, definition);
        CACHE_BY_NAME.put(definition.getName(), definition);
    }

    public static void newProcessDefinitionWasCreated(IFile file) {
        try {
            ProcessDefinition definition = JpdlVersionRegistry.parseProcessDefinition(file);
            cacheProcessDefinition(file, definition);
        } catch (Exception e) {
            DesignerLogger.logError("Parsing process definition failed: " + file.toString(), e);
        }
    }

    public static void processDefinitionWasDeleted(IFile file) {
        try {
            ProcessDefinition definition = CACHE_BY_FILE.remove(file);
            if (definition != null) {
                CACHE_BY_NAME.remove(definition.getName());
            }
        } catch (Exception e) {
            DesignerLogger.logError("Parsing process definition failed: " + file.toString(), e);
        }
    }

    public static Set<ProcessDefinition> getAllProcessDefinitions() {
        return new HashSet<ProcessDefinition>(CACHE_BY_NAME.values());
    }

    public static List<String> getAllProcessDefinitionNames() {
        List<String> list = new ArrayList<String>(CACHE_BY_NAME.keySet());
        Collections.sort(list);
        return list;
    }

    public static Map<IFile, ProcessDefinition> getAllProcessDefinitionsMap() {
        return new HashMap<IFile, ProcessDefinition>(CACHE_BY_FILE);
    }

    public static void invalidateProcessDefinition(IFile file) {
        ProcessDefinition definition = CACHE_BY_FILE.remove(file);
        if (definition != null) {
            CACHE_BY_NAME.remove(definition.getName());
            getProcessDefinition(file);
        }
    }

    public static ProcessDefinition getProcessDefinition(IFile file) {
        if (!CACHE_BY_FILE.containsKey(file)) {
            try {
                ProcessDefinition definition = JpdlVersionRegistry.parseProcessDefinition(file);
                cacheProcessDefinition(file, definition);
            } catch (Exception e) {
            	throw new RuntimeException("Parsing process definition failed: " + file.toString(), e);
            }
        }
        return CACHE_BY_FILE.get(file);
    }

    public static ProcessDefinition getProcessDefinition(String name) {
        if (!CACHE_BY_NAME.containsKey(name)) {
            try {
                for (IFile file : ProjectFinder.getAllProcessDefinitionFiles()) {
                    String processName = file.getFullPath().segment(3);
                    if (name.equals(processName)) {
                        ProcessDefinition definition = JpdlVersionRegistry.parseProcessDefinition(file);
                        cacheProcessDefinition(file, definition);
                        break;
                    }
                }
            } catch (Exception e) {
                DesignerLogger.logError("Parsing process definition failed: " + name, e);
                return null;
            }
        }
        return CACHE_BY_NAME.get(name);
    }

}
