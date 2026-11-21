/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.ui.ribbon.RibbonCustomization.CustomGroup;
import com.microproject.ui.ribbon.RibbonCustomization.CustomTab;
import com.microproject.ui.ribbon.RibbonCustomizationFile.Reason;

class RibbonCustomizationFileTest {
	private static final String KEY = "ribbonCustomization";
	private static final String QAT_KEY = "ribbonQuickAccessCommands";
	private static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private static final String ROOT_OPEN = "<ribbonCustomization format=\"microProject-ribbon-customization\" version=\"1\">";

	private final Preferences preferences = Preferences.userNodeForPackage(RibbonDisplayPreferences.class);
	private String previousValue;
	private String previousQuickAccess;

	@TempDir
	Path directory;

	@BeforeEach
	void isolatePreferences() {
		previousValue = preferences.get(KEY, null);
		previousQuickAccess = preferences.get(QAT_KEY, null);
		preferences.remove(KEY);
		preferences.remove(QAT_KEY);
	}

	@AfterEach
	void restorePreferences() {
		restore(KEY, previousValue);
		restore(QAT_KEY, previousQuickAccess);
	}

	private void restore(String key, String value) {
		if (value == null) preferences.remove(key);
		else preferences.put(key, value);
	}

	private static RibbonCustomization sample() {
		CustomGroup onTask = new CustomGroup("g-task", "My group 日本 <&> \"x\"", List.of("RibbonFind", "RibbonScrollToToday"));
		CustomGroup inCustom = new CustomGroup("g-custom", "Quick", List.of("RibbonSaveProject"));
		return new RibbonCustomization(List.of("ViewRibbonTask", "TaskRibbonTask"), Set.of("HelpRibbonTask"),
			List.of(new CustomTab("my-tab", "My Tab", List.of(inCustom))),
			Map.of("TaskRibbonTask", List.of(onTask)));
	}

	private Path file(String content) throws IOException {
		Path path = directory.resolve("input.mpui");
		Files.writeString(path, content, StandardCharsets.UTF_8);
		return path;
	}

	private void assertRejected(Reason reason, Path path) {
		RibbonCustomizationFile.RejectedException rejected =
			assertThrows(RibbonCustomizationFile.RejectedException.class, () -> RibbonCustomizationFile.read(path));
		assertEquals(reason, rejected.reason(), rejected.getMessage());
	}

	@Test
	void writeThenReadRestoresTheSameContent() throws Exception {
		Path path = directory.resolve("out.mpui");
		List<String> quickAccess = List.of("RibbonFind", "RibbonScrollToToday");
		RibbonCustomizationFile.write(path, sample(), quickAccess);
		String text = Files.readString(path, StandardCharsets.UTF_8);
		assertTrue(text.contains("format=\"microProject-ribbon-customization\""), text);
		assertTrue(text.contains("version=\"1\""), text);

		RibbonCustomizationFile.Content content = RibbonCustomizationFile.read(path);
		assertEquals(sample(), content.customization());
		assertEquals(quickAccess, content.quickAccess());
		assertEquals(0, content.droppedCommandIds());
	}

	@Test
	void anEmptyCustomizationRoundTrips() throws Exception {
		Path path = directory.resolve("empty.mpui");
		RibbonCustomizationFile.write(path, RibbonCustomization.none(), List.of());
		RibbonCustomizationFile.Content content = RibbonCustomizationFile.read(path);
		assertTrue(content.customization().isEmpty());
		assertEquals(List.of(), content.quickAccess());
	}

	@Test
	void brokenXmlIsRejected() throws Exception {
		assertRejected(Reason.BROKEN_XML, file(HEADER + ROOT_OPEN + "<customTabs>"));
	}

	@Test
	void aWrongFormatIsRejected() throws Exception {
		assertRejected(Reason.WRONG_FORMAT,
			file(HEADER + "<ribbonCustomization format=\"something-else\" version=\"1\"/>"));
		assertRejected(Reason.WRONG_FORMAT, file(HEADER + "<other format=\"microProject-ribbon-customization\" version=\"1\"/>"));
	}

	@Test
	void anUnknownVersionIsRejected() throws Exception {
		assertRejected(Reason.UNKNOWN_VERSION,
			file(HEADER + "<ribbonCustomization format=\"microProject-ribbon-customization\" version=\"2\"/>"));
	}

	@Test
	void aMicrosoftProjectFileIsRejectedWithItsOwnReason() throws Exception {
		assertRejected(Reason.MICROSOFT_PROJECT_FILE, file(HEADER
			+ "<mso:customUI xmlns:mso=\"http://schemas.microsoft.com/office/2009/07/customui\"><mso:ribbon/></mso:customUI>"));
		assertRejected(Reason.MICROSOFT_PROJECT_FILE, file(HEADER
			+ "<customUI xmlns=\"http://schemas.microsoft.com/office/2009/07/customui\"><ribbon/></customUI>"));
	}

	@Test
	void aDoctypeIsRejectedAndNoExternalEntityIsRead() throws Exception {
		Path secret = directory.resolve("secret.txt");
		Files.writeString(secret, "top-secret", StandardCharsets.UTF_8);
		String entity = "<!DOCTYPE ribbonCustomization [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>";
		assertRejected(Reason.BROKEN_XML,
			file(HEADER + entity + "<ribbonCustomization format=\"microProject-ribbon-customization\" version=\"1\">"
				+ "<customTabs><tab id=\"a\" name=\"&xxe;\"/></customTabs></ribbonCustomization>"));
		assertRejected(Reason.BROKEN_XML, file(HEADER + "<!DOCTYPE ribbonCustomization>" + ROOT_OPEN + "</ribbonCustomization>"));
	}

	@Test
	void aFileOverOneMebibyteIsRejected() throws Exception {
		String padding = " ".repeat(RibbonCustomizationFile.MAX_BYTES);
		assertRejected(Reason.TOO_LARGE, file(HEADER + ROOT_OPEN + "<!--" + padding + "--></ribbonCustomization>"));
	}

	@Test
	void unknownCommandIdsAreDroppedAndCounted() throws Exception {
		Path path = file(HEADER + ROOT_OPEN
			+ "<customTabs><tab id=\"t\" name=\"T\"><group id=\"g\" name=\"G\">"
			+ "<command id=\"RibbonFind\"/><command id=\"NoSuchCommand\"/></group></tab></customTabs>"
			+ "<quickAccess><command id=\"RibbonScrollToToday\"/><command id=\"Gone1\"/><command id=\"Gone2\"/></quickAccess>"
			+ "</ribbonCustomization>");
		RibbonCustomizationFile.Content content = RibbonCustomizationFile.read(path);
		assertEquals(3, content.droppedCommandIds());
		assertEquals(List.of("RibbonFind"), content.customization().customTabs().get(0).groups().get(0).commandIds());
		assertEquals(List.of("RibbonScrollToToday"), content.quickAccess());
	}

	@Test
	void modelViolationsAreRejected() throws Exception {
		assertRejected(Reason.INVALID_CONTENT, file(HEADER + ROOT_OPEN
			+ "<hiddenTabs><tab id=\"FileRibbonTask\"/></hiddenTabs></ribbonCustomization>"));
		assertRejected(Reason.INVALID_CONTENT, file(HEADER + ROOT_OPEN
			+ "<customTabs><tab id=\"bad id!\" name=\"T\"/></customTabs></ribbonCustomization>"));
		assertRejected(Reason.INVALID_CONTENT, file(HEADER + ROOT_OPEN
			+ "<customTabs><tab id=\"t\" name=\" \"/></customTabs></ribbonCustomization>"));
		assertRejected(Reason.INVALID_CONTENT, file(HEADER + ROOT_OPEN
			+ "<customTabs><tab id=\"t\" name=\"A\"/><tab id=\"t\" name=\"B\"/></customTabs></ribbonCustomization>"));
	}

	@Test
	void aMissingFileIsRejectedAsUnreadable() {
		assertRejected(Reason.UNREADABLE, directory.resolve("missing.mpui"));
	}

	@Test
	void aRejectedFileLeavesTheStoredPreferencesUnchanged() throws Exception {
		RibbonDisplayPreferences.saveRibbonCustomization(sample());
		RibbonDisplayPreferences.saveQuickAccessCommands(List.of("RibbonFind"));
		String customization = preferences.get(KEY, null);
		String quickAccess = preferences.get(QAT_KEY, null);

		assertRejected(Reason.BROKEN_XML, file("not xml"));
		assertRejected(Reason.UNKNOWN_VERSION,
			file(HEADER + "<ribbonCustomization format=\"microProject-ribbon-customization\" version=\"9\"/>"));
		assertRejected(Reason.INVALID_CONTENT, file(HEADER + ROOT_OPEN
			+ "<hiddenTabs><tab id=\"FileRibbonTask\"/></hiddenTabs></ribbonCustomization>"));

		assertEquals(customization, preferences.get(KEY, null));
		assertEquals(quickAccess, preferences.get(QAT_KEY, null));
		assertEquals(sample(), RibbonDisplayPreferences.loadRibbonCustomization());
	}
}
