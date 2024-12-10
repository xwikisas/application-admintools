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

import java.util.List;
import java.util.Objects;

import org.xwiki.stability.Unstable;

@Unstable
public class RightsResult
{
    private String type;

    private String space = "";

    private String docName;

    private String level;

    private String entity;

    private String policy;

    public RightsResult()
    {

    }

    public RightsResult(String type)
    {
        this.type = type;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public String getDocName()
    {
        return docName;
    }

    public void setDocName(String docName)
    {
        this.docName = docName;
    }

    public String getEntity()
    {
        return entity;
    }

    public void setEntity(String entity)
    {
        this.entity = entity;
    }

    public String getPolicy()
    {
        return policy;
    }

    public void setPolicy(String policy)
    {
        this.policy = policy;
    }

    public String getSpace()
    {
        return space;
    }

    public void setSpace(String space)
    {
        this.space = space;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RightsResult that = (RightsResult) obj;
        return Objects.equals(entity, that.entity) &&
            Objects.equals(level, that.level) &&
            Objects.equals(policy, that.policy) &&
            Objects.equals(space, that.space) &&
            Objects.equals(docName, that.docName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, level, policy, space, docName);
    }
}
