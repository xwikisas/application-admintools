/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
package com.xwiki.admintools.security;

import java.util.Objects;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Stores needed information from the groups and users rights processing.
 *
 * @version $Id$
 * @since 1.2
 */
@Unstable
public class RightsResult
{
    private String type;

    private String space = "";

    private DocumentReference docReference;

    private String level;

    private String entity;

    private String policy;

    /**
     * Default constructor.
     */
    public RightsResult()
    {

    }

    /**
     * Constructor with type parameter.
     *
     * @param type the type of the stored rights info.
     */
    public RightsResult(String type)
    {
        this.type = type;
    }

    /**
     * Get the stored level of security.
     *
     * @return the level of security.
     */
    public String getLevel()
    {
        return level;
    }

    /**
     * See {@link #getLevel()}.
     *
     * @param level the level of security.
     */
    public void setLevel(String level)
    {
        this.level = level;
    }

    /**
     * Retrieves the reference of the document where rights are stored.
     *
     * @return the document reference containing the rights.
     */
    public DocumentReference getDocReference()
    {
        return docReference;
    }

    /**
     * See {@link #getDocReference()}.
     *
     * @param docReference the document reference containing the rights.
     */
    public void setDocReference(DocumentReference docReference)
    {
        this.docReference = docReference;
    }

    /**
     * Retrieves the entity for which rights are checked.
     *
     * @return the entity being checked for rights.
     */
    public String getEntity()
    {
        return entity;
    }

    /**
     * See {@link #getEntity()}.
     *
     * @param entity the entity to check rights for.
     */
    public void setEntity(String entity)
    {
        this.entity = entity;
    }

    /**
     * Get the rights level policy. Used values are Allowed and Denied.
     *
     * @return the rights level policy.
     */
    public String getPolicy()
    {
        return policy;
    }

    /**
     * See {@link #getPolicy()}.
     *
     * @param policy the rights level policy.
     */
    public void setPolicy(String policy)
    {
        this.policy = policy;
    }

    /**
     * Get the space where the document is located.
     *
     * @return the space where the document is located.
     */
    public String getSpace()
    {
        return space;
    }

    /**
     * See {@link #getSpace()}.
     *
     * @param space the space where the document is located.
     */
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * Get the type of rights. Used values are Global, Space and Page.
     *
     * @return the type of rights.
     */
    public String getType()
    {
        return type;
    }

    /**
     * See {@link #getType()}.
     *
     * @param type the type of rights.
     */
    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RightsResult that = (RightsResult) obj;
        return Objects.equals(entity, that.entity) && Objects.equals(level, that.level) && Objects.equals(policy,
            that.policy) && Objects.equals(space, that.space) && docReference.equals(that.docReference);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(entity, level, policy, space, docReference);
    }
}
