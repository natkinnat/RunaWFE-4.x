/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package ru.runa.wfe.var;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.audit.VariableCreateLog;
import ru.runa.wfe.audit.VariableDeleteLog;
import ru.runa.wfe.audit.VariableUpdateLog;
import ru.runa.wfe.commons.SystemProperties;
import ru.runa.wfe.execution.ExecutionContext;
import ru.runa.wfe.execution.Process;

import com.google.common.base.Objects;

/**
 * Base class for classes that store variable values in the database.
 */
@Entity
@Table(name = "BPM_VARIABLE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DISCRIMINATOR", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue(value = "V")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class Variable<T extends Object> {
    public static final int MAX_STRING_SIZE = 1024;
    protected Long id;
    private Long version;
    private String name;
    private Process process;
    private Converter converter;
    private String stringValue;

    public Variable() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    @SequenceGenerator(name = "sequence", sequenceName = "SEQ_BPM_VARIABLE")
    @Column(name = "ID")
    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    @Version
    @Column(name = "VERSION")
    protected Long getVersion() {
        return version;
    }

    protected void setVersion(Long version) {
        this.version = version;
    }

    @Column(name = "NAME")
    // causes VERY long time to load processes with variable filter
    // @Index(name = "IX_VARIABLE_NAME")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "CONVERTER")
    @Type(type = "ru.runa.wfe.commons.hibernate.ConverterEnumType")
    public Converter getConverter() {
        return converter;
    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    @ManyToOne(targetEntity = Process.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "PROCESS_ID", nullable = false)
    @ForeignKey(name = "FK_VARIABLE_PROCESS")
    @Index(name = "IX_VARIABLE_PROCESS")
    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    // TODO may be add index on this field
    @Column(name = "STRINGVALUE", length = MAX_STRING_SIZE)
    // @Index(name = "IX_VARIABLE_VALUE")
    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Get the value of the variable.
     */
    @Transient
    public abstract T getStorableValue();

    /**
     * Set new variable value
     */
    protected abstract void setStorableValue(T object);

    private void addLog(ExecutionContext executionContext, Object oldValue, Object newValue) {
        if (oldValue == null) {
            executionContext.addLog(new VariableCreateLog(this, newValue));
        } else if (newValue == null) {
            executionContext.addLog(new VariableDeleteLog(this));
        } else {
            executionContext.addLog(new VariableUpdateLog(this, oldValue, newValue));
        }
    }

    public boolean supports(Object value) {
        if (value == null) {
            return true;
        }
        return converter == null || converter.supports(value);
    }

    public void setValue(ExecutionContext executionContext, Object newValue) {
        Object newStorableValue;
        if (converter != null && newValue != null) {
            if (!converter.supports(newValue)) {
                throw new InternalApplicationException("the converter '" + converter.getClass().getName() + "' in variable '"
                        + this.getClass().getName() + "' does not support values of type '" + newValue.getClass().getName() + "'.");
            }
            newStorableValue = converter.convert(this, newValue);
        } else {
            newStorableValue = newValue;
        }
        Object oldValue = getStorableValue();
        setStringValue(newValue != null ? toString(newValue) : null);
        if (converter != null && oldValue != null) {
            oldValue = converter.revert(oldValue);
        }
        setStorableValue((T) newStorableValue);
        addLog(executionContext, oldValue, newValue);
    }

    @Transient
    public Object getValue() {
        Object value = getStorableValue();
        if (value != null && converter != null) {
            value = converter.revert(value);
        }
        return value;
    }

    public String toString(Object value) {
        String string;
        if (SystemProperties.isV3CompatibilityMode() && value != null && String[].class == value.getClass()) {
            string = Arrays.toString((String[]) value);
        } else {
            string = String.valueOf(value);
        }
        if (string.length() > MAX_STRING_SIZE) {
            string = string.substring(0, MAX_STRING_SIZE);
        }
        return string;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("id", getId()).add("name", getName()).add("value", toString(getValue())).toString();
    }

}
