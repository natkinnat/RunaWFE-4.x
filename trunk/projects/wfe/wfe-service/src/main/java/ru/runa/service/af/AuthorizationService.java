/*
 * This file is part of the RUNA WFE project.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation; version 2.1 
 * of the License. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.service.af;

import java.util.Collection;
import java.util.List;

import javax.security.auth.Subject;

import ru.runa.wfe.presentation.BatchPresentation;
import ru.runa.wfe.security.AuthenticationException;
import ru.runa.wfe.security.AuthorizationException;
import ru.runa.wfe.security.Identifiable;
import ru.runa.wfe.security.Permission;
import ru.runa.wfe.security.SecuredObjectType;
import ru.runa.wfe.security.UnapplicablePermissionException;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.user.ExecutorDoesNotExistException;
import ru.runa.wfe.user.User;

/**
 * Provides methods for authorization mechanism.
 * <p>
 * Created on 20.07.2004
 * </p>
 */
public interface AuthorizationService {

    /**
     * That means when you are using JBossWS with Document Wrapped you can't
     * have two methods with same name inside the package without specifying
     * customizations. Thus to solve your problem you have to specify at least:
     * 
     * @javax.xml.ws.RequestWrapper(className="package.UniqueClassName")
     * @javax.xml.ws.ResponseWrapper(className="package.UniqueClassNameResponse") 
     *                                                                            method
     *                                                                            annotations
     *                                                                            on
     *                                                                            all
     *                                                                            overloaded
     *                                                                            methods
     *                                                                            (
     *                                                                            overloaded
     *                                                                            here
     *                                                                            means
     *                                                                            not
     *                                                                            overloaded
     *                                                                            in
     *                                                                            class
     *                                                                            but
     *                                                                            overloaded
     *                                                                            in
     *                                                                            package
     *                                                                            )
     *                                                                            .
     */

    /**
     * Checks whether {@link Actor}from subject has permission on
     * {@link Identifiable}.
     */
    public boolean isAllowed(User user, Permission permission, Identifiable identifiable) throws AuthorizationException, AuthenticationException;

    public boolean[] isAllowed(User user, Permission permission, List<? extends Identifiable> identifiables) throws AuthenticationException;

    /**
     * Sets permissions for performer on {@link Identifiable}.
     */
    public void setPermissions(User user, Executor performer, Collection<Permission> permissions, Identifiable identifiable)
            throws AuthorizationException, AuthenticationException, ExecutorDoesNotExistException, UnapplicablePermissionException;

    public void setPermissions(User user, List<Long> executorsId, List<Collection<Permission>> permissions, Identifiable identifiable)
            throws AuthorizationException, AuthenticationException, ExecutorDoesNotExistException, UnapplicablePermissionException;

    public void setPermissions(User user, List<Long> executorsId, Collection<Permission> permissions, Identifiable identifiable)
            throws AuthorizationException, AuthenticationException, ExecutorDoesNotExistException, UnapplicablePermissionException;

    /**
     * Returns an array of Permission that executor has on {@link Identifiable}.
     */
    public Collection<Permission> getPermissions(User user, Executor performer, Identifiable identifiable) throws AuthorizationException,
            AuthenticationException, ExecutorDoesNotExistException;

    /**
     * Returns an array of Permission that executor himself has on
     * {@link Identifiable}.
     */
    public Collection<Permission> getOwnPermissions(User user, Executor performer, Identifiable identifiable) throws AuthorizationException,
            AuthenticationException, ExecutorDoesNotExistException;

    /**
     * Load executor's which already has (or not has) some permission on
     * specified identifiable. This query using paging.
     * 
     * @param subject
     *            Current actor {@linkplain Subject}.
     * @param identifiable
     *            {@linkplain Identifiable} to load executors, which has (or
     *            not) permission on this identifiable.
     * @param batchPresentation
     *            {@linkplain BatchPresentation} for loading executors.
     * @param hasPermission
     *            Flag equals true to load executors with permissions on
     *            {@linkplain Identifiable}; false to load executors without
     *            permissions.
     * @return Executors with or without permission on {@linkplain Identifiable}
     *         .
     */
    public List<Executor> getExecutorsWithPermission(User user, Identifiable identifiable, BatchPresentation batchPresentation, boolean hasPermission)
            throws AuthenticationException, AuthorizationException;

    /**
     * Load executor's count which already has (or not has) some permission on
     * specified identifiable.
     * 
     * @param subject
     *            Current actor {@linkplain Subject}.
     * @param identifiable
     *            {@linkplain Identifiable} to load executors, which has (or
     *            not) permission on this identifiable.
     * @param batchPresentation
     *            {@linkplain BatchPresentation} for loading executors.
     * @param hasPermission
     *            Flag equals true to load executors with permissions on
     *            {@linkplain Identifiable}; false to load executors without
     *            permissions.
     * @return Count of executors with or without permission on
     *         {@linkplain Identifiable}.
     */
    public int getExecutorsWithPermissionCount(User user, Identifiable identifiable, BatchPresentation batchPresentation, boolean hasPermission)
            throws AuthenticationException, AuthorizationException;

    public <T extends Object> List<T> getPersistentObjects(User user, BatchPresentation batchPresentation, Class<T> persistentClass,
            Permission permission, SecuredObjectType[] securedObjectTypes, boolean enablePaging) throws AuthenticationException;

}
