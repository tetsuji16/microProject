/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class MspdiProjectBoundaryXmlTest {
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void secureFactoryRejectsDoctypeAndBlocksExternalAccess(boolean namespaceAware) throws Exception {
		DocumentBuilderFactory factory = MspdiProjectBoundary.secureDocumentBuilderFactory(namespaceAware);

		assertEquals(namespaceAware, factory.isNamespaceAware());
		assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD));
		assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA));
		assertTrue(!factory.isXIncludeAware() && !factory.isExpandEntityReferences());
		byte[] hostile = "<!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>".getBytes(StandardCharsets.UTF_8);
		assertThrows(org.xml.sax.SAXParseException.class,
			() -> factory.newDocumentBuilder().parse(new ByteArrayInputStream(hostile)));
	}

	@Test
	void sharedHelpersMatchNamespacedChildrenAndParseWallTime() throws Exception {
		Document document = MspdiProjectBoundary.parseSecureDocument(
			"<Project xmlns=\"http://schemas.microsoft.com/project\"><UID>7</UID></Project>".getBytes(StandardCharsets.UTF_8));
		Element root = document.getDocumentElement();

		assertEquals("7", MspdiProjectBoundary.childText(root, "UID", root.getNamespaceURI()));
		assertNull(MspdiProjectBoundary.childText(root, "UID", null));
		assertEquals(86_400_000L, MspdiProjectBoundary.parseWallTime("1970-01-02T00:00:00"));
		assertThrows(java.time.DateTimeException.class, () -> MspdiProjectBoundary.parseWallTime("not a time"));
	}
}
