package ru.runa.wfe.user;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Used for dynamic assignment multiple executors in swimlanes.
 * 
 * @author Dofs
 */
@Entity
@DiscriminatorValue(value = "T")
public class TemporaryGroup extends Group {
    private static final long serialVersionUID = 1L;
    /**
     * Prefix for temporary group name.
     */
    public static final String GROUP_PREFIX = "__TmpGroup_";

    public TemporaryGroup() {
    }

    public static TemporaryGroup create(String nameSuffix, String description) {
        TemporaryGroup temporaryGroup = new TemporaryGroup();
        temporaryGroup.setName(GROUP_PREFIX + nameSuffix);
        temporaryGroup.setDescription(description);
        return temporaryGroup;
    }

    public static TemporaryGroup create(Long processId, String swimlaneName) {
        String nameSuffix = processId + "_" + swimlaneName;
        String description = processId.toString();
        return create(nameSuffix, description);
    }

}
