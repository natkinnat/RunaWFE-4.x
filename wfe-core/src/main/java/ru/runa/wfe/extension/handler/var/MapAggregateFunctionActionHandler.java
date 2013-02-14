package ru.runa.wfe.extension.handler.var;

import java.util.Collection;
import java.util.Map;

import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.extension.handler.CommonParamBasedHandler;
import ru.runa.wfe.extension.handler.HandlerData;

import com.google.common.collect.Maps;

public class MapAggregateFunctionActionHandler extends CommonParamBasedHandler {
    @Override
    protected void executeAction(HandlerData handlerData) throws Exception {
        Map<?, ?> map = handlerData.getInputParam(Map.class, "map", null);
        if (map == null) {
            map = Maps.newHashMap();
        }
        String function = handlerData.getInputParam(String.class, "function");
        String functionOn = handlerData.getInputParam(String.class, "on");
        Collection<?> collection;
        if ("KEYS".equals(functionOn)) {
            collection = map.keySet();
        } else {
            collection = map.values();
        }
        Object result;
        if ("SUM".equals(function)) {
            result = getSum(collection);
        } else if ("AVERAGE".equals(function)) {
            double sum = getSum(collection).doubleValue();
            result = sum / collection.size();
        } else if ("COUNT".equals(function)) {
            result = collection.size();
        } else if ("MIN".equals(function)) {
            boolean doubleValue = false;
            double min = Double.MAX_VALUE;
            for (Object object : collection) {
                if (object == null) {
                    continue;
                } else if (object instanceof Number) {
                    if (min > ((Number) object).doubleValue()) {
                        min = ((Number) object).doubleValue();
                    }
                    if (object instanceof Double) {
                        doubleValue = true;
                    }
                } else {
                    throw new InternalApplicationException("Function is applicable to list with numbers only, found " + object.getClass());
                }
            }
            if (doubleValue) {
                result = min;
            } else {
                result = new Long((long) min);
            }
        } else if ("MAX".equals(function)) {
            boolean doubleValue = false;
            double max = Double.MIN_VALUE;
            for (Object object : collection) {
                if (object == null) {
                    continue;
                } else if (object instanceof Number) {
                    if (max < ((Number) object).doubleValue()) {
                        max = ((Number) object).doubleValue();
                    }
                    if (object instanceof Double) {
                        doubleValue = true;
                    }
                } else {
                    throw new InternalApplicationException("Function is applicable to list with numbers only, found " + object.getClass());
                }
            }
            if (doubleValue) {
                result = max;
            } else {
                result = new Long((long) max);
            }
        } else {
            throw new Exception("Unknown function '" + function + "'");
        }
        handlerData.setOutputParam("object", result);
    }

    private Number getSum(Collection<?> collection) {
        boolean doubleValue = false;
        double sum = 0;
        for (Object object : collection) {
            if (object == null) {
                continue;
            } else if (object instanceof Number) {
                sum += ((Number) object).doubleValue();
                if (object instanceof Double) {
                    doubleValue = true;
                }
            } else {
                throw new InternalApplicationException("Function is applicable to list with numbers only, found " + object.getClass());
            }
        }
        if (doubleValue) {
            return sum;
        } else {
            return new Long((long) sum);
        }
    }

}
