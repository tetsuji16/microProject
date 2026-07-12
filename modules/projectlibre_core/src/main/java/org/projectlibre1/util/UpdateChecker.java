/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses.
 *******************************************************************************/
package org.projectlibre1.util;

import groovy.lang.GroovyClassLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Alert;
import com.projectlibre1.util.BrowserControl;
import com.projectlibre1.util.VersionUtils;

/*
 * Format:
 * version: series of integers separated by dots
 * name: string
 *
 * %UpdateChecker
 * <groovy formula>
 */
public class UpdateChecker {
	private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());
	private static final int UPDATE_CHECKER_VERSION = 1;
	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int READ_TIMEOUT_MS = 10000;
	private static final String updateAddress = "http://projectlibre.org/versions-" + UPDATE_CHECKER_VERSION;
	private static final String downloadAddress = "http://sourceforge.net/projects/projectlibre/files/latest/download";

	private static final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "projectlibre-update-checker");
			t.setDaemon(true);
			return t;
		}
	});

	private static void checkForUpdate() {
		if (!Preferences.userNodeForPackage(UpdateChecker.class).getBoolean("checkForUpdates", true)) {
			return;
		}
		String thisVersion = VersionUtils.getVersion();
		if (thisVersion == null) thisVersion = "0";
		HttpURLConnection conn = null;
		try {
			URL url = new URL(updateAddress +
					"?version=" + URLEncoder.encode(thisVersion, "UTF-8") +
					"&locale=" + URLEncoder.encode(Locale.getDefault().toString(), "UTF-8") +
					"&timeZone=" + URLEncoder.encode(TimeZone.getDefault().getID(), "UTF-8") +
					"&osName=" + URLEncoder.encode(System.getProperty("os.name"), "UTF-8") +
					"&osVersion=" + URLEncoder.encode(System.getProperty("os.version"), "UTF-8") +
					"&osArch=" + URLEncoder.encode(System.getProperty("os.arch"), "UTF-8") +
					"&javaVersion=" + URLEncoder.encode(System.getProperty("java.version"), "UTF-8") +
					"&javaVendor=" + URLEncoder.encode(System.getProperty("java.vendor"), "UTF-8") +
					"&validation=" + URLEncoder.encode(System.getProperty("projectlibre.validation", "0"), "UTF-8") +
					"&runNumber=" + URLEncoder.encode(System.getProperty("projectlibre.runNumber", "0"), "UTF-8") +
					"&firstRun=" + URLEncoder.encode(System.getProperty("projectlibre.firstRun", "0"), "UTF-8") +
					"&openprojRunNumber=" + URLEncoder.encode(System.getProperty("projectlibre.projectLibreRunNumber", "0"), "UTF-8") +
					"&openprojFirstRun=" + URLEncoder.encode(System.getProperty("projectlibre.projectLibreFirstRun", "0"), "UTF-8") +
					"&projectLibreRunNumber=" + URLEncoder.encode(System.getProperty("projectlibre.projectLibreRunNumber", "0"), "UTF-8") +
					"&projectLibreFirstRun=" + URLEncoder.encode(System.getProperty("projectlibre.projectLibreFirstRun", "0"), "UTF-8") +
					"&email=" + URLEncoder.encode(System.getProperty("projectlibre.userEmail", "0"), "UTF-8"));

			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);
			conn.setRequestMethod("GET");

			InputStream stream = conn.getInputStream();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
				String latestVersion = in.readLine();
				if (thisVersion == null) {
					return;
				}

				UpdateCheckerFormula f = new UpdateCheckerFormula();
				if (f.mainCompare(thisVersion, latestVersion) >= 0) {
					return;
				}

				if (f.mainCompare(Preferences.userNodeForPackage(UpdateChecker.class).get("lastVersionChecked", "-1"), latestVersion) >= 0) {
					int runNumber = Preferences.userNodeForPackage(Class.forName("com.projectlibre1.main.Main")).getInt("projectlibreRunNumber", 0);
					int showEvery = Integer.parseInt(Messages.getString("UpdateDialog.showEvery"));
					int showEveryStagger = Integer.parseInt(Messages.getString("UpdateDialog.showEveryStagger"));
					if ((runNumber - showEveryStagger) % showEvery != 0) {
						return;
					}
				}

				String latestName = in.readLine();

				StringBuilder formulaDef = new StringBuilder();
				String s = null;
				while ((s = in.readLine()) != null) {
					if (s.trim().toUpperCase().equals("%UPDATECHECKER")) break;
				}
				if (s != null) {
					while ((s = in.readLine()) != null) {
						formulaDef.append(s).append('\n');
					}
				}

				UpdateCheckerFormula formula = getFormula(formulaDef.toString().trim());
				if (formula.mainCompare(thisVersion, latestVersion) < 0) {
					final String message = MessageFormat.format(Messages.getString("Text.newVersion"), new Object[]{latestVersion, thisVersion});
					Preferences.userNodeForPackage(UpdateChecker.class).put("lastVersionChecked", latestVersion);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							if (Alert.okCancel(message))
								BrowserControl.displayURL(downloadAddress);
						}
					});
				}
			}
		} catch (Exception e) {
			logger.log(Level.FINE, "Update check failed", e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static UpdateCheckerFormula getFormula(String formulaDef) {
		if (formulaDef.length() == 0) return new UpdateCheckerFormula();
		StringBuilder classText = new StringBuilder();
		classText.append("package org.projectlibre1.util;\n");
		classText.append("public class UpdateCheckerFormulaImpl extends UpdateCheckerFormula{\n");
		classText.append("\tpublic int mainCompare(String currentVersion,String latestVersion){\n");
		classText.append("\t\t").append(formulaDef).append('\n');
		classText.append("\t}\n");
		classText.append("}\n");
		GroovyClassLoader loader = new GroovyClassLoader(UpdateChecker.class.getClassLoader());
		try {
			Class<?> groovyClass = loader.parseClass(classText.toString());
			return (UpdateCheckerFormula) groovyClass.newInstance();
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to parse update formula", e);
			return new UpdateCheckerFormula();
		} finally {
			try {
				loader.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public static void checkForUpdateInBackground() {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				checkForUpdate();
			}
		});
	}

	static void shutdown() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
