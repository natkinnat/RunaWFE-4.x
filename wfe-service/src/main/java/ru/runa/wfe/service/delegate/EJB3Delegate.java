package ru.runa.wfe.service.delegate;

import java.util.Map;
import java.util.Properties;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.commons.ClassLoaderUtil;
import ru.runa.wfe.commons.ftl.ExpressionEvaluator;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

public abstract class EJB3Delegate {
    public static final String EJB_REMOTE = "remote";
    private static final String EJB_LOCAL = "";
    private static final String WFE_SERVICE_JAR_NAME = "wfe-service";
    private static Map<String, InitialContext> initialContexts = Maps.newHashMap();
    private static Map<String, Map<String, Object>> services = Maps.newHashMap();
    private String ejbType;
    private String ejbJndiNameFormat;
    private final String jarName;
    private final String beanName;
    private final String localInterfaceClassName;
    private final String remoteInterfaceClassName;
    private String customProviderUrl;

    /**
     * Creates delegate only for remote usage.
     * 
     * @param beanName
     *            EJB bean name
     * @param remoteInterfaceClass
     *            EJB @Remote class
     */
    public EJB3Delegate(String beanName, Class<?> remoteInterfaceClass, String jarName) {
        this.beanName = beanName;
        localInterfaceClassName = null;
        remoteInterfaceClassName = remoteInterfaceClass.getName();
        setEjbType(EJB_REMOTE, true);
        this.jarName = jarName;
    }

    /**
     * Creates delegate only for remote usage.
     * 
     * @param beanName
     *            EJB bean name
     * @param remoteInterfaceClass
     *            EJB @Remote class
     */
    public EJB3Delegate(String beanName, Class<?> remoteInterfaceClass) {
        this(beanName, remoteInterfaceClass, WFE_SERVICE_JAR_NAME);
    }

    /**
     * Creates delegate based on base interface class (implicit assumptions
     * about @Local, @Remote interface and EJB bean naming)
     * 
     * @param baseInterfaceClass
     */
    public EJB3Delegate(Class<?> baseInterfaceClass) {
        beanName = baseInterfaceClass.getSimpleName() + "Bean";
        localInterfaceClassName = "ru.runa.wfe.service.decl." + baseInterfaceClass.getSimpleName() + "Local";
        remoteInterfaceClassName = "ru.runa.wfe.service.decl." + baseInterfaceClass.getSimpleName() + "Remote";
        jarName = WFE_SERVICE_JAR_NAME;
    }

    public void setEjbType(String ejbType, boolean override) {
        if (this.ejbType != null && !override) {
            return;
        }
        this.ejbType = ejbType;
    }

    public void setEjbJndiNameFormat(String ejbJndiNameFormat) {
        this.ejbJndiNameFormat = ejbJndiNameFormat;
    }

    protected String getCustomProviderUrl() {
        return customProviderUrl;
    }

    public void setCustomProviderUrl(String customProviderUrl) {
        this.customProviderUrl = customProviderUrl;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getService() {
        String providerUrl = Objects.firstNonNull(getCustomProviderUrl(), EJB_LOCAL);
        Map<String, Object> providerServices = services.get(providerUrl);
        if (providerServices == null) {
            providerServices = Maps.newHashMap();
            providerServices.put(providerUrl, providerServices);
        }
        if (!providerServices.containsKey(beanName)) {
            Map<String, String> variables = Maps.newHashMap();
            variables.put("jar.name", jarName);
            variables.put("bean.name", beanName);
            variables.put("ejb.type", ejbType);
            String interfaceClassName = EJB_REMOTE.equals(ejbType) ? remoteInterfaceClassName : localInterfaceClassName;
            variables.put("interface.class.name", interfaceClassName);
            String jndiName = ExpressionEvaluator.substitute(ejbJndiNameFormat, variables);
            try {
                Object service = getInitialContext().lookup(jndiName);
                providerServices.put(beanName, service);
            } catch (NamingException e) {
                throw new InternalApplicationException("Unable to locate bean by jndi name '" + jndiName + "'", e);
            }
        }
        return (T) providerServices.get(beanName);
    }

    private InitialContext getInitialContext() {
        String providerUrl = Objects.firstNonNull(getCustomProviderUrl(), EJB_LOCAL);
        if (!initialContexts.containsKey(providerUrl)) {
            try {
                Properties properties = ClassLoaderUtil.getProperties("jndi.properties", false);
                if (!Objects.equal(EJB_LOCAL, providerUrl)) {
                    properties.put(Context.PROVIDER_URL, providerUrl);
                }
                initialContexts.put(providerUrl, new InitialContext(properties));
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return initialContexts.get(providerUrl);
    }

    protected RuntimeException handleException(Exception e) {
        if (e instanceof EJBException && e.getCause() != null) {
            return Throwables.propagate(e.getCause());
        }
        return Throwables.propagate(e);
    }
}