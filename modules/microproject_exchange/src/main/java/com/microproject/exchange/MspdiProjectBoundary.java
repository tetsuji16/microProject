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
import javax.xml.parsers.ParserConfigurationException;

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

	/**
	 * Creates the XML parser configuration shared by every exchange reader: no DOCTYPE, no
	 * external entities, no XInclude and no external DTD/schema access.
	 */
	public static DocumentBuilderFactory secureDocumentBuilderFactory(boolean namespaceAware)
			throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		return factory;
	}

	public static Document parseSecureDocument(byte[] xml) throws Exception {
		return secureDocumentBuilderFactory(true).newDocumentBuilder().parse(new ByteArrayInputStream(xml));
	}

	/** Parses an MSPDI wall-clock timestamp as UTC epoch milliseconds; malformed text throws a {@code DateTimeException}. */
	public static long parseWallTime(String value) {
		return java.time.LocalDateTime.parse(value).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
	}

	/** Matches by local name (or tag name for non-namespace-aware nodes) and namespace. */
	public static boolean isNamedElement(Element element, String name, String namespace) {
		String localName = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
		return name.equals(localName) && Objects.equals(namespace, element.getNamespaceURI());
	}

	/** Text of the first direct child element with the given name and namespace, or {@code null}. */
	public static String childText(Element parent, String name, String namespace) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element element && isNamedElement(element, name, namespace))
				return element.getTextContent();
		}
		return null;
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
