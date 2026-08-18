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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Reads configuration xml file using the digester
 */
public class ConfigurationReader {
	private static final Logger logger = Logger.getLogger(ConfigurationReader.class.getName());

	public static ProvidesDigesterEvents read(String configurationUrl, ProvidesDigesterEvents root) {
		URL url = ConfigurationReader.class.getClassLoader().getResource(configurationUrl);
		if (url == null) {
			logger.log(Level.SEVERE, "could not find xml configuration file: " + configurationUrl);
			return null;
		}
		//logger.info("Reading configuration from " + url + " " + new java.util.Date());
		ProvidesDigesterEvents result = null;
		try {
			result = readStream(url.openStream(), root);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not read field xml configuration file " + url, e);
		}
		//logger.info("Done reading configuration from " + url + " " + new java.util.Date());
		return result;
	}

	public static ProvidesDigesterEvents readString(String str, ProvidesDigesterEvents root) {
		if (str == null)
			return root;
	    ByteArrayInputStream in=new ByteArrayInputStream(str.getBytes());
		return ConfigurationReader.readStream(in,root);
	}

	public static ProvidesDigesterEvents readStream(InputStream stream, ProvidesDigesterEvents root) {
		ProvidesDigesterEvents result = null;
		Digester digester = new Digester();
		digester.setNamespaceAware(true); // this is so we can use the JADE parser instead which is faster

		digester.setValidating(false);
		digester.push(root);
		root.addDigesterEvents(digester);

		try {
			result = (ProvidesDigesterEvents) digester.parse(stream);
		} catch (Exception e1) { //claur
			logger.log(Level.SEVERE, "Error parsing reading/parsing field xml configuration file.", e1);
		}
		return result;
	}
}
