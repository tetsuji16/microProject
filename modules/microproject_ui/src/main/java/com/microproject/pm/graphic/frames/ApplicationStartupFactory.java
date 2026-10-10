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
package com.microproject.pm.graphic.frames;

import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.microproject.ui.util.SwingFontUtil;
import com.microproject.util.Environment;

@SuppressWarnings({"deprecation", "unchecked"})
public class ApplicationStartupFactory extends StartupFactory {
	private static final Logger logger = Logger.getLogger(ApplicationStartupFactory.class.getName());

	public ApplicationStartupFactory(String args[]){
		this(ApplicationStartupFactory.extractOpts(args));
	}
	public ApplicationStartupFactory(HashMap<String, Object> opts) {
		this.opts=opts;
		ensureSupportedOptions(opts);
		Environment.setStandAlone(true);
		dumpOpts();

		String font=(String)getOpt("font");
		if (font==null){
			String javaVendor=System.getProperty("java.vendor");
			if (javaVendor.startsWith("IBM")){ //to avoid font bug on SLED with IBM jvm
				font=SwingFontUtil.getValidFont(new String[]{"DejaVu Sans","Andale Sans"}); //Lucida Sans
			}
		}else{
			font=font.replace('_', ' ');
		}
		if (font!=null){
			Environment.resetFonts();
			Environment.setFont(font,Environment.DEFAULT_FONT);
			SwingFontUtil.setUIFont(font);
		}

		Object o=opts.get("fileNames");
		List<String> fileNames;
		if (o==null) fileNames=null;
		else if (o instanceof List){
			@SuppressWarnings("unchecked")
			List<String> typedFileNames = (List<String>) o;
			fileNames = typedFileNames;
		}else{
			fileNames=new ArrayList<>(1);
			fileNames.add((String) o);
		}

		if (fileNames!=null) projectUrls=(String[])fileNames.toArray(new String[]{});

	}

	private static void ensureSupportedOptions(HashMap<String, Object> options) {
		if (options.containsKey("serverUrl") || options.containsKey("projectId") || options.containsKey("credentials"))
			throw new IllegalArgumentException("Server login and projectId startup options are no longer supported; open a local project file instead.");
	}

	private String getOpt(String name){
		return getOpt(name,0);
	}
	private String getOpt(String name,int index){
		if (index<0) return null;
		Object o=opts.get(name);
		if (o==null) return null;
		else if (o instanceof String) return (index==0)?((String)o):null;
		else if (o instanceof List){
			@SuppressWarnings("unchecked")
			List<String> lopt=(List<String>)o;
			if (index>=lopt.size()) return null;
			return lopt.get(index);
		}
		else return null;
	}

//	private void computeOpts(String args[]){
//		opts = extractOpts(args);
//	}
	public static HashMap<String, Object> extractOpts(String args[]){
		HashMap<String, Object> opts = new HashMap<>();
		if (args.length==0) return opts;
		String arg=args[0];
		if (arg!=null&&arg.length()>1&&(!arg.startsWith("--"))) {
			if (arg.startsWith("http://") || arg.startsWith("https://"))
				throw new IllegalArgumentException("Legacy server startup arguments are no longer supported; open a local project file instead.");
			List<String> fileNames=new ArrayList<>();
			for (String fileName : args) fileNames.add(fileName);
			opts.put("fileNames", fileNames);
		} else {
			String opt=null,label=null;
			List<String> lopt=null;
			for (int i=0;i<args.length;i++){
				arg=args[i];
				if (arg.length()>2&&arg.startsWith("--")){
					if (label!=null){
						if (lopt!=null) opts.put(label,lopt);
						else if (opt!=null) opts.put(label,opt);
					}
					label=arg.substring(2);
					if ("serverUrl".equals(label) || "projectId".equals(label) || "credentials".equals(label))
						throw new IllegalArgumentException("Server login and projectId startup options are no longer supported; open a local project file instead.");
					opt=null;
					lopt=null;
				}else{
					if (lopt!=null) lopt.add(arg);
					else if (opt!=null){
						lopt=new LinkedList();
						lopt.add(opt);
						lopt.add(arg);
						opt=null;
					}else opt=arg;
				}
			}
			if (label!=null){
				if (lopt!=null) opts.put(label,lopt);
				else if (opt!=null) opts.put(label,opt);
			}
		}
		return opts;
	}
	public void dumpOpts() {
		logger.info("opts:");
		for (String opt : opts.keySet()) {
			logger.info(opt + ":");
			String arg;
			int index=0;
			while ((arg=getOpt(opt,index++))!=null) logger.info("\t" + arg);
		}
	}
	public void doPostInitView(Container container) {
		if (!Environment.isPlugin()) ((JFrame)container).pack();
	}


}
