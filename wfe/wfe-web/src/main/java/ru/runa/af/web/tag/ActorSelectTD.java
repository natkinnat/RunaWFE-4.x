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
package ru.runa.af.web.tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.ecs.html.Option;
import org.apache.ecs.html.Select;
import org.apache.ecs.html.TD;

import ru.runa.service.ExecutorService;
import ru.runa.service.delegate.Delegates;
import ru.runa.wfe.presentation.BatchPresentationFactory;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.user.User;

public class ActorSelectTD extends TD {
    private static final long serialVersionUID = 1L;

    public ActorSelectTD(User user, String name) {
        this(user, name, (String) null, true);
    }

    public ActorSelectTD(User user, String name, String current) {
        this(user, name, current, true);
    }

    public ActorSelectTD(String name, String current, Collection<Executor> executors) {
        Select select = new Select();
        select.setName(name);
        ArrayList<Option> options = new ArrayList<Option>();
        for (Executor executor : executors) {
            Option option = new Option();
            option.setValue(executor.getName());
            option.addElement(executor.getName());
            if (executor.getName().equals(current)) {
                option.setSelected(true);
            }
            options.add(option);
        }
        Option[] opts = options.toArray(new Option[0]);
        Comparator<Option> comp = new Comparator<Option>() {
            @Override
            public int compare(Option o1, Option o2) {
                return o1.getValue().compareToIgnoreCase(o2.getValue());
            }
        };
        Arrays.sort(opts, comp);
        select.addElement(opts);
        super.addElement(select);
    }

    public ActorSelectTD(User user, String name, String current, boolean actorOnly) {
        Select select = new Select();
        select.setName(name);
        boolean exist = false;
        ExecutorService executorService = Delegates.getExecutorService();
        List<Executor> executors = executorService.getAll(user, BatchPresentationFactory.EXECUTORS.createNonPaged());
        ArrayList<Option> options = new ArrayList<Option>();
        for (Executor executor : executors) {
            if (!(executor instanceof Actor) && actorOnly) {
                continue;
            }
            Option option = new Option();
            option.setValue(executor.getName());
            option.addElement(executor.getName());
            if (executor.getName().equals(current)) {
                option.setSelected(true);
                exist = true;
            }
            options.add(option);
        }
        if (!exist && current != null) {
            Option option = new Option();
            option.setValue(current);
            option.addElement(current);
            option.setSelected(true);
            options.add(option);
        }
        Option[] opts = options.toArray(new Option[0]);
        Comparator<Option> comp = new Comparator<Option>() {
            @Override
            public int compare(Option o1, Option o2) {
                return o1.getValue().compareToIgnoreCase(o2.getValue());
            }
        };
        Arrays.sort(opts, comp);
        select.addElement(opts);
        super.addElement(select);
    }
}
