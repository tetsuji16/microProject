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
package com.microproject.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.beanutils.Converter;
import org.apache.commons.digester.Digester;

import com.microproject.configuration.Dictionary;
import com.microproject.configuration.NamedItem;
import com.microproject.strings.Messages;


public class ContextStore  implements NamedItem {
	public static final String category="ContextStoreCategory";
	private static final Logger logger = Logger.getLogger(ContextStore.class.getName());

	protected String name = null;
	protected String id = null;
	protected Map<Integer, List<ConverterContext>> contexts=new HashMap<>();


	public String getCategory() {
		return category;
	}
	public String getName() {
		return name;
	}
	public final void setName(String name) {
		this.name = name;
	}
	public final String getId() {
		return id;
	}
	public final void setId(String id) {
		this.id = id;
		if (name == null)
			name = Messages.getString(id);
	}
    public static void addDigesterEvents(Digester digester){
		digester.addObjectCreate("*/converterContexts", "com.microproject.script.ContextStore");
	    digester.addSetProperties("*/converterContexts");
		digester.addSetNext("*/converterContexts", "add", "com.microproject.configuration.NamedItem");

		digester.addObjectCreate("*/converterContexts/context", "com.microproject.script.ConverterContext");
	    digester.addSetProperties("*/converterContexts/context");
		digester.addSetNext("*/converterContexts/context", "addContext", "com.microproject.script.ConverterContext");
	}

	public void addContext(ConverterContext ctx) {
		List<ConverterContext> list=contexts.get(ctx.getType());
		if (list==null){
			list=new ArrayList<ConverterContext>();
			contexts.put(ctx.getType(),list);
		}
		if (ctx.getName() == null && ctx.getFieldArrayId() != null)
			ctx.setName(Messages.getString(ctx.getFieldArrayId()));
		list.add(ctx);
	}


	public List<ConverterContext> getContexts(int type,Predicate<ConverterContext> filter){
//	System.out.println("getContext type="+type);
//	for (int t: contexts.keySet()){
//		System.out.println("type="+t);
//		for (ConverterContext ctx: contexts.get(t))
//			System.out.println("\tctx="+ctx);
//	}
		List<ConverterContext> ctxs=contexts.get(type);
		if (ctxs == null)
			return Collections.emptyList();
		List<ConverterContext> c = new ArrayList<>(ctxs.size());
		for (ConverterContext ctx: ctxs){
			if (filter==null||filter.test(ctx)) c.add((ConverterContext)ctx.clone());
		}
		return c;
	}
//	public List<ConverterContext> getContexts(int type){
////		System.out.println("getContext type="+type);
////		for (int t: contexts.keySet()){
////			System.out.println("type="+t);
////			for (ConverterContext ctx: contexts.get(t))
////				System.out.println("\tctx="+ctx);
////		}
//		return contexts.get(type);
//	}
	public  ConverterContext createDefaultContext(int type){
		List<ConverterContext> available = contexts.get(type);
		if (available == null || available.isEmpty())
			return null;
		ConverterContext ctx=available.get(0);
		if (ctx==null) return null;
		else{
			ConverterContext c=(ConverterContext)ctx.clone();
			c.setDistribution(true); //should be outside but this method is only called by ctx that requires distribution
			return c;
		}
	}

	protected static ContextStore instance=null;
	public static ContextStore getInstance(){
		if (instance==null){
	    	long t=System.currentTimeMillis();
	    	instance=(ContextStore)Dictionary.getInstance().get(category,"default");
	    	logger.log(Level.INFO, "Configuration loaded in {0} ms", System.currentTimeMillis() - t);
	    }
	    return instance;
	}


}
