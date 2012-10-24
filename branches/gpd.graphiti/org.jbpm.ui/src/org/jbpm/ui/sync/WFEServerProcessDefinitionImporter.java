package org.jbpm.ui.sync;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jbpm.ui.DesignerLogger;

import ru.runa.af.presentation.BatchPresentation;
import ru.runa.af.presentation.BatchPresentationConsts;
import ru.runa.af.presentation.BatchPresentationFactory;
import ru.runa.service.wf.DefinitionService;
import ru.runa.wf.DefinitionDoesNotExistException;
import ru.runa.wf.WfDefinition;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class WFEServerProcessDefinitionImporter extends DataImporter {

    private final Map<WfDefinition, List<WfDefinition>> definitions = Maps.newHashMap();

    private static WFEServerProcessDefinitionImporter instance;

    private WFEServerProcessDefinitionImporter() {
        super(WFEServerConnector.getInstance());
    }

    public static synchronized WFEServerProcessDefinitionImporter getInstance() {
        if (instance == null) {
            instance = new WFEServerProcessDefinitionImporter();
        }
        return instance;
    }

    @Override
    public boolean hasCachedData() {
        return definitions.size() > 0;
    }

    @Override
    protected void clearInMemoryCache() {
        definitions.clear();
    }

    @Override
    public Map<WfDefinition, List<WfDefinition>> loadCachedData() {
        return definitions;
    }

    @Override
    protected void loadRemoteData(IProgressMonitor monitor) throws Exception {
        DefinitionService definitionService = getDefinitionService();
        BatchPresentation batch = BatchPresentationFactory.DEFINITIONS.createDefault();
        batch.setRangeSize(BatchPresentationConsts.MAX_UNPAGED_REQUEST_SIZE);
        List<WfDefinition> latests = definitionService.getLatestProcessDefinitions(WFEServerConnector.getInstance().getSubject(), batch);
        monitor.worked(30);
        double perDefinition = (double) 70 / latests.size();
        for (WfDefinition latest : latests) {
            List<WfDefinition> historyDefinitionStubs;
            try {
                historyDefinitionStubs = definitionService.getProcessDefinitionHistory(WFEServerConnector.getInstance().getSubject(), latest
                        .getName());
            } catch (Exception e) {
                DesignerLogger.logErrorWithoutDialog("definitions sync", e);
                historyDefinitionStubs = Lists.newArrayList();
            }
            definitions.put(latest, historyDefinitionStubs);
            monitor.internalWorked(perDefinition);
        }
    }

    @Override
    protected void saveCachedData() throws Exception {
    }

    public byte[] loadPar(WfDefinition definition) throws Exception {
        WFEServerConnector.getInstance().connect();
        return getDefinitionService().getFile(WFEServerConnector.getInstance().getSubject(), definition.getId(), "par");
    }

    public void uploadPar(String definitionName, byte[] par) throws Exception {
        WfDefinition oldVersion = null;
        if (!hasCachedData()) {
            synchronize();
        } else {
            WFEServerConnector.getInstance().connect();
        }
        for (WfDefinition stub : definitions.keySet()) {
            if (definitionName.equals(stub.getName())) {
                oldVersion = stub;
                break;
            }
        }
        WfDefinition lastDefinition;
        List<WfDefinition> lastHistory;
        if (oldVersion != null) {
            String[] types = oldVersion.getCategories();
            if (types == null) {
                types = new String[]{"GPD"};
            }
            try {
                lastDefinition = getDefinitionService().redeployProcessDefinition(WFEServerConnector.getInstance().getSubject(),
                        oldVersion.getId(), par, Lists.newArrayList(types));
                List<WfDefinition> oldHistory = definitions.remove(oldVersion);
                lastHistory = Lists.newArrayList(oldVersion);
                lastHistory.addAll(oldHistory);
            } catch (DefinitionDoesNotExistException e) {
                lastDefinition = getDefinitionService().deployProcessDefinition(WFEServerConnector.getInstance().getSubject(), par,
                        Lists.newArrayList("GPD"));
                lastHistory = Lists.newArrayList();
            }
        } else {
            lastDefinition = getDefinitionService().deployProcessDefinition(WFEServerConnector.getInstance().getSubject(), par,
                    Lists.newArrayList("GPD"));
            lastHistory = Lists.newArrayList();
        }
        definitions.put(lastDefinition, lastHistory);
    }

    private DefinitionService getDefinitionService() throws Exception {
        return WFEServerConnector.getInstance().getService("DefinitionServiceBean");
    }

}
