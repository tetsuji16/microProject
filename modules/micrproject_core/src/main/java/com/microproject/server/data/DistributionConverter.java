/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.server.data;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;

/**
 *
 */
public class DistributionConverter {
	private static final Logger logger = Logger.getLogger(DistributionConverter.class.getName());
	protected Object delegate;
	protected Class delegateClass;
	public DistributionConverter(){
		if (!Environment.getStandAlone()){
			String className=null;
			try {
				className=Messages.getMetaString("DistributionConverter");
			} catch (Exception e1) {
			}
			if (className!=null){
				try {
					delegateClass = Class.forName(className);
					delegate = delegateClass.getDeclaredConstructor().newInstance();
				} catch (ReflectiveOperationException e) {
					logger.log(Level.WARNING, "Failed to create distribution converter delegate", e);
				}
			}
		}
	}
	public List createDistributionData(Project project,boolean incremental){
		if (delegate == null)
			return Collections.emptyList();
		if (delegate!=null){
			try {
				return (List)delegateClass.getMethod("createDistributionData", new Class[]{Project.class,boolean.class}).invoke(delegate,new Object[]{project,incremental});
			} catch (IllegalArgumentException | SecurityException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new IllegalStateException("Failed to create distribution data", e);
			}
		}
		return Collections.emptyList();
	}
	public void substractDistributionFromProject(Project project){
		if (delegate!=null){
			try {
				delegateClass.getMethod("substractDistributionFromProject", new Class[]{Project.class}).invoke(delegate,new Object[]{project});
			} catch (IllegalArgumentException e) {
				logger.log(Level.WARNING, "Failed to subtract distribution from project", e);
			} catch (SecurityException e) {
				logger.log(Level.WARNING, "Failed to subtract distribution from project", e);
			} catch (IllegalAccessException e) {
				logger.log(Level.WARNING, "Failed to subtract distribution from project", e);
			} catch (InvocationTargetException e) {
				logger.log(Level.WARNING, "Failed to subtract distribution from project", e);
			} catch (NoSuchMethodException e) {
				logger.log(Level.WARNING, "Failed to subtract distribution from project", e);
			}
		}

	}
}
