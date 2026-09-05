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
package com.microproject.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.microproject.util.Environment;
import com.microproject.util.FlatLafSupport;


/**
 *
 */
public class Main {
	public static void main(String[] args) {
		configureRuntimeLogging();
		FlatLafSupport.initialize();

		int runNumber=getRunNumber()+1;
		long firstRun=getFirstRun();
		Preferences.userNodeForPackage(Main.class).putInt("projectlibreRunNumber",runNumber);
		Preferences.userNodeForPackage(Main.class).putLong("projectlibrefirstRun",firstRun);		
		
//		System.setProperty("file.encoding", "UTF-8");

		Environment.setStandAlone(true);
		// Desktop startup uses the local-document path in StartupFactory.  Keep
		// client-side mode explicit; its default is false for server/plugin hosts.
		Environment.setClientSide(true);
		String[] formatedArgs;
		if (args!=null && args.length>0){
			ArrayList<String> nonEmptyArgs = new ArrayList<String>(args.length);
			for (int i=0;i<args.length;i++){
				if (args[i]!=null&& args[i].length()>0) nonEmptyArgs.add(args[i]);
			}
			if (nonEmptyArgs.size()>0){
				ArrayList<String> formatedList = new ArrayList<String>();
				if (nonEmptyArgs.get(0).startsWith("--")) {
					formatedArgs = normalizeFileNameArguments(nonEmptyArgs).toArray(new String[]{});
					startApplication(formatedArgs);
					return;
				}
				if (allExistingFiles(nonEmptyArgs)) {
					formatedList.add("--fileNames");
					formatedList.addAll(nonEmptyArgs);
					formatedArgs=formatedList.toArray(new String[]{});
					startApplication(formatedArgs);
					return;
				}
				String s1,s2;
				for (Iterator<String> i=nonEmptyArgs.iterator();i.hasNext();){
					s1=i.next();
					if (i.hasNext()){
						s2=i.next();
					}else{
						s2=s1;
						s1="--fileNames";
					}
					formatedList.add(s1);
					formatedList.add(s2);
				}
				formatedArgs=formatedList.toArray(new String[]{});
			} else formatedArgs=args;
		} else formatedArgs=args;

		startApplication(formatedArgs);
	}

	private static void startApplication(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				com.microproject.pm.graphic.gantt.Main.main(args);
			}
		});
	}

	private static boolean allExistingFiles(ArrayList<String> args) {
		if (args == null || args.size() == 0) {
			return false;
		}
		for (String fileName : args)
			if (fileName == null || !new File(fileName).isFile()) return false;
		return true;
	}

	/**
	 * Windows batch launchers can pass an unquoted path with spaces as several
	 * Java arguments.  Reassemble the documented --fileNames values at the
	 * application boundary, using a native project-file suffix as the end of a
	 * path.  Properly quoted arguments remain unchanged.
	 */
	private static ArrayList<String> normalizeFileNameArguments(ArrayList<String> args) {
		if (args.size() < 2 || !"--fileNames".equals(args.get(0)))
			return args;
		ArrayList<String> normalized = new ArrayList<>();
		normalized.add(args.get(0));
		StringBuilder path = new StringBuilder();
		for (int i = 1; i < args.size(); i++) {
			String value = args.get(i);
			if (value.startsWith("--")) {
				if (path.length() > 0) normalized.add(path.toString());
				path.setLength(0);
				normalized.add(value);
				continue;
			}
			if (path.length() > 0) path.append(' ');
			path.append(value);
			String lower = path.toString().toLowerCase(java.util.Locale.ROOT);
			if (lower.endsWith(".mpo") || lower.endsWith(".pod") || lower.endsWith(".xml")) {
				normalized.add(path.toString());
				path.setLength(0);
			}
		}
		if (path.length() > 0) normalized.add(path.toString());
		return normalized;
	}
	public static int getRunNumber() {
		return Preferences.userNodeForPackage(Main.class).getInt("projectlibreRunNumber",0);
	}
	public static long getFirstRun() {
		return Preferences.userNodeForPackage(Main.class).getLong("projectlibreFirstRun",System.currentTimeMillis());
	}
	private static void configureRuntimeLogging() {
		Logger.getLogger("com.microproject.pm.calendar.WorkWeek").setLevel(Level.WARNING);
		Logger.getLogger("org.openide.util").setLevel(Level.SEVERE);
		Logger.getLogger("org.openide.util.ImageUtilities").setLevel(Level.SEVERE);
		Logger.getLogger("org.openide.util.ImageUtilities$CachedLookupLoader").setLevel(Level.SEVERE);
	}

}
