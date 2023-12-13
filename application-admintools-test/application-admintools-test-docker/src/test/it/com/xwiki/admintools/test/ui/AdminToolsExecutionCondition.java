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
package com.xwiki.admintools.test.ui;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;

/**
 * Prevent the execution of the PDF export tests from inside a Docker container (such as {@code xwiki-build}) when
 * {@link ServletEngine#JETTY_STANDALONE} is used.
 *
 * @version $Id$
 * @since 1.0
 */
public class AdminToolsExecutionCondition implements ExecutionCondition
{
    private final List<ServletEngine> supportedServers = List.of(ServletEngine.TOMCAT);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        if (DockerTestUtils.isInAContainer()) {
            List<ServletEngine> filteredServletEngines =
                Stream.of(ServletEngine.values()).filter(engine -> !supportedServers.contains(engine))
                    .collect(Collectors.toList());
            TestConfiguration configuration = DockerTestUtils.getTestConfiguration(context);
            for (ServletEngine servletEngine : filteredServletEngines) {
                if (servletEngine.equals(configuration.getServletEngine())) {
                    return ConditionEvaluationResult.disabled(String.format("Servlet engine [%s] is forbidden "
                            + "when the PDF export tests are executed inside a Docker container, skipping.",
                        configuration.getServletEngine()));
                }
            }
        }

        return ConditionEvaluationResult.enabled("The configured servlet engine is supported, continuing.");
    }
}