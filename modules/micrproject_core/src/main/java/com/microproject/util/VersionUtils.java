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
package com.microproject.util;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import com.microproject.strings.Messages;

public class VersionUtils {
	private static final Logger logger = Logger.getLogger(VersionUtils.class.getName());
	public static String getVersion(){
		String version=null;
		try {
			ResourceBundle bundle=ResourceBundle.getBundle("com.microproject.version.version",Locale.ENGLISH,ClassLoaderUtils.getLocalClassLoader()); //$NON-NLS-1$
			if (bundle!=null) version=bundle.getString("microproject.version");
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to load version from com.microproject.version.version", e);
		}
		if (version==null){
			try {
				ResourceBundle bundle=ResourceBundle.getBundle("com.microproject.strings.version",Locale.ENGLISH,ClassLoaderUtils.getLocalClassLoader()); //$NON-NLS-1$
				if (bundle!=null) version=bundle.getString("microproject.version");
			} catch (Exception e) {
				logger.log(Level.FINE, "Failed to load version from com.microproject.strings.version", e);
			}
		}
		if (version!=null)
			return version; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		else return null;//return Messages.getString("Release.version"); 

	}
	public static String getJnlpVersion(){
		return System.getProperty("microproject.version");
	}

	public static String toAppletVersion(String v){
		StringBuilder sb = new StringBuilder();
		String vNumbers[]=v.split("\\.");
		for (int i=0;i<4;i++){
			int vn=(i>=vNumbers.length)?0:Integer.parseInt(vNumbers[i]);
			if (i>0) sb.append('.');
			String hex=Integer.toHexString(vn);
			for (int j=0;j<hex.length()-4;j++) sb.append('0');
			sb.append(hex);
		}
		return sb.toString();
	}

	public static boolean isJnlpUpToDate(){
		String v=getVersion();
		String jv=getJnlpVersion();
		if (v==null||jv==null) return true;
		try{
			return jv.equals(toAppletVersion(v));
		}catch(Exception e){return false;}
	}
	public static boolean versionCheck(boolean warnIfBad) {
		String version = VersionUtils.getVersion();
		if (version == null) // for running in debugger
			version="0";
		Preferences pref=Preferences.userNodeForPackage(VersionUtils.class);
		String localVersion = pref.get("PODVersion","0");
		boolean updated = !localVersion.equals(version);
		String javaVersion = System.getProperty("java.version");
		logger.info("ProjectLibre Version: "+version + " local version " + localVersion + " updated=" + updated + " java version=" + javaVersion);


		pref.put("JavaVersion",javaVersion);

		if (updated) {
			Environment.setUpdated(true);
			pref.put("PODVersion",version);
			try {
				pref.flush();
			} catch (BackingStoreException e) {
				logger.log(Level.WARNING, "Unexpected error", e);
			}

			if (warnIfBad && Environment.isApplet()) {
				if (javaVersion.equals("1.6.0_09") || javaVersion.equals("1.6.0_08") || javaVersion.equals("1.6.0_07")|| javaVersion.equals("1.6.0_06")|| javaVersion.equals("1.6.0_05") || javaVersion.equals("1.6.0_04")) {
					Environment.setNeedToRestart(true);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							Alert.error(Messages.getString("Error.restart"));
						}});
				}
			}
		}else{
			try {
				pref.flush();
			} catch (BackingStoreException e) {
				logger.log(Level.WARNING, "Unexpected error", e);
			}
		}
		return updated;
	}


}
