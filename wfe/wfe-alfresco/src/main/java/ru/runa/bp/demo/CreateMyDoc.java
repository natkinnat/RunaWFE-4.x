package ru.runa.bp.demo;

import ru.runa.alfresco.AlfConnection;
import ru.runa.bp.AlfHandler;
import ru.runa.bp.AlfHandlerData;
import ru.runa.wfe.var.FileVariable;

public class CreateMyDoc extends AlfHandler {

    @Override
    protected void executeAction(AlfConnection alfConnection, AlfHandlerData alfHandlerData) throws Exception {
        MyDoc myDoc = new MyDoc();
        alfConnection.createObject(myDoc);
        FileVariable var = alfHandlerData.getInputParamValueNotNull(FileVariable.class, "file");
        alfConnection.setContent(myDoc, var.getData(), var.getContentType());
        alfHandlerData.setOutputParam("uuid", myDoc.getUuidRef());
    }

}
