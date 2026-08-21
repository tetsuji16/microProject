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
package com.microproject.core.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;

import com.microproject.core.dictionary.Dictionary;
import com.microproject.core.dictionary.DictionaryCategory;
import com.microproject.core.dictionary.HasStringId;

import com.microproject.core.fields.FieldManager;

/**
 * Legacy JAXB configuration engine used only by the {@code core.fields}/
 * {@code core.nodes} compatibility subsystem. New application code must use
 * {@link com.microproject.configuration.Configuration}; do not introduce new
 * callers to this engine while the compatibility subsystem is being migrated.
 *
 * @author Laurent Chretienneau
 */
@Deprecated(forRemoval = false)
public class Configuration {
	private static final Logger logger = Logger.getLogger(Configuration.class.getName());
	protected static Configuration instance;
	protected List<ConfigurationFile> configurations=new ArrayList<ConfigurationFile>();
	protected Dictionary dictionary=new Dictionary();
	
	public static synchronized Configuration getInstance(){
		if (instance==null)
			instance=new Configuration();
		return instance;
	}

	public Dictionary getDictionary() {
		return dictionary;
	}
	
	public synchronized void register(String file,Class<?>... classesToBeBound){
		configurations.add(new ConfigurationFile(file, classesToBeBound));
	}
	
	public synchronized void load(){
//		try {
//			Thread.sleep(5000L);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		for (ConfigurationFile config : configurations){
			if (!config.isBinded()){
				config.setRoot(load(config.getFile(),config.getClassesToBeBound()));
				config.setBinded(true);
			}
		}
	}
	
	
	public class DictionaryListener extends Listener{

		@Override
		public void beforeUnmarshal(Object target, Object parent) {
		}

		@Override
		public void afterUnmarshal(Object target, Object parent) {
			if (target instanceof HasStringId)
				dictionary.add((HasStringId)target);
		}

	}
	
	
	
	public synchronized Object load(String resourceName, Class<?>... classesToBeBound) {
		try {
			JAXBContext context = JAXBContext.newInstance(classesToBeBound);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(null);
			unmarshaller.setListener(new DictionaryListener());
			try (InputStream in = Configuration.class.getClassLoader().getResourceAsStream(resourceName)) {
				if (in == null) {
					logger.log(Level.SEVERE, "Configuration resource not found: {0}", resourceName);
					return null;
				}
				return unmarshaller.unmarshal(in);
			}
		} catch(JAXBException | IOException e) {
			logger.log(Level.SEVERE, "Failed to load configuration {0}", resourceName);
			logger.log(Level.FINE, "Configuration load failure", e);
		}
		return null;
	}
	public synchronized void save(String resourceName, Object configuration, Class<?> configurationClass) {
		try {
			JAXBContext context = JAXBContext.newInstance(configurationClass);
			Marshaller marshaller = context.createMarshaller();			
			marshaller.marshal(configuration, new File(resourceName));
		} catch(JAXBException e) {
			logger.log(Level.SEVERE, "Failed to save configuration {0}", resourceName);
			logger.log(Level.FINE, "Configuration save failure", e);
		}
	}
	public static synchronized void dump(Object obj, java.io.OutputStream out){
		dump(obj.getClass(),obj,out);
	}
	public static synchronized void dump(Class<?> classe, Object obj,  java.io.OutputStream out){
		if (obj==null)
			logger.fine("null");
		else{
			try {
				JAXBContext context = JAXBContext.newInstance(classe);
				Marshaller marshaller = context.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				marshaller.marshal(obj, out);
			} catch (PropertyException e) {
				logger.log(Level.WARNING, "Failed to configure JAXB marshaller", e);
			} catch (JAXBException e) {
				logger.log(Level.SEVERE, "Failed to dump configuration", e);
			}
		}
	}
	
	public synchronized void dumpDictionary(){
		try {
			JAXBContext context = JAXBContext.newInstance(dictionary.getClassesAsArray());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			for (DictionaryCategory category : dictionary.keySet()){
				logger.info("============================== " + category + " ===============================");
				Map<String,HasStringId> map=dictionary.get(category);
				for(String id : map.keySet()){
					logger.info("------------------ " + id + " ------------------");
					StringWriter writer = new StringWriter();
					marshaller.marshal(map.get(id), writer);
					logger.info(writer.toString());
				}
			}
		} catch (PropertyException e) {
			logger.log(Level.WARNING, "Failed to configure dictionary dump marshaller", e);
		} catch (JAXBException e) {
			logger.log(Level.SEVERE, "Failed to dump dictionary", e);
		}

	}
	
	protected FieldManager fieldManager=new FieldManager();

	public FieldManager getFieldManager() {
		return fieldManager;
	}



}
