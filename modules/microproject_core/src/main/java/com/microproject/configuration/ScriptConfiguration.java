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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.digester.Digester;

import com.microproject.contrib.ClassResolverFilter;

/**
 * Stores configuration info for scripts read in from config
 */
public class ScriptConfiguration implements ClassResolverFilter{
	protected Set classesToResolve=new HashSet();
	public static ScriptConfiguration getInstance(){
		return Configuration.getInstance().getScriptConfiguration();
	}
	
	public void addClassToResolve(ScriptClass scriptClass){
		classesToResolve.add(scriptClass.getName());
	}
	
	public boolean canBeResoved(String className){
		return classesToResolve.contains(className);
	}
	
	public static void addDigesterEvents(Digester digester){
		digester.addObjectCreate("*/script", "com.microproject.configuration.ScriptConfiguration");
	    digester.addSetProperties("*/script");
		digester.addSetNext("*/script", "setScriptConfiguration", "com.microproject.configuration.ScriptConfiguration");

		digester.addObjectCreate("*/script/class", "com.microproject.configuration.ScriptClass");
	    digester.addSetProperties("*/script/class");
		digester.addSetNext("*/script/class", "addClassToResolve", "com.microproject.configuration.ScriptClass");
		
	}
}
