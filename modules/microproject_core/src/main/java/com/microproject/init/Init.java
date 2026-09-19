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
package com.microproject.init;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.configuration.ScriptConfiguration;
import com.microproject.contrib.ClassResolverFilter;
import com.microproject.field.FieldConverter;
import com.microproject.util.Environment;
/**
 * Class to perform static initializations
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Init {
	private static final Logger logger = Logger.getLogger(Init.class.getName());
	private static Init instance = null;
	private Init(boolean loadConfiguration) {
		instance = this;
		if (loadConfiguration) loadConfiguration();
		FieldConverter.getInstance();
		if (!Environment.getStandAlone()){
			try {
				Class.forName("org.codehaus.groovy.control.ResolveVisitor").getMethod("setClassResolverFilter", new Class[]{ClassResolverFilter.class}).invoke(null,new Object[]{ScriptConfiguration.getInstance()});
			} catch (Exception e) {
				logger.log(Level.FINE, "Groovy resolver filtering hook is unavailable", e);
			}
		}
	}
	/**
	 *
	 */
	private void loadConfiguration() {
		//Dictionary.getInstance(); //claur fix to avoid Digester3 failure
		com.microproject.configuration.Configuration.getInstance();
	}
	/**
	 * Provides a means to force initialization to happen now if it hasn't happened already.
	 *
	 */
	synchronized public static void initialize() {
		if (instance == null) {
			instance = new Init(true);
		}
	}
	//for dialogs tests
	synchronized public static void initialize(boolean loadConfiguration) {
		if (instance == null) {
			instance = new Init(loadConfiguration);
		}
	}


}
