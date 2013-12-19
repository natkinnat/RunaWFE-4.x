package ru.runa.alfresco;

import java.util.Collection;
import java.util.List;

import ru.runa.alfresco.search.Search;

/**
 * Connector interface to Alfresco repository.
 * 
 * @author dofs
 */
public interface AlfConn {

    /**
     * Initializes type definition if not initialized yet. Initialization means
     * that some attributes about property and associations will be requested
     * from Alfresco.
     * 
     * @param typeDesc
     */
    public void initializeTypeDefinition(AlfTypeDesc typeDesc);

    /**
     * Load object from Alfresco repository.
     * 
     * @param uuidRef
     *            UUID reference with space store
     *            (workspace://SpacesStore/05b9ec4c
     *            -74f7-4d52-9015-5697374a9b6a).
     * @return loaded object or <code>null</code>
     */
    public <T extends AlfObject> T loadObject(String uuidRef);

    /**
     * Load object from Alfresco repository or throws exception.
     * 
     * @param uuidRef
     *            UUID reference with space store
     *            (workspace://SpacesStore/05b9ec4c
     *            -74f7-4d52-9015-5697374a9b6a).
     * @return loaded object
     */
    public <T extends AlfObject> T loadObjectNotNull(String uuidRef);

    /**
     * Loads association from Alfresco repository.
     * 
     * @param uuidRef
     *            UUID reference with space store
     *            (workspace://SpacesStore/05b9ec4c
     *            -74f7-4d52-9015-5697374a9b6a).
     * @param collection
     *            container for association objects.
     * @param desc
     *            descriptor.
     */
    @SuppressWarnings("rawtypes")
    public void loadAssociation(String uuidRef, Collection collection, AlfSerializerDesc desc);

    /**
     * Finds object in Alfresco repository.
     * 
     * @param <T>
     *            result type
     * @param search
     *            valid Lucene query in object presentation.
     * @return found object or <code>null</code>
     */
    public <T extends AlfObject> T findObject(Search search);

    /**
     * Finds objects in Alfresco repository.
     * 
     * @param <T>
     *            result type
     * @param search
     *            valid Lucene query in object presentation.
     * @return list of found objects
     */
    public <T extends AlfObject> List<T> findObjects(Search search);

    /**
     * Updates object in Alfresco without creation new version.
     * 
     * @param object
     * @param force
     *            all properties
     */
    public boolean updateObject(AlfObject object, boolean force);

    /**
     * Updates object in Alfresco with creation new version.
     * 
     * @param object
     * @param force
     *            all properties
     * @param comment
     *            version comment
     */
    public boolean updateObject(AlfObject object, boolean force, String comment);

    /**
     * Updates object associations in Alfresco.
     * 
     * @param object
     */
    public boolean updateObjectAssociations(AlfObject object);

    /**
     * Deletes object from Alfresco.
     * 
     * @param object
     */
    public void deleteObject(AlfObject object);
}