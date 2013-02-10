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
package ru.runa.wfe.definition.dto;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ru.runa.wfe.definition.Deployment;
import ru.runa.wfe.definition.IFileDataProvider;
import ru.runa.wfe.lang.ProcessDefinition;
import ru.runa.wfe.security.Identifiable;
import ru.runa.wfe.security.SecuredObjectType;

import com.google.common.base.Objects;

/**
 * Created on 29.09.2004
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WfDefinition", namespace = "http://runa.ru/workflow/webservices")
public class WfDefinition implements Identifiable {
    private static final long serialVersionUID = -6032491529439317948L;

    @XmlElement(namespace = "http://runa.ru/workflow/webservices")
    private Long id;
    @XmlElement(namespace = "http://runa.ru/workflow/webservices")
    private String name;
    @XmlElement(namespace = "http://runa.ru/workflow/webservices")
    private String description;
    @XmlElement(namespace = "http://runa.ru/workflow/webservices")
    private String[] categories;
    @XmlElement(namespace = "http://runa.ru/workflow/webservices")
    private Long version;
    @XmlElement(namespace = "http://runa.ru/workflow/webservices")
    private boolean hasHtmlDescription;
    @XmlElement(namespace = "http://runa.ru/workflow/webservices")
    private boolean hasStartImage;
    @XmlElement(namespace = "http://runa.ru/workflow/webservices")
    private boolean hasDisabledImage;
    @XmlElement(namespace = "http://runa.ru/workflow/webservices")
    private Date deployedDate;

    public WfDefinition() {
    }

    public WfDefinition(ProcessDefinition definition) {
        this(definition.getDeployment());
        hasHtmlDescription = definition.getFileData(IFileDataProvider.INDEX_FILE_NAME) != null;
        hasStartImage = definition.getFileData(IFileDataProvider.START_IMAGE_FILE_NAME) != null;
        hasDisabledImage = definition.getFileData(IFileDataProvider.START_DISABLED_IMAGE_FILE_NAME) != null;
    }

    public WfDefinition(Deployment deployment) {
        id = deployment.getId();
        version = deployment.getVersion();
        name = deployment.getName();
        description = deployment.getDescription();
        categories = deployment.getCategories();
        deployedDate = deployment.getDeployedDate();
    }

    @Override
    public Long getIdentifiableId() {
        return new Long(getName().hashCode());
    }

    @Override
    public SecuredObjectType getSecuredObjectType() {
        return SecuredObjectType.DEFINITION;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getVersion() {
        return version;
    }

    public String[] getCategories() {
        return categories;
    }

    public boolean hasHtmlDescription() {
        return hasHtmlDescription;
    }

    public boolean hasStartImage() {
        return hasStartImage;
    }

    public boolean hasDisabledImage() {
        return hasDisabledImage;
    }

    public Date getDeployedDate() {
        return deployedDate;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WfDefinition) {
            return Objects.equal(id, ((WfDefinition) obj).id);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("id", id).add("name", name).add("version", version).toString();
    }
}
