/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.microproject.ui.ribbon.RibbonCustomization.CustomGroup;
import com.microproject.ui.ribbon.RibbonCustomization.CustomTab;

/**
 * Exports and imports the ribbon customization and the Quick Access Toolbar as a {@code .mpui} file: UTF-8 XML whose
 * root carries {@code format="microProject-ribbon-customization"} and {@code version="1"}. Reading never touches the
 * stored preferences; a file that cannot be used is rejected with a {@link Reason}. Unknown command IDs are dropped,
 * like the model does, and counted in the result. Microsoft Project's {@code .exportedUI} files name commands by
 * idMso, which has no mapping to the canonical IDs, so they are refused.
 */
public final class RibbonCustomizationFile {
	public static final String FORMAT = "microProject-ribbon-customization";
	public static final String VERSION = "1";
	public static final String EXTENSION = "mpui";
	public static final int MAX_BYTES = 1024 * 1024;

	/** Why a file was refused. */
	public enum Reason { UNREADABLE, TOO_LARGE, BROKEN_XML, MICROSOFT_PROJECT_FILE, WRONG_FORMAT, UNKNOWN_VERSION, INVALID_CONTENT }

	/** A file that was refused; {@link #reason()} says why. */
	public static final class RejectedException extends Exception {
		private static final long serialVersionUID = 1L;
		private final Reason reason;

		RejectedException(Reason reason, String detail, Throwable cause) {
			super(reason + (detail == null || detail.isBlank() ? "" : ": " + detail), cause);
			this.reason = reason;
		}

		public Reason reason() { return reason; }
	}

	/** What a file holds. {@code droppedCommandIds} counts command IDs that this version does not know. */
	public record Content(RibbonCustomization customization, List<String> quickAccess, int droppedCommandIds) { }

	private RibbonCustomizationFile() { }

	public static void write(Path path, RibbonCustomization customization, List<String> quickAccess) throws IOException {
		try {
			Document document = newBuilder().newDocument();
			Element root = document.createElement("ribbonCustomization");
			root.setAttribute("format", FORMAT);
			root.setAttribute("version", VERSION);
			document.appendChild(root);
			Element order = child(document, root, "defaultTabOrder");
			for (String tabId : customization.defaultTabOrder()) child(document, order, "tab").setAttribute("id", tabId);
			Element hidden = child(document, root, "hiddenTabs");
			for (String tabId : new TreeSet<>(customization.hiddenDefaultTabs())) {
				child(document, hidden, "tab").setAttribute("id", tabId);
			}
			Element customTabs = child(document, root, "customTabs");
			for (CustomTab tab : customization.customTabs()) {
				Element element = child(document, customTabs, "tab");
				element.setAttribute("id", tab.id());
				element.setAttribute("name", tab.name());
				for (CustomGroup group : tab.groups()) appendGroup(document, element, group);
			}
			Element defaults = child(document, root, "defaultTabGroups");
			for (Map.Entry<String, List<CustomGroup>> entry : new TreeMap<>(customization.defaultTabGroups()).entrySet()) {
				Element element = child(document, defaults, "tab");
				element.setAttribute("id", entry.getKey());
				for (CustomGroup group : entry.getValue()) appendGroup(document, element, group);
			}
			Element toolbar = child(document, root, "quickAccess");
			for (String commandId : quickAccess) child(document, toolbar, "command").setAttribute("id", commandId);

			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			StringWriter text = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(text));
			Files.writeString(path, text.toString(), StandardCharsets.UTF_8);
		} catch (ParserConfigurationException | TransformerException failure) {
			throw new IOException("Cannot write the ribbon customization: " + failure.getMessage(), failure);
		}
	}

	public static Content read(Path path) throws RejectedException {
		byte[] bytes;
		try (InputStream in = Files.newInputStream(path)) {
			bytes = in.readNBytes(MAX_BYTES + 1);
		} catch (IOException failure) {
			throw new RejectedException(Reason.UNREADABLE, failure.getMessage(), failure);
		}
		if (bytes.length > MAX_BYTES) throw new RejectedException(Reason.TOO_LARGE, null, null);
		Document document;
		try {
			document = newBuilder().parse(new ByteArrayInputStream(bytes));
		} catch (SAXException | IOException | ParserConfigurationException broken) {
			throw new RejectedException(Reason.BROKEN_XML, broken.getMessage(), broken);
		}
		Element root = document.getDocumentElement();
		if ("customUI".equals(root.getLocalName())) throw new RejectedException(Reason.MICROSOFT_PROJECT_FILE, null, null);
		if (!"ribbonCustomization".equals(root.getTagName()) || !FORMAT.equals(root.getAttribute("format"))) {
			throw new RejectedException(Reason.WRONG_FORMAT, root.getTagName(), null);
		}
		if (!VERSION.equals(root.getAttribute("version"))) {
			throw new RejectedException(Reason.UNKNOWN_VERSION, root.getAttribute("version"), null);
		}
		try {
			return parse(root);
		} catch (RuntimeException invalid) {
			throw new RejectedException(Reason.INVALID_CONTENT, invalid.getMessage(), invalid);
		}
	}

	private static Content parse(Element root) {
		int[] dropped = new int[1];
		List<String> order = new ArrayList<>();
		Set<String> hidden = new LinkedHashSet<>();
		for (Element tab : children(first(root, "defaultTabOrder"), "tab")) order.add(attribute(tab, "id"));
		for (Element tab : children(first(root, "hiddenTabs"), "tab")) hidden.add(attribute(tab, "id"));
		if (order.contains(RibbonCustomization.FILE_TAB_ID) || hidden.contains(RibbonCustomization.FILE_TAB_ID)) {
			throw new IllegalArgumentException("The File tab cannot be hidden or moved");
		}
		List<CustomTab> customTabs = new ArrayList<>();
		for (Element tab : children(first(root, "customTabs"), "tab")) {
			customTabs.add(new CustomTab(attribute(tab, "id"), attribute(tab, "name"), groups(tab, dropped)));
		}
		Map<String, List<CustomGroup>> defaultGroups = new LinkedHashMap<>();
		for (Element tab : children(first(root, "defaultTabGroups"), "tab")) {
			String tabId = attribute(tab, "id");
			if (RibbonCustomization.FILE_TAB_ID.equals(tabId)) {
				throw new IllegalArgumentException("The File tab cannot hold custom groups");
			}
			if (defaultGroups.put(tabId, groups(tab, dropped)) != null) {
				throw new IllegalArgumentException("Duplicate tab entry: " + tabId);
			}
		}
		LinkedHashSet<String> toolbar = new LinkedHashSet<>();
		for (Element command : children(first(root, "quickAccess"), "command")) {
			String id = attribute(command, "id");
			if (RibbonCommandCatalog.quickAccessCandidates().contains(id)) toolbar.add(id);
			else dropped[0]++;
		}
		RibbonCustomization customization = new RibbonCustomization(order, hidden, customTabs, defaultGroups);
		return new Content(customization, List.copyOf(toolbar), dropped[0]);
	}

	private static List<CustomGroup> groups(Element tab, int[] dropped) {
		List<CustomGroup> groups = new ArrayList<>();
		for (Element group : children(tab, "group")) {
			List<String> commands = new ArrayList<>();
			for (Element command : children(group, "command")) {
				String id = attribute(command, "id");
				if (RibbonCommandCatalog.quickAccessCandidates().contains(id)) commands.add(id);
				else dropped[0]++;
			}
			groups.add(new CustomGroup(attribute(group, "id"), attribute(group, "name"), commands));
		}
		return groups;
	}

	private static String attribute(Element element, String name) {
		if (!element.hasAttribute(name)) {
			throw new IllegalArgumentException("<" + element.getTagName() + "> needs the attribute " + name);
		}
		return element.getAttribute(name);
	}

	private static Element first(Element parent, String name) {
		List<Element> found = children(parent, name);
		return found.isEmpty() ? null : found.get(0);
	}

	private static List<Element> children(Element parent, String name) {
		List<Element> found = new ArrayList<>();
		if (parent == null) return found;
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && name.equals(element.getTagName())) found.add(element);
		}
		return found;
	}

	private static void appendGroup(Document document, Element parent, CustomGroup group) {
		Element element = child(document, parent, "group");
		element.setAttribute("id", group.id());
		element.setAttribute("name", group.name());
		for (String commandId : group.commandIds()) child(document, element, "command").setAttribute("id", commandId);
	}

	private static Element child(Document document, Element parent, String name) {
		Element element = document.createElement(name);
		parent.appendChild(element);
		return element;
	}

	private static DocumentBuilder newBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		return factory.newDocumentBuilder();
	}
}
