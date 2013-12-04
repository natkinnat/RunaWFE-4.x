
package ru.runa.wfe.webservice;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2-12/14/2009 02:16 PM(ramkris)-
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "ProfileWebService", targetNamespace = "http://impl.service.wfe.runa.ru/", wsdlLocation = "http://localhost:8080/runawfe-wfe-service-4.1.0/ProfileServiceBean?wsdl")
public class ProfileWebService
    extends Service
{

    private final static URL PROFILEWEBSERVICE_WSDL_LOCATION;
    private final static WebServiceException PROFILEWEBSERVICE_EXCEPTION;
    private final static QName PROFILEWEBSERVICE_QNAME = new QName("http://impl.service.wfe.runa.ru/", "ProfileWebService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8080/runawfe-wfe-service-4.1.0/ProfileServiceBean?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        PROFILEWEBSERVICE_WSDL_LOCATION = url;
        PROFILEWEBSERVICE_EXCEPTION = e;
    }

    public ProfileWebService() {
        super(__getWsdlLocation(), PROFILEWEBSERVICE_QNAME);
    }

    public ProfileWebService(WebServiceFeature... features) {
        super(__getWsdlLocation(), PROFILEWEBSERVICE_QNAME, features);
    }

    public ProfileWebService(URL wsdlLocation) {
        super(wsdlLocation, PROFILEWEBSERVICE_QNAME);
    }

    public ProfileWebService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, PROFILEWEBSERVICE_QNAME, features);
    }

    public ProfileWebService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ProfileWebService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns ProfileAPI
     */
    @WebEndpoint(name = "ProfileAPIPort")
    public ProfileAPI getProfileAPIPort() {
        return super.getPort(new QName("http://impl.service.wfe.runa.ru/", "ProfileAPIPort"), ProfileAPI.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ProfileAPI
     */
    @WebEndpoint(name = "ProfileAPIPort")
    public ProfileAPI getProfileAPIPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://impl.service.wfe.runa.ru/", "ProfileAPIPort"), ProfileAPI.class, features);
    }

    private static URL __getWsdlLocation() {
        if (PROFILEWEBSERVICE_EXCEPTION!= null) {
            throw PROFILEWEBSERVICE_EXCEPTION;
        }
        return PROFILEWEBSERVICE_WSDL_LOCATION;
    }

}
