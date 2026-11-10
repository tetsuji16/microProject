/*******************************************************************************
 * MIT License
 *
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

import com.microproject.pm.graphic.frames.ApplicationStartupFactory;
import com.microproject.pm.graphic.frames.MainFrameFactory;
import com.microproject.preference.ConfigurationFile;
import com.microproject.preference.GlobalPreferences;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;
import com.microproject.util.FlatLafSupport;

/**
 *
 */
@SuppressWarnings("unchecked")
public class Main {
	public static void main(String[] args) {
		FlatLafSupport.ensureInitialized();

		System.setProperty("apple.awt.application.name","microProject");
		System.setProperty("apple.laf.useScreenMenuBar","true");
		Locale.setDefault(ConfigurationFile.getLocale());
		new GlobalPreferences().applyFormatPreferences();
		HashMap<String, Object> opts = ApplicationStartupFactory.extractOpts(args);
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
		ApplicationStartupFactory startupFactory=new ApplicationStartupFactory(opts);
		Frame frame = MainFrameFactory.creareMainFrame(Messages.getContextString("Text.ApplicationTitle"), null);
		boolean doWelcome = true; // to do see if project param exists in args
		startupFactory.instanceFromNewSession(frame,doWelcome);
	}
}
