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
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import ru.runa.wfe.definition.Deployment;
import ru.runa.wfe.definition.IFileDataProvider;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;
import ru.runa.wfe.lang.ProcessDefinition;
import ru.runa.wfe.lang.SubprocessDefinition;
import ru.runa.wfe.security.Identifiable;
import ru.runa.wfe.security.SecuredObjectType;

import com.google.common.base.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
public class WfDefinition extends Identifiable implements Comparable<WfDefinition> {
    private static final long serialVersionUID = -6032491529439317948L;

    private ProcessDefinition definition = null;
    private Long id;
    private String name;
    private String description;
    private String[] categories;
    private Long version;
    private boolean hasHtmlDescription;
    private boolean hasStartImage;
    private boolean hasDisabledImage;
    private Date deployedDate;
    private boolean subprocessOnly;
    private boolean canBeStarted;

    public WfDefinition() {
    }

    public WfDefinition(ProcessDefinition definition, boolean canBeStarted) {
        this(definition.getDeployment());
        this.definition = definition;
        hasHtmlDescription = definition.getFileData(IFileDataProvider.INDEX_FILE_NAME) != null;
        hasStartImage = definition.getFileData(IFileDataProvider.START_IMAGE_FILE_NAME) != null;
        hasDisabledImage = definition.getFileData(IFileDataProvider.START_DISABLED_IMAGE_FILE_NAME) != null;
        subprocessOnly = definition.getAccessType() == ProcessDefinitionAccessType.OnlySubprocess;
        this.canBeStarted = canBeStarted && !subprocessOnly;
    }

    public WfDefinition(Deployment deployment) {
        id = deployment.getId();
        version = deployment.getVersion();
        name = deployment.getName();
        description = deployment.getDescription();
        categories = deployment.getCategories();
        deployedDate = deployment.getCreateDate();
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

    public boolean isSubprocessOnly() {
        return subprocessOnly;
    }

    public boolean isCanBeStarted() {
        return canBeStarted;
    }

    public void setCanBeStarted(boolean canBeStarted) {
        this.canBeStarted = canBeStarted;
    }

    public Date getDeployedDate() {
        return deployedDate;
    }

    @Override
    public int compareTo(WfDefinition o) {
        if (name != null && name.equals(o.name)) {
            return 0;
        }
        if (o.definition != null && isSubprocessOf(o.definition.getEmbeddedSubprocesses())) return 1;
        return -1;
    }
    
    private boolean isSubprocessOf(Map<String, SubprocessDefinition> supprocesses) {
        boolean result = false;
        for (Map.Entry<String, SubprocessDefinition> entry : supprocesses.entrySet()) {
            if (this.equals(entry.getValue())) {
                result = true;
                break;
            }
            if (!isSubprocessOf(entry.getValue().getEmbeddedSubprocesses()))
                continue;
            result = true;
            break;
        }
        return result;
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
