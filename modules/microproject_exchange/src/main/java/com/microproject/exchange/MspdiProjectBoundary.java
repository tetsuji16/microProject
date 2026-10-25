/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sf.mpxj.ScheduleFrom;

/** Reads declared MSPDI project boundary fields without inferring them from tasks. */
public final class MspdiProjectBoundary {
	public record Declarations(boolean startDate, boolean finishDate) {
		public static final Declarations NONE = new Declarations(false, false);

		public boolean appliesTo(ScheduleFrom scheduleFrom) {
			return scheduleFrom == ScheduleFrom.FINISH ? finishDate : startDate;
		}
	}

	private MspdiProjectBoundary() { }

	public static Document parseSecureDocument(byte[] xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
	}

	public static Declarations declarations(Document document) {
		Element project = document.getDocumentElement();
		String namespace = project.getNamespaceURI();
		return new Declarations(hasDeclaredValue(project, "StartDate", namespace),
			hasDeclaredValue(project, "FinishDate", namespace));
	}

	private static boolean hasDeclaredValue(Element parent, String name, String namespace) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element element && name.equals(element.getLocalName())
					&& Objects.equals(namespace, element.getNamespaceURI()))
				return !element.getTextContent().isBlank();
		}
		return false;
	}
}
