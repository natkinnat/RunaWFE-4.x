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
package ru.runa.wfe.user.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ru.runa.wfe.commons.ApplicationContextFactory;
import ru.runa.wfe.commons.cache.BaseCacheImpl;
import ru.runa.wfe.commons.cache.Cache;
import ru.runa.wfe.presentation.BatchPresentation;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.user.ExecutorGroupMembership;
import ru.runa.wfe.user.Group;

/**
 * Cache for executors.
 * 
 * @author Konstantinov Aleksey
 */
public class ExecutorCacheImpl extends BaseCacheImpl implements ExecutorCache {

    /* Ehcaches names. */
    public static final String actorsByCodesName = "ru.runa.wfe.wfe.af.caches.actorsByCodes";
    public static final String executorsByIdName = "ru.runa.wfe.wfe.af.caches.executorsById";
    public static final String executorsByNameName = "ru.runa.wfe.wfe.af.caches.executorsByName";
    public static final String groupMembersName = "ru.runa.wfe.wfe.af.caches.groupMembers";
    public static final String executorParentsName = "ru.runa.wfe.wfe.af.caches.executorParents";
    public static final String allGroupActorsName = "ru.runa.wfe.wfe.af.caches.allGroupActors";
    public static final String allExecutorGroupsName = "ru.runa.wfe.wfe.af.caches.allExecutorGroups";
    public static final String allExecutorsListsName = "ru.runa.wfe.wfe.af.caches.allExecutorsLists";

    /* Caches implementation. */
    private final Cache<Long, Actor> codeToActorCache;
    private final Cache<Long, Executor> idToExecutorCache;
    private final Cache<String, Executor> nameToExecutorCache;
    private final Cache<Long, HashSet<Executor>> groupToMembersCache;
    private final Cache<Long, HashSet<Group>> executorToParentGroupsCache;
    private final Cache<Long, HashSet<Actor>> groupToAllActorMembersCache;
    private final Cache<Long, HashSet<Group>> executorToAllParentGroupsCache;
    private final Cache<Class<?>, ConcurrentHashMap<BatchPresentationFieldEquals, List<Executor>>> batchAllExecutors;

    public ExecutorCacheImpl() {
        codeToActorCache = createCache(actorsByCodesName);
        idToExecutorCache = createCache(executorsByIdName);
        nameToExecutorCache = createCache(executorsByNameName);
        groupToMembersCache = createCache(groupMembersName);
        executorToParentGroupsCache = createCache(executorParentsName);
        groupToAllActorMembersCache = createCache(allGroupActorsName);
        executorToAllParentGroupsCache = createCache(allExecutorGroupsName);
        batchAllExecutors = createCache(allExecutorsListsName);
        List<Executor> allExecutors = getAllExecutors();
        List<ExecutorGroupMembership> relationsList = getAllRelations();
        for (Executor executor : allExecutors) {
            addExecutorToCaches(executor);
        }
        fillRelationsCaches(relationsList, allExecutors);
        return;
    }

    @Override
    public Actor getActor(Long code) {
        return codeToActorCache.get(code);
    }

    @Override
    public Executor getExecutor(String name) {
        return nameToExecutorCache.get(name);
    }

    @Override
    public Executor getExecutor(Long id) {
        return idToExecutorCache.get(id);
    }

    @Override
    public Set<Executor> getGroupMembers(Group group) {
        return groupToMembersCache.get(group.getId());
    }

    @Override
    public Set<Actor> getGroupActorsAll(Group group) {
        return groupToAllActorMembersCache.get(group.getId());
    }

    @Override
    public Set<Group> getExecutorParents(Executor executor) {
        return executorToParentGroupsCache.get(executor.getId());
    }

    @Override
    public Set<Group> getExecutorParentsAll(Executor executor) {
        return executorToAllParentGroupsCache.get(executor.getId());
    }

    @Override
    public <T extends Executor> List<T> getAllExecutor(Class<T> clazz, BatchPresentation batch) {
        synchronized (this) {
            ConcurrentHashMap<BatchPresentationFieldEquals, List<Executor>> map = batchAllExecutors.get(clazz);
            if (map == null) {
                return null;
            }
            List<Executor> cachedExecutors = map.get(new BatchPresentationFieldEquals(batch));
            if (cachedExecutors == null) {
                return null;
            }
            return (List<T>) Collections.unmodifiableList(cachedExecutors);
        }
    }

    @Override
    public void addAllExecutor(int cacheVersion, Class<?> clazz, BatchPresentation batch, List<? extends Executor> executors) {
        if (cacheVersion != currentCacheVersion) {
            return;
        }
        synchronized (this) {
            ConcurrentHashMap<BatchPresentationFieldEquals, List<Executor>> map = batchAllExecutors.get(clazz);
            if (map == null) {
                map = new ConcurrentHashMap<BatchPresentationFieldEquals, List<Executor>>();
            }
            List<Executor> result = new ArrayList<Executor>();
            for (Executor executor : executors) {
                result.add(executor);
            }
            map.put(new BatchPresentationFieldEquals(batch), result);
            batchAllExecutors.put(clazz, map);
        }
    }

    public <T extends Executor> boolean onExecutorChange(String executorName, Class<T> executorClass, boolean new_delete) {
        Executor executor = nameToExecutorCache.get(executorName);
        if (executor == null) {
            return true;
        }
        nameToExecutorCache.remove(executor.getName());
        idToExecutorCache.remove(executor.getId());
        if (executor instanceof Actor) {
            codeToActorCache.remove(((Actor) executor).getCode());
        }
        batchAllExecutors.clear();
        Set<Group> upperGroups = executorToAllParentGroupsCache.get(executor.getId());
        if (upperGroups != null) {
            for (Group upperGroup : upperGroups) {
                groupToMembersCache.remove(upperGroup.getId());
                groupToAllActorMembersCache.remove(upperGroup.getId());
            }
        }
        return clearGroupMembersCaches(executor);
    }

    private boolean clearGroupMembersCaches(Executor executor) {
        boolean result = true;
        result = result && executorToAllParentGroupsCache.remove(executor.getId());
        result = result && executorToParentGroupsCache.remove(executor.getId());
        if (executor instanceof Group) {
            Set<Executor> executors = groupToMembersCache.get(executor.getId());
            if (executors != null) {
                for (Executor ex : executors) {
                    result = result && clearGroupMembersCaches(ex);
                }
            }
        }
        return result;
    }

    public boolean onGroupMembersChange(Group group) {
        boolean result = true;
        groupToMembersCache.remove(group.getId());
        groupToAllActorMembersCache.remove(group.getId());
        batchAllExecutors.clear();
        Set<Group> upperGroups = executorToAllParentGroupsCache.get(group.getId());
        if (upperGroups != null) {
            for (Group upperGroup : upperGroups) {
                groupToMembersCache.remove(upperGroup.getId());
                groupToAllActorMembersCache.remove(upperGroup.getId());
            }
        }
        return result;
    }

    public boolean onExecutorInGroupChange(Executor executor) {
        batchAllExecutors.clear();
        return clearGroupMembersCaches(executor);
    }

    private void addExecutorToCaches(Executor executor) {
        if (executor instanceof Actor) {
            idToExecutorCache.put(executor.getId(), executor);
            nameToExecutorCache.put(executor.getName(), executor);
            codeToActorCache.put(((Actor) executor).getCode(), (Actor) executor);
        } else {
            idToExecutorCache.put(executor.getId(), executor);
            nameToExecutorCache.put(executor.getName(), executor);
        }
    }

    private <Key extends Serializable, ValueInSet> Set<ValueInSet> getCollectionFromMap(Cache<Key, HashSet<ValueInSet>> map, Key key) {
        HashSet<ValueInSet> retVal = map.get(key);
        if (retVal == null) {
            retVal = new HashSet<ValueInSet>();
            map.put(key, retVal);
        }
        return retVal;
    }

    private void fillMapGroupToMembers(List<ExecutorGroupMembership> relationsList) {
        for (ExecutorGroupMembership relation : relationsList) {
            Executor ex = relation.getExecutor();
            Group gr = relation.getGroup();
            getCollectionFromMap(groupToMembersCache, gr.getId()).add(nameToExecutorCache.get(ex.getName()));
        }
    }

    private void fillMapExecutorToParents(List<ExecutorGroupMembership> relationsList) {
        for (ExecutorGroupMembership relation : relationsList) {
            Executor ex = relation.getExecutor();
            Group gr = relation.getGroup();
            getCollectionFromMap(executorToParentGroupsCache, ex.getId()).add((Group) (nameToExecutorCache.get(gr.getName())));
        }
    }

    private void fillRelationsCaches(List<ExecutorGroupMembership> relationsList, List<Executor> executors) {
        fillMapGroupToMembers(relationsList);
        fillMapExecutorToParents(relationsList);
        for (Executor executor : executors) {
            if (executorToParentGroupsCache.get(executor.getId()) == null) {
                executorToParentGroupsCache.put(executor.getId(), new HashSet<Group>());
            }
            if (executor instanceof Group && groupToMembersCache.get(executor.getId()) == null) {
                groupToMembersCache.put(executor.getId(), new HashSet<Executor>());
            }
        }

        for (Executor executor : executors) {
            fillAllParentsCache(executorToAllParentGroupsCache, nameToExecutorCache.get(executor.getName()), executorToParentGroupsCache);
            if (executor instanceof Group) {
                fillActorMembersCache(groupToAllActorMembersCache, (Group) (nameToExecutorCache.get(executor.getName())), groupToMembersCache);
            }
        }
    }

    private Set<Group> fillAllParentsCache(Cache<Long, HashSet<Group>> cache, Executor executor, Cache<Long, HashSet<Group>> mapExecutorToParents) {
        HashSet<Group> executorGroups = cache.get(executor.getId());
        if (executorGroups != null) {
            return executorGroups;
        }
        executorGroups = new HashSet<Group>();
        cache.put(executor.getId(), executorGroups);
        if (mapExecutorToParents.get(executor.getId()) != null) {
            for (Group group : mapExecutorToParents.get(executor.getId())) {
                executorGroups.add(group);
                executorGroups.addAll(fillAllParentsCache(cache, group, mapExecutorToParents));
            }
        }
        return executorGroups;
    }

    private Set<Actor> fillActorMembersCache(Cache<Long, HashSet<Actor>> cache, Group group, Cache<Long, HashSet<Executor>> mapGroupToMembers) {
        HashSet<Actor> actorMembers = cache.get(group.getId());
        if (actorMembers != null) {
            return actorMembers;
        }
        actorMembers = new HashSet<Actor>();
        cache.put(group.getId(), actorMembers);
        if (mapGroupToMembers.get(group.getId()) != null) {
            for (Executor ex : mapGroupToMembers.get(group.getId())) {
                if (ex instanceof Actor) {
                    actorMembers.add((Actor) ex);
                } else {
                    actorMembers.addAll(fillActorMembersCache(cache, (Group) ex, mapGroupToMembers));
                }
            }
        }
        return actorMembers;
    }

    private <T> List<T> getAll(Class<?> clazz) {
        Session session = ApplicationContextFactory.getCurrentSession();
        Criteria criteria = session.createCriteria(clazz);
        return criteria.list();
    }

    private List<ExecutorGroupMembership> getAllRelations() {
        return getAll(ExecutorGroupMembership.class);
    }

    private List<Executor> getAllExecutors() {
        return getAll(Executor.class);
    }

    private class BatchPresentationFieldEquals {
        private final BatchPresentation batchPresentation;

        BatchPresentationFieldEquals(BatchPresentation batchPresentation) {
            this.batchPresentation = batchPresentation.clone();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ExecutorCacheImpl.BatchPresentationFieldEquals) {
                return batchPresentation.fieldEquals(((ExecutorCacheImpl.BatchPresentationFieldEquals) obj).batchPresentation);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return batchPresentation.hashCode();
        }
    }
}
