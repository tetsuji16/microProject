/*
 * (C) Copyright 2012-2019, by ProjectLibre Inc. and Contributors.
 * http://www.projectlibre.com
 *
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 *
*/

package com.projectlibre1.contrib;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassLoaderUtils {
	private static final Logger logger = LoggerFactory.getLogger(ClassLoaderUtils.class);
	protected static ClassLoaderTransformer transformer;

	public static ClassLoaderTransformer getTransformer() {
		return transformer;
	}

	public static void setTransformer(ClassLoaderTransformer transformer) {
		ClassLoaderUtils.transformer = transformer;
	}

	public static interface ClassLoaderTransformer{
		public ClassLoader transform(ClassLoader c);
	}

	public static ClassLoader getLocalClassLoader(){
		return getModifiedClassLoader(ClassLoaderUtils.class.getClassLoader());
	}
	public static ClassLoader getModifiedClassLoader(ClassLoader defaultClassLoader){
		if (transformer==null||!needModifiedClassloader()) return defaultClassLoader;
		else return transformer.transform(defaultClassLoader);
	}

	public static ResourceBundle getBundle(String baseName){
		return ResourceBundle.getBundle(baseName,Locale.getDefault(),getLocalClassLoader());
	}

	public static Class forName(String name) throws ClassNotFoundException{
		return Class.forName(name, true, getLocalClassLoader());
	}
	public static String getString(String key) {
		String value = null;
		try {
			Class<?> messagesClass = Class.forName("com.projectlibre1.strings.Messages", true, getLocalClassLoader());
			Method getStringMethod = messagesClass.getMethod("getString", String.class);
			value = (String) getStringMethod.invoke(null, key);
		} catch (ReflectiveOperationException e) {
			logger.warn("Unable to resolve localized string for key {}", key, e);
		}
		if (value == null) {
			logger.warn("UIManager fallback used for missing message key {}", key);
			value = UIManager.getString(key);
		}
		return value;
	}



	private static boolean modifClassloader=false;
	private static String javaVersion=null;

	/**
    * Compares this version with the specified version for order.  Returns a
    * negative integer, zero, or a positive integer as this version is less
    * than, equal to, or greater than the specified version.
    */
	public static int compareJavaVersionTo(String version){
		return compareJavaVersion(getJavaVersion(), version);
	}
	public static int compareJavaVersion(String version1,String version2){
		Objects.requireNonNull(version1, "version1");
		Objects.requireNonNull(version2, "version2");
		StringTokenizer javaVersionTok1=new StringTokenizer(version1,".");
		StringTokenizer javaVersionTok2=new StringTokenizer(version2,".");
		while (javaVersionTok1.hasMoreTokens() || javaVersionTok2.hasMoreTokens()){
			int v1=nextVersionPart(javaVersionTok1);
			int v2=nextVersionPart(javaVersionTok2);
			if (v1==0 && v2==-1) return -1;
			else if (v1==-1 && v2==0) return 1;
			if (v1<v2) return -1;
			else if (v1>v2) return 1;
		}
		return 0;
	}

	private static int nextVersionPart(StringTokenizer versionTokens) {
		if (!versionTokens.hasMoreTokens())
			return 0;
		try {
			return Integer.parseInt(versionTokens.nextToken());
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}
	public static boolean needModifiedClassloader(){
		if (javaVersion==null) getJavaVersion(); //init
		return modifClassloader;
	}

	public static String getJavaVersion(){
		if (javaVersion==null){
			javaVersion=System.getProperty("java.specification.version");
			modifClassloader=compareJavaVersionTo("1.6")<0;
		}
		return javaVersion;
	}
}
