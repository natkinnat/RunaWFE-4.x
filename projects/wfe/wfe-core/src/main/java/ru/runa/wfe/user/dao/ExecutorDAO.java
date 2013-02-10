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
package ru.runa.wfe.user.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

import ru.runa.wfe.commons.dao.CommonDAO;
import ru.runa.wfe.presentation.BatchPresentation;
import ru.runa.wfe.presentation.BatchPresentationFactory;
import ru.runa.wfe.presentation.hibernate.BatchPresentationHibernateCompiler;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.user.ExecutorAlreadyExistsException;
import ru.runa.wfe.user.ExecutorDoesNotExistException;
import ru.runa.wfe.user.ExecutorGroupMembership;
import ru.runa.wfe.user.Group;
import ru.runa.wfe.user.cache.ExecutorCache;
import ru.runa.wfe.user.cache.ExecutorCacheCtrl;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * DAO for managing executors.
 * 
 * @since 2.0
 */
@SuppressWarnings("unchecked")
public class ExecutorDAO extends CommonDAO {
    private final ExecutorCache executorCache = ExecutorCacheCtrl.getInstance();
    private static final String NAME_PROPERTY_NAME = "name";
    private static final String ID_PROPERTY_NAME = "id";
    private static final String CODE_PROPERTY_NAME = "code";

    /**
     * Check if executor with given name exists.
     * 
     * @param executorName
     *            Executor name to check.
     * @return Returns true, if executor with given name exists; false
     *         otherwise.
     */
    public boolean isExecutorExist(String executorName) {
        return getExecutorByName(Executor.class, executorName) != null;
    }

    /**
     * Check if {@linkplain Actor} with given code exists.
     * 
     * @param code
     *            {@linkplain Actor} code to check.
     * @return Returns true, if {@linkplain Actor} with given name exists; false
     *         otherwise.
     */
    public boolean isActorExist(Long code) {
        return getActorByCodeInternal(code) != null;
    }

    /**
     * Load {@linkplain Executor} by name. Throws exception if load is
     * impossible.
     * 
     * @param name
     *            Loaded executor name.
     * @return Executor with specified name.
     */
    public Executor getExecutor(String name) {
        return getExecutor(Executor.class, name);
    }

    /**
     * Load {@linkplain Executor} by identity. Throws exception if load is
     * impossible.
     * 
     * @param name
     *            Loaded executor identity.
     * @return {@linkplain Executor} with specified identity.
     */
    public Executor getExecutor(Long id) {
        return getExecutor(Executor.class, id);
    }

    /**
     * Load {@linkplain Actor} by name. Throws exception if load is impossible,
     * or exist group with same name.
     * 
     * @param name
     *            Loaded actor name.
     * @return {@linkplain Actor} with specified name.
     */
    public Actor getActor(String name) {
        return getExecutor(Actor.class, name);
    }

    /**
     * Load {@linkplain Actor} by name without case check. This method is a big
     * shame for us. It should never have its way out of DAO! It only purpose is
     * to use with stupid Microsoft Active Directory authentication, which is
     * case insensitive. <b>Never use it! </b>
     * 
     * @param name
     *            Loaded actor name.
     * @return {@linkplain Actor} with specified name (case insensitive).
     */
    public Actor getActorCaseInsensitive(final String name) {
        return getHibernateTemplate().execute(new HibernateCallback<Actor>() {

            @Override
            public Actor doInHibernate(Session session) {
                Criteria criteria = session.createCriteria(Actor.class);
                criteria.add(Restrictions.ilike(NAME_PROPERTY_NAME, name, MatchMode.EXACT));
                Actor actor = (Actor) getFirstOrNull(criteria.list());
                return checkExecutorNotNull(actor, name, Actor.class);
            }
        });
    }

    /**
     * Load {@linkplain Actor} by identity. Throws exception if load is
     * impossible, or exist group with same identity.
     * 
     * @param name
     *            Loaded actor identity.
     * @return {@linkplain Actor} with specified identity.
     */
    public Actor getActor(Long id) {
        return getExecutor(Actor.class, id);
    }

    /**
     * Load {@linkplain Actor} by code. Throws exception if load is impossible.
     * 
     * @param name
     *            Loaded actor code.
     * @return {@linkplain Actor} with specified code.
     */
    public Actor getActorByCode(Long code) {
        Actor actor = getActorByCodeInternal(code);
        return checkExecutorNotNull(actor, "with code " + code, Actor.class);
    }

    /**
     * Load {@linkplain Group} by name. Throws exception if load is impossible,
     * or exist actor with same name.
     * 
     * @param name
     *            Loaded group name.
     * @return {@linkplain Group} with specified name.
     */
    public Group getGroup(String name) {
        return getExecutor(Group.class, name);
    }

    /**
     * Load {@linkplain Group} by identity. Throws exception if load is
     * impossible, or exist actor with same identity.
     * 
     * @param name
     *            Loaded group identity.
     * @return {@linkplain Group} with specified identity.
     */
    public Group getGroup(Long id) {
        return getExecutor(Group.class, id);
    }

    /**
     * Load {@linkplain Executor}'s with given identities.
     * 
     * @param ids
     *            Loading {@linkplain Executor}'s identities.
     * @return Loaded executors in same order, as identities.
     */
    public List<Executor> getExecutors(List<Long> ids) {
        return getExecutors(Executor.class, ids, false);
    }

    /**
     * Load {@linkplain Actor}'s with given identities.
     * 
     * @param executorIds
     *            Loading {@linkplain Actor}'s identities.
     * @return Loaded actors in same order, as identities.
     */
    public List<Actor> getActors(List<Long> ids) {
        return getExecutors(Actor.class, ids, false);
    }

    /**
     * Returns Actors by array of executor identities. If id element belongs to
     * group it is replaced by all actors in group recursively.
     * 
     * @param ids
     *            Executors identities, to load actors.
     * @return Loaded actors, belongs to executor identities.
     */
    public List<Actor> getActorsByExecutorIds(List<Long> executorIds) {
        Set<Actor> actorSet = new HashSet<Actor>();
        for (Executor executor : getExecutors(executorIds)) {
            if (executor instanceof Actor) {
                actorSet.add((Actor) executor);
            } else {
                actorSet.addAll(getGroupActors((Group) executor));
            }
        }
        return Lists.newArrayList(actorSet);
    }

    /**
     * Load {@linkplain Actor}'s with given codes.
     * 
     * @param executorIds
     *            Loading {@linkplain Actor}'s codes.
     * @return Loaded actors in same order, as codes.
     */
    public List<Actor> getActorsByCodes(List<Long> codes) {
        return getExecutors(Actor.class, codes, true);
    }

    /**
     * Returns identities of {@linkplain Actor} and all his groups recursively.
     * Actor identity is always result[0], but groups identities order is not
     * specified. </br> For example G1 contains A1 and G2 contains G1. In this
     * case:</br>
     * <code>getActorAndGroupsIds(A1) == {A1.id, G1.id, G2.id}.</code>
     * 
     * @param actor
     *            {@linkplain Actor}, which identity and groups must be loaded.
     * @return Returns identities of {@linkplain Actor} and all his groups
     *         recursively.
     */
    public List<Long> getActorAndGroupsIds(Actor actor) {
        Set<Group> groupSet = getExecutorParentsAll(actor);
        List<Long> ids = Lists.newArrayListWithExpectedSize(groupSet.size() + 1);
        ids.add(actor.getId());
        for (Group group : groupSet) {
            ids.add(group.getId());
        }
        return ids;
    }

    /**
     * Load available {@linkplain Actor}'s with given codes. If actor with some
     * code not available, it will be ignored. Result order is not specified.
     * 
     * @param executorIds
     *            Loading {@linkplain Actor}'s codes.
     * @return Loaded actors.
     */
    public List<Actor> getAvailableActorsByCodes(List<Long> codes) {
        return getHibernateTemplate().find("select actor from Actor as actor where actor.code in ?", codes);
    }

    /**
     * Load {@linkplain Group}'s with given identities.
     * 
     * @param executorIds
     *            Loading {@linkplain Group}'s identities.
     * @return Loaded groups in same order, as identities.
     */
    public List<Group> getGroups(List<Long> ids) {
        return getExecutors(Group.class, ids, false);
    }

    /**
     * Create executor (save it to database). Generate code property for
     * {@linkplain Actor} with code == 0.
     * 
     * @param <T>
     *            Creating executor class.
     * @param executor
     *            Creating executor.
     * @return Returns created executor.
     */
    public <T extends Executor> T create(T executor) {
        if (isExecutorExist(executor.getName())) {
            throw new ExecutorAlreadyExistsException(executor.getName());
        }
        if (executor instanceof Actor) {
            checkActorCode((Actor) executor);
        }
        getHibernateTemplate().save(executor);
        return executor;
    }

    /**
     * Create executors (save it to database). Generate code property for
     * {@linkplain Actor} with code == 0.
     * 
     * @param executors
     *            Creating executors
     * @return Returns created executors.
     */
    public void create(List<? extends Executor> executors) {
        for (Executor executor : executors) {
            create(executor);
        }
    }

    /**
     * Updates password for {@linkplain Actor}.
     * 
     * @param actor
     *            {@linkplain Actor} to update password.
     * @param password
     *            New actor password.
     */
    public void setPassword(Actor actor, String password) {
        Preconditions.checkNotNull(password, "Password must be specified.");
        ActorPassword actorPassword = getActorPassword(actor);
        if (actorPassword == null) {
            actorPassword = new ActorPassword(actor, password);
            getHibernateTemplate().save(actorPassword);
        } else {
            actorPassword.setPassword(password);
            getHibernateTemplate().merge(actorPassword);
        }
    }

    /**
     * Check if password is valid for user.
     * 
     * @param actor
     *            {@linkplain Actor}, which password is checking.
     * @param password
     *            Checking password.
     * @return Returns true, if password is correct for actor and false
     *         otherwise.
     */
    public boolean isPasswordValid(Actor actor, String password) {
        Preconditions.checkNotNull(password, "Password must be specified.");
        ActorPassword actorPassword = new ActorPassword(actor, password);
        ActorPassword result = getActorPassword(actor);
        return actorPassword.equals(result);
    }

    /**
     * Set {@linkplain Actor} active state.
     * 
     * @param actor
     *            {@linkplain Actor}, which active state is set.
     * @param isActive
     *            Flag, equals true to set actor active and false, to set actor
     *            inactive.
     */
    public void setStatus(Actor actor, boolean isActive) {
        actor.setActive(isActive);
        getHibernateTemplate().merge(actor);
    }

    /**
     * Update executor.
     * 
     * @param <T>
     *            Updated executor class.
     * @param newExecutor
     *            Updated executor new state.
     * @return Returns updated executor state after update.
     */
    public <T extends Executor> T update(T newExecutor) {
        T oldExecutor = (T) getExecutor(newExecutor.getId());
        if (!Objects.equal(oldExecutor.getName(), newExecutor.getName())) {
            if (isExecutorExist(newExecutor.getName())) {
                throw new ExecutorAlreadyExistsException(newExecutor.getName());
            }
        }
        if (newExecutor instanceof Actor) {
            Actor newActor = (Actor) newExecutor;
            if (!Objects.equal(((Actor) oldExecutor).getCode(), newActor.getCode())) {
                if (isActorExist(newActor.getCode())) {
                    throw new ExecutorAlreadyExistsException(newActor.getCode());
                }
            }
        }
        return getHibernateTemplate().merge(newExecutor);
    }

    /**
     * Clear group, i. e. removes all children's from group.
     * 
     * @param groupId
     *            Clearing group id.
     */
    public void clearGroup(Long groupId) {
        Group group = getGroup(groupId);
        List<ExecutorGroupMembership> list = getGroupMemberships(group);
        getHibernateTemplate().deleteAll(list);
    }

    /**
     * Load all {@linkplain Executor}s according to
     * {@linkplain BatchPresentation}.</br> <b>Paging is not enabled. Really ALL
     * executors is loading.</b>
     * 
     * @param batchPresentation
     *            {@linkplain BatchPresentation} to load executors.
     * @return {@linkplain Executor}s, loaded according to
     *         {@linkplain BatchPresentation}.
     */
    public List<Executor> getAll(BatchPresentation batchPresentation) {
        return getAll(Executor.class, batchPresentation);
    }

    /**
     * Load all {@linkplain Actor}s according to {@linkplain BatchPresentation}
     * .</br> <b>Paging is not enabled. Really ALL actors is loading.</b>
     * 
     * @param batchPresentation
     *            {@linkplain BatchPresentation} to load actors.
     * @return {@linkplain Actor}s, loaded according to
     *         {@linkplain BatchPresentation}.
     */
    public List<Actor> getAllActors(BatchPresentation batchPresentation) {
        return getAll(Actor.class, batchPresentation);
    }

    /**
     * Load all {@linkplain Group}s.</br> <b>Paging is not enabled. Really ALL
     * groups is loading.</b>
     * 
     * @return {@linkplain Group}s.
     */
    public List<Group> getAllGroups() {
        BatchPresentation batchPresentation = BatchPresentationFactory.GROUPS.createNonPaged();
        return getAll(Group.class, batchPresentation);
    }

    /**
     * Add {@linkplain Executor}'s to {@linkplain Group}.
     * 
     * @param executors
     *            {@linkplain Executor}'s, added to {@linkplain Group}.
     * @param group
     *            {@linkplain Group}, to add executors in.
     */
    public void addExecutorsToGroup(Collection<? extends Executor> executors, Group group) {
        for (Executor executor : executors) {
            createMembership(executor, group);
        }
    }

    /**
     * Add {@linkplain Executor} to {@linkplain Group}'s.
     * 
     * @param executors
     *            {@linkplain Executor}, added to {@linkplain Group}'s.
     * @param group
     *            {@linkplain Group}s, to add executors in.
     */
    public void addExecutorToGroups(Executor executor, List<Group> groups) {
        for (Group group : groups) {
            createMembership(executor, group);
        }
    }

    /**
     * Remove {@linkplain Executor}'s from {@linkplain Group}.
     * 
     * @param executors
     *            {@linkplain Executor}'s, removed from {@linkplain Group}.
     * @param group
     *            {@linkplain Group}, to remove executors from.
     */
    public void removeExecutorsFromGroup(List<? extends Executor> executors, Group group) {
        for (Executor executor : executors) {
            removeMembership(executor, group);
        }
    }

    /**
     * Remove {@linkplain Executor} from {@linkplain Group}'s.
     * 
     * @param executors
     *            {@linkplain Executor}, removed from {@linkplain Group}'s.
     * @param group
     *            {@linkplain Group}s, to remove executors from.
     */
    public void removeExecutorFromGroups(Executor executor, List<Group> groups) {
        for (Group group : groups) {
            removeMembership(executor, group);
        }
    }

    /**
     * Returns true if executor belongs to group recursively or false in any
     * other case.</br> For example G1 contains G2, G2 contains A1. In this
     * case:</br> <code>isExecutorInGroup(A1,G2) == true;</code>
     * 
     * @param executor
     *            An executor to check if it in group.
     * @param group
     *            A group to check if it contains executor.
     * @return true if executor belongs to group recursively; false in any other
     *         case.
     */
    public boolean isExecutorInGroup(Executor executor, Group group) {
        return getExecutorParentsAll(executor).contains(group);
    }

    /**
     * Returns group children (first level children, not recursively).</br> For
     * example G1 contains G2, G2 contains A1 and A2. In this case:</br>
     * <code> getGroupChildren(G2) == {A1, A2}</code><br/>
     * <code> getGroupChildren(G1) == {G2} </code>
     * 
     * @param group
     *            A group to load children's from.
     * @param batchPresentation
     *            As {@linkplain BatchPresentation} of array returned.
     * @return Array of group children.
     */
    public Set<Executor> getGroupChildren(Group group) {
        Set<Executor> result = executorCache.getGroupMembers(group);
        if (result != null) {
            return result;
        }
        result = new HashSet<Executor>();
        for (ExecutorGroupMembership relation : getGroupMemberships(group)) {
            result.add(relation.getExecutor());
        }
        return result;
    }

    private List<ExecutorGroupMembership> getGroupMemberships(Group group) {
        return getHibernateTemplate().find("from ExecutorGroupMembership where group=?", group);
    }

    private List<ExecutorGroupMembership> getExecutorMemberships(Executor executor) {
        return getHibernateTemplate().find("from ExecutorGroupMembership where executor=?", executor);
    }

    private ExecutorGroupMembership getMembership(Group group, Executor executor) {
        return findFirstOrNull("from ExecutorGroupMembership where group=? and executor=?", group, executor);
    }

    /**
     * Returns all {@linkplain Actor}s from {@linkplain Group} recursively. All
     * actors from subgroups is also added to result. For example G1 contains G2
     * and A3, G2 contains A1 and A2. In this case:</br>
     * <code> getGroupActors(G2) == {A1, A2}</code><br/>
     * <code> getGroupActors(G1) == {A1, A2, A3} </code>
     * 
     * @param group
     *            {@linkplain Group} to load {@linkplain Actor} children's
     * @return Set of actor children's.
     */
    public Set<Actor> getGroupActors(Group group) {
        Set<Actor> result = executorCache.getGroupActorsAll(group);
        if (result == null) {
            result = getGroupActors(group, new HashSet<Group>());
        }
        return result;
    }

    /**
     * Returns all executor parent {@linkplain Groups}s recursively. For example
     * G1 contains G2 and A3, G2 contains A1 and A2. In this case:</br>
     * <code> getExecutorParentsAll(A1) == {G1, G2}</code><br/>
     * <code> getExecutorParentsAll(A3) == {G1} </code>
     * 
     * @param executor
     *            {@linkplain Executor} to load parent groups.
     * @return Set of executor parents.
     */
    public Set<Group> getExecutorParentsAll(Executor executor) {
        return getExecutorGroupsAll(executor, new HashSet<Executor>());
    }

    /**
     * Returns an array of actors from group (first level children, not
     * recursively).</br> For example G1 contains G2 and A0, G2 contains A1 and
     * A2. In this case: Only actor (non-group) executors are returned.</br>
     * <code> getAllNonGroupExecutorsFromGroup(G2) returns {A1, A2}</code>;
     * <code> getAllNonGroupExecutorsFromGroup(G1) returns {A0} </code>
     * 
     * @param group
     *            {@linkplain Group}, to load actor children's.
     * @return Array of executors from group.
     */
    public List<Executor> getAllNonGroupExecutorsFromGroup(Group group) {
        Set<Executor> childrenSet = getGroupChildren(group);
        List<Executor> retVal = new ArrayList<Executor>();
        for (Executor executor : childrenSet) {
            if (!(executor instanceof Group)) {
                retVal.add(executor);
            }
        }
        return retVal;
    }

    public void remove(Executor executor) {
        getHibernateTemplate().deleteAll(getExecutorMemberships(executor));
        if (executor instanceof Group) {
            getHibernateTemplate().deleteAll(getGroupMemberships((Group) executor));
        } else {
            ActorPassword actorPassword = getActorPassword((Actor) executor);
            if (actorPassword != null) {
                getHibernateTemplate().delete(actorPassword);
            }
        }
        // TODO avoid DuplicateKeyException
        executor = getHibernateTemplate().get(executor.getClass(), executor.getId());
        getHibernateTemplate().delete(executor);
    }

    /**
     * Generates code for actor, if code not set (equals 0). If code is already
     * set, when throws {@linkplain ExecutorAlreadyExistsException} if executor
     * with what code exists in database.
     * 
     * @param actor
     *            Actor to generate code if not set.
     */
    private void checkActorCode(Actor actor) {
        if (actor.getCode() == null) {
            Long nextCode = getHibernateTemplate().execute(new HibernateCallback<Long>() {
                @Override
                public Long doInHibernate(Session session) {
                    Criteria criteria = session.createCriteria(Actor.class);
                    criteria.setMaxResults(1);
                    criteria.addOrder(Order.asc(CODE_PROPERTY_NAME));
                    List<Actor> actors = criteria.list();
                    if (actors.size() > 0) {
                        return new Long(actors.get(0).getCode().longValue() - 1);
                    }
                    return -1L;
                }
            });
            actor.setCode(nextCode);
        }
        if (isActorExist(actor.getCode())) {
            throw new ExecutorAlreadyExistsException(actor.getCode());
        }
    }

    private <T extends Executor> List<T> getAll(Class<T> clazz, BatchPresentation batchPresentation) {
        List<T> retVal = executorCache.getAllExecutor(clazz, batchPresentation);
        if (retVal != null) {
            return retVal;
        }
        int cacheVersion = executorCache.getCacheVersion();
        retVal = new BatchPresentationHibernateCompiler(batchPresentation).getBatch(clazz, false);
        executorCache.addAllExecutor(cacheVersion, clazz, batchPresentation, retVal);
        return retVal;
    }

    private void createMembership(Executor executor, Group group) {
        if (getMembership(group, executor) == null) {
            getHibernateTemplate().save(new ExecutorGroupMembership(group, executor));
        }
    }

    private void removeMembership(Executor executor, Group group) {
        ExecutorGroupMembership membership = getMembership(group, executor);
        if (membership != null) {
            getHibernateTemplate().delete(membership);
        }
    }

    private Set<Actor> getGroupActors(Group group, Set<Group> visited) {
        Set<Actor> result = executorCache.getGroupActorsAll(group);
        if (result != null) {
            return result;
        }
        result = new HashSet<Actor>();
        if (visited.contains(group)) {
            return result;
        }
        visited.add(group);
        for (Executor executor : getGroupChildren(group)) {
            if (executor instanceof Group) {
                result.addAll(getGroupActors((Group) executor, visited));
            } else {
                result.add((Actor) executor);
            }
        }
        return result;
    }

    private Set<Group> getExecutorGroups(Executor executor) {
        Set<Group> result = executorCache.getExecutorParents(executor);
        if (result == null) {
            result = new HashSet<Group>();
            for (ExecutorGroupMembership membership : getExecutorMemberships(executor)) {
                result.add(membership.getGroup());
            }
        }
        return result;
    }

    private Set<Group> getExecutorGroupsAll(Executor executor, Set<Executor> visited) {
        Set<Group> result = executorCache.getExecutorParentsAll(executor);
        if (result == null) {
            result = new HashSet<Group>();
            if (visited.contains(executor)) {
                return result;
            }
            visited.add(executor);
            for (Group group : getExecutorGroups(executor)) {
                result.add(group);
                result.addAll(getExecutorGroupsAll(group, visited));
            }
        }
        return result;
    }

    /**
     * Loads executors by id or code (for {@link Actor}).
     * 
     * @param clazz
     *            Loaded executors class.
     * @param identifiers
     *            Loaded executors identities or codes.
     * @param loadByCodes
     *            Flag, equals true, to loading actors by codes; false to load
     *            executors by identity.
     * @return Loaded executors.
     */
    private <T extends Executor> List<T> getExecutors(final Class<T> clazz, final List<Long> identifiers, boolean loadByCodes) {
        final String propertyName = loadByCodes ? CODE_PROPERTY_NAME : ID_PROPERTY_NAME;
        List<T> executors = getExecutorsFromCache(clazz, identifiers, loadByCodes);
        if (executors != null) {
            return executors;
        }
        List<T> list = getHibernateTemplate().executeFind(new HibernateCallback<List<T>>() {

            @Override
            public List<T> doInHibernate(Session session) {
                Query query = session.createQuery("from " + clazz.getName() + " where " + propertyName + " in (:ids)");
                query.setParameterList("ids", identifiers);
                return query.list();
            }
        });
        HashMap<Long, Executor> idExecutorMap = Maps.newHashMapWithExpectedSize(list.size());
        for (Executor executor : list) {
            idExecutorMap.put(loadByCodes ? ((Actor) executor).getCode() : executor.getId(), executor);
        }
        executors = Lists.newArrayListWithExpectedSize(identifiers.size());
        for (Long id : identifiers) {
            Executor executor = idExecutorMap.get(id);
            if (executor == null) {
                throw new ExecutorDoesNotExistException("with identifier " + id + " for property " + propertyName, clazz);
            }
            executors.add((T) executor);
        }
        return executors;
    }

    /**
     * Loads executors by id or code (for {@link Actor}) from caches.
     * 
     * @param clazz
     *            Loaded executors class.
     * @param identifiers
     *            Loaded executors identities or codes.
     * @param loadByCodes
     *            Flag, equals true, to loading actors by codes; false to load
     *            executors by identity.
     * @return Loaded executors or null, if executors couldn't load from cache.
     */
    private <T extends Executor> List<T> getExecutorsFromCache(Class<T> clazz, List<Long> identifiers, boolean loadByCodes) {
        List<T> executors = Lists.newArrayListWithExpectedSize(identifiers.size());
        for (Long id : identifiers) {
            Preconditions.checkArgument(id != null, "id == null");
            Executor ex = !loadByCodes ? executorCache.getExecutor(id) : executorCache.getActor(id);
            if (ex == null) {
                return null;
            }
            if (!clazz.isAssignableFrom(ex.getClass())) {
                String propertyName = loadByCodes ? CODE_PROPERTY_NAME : ID_PROPERTY_NAME;
                throw new ExecutorDoesNotExistException("with identifier " + id + " for property " + propertyName, clazz);
            }
            executors.add((T) ex);
        }
        return executors;
    }

    private <T extends Executor> T getExecutorById(Class<T> clazz, Long id) {
        Executor executor = executorCache.getExecutor(id);
        if (executor != null) {
            return clazz.isAssignableFrom(executor.getClass()) ? (T) executor : null;
        } else {
            return getHibernateTemplate().get(clazz, id);
        }
    }

    private <T extends Executor> T getExecutorByName(Class<T> clazz, String name) {
        Executor executor = executorCache.getExecutor(name);
        if (executor != null) {
            return (T) (clazz.isAssignableFrom(executor.getClass()) ? executor : null);
        } else {
            return (T) findFirstOrNull("from " + clazz.getName() + " where name=?", name);
        }
    }

    private <T extends Executor> T getExecutor(Class<T> clazz, Long id) {
        return checkExecutorNotNull(getExecutorById(clazz, id), id, clazz);
    }

    private <T extends Executor> T getExecutor(Class<T> clazz, String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new NullPointerException("Executor name must be specified");
        }
        return checkExecutorNotNull(getExecutorByName(clazz, name), name, clazz);
    }

    private ActorPassword getActorPassword(Actor actor) {
        return findFirstOrNull("from ActorPassword where actorId=?", actor.getId());
    }

    private Actor getActorByCodeInternal(Long code) {
        Actor actor = executorCache.getActor(code);
        if (actor != null) {
            return actor;
        }
        return findFirstOrNull("from Actor where code=?", code);
    }

    private <T extends Executor> T checkExecutorNotNull(T executor, Long id, Class<T> clazz) {
        if (executor == null) {
            throw new ExecutorDoesNotExistException(id, clazz);
        }
        return executor;
    }

    private <T extends Executor> T checkExecutorNotNull(T executor, String name, Class<T> clazz) {
        if (executor == null) {
            throw new ExecutorDoesNotExistException(name, clazz);
        }
        return executor;
    }
}
