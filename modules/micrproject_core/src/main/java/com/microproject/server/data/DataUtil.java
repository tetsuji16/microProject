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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.session.Session;

public class DataUtil {
	private static final Logger logger = Logger.getLogger(DataUtil.class.getName());
	protected Object obj;
	protected Class<?> clazz;
	public DataUtil(){
		try {
			clazz = Class.forName("com.microproject.server.data.Serializer");
			obj = clazz.getDeclaredConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Serializer class not found", e);
		} catch (NoSuchMethodException e) {
			logger.log(Level.SEVERE, "Failed to construct serializer", e);
		} catch (InvocationTargetException e) {
			logger.log(Level.SEVERE, "Failed to construct serializer", e);
		} catch (InstantiationException e) {
			logger.log(Level.SEVERE, "Failed to construct serializer", e);
		} catch (IllegalAccessException e) {
			logger.log(Level.SEVERE, "Failed to construct serializer", e);
		}
	}
	
    public DocumentData serializeDocument(Project project) throws Exception{
		try {
			return (DocumentData) clazz.getMethod("serializeDocument", Project.class).invoke(obj, project);
		} catch (IllegalArgumentException e) {
			logger.log(Level.WARNING, "Failed to serialize document", e);
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "Failed to serialize document", e);
		} catch (IllegalAccessException e) {
			logger.log(Level.WARNING, "Failed to serialize document", e);
		} catch (InvocationTargetException e) {
			logger.log(Level.WARNING, "Failed to serialize document", e);
		} catch (NoSuchMethodException e) {
			logger.log(Level.WARNING, "Failed to serialize document", e);
		}
		return null;
    	
    }
    public Project deserializeLocalDocument(DocumentData documentData) throws IOException, ClassNotFoundException {
		try {
			return (Project) clazz.getMethod("deserializeLocalDocument", DocumentData.class).invoke(obj, documentData);
		} catch (IllegalArgumentException e) {
			logger.log(Level.WARNING, "Failed to deserialize local document", e);
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "Failed to deserialize local document", e);
		} catch (IllegalAccessException e) {
			logger.log(Level.WARNING, "Failed to deserialize local document", e);
		} catch (InvocationTargetException e) {
			logger.log(Level.WARNING, "Failed to deserialize local document", e);
		} catch (NoSuchMethodException e) {
			logger.log(Level.WARNING, "Failed to deserialize local document", e);
		}
		return null;
    }
	
    public static void setEnterpriseResources(Collection<?> resources,ResourcePool resourcePool) throws IOException, ClassNotFoundException{
		try {
			Class.forName("com.microproject.server.data.Serializer")
				.getMethod("setEnterpriseResources", Collection.class, ResourcePool.class, Session.class)
				.invoke(null, resources, resourcePool, null);
		} catch (IllegalArgumentException e) {
			logger.log(Level.WARNING, "Failed to set enterprise resources", e);
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "Failed to set enterprise resources", e);
		} catch (IllegalAccessException e) {
			logger.log(Level.WARNING, "Failed to set enterprise resources", e);
		} catch (InvocationTargetException e) {
			logger.log(Level.WARNING, "Failed to set enterprise resources", e);
		} catch (NoSuchMethodException e) {
			logger.log(Level.WARNING, "Failed to set enterprise resources", e);
		}
	}

}
