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
package ru.runa.wfe.relation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ru.runa.wfe.commons.hibernate.Proxies;
import ru.runa.wfe.security.IdentifiableBase;
import ru.runa.wfe.security.SecuredObjectType;
import ru.runa.wfe.user.Executor;

/**
 * Describes relation between two executor. If relation contains pair of
 * executors {left, right}, then left=@relation(right)
 */
@Entity
@Table(name = "EXECUTOR_RELATION_PAIR")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class RelationPair extends IdentifiableBase {
    private static final long serialVersionUID = 1L;

    /**
     * Identity of relation pair. This field is set then relation pair is stored
     * in database.
     */
    private Long id;

    /**
     * Left part of relation (Boss, and so on). If relation contains pair of
     * executors {left, right}, then left=@relation(right)
     */
    private Executor left;

    /**
     * Right part of relation (Employer, and so on). If relation contains pair
     * of executors {left, right}, then left=@relation(right)
     */
    private Executor right;

    /**
     * Relation to which belongs this executors pair.
     */
    private Relation relation;

    protected RelationPair() {
    }

    /**
     * Create relation pair instance for relation {@link #relation} and executor
     * {@link #left} as left, and {@link #right} as right part of relation.
     * 
     * @param relation
     *            Relation, which belongs this pair.
     * @param left
     *            Left part of relation pair.
     * @param right
     *            Right part of relation pair.
     */
    public RelationPair(Relation relation, Executor left, Executor right) {
        this.relation = relation;
        this.left = left;
        this.right = right;
    }

    @Transient
    @Override
    public SecuredObjectType getSecuredObjectType() {
        return SecuredObjectType.RELATIONPAIR;
    }

    @Override
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    @SequenceGenerator(name = "sequence", sequenceName = "SEQ_EXECUTOR_RELATION")
    @Column(name = "ID", nullable = false)
    public Long getId() {
        return id;
    }

    // This is need by hibernate.
    protected void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns left part of relation pair: {@link #left}.
     * 
     * @return Left part of relation pair.
     */
    @ManyToOne(targetEntity = Executor.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "EXECUTOR_FROM", nullable = false, insertable = true, updatable = false)
    @ForeignKey(name = "FK_ERP_EXECUTOR_FROM")
    @Index(name = "IX_ERP_EXECUTOR_FROM")
    public Executor getLeft() {
        return Proxies.getImplementation(left);
    }

    // This is need by hibernate.
    protected void setLeft(Executor relationFrom) {
        left = relationFrom;
    }

    /**
     * Returns right part of relation pair: {@link #right}.
     * 
     * @return Right part of relation pair.
     */
    @ManyToOne(targetEntity = Executor.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "EXECUTOR_TO", nullable = false, insertable = true, updatable = false)
    @ForeignKey(name = "FK_ERP_EXECUTOR_TO")
    @Index(name = "IX_ERP_EXECUTOR_TO")
    public Executor getRight() {
        return Proxies.getImplementation(right);
    }

    // This is need by hibernate.
    protected void setRight(Executor relationTo) {
        right = relationTo;
    }

    /**
     * Return relation, to which belongs this executors pair: {@link #relation}.
     * 
     * @return Relation, to which belongs this executors pair.
     */
    @ManyToOne(targetEntity = Relation.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "RELATION_ID", nullable = false, insertable = true, updatable = false)
    @ForeignKey(name = "FK_ERP_RELATION")
    @Index(name = "IX_ERP_RELATION")
    public Relation getRelation() {
        return relation;
    }

    // This is need by hibernate.
    protected void setRelation(Relation relationsGroup) {
        relation = relationsGroup;
    }
}
