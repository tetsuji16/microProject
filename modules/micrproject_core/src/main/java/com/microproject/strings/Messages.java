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
package com.microproject.strings;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import com.microproject.preference.ConfigurationFile;
import com.microproject.util.ClassLoaderUtils;
import com.microproject.util.Environment;

/**
 *
 */
public class Messages {
	private static final Logger logger = Logger.getLogger(Messages.class.getName());
	private static final String META_BUNDLE_NAME = "com.microproject.configuration.meta"; //$NON-NLS-1$
	private static ResourceBundle metaBundle = null;

	public static void setMetaBundle(String bundleName) {
		metaBundle = ResourceBundle.getBundle(bundleName,Locale.getDefault(),ClassLoaderUtils.getLocalClassLoader()/*Messages.class.getClassLoader()*/);
	}

	public static String getMetaString(String key) {
		if (metaBundle==null){
			lock.lock(); //use lock to avoid useless synchronized when it's already initialized
			try{
				if (metaBundle==null){ //if it hasn't been initialized by an other thread
					metaBundle=ResourceBundle.getBundle(META_BUNDLE_NAME,Locale.getDefault(),ClassLoaderUtils.getLocalClassLoader()/*Messages.class.getClassLoader()*/);
				}
			}finally{
				lock.unlock();
			}
		}
		return metaBundle.getString(key);
	}

	static LinkedList<ResourceBundle> bundles = null;
	static Lock lock=new ReentrantLock();

	private static ResourceBundle[] bundleArray = null;
	private static String getStringFromBundles(String key) {
		if (key==null)
			return null;
		LinkedList<ResourceBundle> buns = new LinkedList<ResourceBundle>();
		LinkedList<String> foundBundles = new LinkedList<String>();;
		if (bundles==null) {
			lock.lock(); //use lock to avoid useless synchronized when it's already initialized
			try {
				if (bundles==null){ //if it hasn't been initialized by an other thread
					String bundleNames[] = getMetaString("ResourceBundles").split(";");
					String directoryBundleNames[] = getMetaString("DirectoryResourceBundles").split(";");

					for (int i =0; i < directoryBundleNames.length;i++) {
						try {
							ResourceBundle bundle=ConfigurationFile.getDirectoryBundle(directoryBundleNames[i]);
							if (bundle==null)
								continue;
							buns.add(bundle);
							foundBundles.add("com.microproject.strings."+directoryBundleNames[i]);
						}catch (Exception e) {
							logger.log(java.util.logging.Level.WARNING, "Failed to load directory bundle " + directoryBundleNames[i], e);
						}
					}

					for (int i =bundleNames.length-1; i >=0; i--) { // reverse order since the later ones should be searched first
						String bname=bundleNames[i];
						
						//find right position to insert in bundles

						int pos=0;
						boolean found=false;
						for (String b : foundBundles){
							if (bname.equals(b)) {
								found=true;
								break;
							}
							pos++;
						}
						if (!found) { 
							buns.add(pos,ResourceBundle.getBundle(bname,Locale.getDefault(),ClassLoaderUtils.getLocalClassLoader()/*Messages.class.getClassLoader()*/));
							foundBundles.add(pos,bname);
						}
					}
				}
			} finally {
				bundles = buns;
				lock.unlock();
			}
		}
		for (ResourceBundle bundle : bundles) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException e) {
			}
		}
		return null;
	}

	/**
	 * @param key
	 * @return
	 */
	public static String getString(String key) {

		if (key==null) return null;
		String result = getStringFromBundles(key);
		if (result == null)
			result = '!' + key + '!';
		return result;
	}
	public static String getStringOrSelf(String key) {
		if (key==null)
			return null;
		String result = getStringFromBundles(key);
		if (result == null)
			result = key;
		return result;

	}

	public static void reset() {
		lock.lock();
		try {
			bundles = null;
			metaBundle = null;
			ResourceBundle.clearCache(ClassLoaderUtils.getLocalClassLoader());
		} finally {
			lock.unlock();
		}
	}
    public static Properties getTipProperties() {
    	ResourceBundle bundle;
    	try {
    		bundle = ResourceBundle.getBundle("com.microproject.strings.client", Locale.getDefault(), ClassLoaderUtils.getLocalClassLoader());
    	} catch (MissingResourceException e) {
    		if (bundles != null && bundles.size() > 1) {
    			bundle = bundles.get(1);
    		} else {
    			return new Properties();
    		}
    	}
    	return getProperties(bundle);
    }
    public static Properties getProperties(ResourceBundle bundle) {
        Properties properties = new Properties();

        for (Enumeration keys = bundle.getKeys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            properties.put(key, bundle.getString(key));
        }
        return properties;
    }

	public static String getStringWithParam(String key, String param) {
		return MessageFormat.format(getString(key),new Object[] {param});
	}
	public static String getStringWithParam(String key, Object[] params) {
		return MessageFormat.format(getString(key),params);
	}


	public static String toAppletVersion(String v){
		StringBuilder sb = new StringBuilder();
		String vNumbers[]=v.split("\\.");
		for (int i=0;i<4;i++){
			int vn=(i>=vNumbers.length)?0:Integer.parseInt(vNumbers[i]);
			if (i>0) sb.append('.');
			String hex=Integer.toHexString(vn);
			//for (int j=0;j<4-hex.length();j++) sb.append('0');
			sb.append(hex);
		}
		//System.out.println("toAppletVersion: "+v+" --> "+sb);
		return sb.toString();
	}
	public static String getContextString(String key) {
		if ("Text.ApplicationTitle".equals(key) || "Text.ShortTitle".equals(key)) {
			return "microProject";
		}
		if (Environment.isProjectLibre()) {
			String result = getStringFromBundles("Open_" + key);
			if (result == null) {
				logger.fine("getContextString not found Open_" + key);
			} else
				return result;
		}
		return getStringFromBundles(key);
	}

}
