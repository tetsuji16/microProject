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
package com.microproject.preference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import com.microproject.session.FileHelper;

public class ConfigurationFile {
	private static final Logger logger = Logger.getLogger(ConfigurationFile.class.getName());
	   
	private static final String[] OPENPROJ_CONF_DIRS={".projectlibre","ProjectLibre"};
	private static File confFile;
	public static File getConfDir(){
		if (confFile==null){
	    	String home=System.getProperty("user.home");
	    	if (home!=null){
	    		File f;
	    		for (int i=0;i<OPENPROJ_CONF_DIRS.length;i++){
	    			f=new File(home+File.separator+OPENPROJ_CONF_DIRS[i]);
	        		if (f.isDirectory()){
	        			logger.log(Level.FINE, "Configuration directory found: {0}", f.getPath());
	        			confFile=f;
	        			return f;
	        		}
	    		}
	     	}
		}
    	return confFile;
	}
	
	private static final String OPENPROJ_CONF_FILE="projectlibre.conf";
	private static Properties confProps;
	public static String getProperty(String key){
		if ("locale".equals(key)) {
			String locale=Preferences.userNodeForPackage(ConfigurationFile.class).get("locale","default");
			if (!"default".equals(locale)) {
				return locale;
			}
		}
		if (confProps==null){
			File confDir=getConfDir();
			if (confDir==null) return null;
			File f=new File(confDir,OPENPROJ_CONF_FILE);
			if (!f.exists()) return null;
			confProps=new Properties();
			try (FileInputStream in = new FileInputStream(f)) {
				confProps.load(in);
			} catch (IOException e) {
				logger.log(Level.WARNING, "Failed to load configuration from {0}", f);
			}
		}
		return confProps.getProperty(key);
	}
	
	private static Locale locale=null;
	public static Locale getLocale(){
		if (locale==null){
			String l=getProperty("locale");
			if (l==null) locale=Locale.getDefault();
			else locale=getLocale(l);
		}
		return locale;
	}
	public static Locale getLocale(String code){
		Locale defaultLocale=Locale.getDefault();
		String language=null;
		String country=null;
		String variant=null;
		StringTokenizer st=new StringTokenizer(code,"_-");
		if (!st.hasMoreTokens()) locale=defaultLocale;
		else{
			language=st.nextToken();
			if (!st.hasMoreTokens()) locale=new Locale(language,defaultLocale.getCountry());
			else{
				country=st.nextToken();
				if (!st.hasMoreTokens()) locale=new Locale(language,country);
				else{
					variant=st.nextToken();
					locale=new Locale(language,country,variant);
				}
				
			}
			
		}
		return locale;
	}
	public static String[] getLocaleCodes(String code){
		Locale defaultLocale=Locale.getDefault();
		String language=null;
		String country=null;
		String variant=null;
		StringTokenizer st=new StringTokenizer(code,"_-");
		if (!st.hasMoreTokens()) locale=defaultLocale;
		else{
			language=st.nextToken();
			if (!st.hasMoreTokens()) locale=new Locale(language,defaultLocale.getCountry());
			else{
				country=st.nextToken();
				if (!st.hasMoreTokens()) locale=new Locale(language,country);
				else{
					variant=st.nextToken();
				}
				
			}
			
		}
		return new String[] {language, country, variant};
	}
	
	private static final String OPENPROJ_RUN_CONF_FILE="run.conf";
	private static Properties runProps;
	public static String getRunProperty(String key){
		if (runProps==null){
			File confDir=getConfDir();
			if (confDir==null) return null;
			File f=new File(confDir,OPENPROJ_RUN_CONF_FILE);
			if (!f.exists()) return null;
			runProps=new Properties();
			try (FileInputStream in = new FileInputStream(f)) {
				runProps.load(in);
			} catch (Exception e) {
				logger.log(java.util.logging.Level.FINE, "Failed to load runtime properties", e);
			}
		}
		return runProps.getProperty(key);
	}
	
	public static File getGeneratedDirectory(String externalDirectory) {
		File directory=new File(externalDirectory,"import");
		return directory.isDirectory()?directory:null;

		//		File directory=null;
//		Preferences pref=Preferences.userNodeForPackage(ConfigurationFile.class);
//		if (pref.getBoolean("useExternalLocales",false)) {
//			String dir=pref.get("externalLocalesDirectory","");
//			directory=new File(dir,"generated");
//			if (!directory.isDirectory())
//				return null;
//		}		
//		return directory;
	}
	
	public static File getExportDirectory(String externalDirectory) {
		File directory=new File(externalDirectory,"export");
		return directory.isDirectory()?directory:null;
	}
	

	
	public static ResourceBundle getDirectoryBundle(String name) {
		File directory=null;
		Preferences pref=Preferences.userNodeForPackage(ConfigurationFile.class);
		if (pref.getBoolean("useExternalLocales",false)) {
			String dir=pref.get("externalLocalesDirectory","");
			directory=new File(dir,"import");
			if (!directory.isDirectory())
				return null;
		}
//		if (directory==null)
//			directory=getConfDir();
		if (directory == null)
			return null;
		try {
			URL[] urls={directory.toURI().toURL()};
			ClassLoader cl=new URLClassLoader(urls);
			ResourceBundle rb=ResourceBundle.getBundle(name, Locale.getDefault(), cl);
			return rb;
		} catch (MalformedURLException e1) {
			logger.log(Level.WARNING, "Failed to load bundle from directory", e1);
		}
		
//			Locale locale=Locale.getDefault();
		return null;
	}


}
