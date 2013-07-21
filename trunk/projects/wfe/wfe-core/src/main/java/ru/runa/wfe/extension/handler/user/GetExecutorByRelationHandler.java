package ru.runa.wfe.extension.handler.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.runa.wfe.extension.handler.CommonParamBasedHandler;
import ru.runa.wfe.extension.handler.HandlerData;
import ru.runa.wfe.relation.Relation;
import ru.runa.wfe.relation.RelationPair;
import ru.runa.wfe.relation.dao.RelationDAO;
import ru.runa.wfe.relation.dao.RelationPairDAO;
import ru.runa.wfe.user.Executor;

import com.google.common.collect.Lists;

public class GetExecutorByRelationHandler extends CommonParamBasedHandler {
    @Autowired
    private RelationDAO relationDAO;
    @Autowired
    private RelationPairDAO relationPairDAO;

    @Override
    protected void executeAction(HandlerData handlerData) throws Exception {
        String relationName = handlerData.getInputParam(String.class, "name");
        Executor parameter = handlerData.getInputParam(Executor.class, "parameter");
        boolean inversed = handlerData.getInputParam(boolean.class, "inversed");
        List<Executor> executors = Lists.newArrayList(parameter);
        Relation relation = relationDAO.getNotNull(relationName);
        List<RelationPair> pairs;
        if (inversed) {
            pairs = relationPairDAO.getExecutorsRelationPairsLeft(relation, executors);
        } else {
            pairs = relationPairDAO.getExecutorsRelationPairsRight(relation, executors);
        }
        if (pairs.size() == 0) {
            String option = handlerData.getInputParam(String.class, "missedCaseOption");
            if ("THROW_ERROR".equals(option)) {
                throw new Exception("Relation " + (inversed ? "!" : "") + "'" + relationName + "' does not defined for " + parameter.getLabel());
            }
            handlerData.setOutputParam("result", null);
            return;
        }
        if (pairs.size() > 1) {
            log.warn(pairs);
            String option = handlerData.getInputParam(String.class, "multipleCaseOption");
            if ("THROW_ERROR".equals(option)) {
                throw new Exception("Relation " + (inversed ? "!" : "") + "'" + relationName + "' has multiple choice for " + parameter.getLabel());
            }
        }
        Executor result;
        if (inversed) {
            result = pairs.get(0).getRight();
        } else {
            result = pairs.get(0).getLeft();
        }
        handlerData.setOutputParam("result", result);
    }

}
