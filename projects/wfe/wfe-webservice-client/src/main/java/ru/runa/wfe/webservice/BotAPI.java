
package ru.runa.wfe.webservice;

import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2-12/14/2009 02:16 PM(ramkris)-
 * Generated source version: 2.2
 * 
 */
@WebService(name = "BotAPI", targetNamespace = "http://impl.service.wfe.runa.ru/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface BotAPI {


    /**
     * 
     * @param bot
     * @param user
     * @return
     *     returns ru.runa.wfe.webservice.Bot
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "createBot", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.CreateBot")
    @ResponseWrapper(localName = "createBotResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.CreateBotResponse")
    public Bot createBot(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "bot", targetNamespace = "")
        Bot bot);

    /**
     * 
     * @param botStation
     * @param user
     * @return
     *     returns ru.runa.wfe.webservice.BotStation
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "createBotStation", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.CreateBotStation")
    @ResponseWrapper(localName = "createBotStationResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.CreateBotStationResponse")
    public BotStation createBotStation(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "botStation", targetNamespace = "")
        BotStation botStation);

    /**
     * 
     * @param botTask
     * @param user
     * @return
     *     returns ru.runa.wfe.webservice.BotTask
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "createBotTask", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.CreateBotTask")
    @ResponseWrapper(localName = "createBotTaskResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.CreateBotTaskResponse")
    public BotTask createBotTask(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "botTask", targetNamespace = "")
        BotTask botTask);

    /**
     * 
     * @param bot
     * @param user
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "exportBot", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ExportBot")
    @ResponseWrapper(localName = "exportBotResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ExportBotResponse")
    public byte[] exportBot(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "bot", targetNamespace = "")
        Bot bot);

    /**
     * 
     * @param botStation
     * @param user
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "exportBotStation", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ExportBotStation")
    @ResponseWrapper(localName = "exportBotStationResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ExportBotStationResponse")
    public byte[] exportBotStation(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "botStation", targetNamespace = "")
        BotStation botStation);

    /**
     * 
     * @param bot
     * @param botTaskName
     * @param user
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "exportBotTask", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ExportBotTask")
    @ResponseWrapper(localName = "exportBotTaskResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ExportBotTaskResponse")
    public byte[] exportBotTask(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "bot", targetNamespace = "")
        Bot bot,
        @WebParam(name = "botTaskName", targetNamespace = "")
        String botTaskName);

    /**
     * 
     * @param id
     * @param user
     * @return
     *     returns ru.runa.wfe.webservice.Bot
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getBot", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBot")
    @ResponseWrapper(localName = "getBotResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotResponse")
    public Bot getBot(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "id", targetNamespace = "")
        Long id);

    /**
     * 
     * @param id
     * @return
     *     returns ru.runa.wfe.webservice.BotStation
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getBotStation", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotStation")
    @ResponseWrapper(localName = "getBotStationResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotStationResponse")
    public BotStation getBotStation(
        @WebParam(name = "id", targetNamespace = "")
        Long id);

    /**
     * 
     * @param name
     * @return
     *     returns ru.runa.wfe.webservice.BotStation
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getBotStationByName", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotStationByName")
    @ResponseWrapper(localName = "getBotStationByNameResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotStationByNameResponse")
    public BotStation getBotStationByName(
        @WebParam(name = "name", targetNamespace = "")
        String name);

    /**
     * 
     * @return
     *     returns java.util.List<ru.runa.wfe.webservice.BotStation>
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getBotStations", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotStations")
    @ResponseWrapper(localName = "getBotStationsResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotStationsResponse")
    public List<BotStation> getBotStations();

    /**
     * 
     * @param id
     * @param user
     * @return
     *     returns ru.runa.wfe.webservice.BotTask
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getBotTask", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotTask")
    @ResponseWrapper(localName = "getBotTaskResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotTaskResponse")
    public BotTask getBotTask(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "id", targetNamespace = "")
        Long id);

    /**
     * 
     * @param id
     * @param user
     * @return
     *     returns java.util.List<ru.runa.wfe.webservice.BotTask>
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getBotTasks", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotTasks")
    @ResponseWrapper(localName = "getBotTasksResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotTasksResponse")
    public List<BotTask> getBotTasks(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "id", targetNamespace = "")
        Long id);

    /**
     * 
     * @param botStationId
     * @param user
     * @return
     *     returns java.util.List<ru.runa.wfe.webservice.Bot>
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getBots", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBots")
    @ResponseWrapper(localName = "getBotsResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.GetBotsResponse")
    public List<Bot> getBots(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "botStationId", targetNamespace = "")
        Long botStationId);

    /**
     * 
     * @param replace
     * @param botStation
     * @param archive
     * @param user
     */
    @WebMethod
    @RequestWrapper(localName = "importBot", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ImportBot")
    @ResponseWrapper(localName = "importBotResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ImportBotResponse")
    public void importBot(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "botStation", targetNamespace = "")
        BotStation botStation,
        @WebParam(name = "archive", targetNamespace = "")
        byte[] archive,
        @WebParam(name = "replace", targetNamespace = "")
        boolean replace);

    /**
     * 
     * @param replace
     * @param archive
     * @param user
     */
    @WebMethod
    @RequestWrapper(localName = "importBotStation", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ImportBotStation")
    @ResponseWrapper(localName = "importBotStationResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.ImportBotStationResponse")
    public void importBotStation(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "archive", targetNamespace = "")
        byte[] archive,
        @WebParam(name = "replace", targetNamespace = "")
        boolean replace);

    /**
     * 
     * @param id
     * @param user
     */
    @WebMethod
    @RequestWrapper(localName = "removeBot", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.RemoveBot")
    @ResponseWrapper(localName = "removeBotResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.RemoveBotResponse")
    public void removeBot(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "id", targetNamespace = "")
        Long id);

    /**
     * 
     * @param id
     * @param user
     */
    @WebMethod
    @RequestWrapper(localName = "removeBotStation", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.RemoveBotStation")
    @ResponseWrapper(localName = "removeBotStationResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.RemoveBotStationResponse")
    public void removeBotStation(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "id", targetNamespace = "")
        Long id);

    /**
     * 
     * @param id
     * @param user
     */
    @WebMethod
    @RequestWrapper(localName = "removeBotTask", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.RemoveBotTask")
    @ResponseWrapper(localName = "removeBotTaskResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.RemoveBotTaskResponse")
    public void removeBotTask(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "id", targetNamespace = "")
        Long id);

    /**
     * 
     * @param bot
     * @param user
     */
    @WebMethod
    @RequestWrapper(localName = "updateBot", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.UpdateBot")
    @ResponseWrapper(localName = "updateBotResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.UpdateBotResponse")
    public void updateBot(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "bot", targetNamespace = "")
        Bot bot);

    /**
     * 
     * @param botStation
     * @param user
     */
    @WebMethod
    @RequestWrapper(localName = "updateBotStation", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.UpdateBotStation")
    @ResponseWrapper(localName = "updateBotStationResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.UpdateBotStationResponse")
    public void updateBotStation(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "botStation", targetNamespace = "")
        BotStation botStation);

    /**
     * 
     * @param botTask
     * @param user
     */
    @WebMethod
    @RequestWrapper(localName = "updateBotTask", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.UpdateBotTask")
    @ResponseWrapper(localName = "updateBotTaskResponse", targetNamespace = "http://impl.service.wfe.runa.ru/", className = "ru.runa.wfe.webservice.UpdateBotTaskResponse")
    public void updateBotTask(
        @WebParam(name = "user", targetNamespace = "")
        User user,
        @WebParam(name = "botTask", targetNamespace = "")
        BotTask botTask);

}
