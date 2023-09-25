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
package com.xwiki.admintools.internal.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

/**
 * Collection of file operations.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = DefaultFileOperations.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultFileOperations
{
    private Scanner scanner;

    private File file;

    /**
     * Check if the searched file exists.
     *
     * @return {@code true} if the file exists, {@code false} otherwise.
     */
    public boolean fileExists()
    {
        return file.exists();
    }

    /**
     * Initialize the file.
     *
     * @param path to the file.
     */
    public void openFile(String path)
    {
        file = new File(path);
    }

    /**
     * Initialize the scanner for the needed file.
     *
     * @throws FileNotFoundException
     */
    public void initializeScanner() throws FileNotFoundException
    {
        scanner = new Scanner(file);
    }

    /**
     * Get the next line from the file.
     *
     * @return next line from the file.
     */
    public String nextLine()
    {
        return scanner.nextLine();
    }

    /**
     * Verify if there is a line left in the file.
     *
     * @return {@code true} if there is at least one line left, {@code false} otherwise.
     */
    public boolean hasNextLine()
    {
        return scanner.hasNextLine();
    }

    /**
     * Close the opened scanner.
     */
    public void closeScanner()
    {
        scanner.close();
    }
}
