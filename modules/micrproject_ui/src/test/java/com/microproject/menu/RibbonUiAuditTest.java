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
package com.microproject.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.dialog.LicenseDialog;
import com.microproject.help.HelpUtil;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.RibbonDuplicationAuditSupport;
import com.microproject.menu.testsupport.RibbonIconAuditSupport;
import com.microproject.menu.testsupport.RibbonInventory;
import com.microproject.menu.testsupport.RibbonLinkAuditSupport;
import com.microproject.pm.graphic.IconManager;
import com.microproject.util.UiLinkTargets;

class RibbonUiAuditTest {
	private final RibbonInventory inventory = RibbonInventory.standardRibbon();

	@Test
	void standardRibbonInventoryCoversEveryKnownRibbonButton() {
		assertEquals(MenuDefinitionSupport.ribbonButtonIds(), inventory.buttonIds());
	}

	@Test
	void standardRibbonButtonsWithIconsHaveMappingsAndResources() {
		assertTrue(RibbonIconAuditSupport.missingIconMappings(inventory).isEmpty(),
			() -> "Missing icon mappings: " + RibbonIconAuditSupport.missingIconMappings(inventory));
		assertTrue(RibbonIconAuditSupport.missingResources(inventory).isEmpty(),
			() -> "Missing icon resources: " + RibbonIconAuditSupport.missingResources(inventory));
	}

	@Test
	void standardRibbonButtonsWithConfiguredIconsCreateVisibleRibbonIcons() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(java.util.Locale.ROOT));
		javax.swing.SwingUtilities.invokeAndWait(() -> {
			for (RibbonInventory.ButtonSpec spec : inventory.buttons().values()) {
				if (!spec.requiresIcon()) {
					continue;
				}
				var button = factory.createJButton(spec.id());
				assertNotNull(button.getIcon(), () -> spec.id() + " did not create a ribbon icon");
				assertTrue(button.getIcon().getIconWidth() > 0, () -> spec.id() + " ribbon icon width was not positive");
				assertTrue(button.getIcon().getIconHeight() > 0, () -> spec.id() + " ribbon icon height was not positive");
			}
		});
	}

	@Test
	void standardRibbonIconsRemainReadableAtAllRibbonSizes() {
		for (RibbonInventory.ButtonSpec spec : inventory.buttons().values()) {
			if (!spec.requiresIcon()) continue;
			for (int size : java.util.List.of(16, 20, 32)) {
				var icon = IconManager.getRibbonIcon(spec.iconKey(), size, size);
				assertNotNull(icon, () -> spec.id() + " has no " + size + "px ribbon icon");
				assertEquals(size, icon.getIconWidth(), () -> spec.id() + " icon width at " + size + "px");
				assertEquals(size, icon.getIconHeight(), () -> spec.id() + " icon height at " + size + "px");
			}
		}
	}

	@Test
	void standardRibbonButtonsPreferSvgAssets() {
		assertTrue(RibbonIconAuditSupport.missingSvgResources(inventory).isEmpty(),
			() -> "Missing SVG resources: " + RibbonIconAuditSupport.missingSvgResources(inventory));
	}

	@Test
	void standardRibbonHasNoDuplicateButtonsOrIconKeysWithinATask() {
		assertTrue(RibbonDuplicationAuditSupport.duplicateButtonIdsByTask(inventory).isEmpty(),
			() -> "Duplicate button IDs: " + RibbonDuplicationAuditSupport.duplicateButtonIdsByTask(inventory));
		assertTrue(RibbonDuplicationAuditSupport.duplicateIconKeysByTask(inventory).isEmpty(),
			() -> "Duplicate icon keys: " + RibbonDuplicationAuditSupport.duplicateIconKeysByTask(inventory));
	}

	@Test
	void helpUrlsAreNormalizedToCurrentDocumentationTargets() {
		assertEquals(UiLinkTargets.DOCUMENTATION_HOME, HelpUtil.getHelpURL(null));
		assertEquals(UiLinkTargets.DOCUMENTATION_HOME, HelpUtil.getHelpURL(""));
		assertEquals("https://example.com/help", HelpUtil.getHelpURL("https://example.com/help"));
		assertTrue(HelpUtil.getHelpURL("Task_Information_Dialog").startsWith(UiLinkTargets.DOCUMENTATION_HOME + "?topic="));
	}

	@Test
	void bundledThirdPartyLicensePageIsUsed() {
		URL url = LicenseDialog.resolveThirdPartyLicenseUrl();
		assertNotNull(url);
		assertTrue(url.toExternalForm().contains("license/third-party/index.html"));
	}

	@Test
	void majorHelpAndProjectLinksRespondSuccessfully() throws Exception {
		for (Map.Entry<String, String> entry : RibbonLinkAuditSupport.majorLinks().entrySet()) {
			int status = RibbonLinkAuditSupport.fetchStatus(entry.getValue());
			assertTrue(status >= 200 && status < 400, () -> entry.getKey() + " returned " + status);
		}
	}

	@Test
	void noKnownInformationButtonsFallbackToSharedInformationAction() {
		assertFalse(hasLegacyInformationAction(ResourceBundle.getBundle("com.microproject.menu.menuInternal"), "RibbonTaskInformation"));
		assertFalse(hasLegacyInformationAction(ResourceBundle.getBundle("com.microproject.menu.menuInternal"), "RibbonResourceInformation"));
		assertFalse(hasLegacyInformationAction(ResourceBundle.getBundle("com.microproject.menu.menu_ta"), "RibbonTaskInformation"));
		assertFalse(hasLegacyInformationAction(ResourceBundle.getBundle("com.microproject.menu.menu_ta"), "RibbonResourceInformation"));
		assertFalse(resourceContains("com/microproject/menu/nativeCodePage/menu_zh_CN.properties", "Tool24TaskInformation.action\t=InformationAction"));
		assertFalse(resourceContains("com/microproject/menu/nativeCodePage/menu_zh_CN.properties", "Tool24ResourceInformation.action\t=InformationAction"));
		assertFalse(resourceContains("com/microproject/menu/nativeCodePage/menu_ru.properties.1251", "Tool24TaskInformation.action\t=InformationAction"));
		assertFalse(resourceContains("com/microproject/menu/nativeCodePage/menu_ru.properties.1251", "Tool24ResourceInformation.action\t=InformationAction"));
	}

	private static boolean hasLegacyInformationAction(ResourceBundle bundle, String key) {
		return "InformationAction".equals(bundle.getString(key + ".action"));
	}

	private static boolean resourceContains(String path, String needle) {
		try (var stream = RibbonUiAuditTest.class.getClassLoader().getResourceAsStream(path)) {
			assertNotNull(stream, () -> "Missing resource " + path);
			String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			return text.contains(needle);
		} catch (java.io.IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
