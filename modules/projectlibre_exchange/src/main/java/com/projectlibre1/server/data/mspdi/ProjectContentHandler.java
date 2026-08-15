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

package com.projectlibre1.server.data.mspdi;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ProjectContentHandler extends DefaultHandler {
	private static final Logger logger = Logger.getLogger(ProjectContentHandler.class.getName());
	Unmarshaller unmarshaller = null;
	InputSource inputSource;
    JAXBContext context;
	
	public ProjectContentHandler(InputSource inputSource) {
		this.inputSource = inputSource;
		try {
			context = JAXBContext.newInstance ("net.sf.mpxj.mspdi.schema");
			unmarshaller = context.createUnmarshaller ();
		} catch (JAXBException e) {
			logger.log(Level.WARNING, "Failed to initialize JAXB unmarshaller", e);
		}
	}
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equals("Task"))
			logger.fine("task found -- end");
		super.endElement(uri, localName, qName);
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("Task")) {
			logger.fine("task found -- start");
			try {
//                context.startElement("http://schemas.microsoft.com/project", "Task");

				Object o = unmarshaller.unmarshal(inputSource);
				logger.fine("unmarshalled " + o.getClass());
			} catch (JAXBException e) {
				logger.log(Level.WARNING, "Failed to unmarshal project content", e);
			}
		} else {
			super.startElement(uri, localName, qName, attributes);
		}
	}

}
