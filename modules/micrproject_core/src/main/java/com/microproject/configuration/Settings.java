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
package com.microproject.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import com.microproject.strings.Messages;

/**
 * Stores global constants related to the application settings. Some are also read in from config
 */
public class Settings {
	public static final boolean CLUSTERED=true;
	public static final String CLUSTER_NODES="jnp://localhost:1100";
	public static final String SITE_HOME = "https://github.com/tetsuji16/ProjectLibre";
	/** GitHub Pages publishes the repository's docs directory at the site root. */
	public static final String HELP_HOME = "https://tetsuji16.github.io/ProjectLibre/";
	public static final String WEB_APP= "web";
	public static final String WEB_HOME = SITE_HOME + "/" + WEB_APP;
	
	public static int NUM_ARRAY_BASELINES = 11; // should get set from config file 
	public static int NUM_COST_RATES = 5;
	public static String COST_RATE_NAMES = "A;B;C;D;E";
	public static int NUM_HIERARCHIES = 11;
	public static int CALENDAR_INTERVALS = 5;
	public static int numBaselines() {return NUM_ARRAY_BASELINES + 2;}// "baseline" + basline1-10 + timesheet
	public static int numHierarchies() {return NUM_HIERARCHIES;}// "wbs" + hierachy 1-10
	public static int numGanttBaselines() {return NUM_ARRAY_BASELINES + 1;}// "baseline" + basline1-10
	public static String LIST_SEPARATOR = Messages.getString("Symbol.listSeparator"); //; for example
	public static String LEFT_BRACKET = Messages.getString("Symbol.leftBracket"); //[ for example
	public static String RIGHT_BRACKET = Messages.getString("Symbol.rightBracket"); //] for example	
	public static String PERCENT = Messages.getString("Symbol.percent"); //] for example
	public static String SLASH = Messages.getString("Symbol.slash"); // / for example
	public static String ELLIPSIS = Messages.getString("Symbol.ellipsis"); // / for example ...
	public static int STRING_LIST_LIMIT = 20; // maximum item names shown in multi-selection messages
    public static boolean SHOW_HELP_LINKS = true;
    public static String VERSION_TYPE_STANDALONE="standalone";
    public static String VERSION_TYPE_SERVER="server";
    
    public static String LANGUAGES = "default"; //defaults are set in configuration.xml
 }
