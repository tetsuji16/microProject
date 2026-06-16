package com.projectlibre1.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.projectlibre1.dialog.LicenseDialog;
import com.projectlibre1.help.HelpUtil;
import com.projectlibre1.menu.testsupport.MenuDefinitionSupport;
import com.projectlibre1.menu.testsupport.RibbonDuplicationAuditSupport;
import com.projectlibre1.menu.testsupport.RibbonIconAuditSupport;
import com.projectlibre1.menu.testsupport.RibbonInventory;
import com.projectlibre1.menu.testsupport.RibbonLinkAuditSupport;
import com.projectlibre1.util.UiLinkTargets;

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
		ExtRibbonFactory factory = new ExtRibbonFactory(
			MenuDefinitionSupport.stubActionMap(),
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
}
