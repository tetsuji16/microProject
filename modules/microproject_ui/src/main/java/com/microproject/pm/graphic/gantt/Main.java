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
package com.microproject.pm.graphic.gantt;

import java.awt.Frame;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JOptionPane;

import com.microproject.pm.graphic.frames.ApplicationStartupFactory;
import com.microproject.pm.graphic.frames.MainFrameFactory;
import com.microproject.preference.ConfigurationFile;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;
import com.microproject.util.FlatLafSupport;
import com.microproject.util.PopupDialogSupport;

/**
 *
 */
@SuppressWarnings("unchecked")
public class Main {
	public static void main(String[] args) {
		FlatLafSupport.ensureInitialized();

		System.setProperty("apple.awt.application.name","ProjectLibre");
		System.setProperty("apple.laf.useScreenMenuBar","true");
		Locale.setDefault(ConfigurationFile.getLocale());
		HashMap<String, Object> opts = ApplicationStartupFactory.extractOpts(args);
		String osName=System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (osName.startsWith("linux")){
			String javaExec=ConfigurationFile.getRunProperty("JAVA_EXE");
			//check jvm
			String javaVersion=System.getProperty("java.version");
			if (Environment.compareJavaVersion(javaVersion,"1.5")<0){
				String message=Messages.getStringWithParam("Text.badJavaVersion", javaVersion);
				if (javaExec!=null&&javaExec.length()>0) message+="\n"+Messages.getStringWithParam("Text.javaExecutable", new Object[]{javaExec,"JAVA_EXE","$HOME/.projectlibre/run.conf"});
				if (!opts.containsKey("silentlyFail")) PopupDialogSupport.showMessageDialog(null,message, Messages.getContextString("Title.ProjectLibreError"),JOptionPane.ERROR_MESSAGE);
				System.exit(64);
			}
			//claur -  now assuming that now all vendors have correct implementation
//			String javaVendor=System.getProperty("java.vendor");
//			if (javaVendor==null || !(javaVendor.startsWith("Sun")||javaVendor.startsWith("IBM"))){
//				String message=Messages.getStringWithParam("Text.badJavaVendor", javaVendor);
//				if (javaExec!=null&&javaExec.length()>0) message+="\n"+Messages.getStringWithParam("Text.javaExecutable", new Object[]{javaExec,"JAVA_EXE","$HOME/.projectlibre/run.conf"});
//				if (!opts.containsKey("silentlyFail")) PopupDialogSupport.showMessageDialog(null,message, Messages.getContextString("Title.ProjectLibreError"),JOptionPane.ERROR_MESSAGE);
//				System.exit(64);
//			}
		}
		boolean newLook = true;
//		HashMap opts = ApplicationStartupFactory.extractOpts(args); // allow setting menu look on command line - primarily for testing or webstart args
//		newLook = opts.get("menu") == null;

		Environment.setNewLook(newLook);
//		if (!Environment.isNewLaf()) {
//			try {
//				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//			} catch (Exception e) {
//			}
//		}
		ApplicationStartupFactory startupFactory=new ApplicationStartupFactory(opts); //put before to initialize standalone flag
		Frame frame = MainFrameFactory.creareMainFrame(Messages.getContextString("Text.ApplicationTitle"), null, null);
		boolean doWelcome = true; // to do see if project param exists in args
		startupFactory.instanceFromNewSession(frame,doWelcome);
	}
}
