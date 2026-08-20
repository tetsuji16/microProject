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

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import com.microproject.company.ApplicationUser;
import com.microproject.util.ClassLoaderUtils;
import com.microproject.session.SessionFactory;

public class Environment {
	private static final Logger logger = Logger.getLogger(Environment.class.getName());
	private static boolean clientSide = false;
	private static boolean standAlone = false;
	private static boolean batchMode = false;
	private static ApplicationUser user = null;
	private static String partnerId = null;
	private static String login = null;
	private static boolean importing = false;
	private static boolean ribbonUI = true;
	private static boolean newLook = false;
	private static boolean newLaf = false;
	private static boolean scripting = false;
	private static boolean visible = true;
	private static boolean applet = false;
	private static boolean outOfMemory = false;
	public static int LINUX=1;
	public static int MAC=2;
	private static int os=-1;
	private static boolean updated=false;
	private static boolean needToRestart = false;
	protected static boolean keepExternalLinks=true;
	private static boolean plugin = false;
	public static final boolean isBatchMode() {
		return batchMode;
	}
	public static final void setBatchMode(boolean processingUndoRedo) {
		Environment.batchMode = processingUndoRedo;
	}
	public Environment() {
		super();
	}
	public static String getLogin() {
		if (login == null)
		   login = SessionFactory.getInstance().getLogin();
		return login;
	}
	public static final boolean isClientSide() {
		return clientSide;
	}
	public static final void setClientSide(boolean clientSide) {
		Environment.clientSide = clientSide;
	}

	public static float getJavaVersionNumber() {
		String javaVersion = System.getProperty("java.specification.version");
		return Float.parseFloat(javaVersion);
	}
	public static final boolean getStandAlone() {
		return standAlone;
	}
	public static final void setStandAlone(boolean standAlone) {
		Environment.standAlone = standAlone;
	}
	public static final ApplicationUser getUser() {
		return user;
	}
	public static final void setUser(ApplicationUser user) {
		Environment.user = user;
	}
	public static final boolean isAdministrator() {
		return user != null && user.isAdministrator();
	}
	public static final boolean isExternal() {
		return user != null && user.isExternal();
	}
	public static final String getPartnerId() {
		return partnerId;
	}
	public static final void setPartnerId(String partnerId) {
		Environment.partnerId = partnerId;
	}
	public static final boolean isWindows() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return osName.startsWith("windows");
	}
	public static boolean isImporting() {
		return importing;
	}
	public static void setImporting(boolean importing) {
		logger.log(Level.INFO, "set importing {0}", importing);
		Environment.importing = importing;
	}
	public static boolean isNewLook() {
		return newLook;// || isRibbonUI();
	}
	public static void setNewLook(boolean newLook) {
		Environment.newLook = newLook;
//Environment.setNewLaf(false);
//		Environment.setNewLaf(newLook && Environment.getJavaVersionNumber() >= 1.5f && Environment.getOs()!=Environment.LINUX && Environment.getOs()!=Environment.MAC
//		&& !Environment.isChinese());
	}
	public static boolean isNewLaf() {
		return newLaf || isFlatLafLookAndFeel();
	}
	public static void setNewLaf(boolean newLaf) {
		Environment.newLaf = newLaf;
	}
	public static boolean isScripting() {
		return scripting;
	}
	public static void setScripting(boolean scripting) {
		Environment.scripting = scripting;
	}
	public static boolean isVisible() {
		return visible;
	}
	public static void setVisible(boolean visible) {
		Environment.visible = visible;
	}
	public static boolean isProjectLibre() {
		return getStandAlone();
	}
	public static boolean isApplet() {
		return applet;
	}
	public static void setApplet(boolean applet) {
		Environment.applet = applet;
	}
	public static int getOs() {
		if (os==-1){
			String osName=System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
			if (osName.startsWith("linux")) os=LINUX;
			else if (osName.startsWith("mac os x")) os=MAC;
			else os=0;
		}
		return os;
	}
	public static boolean isMac(){
		return Environment.getOs()==Environment.MAC;
	}

	public static final int DEFAULT_FONT=0;
	public static final int GANTT_ANNOTATIONS_FONT=1;
	public static final int NETWORK_FONT=2;

	private static final ConcurrentHashMap<Integer, String> fonts=new ConcurrentHashMap<>();
	static{
		fonts.put(DEFAULT_FONT, "Dialog PLAIN 12");
		fonts.put(GANTT_ANNOTATIONS_FONT,"Dialog BOLD 11");
		fonts.put(NETWORK_FONT, "Dialog PLAIN 11");
	}
	public static String getFont(int type) {
		String font=fonts.get(type);
		return font==null?fonts.get(DEFAULT_FONT):font;
	}
	public static void setFont(String font,int type) {
		fonts.put(type, font);
	}
	public static void resetFonts(){
		fonts.clear();
	}

	public static boolean isChinese(){
		Locale locale = Locale.getDefault();
		return locale.equals(Locale.SIMPLIFIED_CHINESE) || locale.equals(Locale.TRADITIONAL_CHINESE);
	}

	/**
    * Compares this version with the specified version for order.  Returns a
    * negative integer, zero, or a positive integer as this version is less
    * than, equal to, or greater than the specified version.
    */
	public static int compareJavaVersionTo(String version){
		return ClassLoaderUtils.compareJavaVersionTo(version);
	}
	public static int compareJavaVersion(String version1,String version2){
		return ClassLoaderUtils.compareJavaVersion(version1, version2);
	}
	public static boolean isOutOfMemory() {
		return outOfMemory;
	}
	public static void setOutOfMemory(boolean outOfMemory) {
		Environment.outOfMemory = outOfMemory;
	}
	public static boolean isUpdated() {
		return updated;
	}
	public static void setUpdated(boolean updated) {
		Environment.updated = updated;
	}
	public static boolean isNeedToRestart() {
		return needToRestart;
	}
	public static void setNeedToRestart(boolean needToRestart) {
		Environment.needToRestart = needToRestart;
	}
	public static boolean isKeepExternalLinks() {
		return keepExternalLinks;
	}
	public static void setKeepExternalLinks(boolean keepExternalLinks) {
		Environment.keepExternalLinks = keepExternalLinks;
	}
	public static boolean isPlugin() {
		return plugin;
	}
	public static void setPlugin(boolean plugin) {
		Environment.plugin = plugin;
	}
	public static boolean isRibbonUI() {
		return ribbonUI;
	}
	public static void setRibbonUI(boolean ribbonUI) {
		Environment.ribbonUI = ribbonUI;
	}

	private static boolean isFlatLafLookAndFeel() {
		if (UIManager.getLookAndFeel() == null) {
			return false;
		}
		String className = UIManager.getLookAndFeel().getClass().getName();
		return className != null && className.startsWith("com.formdev.flatlaf.");
	}


}
