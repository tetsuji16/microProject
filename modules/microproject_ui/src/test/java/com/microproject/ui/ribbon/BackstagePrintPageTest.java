/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ui.ribbon.backstage.BackstagePrintPage;
import com.microproject.ui.theme.MicroProjectTheme;

/** Issue #1118: the Backstage Print page shows settings and a preview without side effects. */
class BackstagePrintPageTest {
	private static final Map<String, String> LABELS = Map.of(
			"print", "Print", "noPrinters", "No printers available", "pageSetup", "Page Setup",
			"copies", "Copies", "printer", "Printer", "range", "Print range",
			"orientation", "Orientation", "paperSize", "Paper size",
			"loadingPrinters", "Looking for printers...");

	private static final List<String> RANGES = List.of("All", "Current page", "Selection");
	private static final List<String> ORIENTATIONS = List.of("Portrait", "Landscape");
	private static final List<String> PAPER = List.of("A4", "Letter", "Legal");

	private static final class RecordingActions implements BackstagePrintPage.Actions {
		final AtomicInteger printCalls = new AtomicInteger();
		final AtomicInteger setupCalls = new AtomicInteger();
		final AtomicInteger settingsCalls = new AtomicInteger();
		final AtomicBoolean documentModified = new AtomicBoolean();

		public void print() {
			printCalls.incrementAndGet();
			documentModified.set(true); // a real print job touches document/page state
		}
		@Override public void openPageSetup() { setupCalls.incrementAndGet(); }
		@Override public void settingsChanged() { settingsCalls.incrementAndGet(); }
	}

	private static BackstagePrintPage.Settings settings(List<String> printers) {
		return new BackstagePrintPage.Settings(2, printers, printers.isEmpty() ? "" : printers.getFirst(),
				RANGES, RANGES.getFirst(), ORIENTATIONS, ORIENTATIONS.getFirst(), PAPER, PAPER.getFirst());
	}

	private static RecordingActions actions() { return new RecordingActions(); }

	private static JButton printButton(RecordingActions actions) {
		JButton button = new JButton("Print");
		button.setName("officeBackstagePrintButton");
		button.addActionListener(event -> actions.print());
		return button;
	}

	@Test
	void noPrintersShowsMessageDisablesPrintAndStillShowsPreview() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			JLabel preview = new JLabel("preview");
			BackstagePrintPage page = new BackstagePrintPage(settings(List.of()), printButton(actions), preview, actions, LABELS::get);

			assertTrue(texts(page).contains("No printers available"), texts(page).toString());
			assertFalse(page.getPrintButton().isEnabled(), "Print must be disabled without a printer");
			assertFalse(page.getPrinterCombo().isEnabled(), "Printer field must be disabled without a printer");
			assertSame(preview, page.getPreviewComponent(), "preview must still be supplied when there is no printer");
			assertNotNull(named(page, "officeBackstagePrintPreviewPane"));
			assertEquals(0, actions.printCalls.get());
			assertFalse(actions.documentModified.get(), "no error path / document mutation on no-printer state");
		});
	}

	@Test
	void narrowPageMovesThePreviewUnderTheSettingsInsteadOfPushingItOutOfView() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			JPanel preview = new JPanel(new java.awt.BorderLayout());
			JPanel controls = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 4));
			controls.add(new JButton("<"));
			controls.add(new JButton(">"));
			JButton previewButton = new JButton("Preview");
			controls.add(previewButton);
			preview.add(controls, java.awt.BorderLayout.NORTH);
			JPanel pageImage = new JPanel();
			pageImage.setPreferredSize(new java.awt.Dimension(600, 800));
			preview.add(pageImage, java.awt.BorderLayout.CENTER);
			BackstagePrintPage page = new BackstagePrintPage(settings(List.of("Printer One")),
					printButton(actions), preview, actions, LABELS::get);
			Component pane = named(page, "officeBackstagePrintPreviewPane");

			// 683 logical px at 150 % leaves about 380 px beside the Backstage navigation.
			layoutTree(page, 380, 420);
			java.awt.Rectangle narrow = SwingUtilities.convertRectangle(previewButton.getParent(),
					previewButton.getBounds(), page);
			assertTrue(new java.awt.Rectangle(page.getSize()).contains(narrow),
					"Preview must stay inside a narrow Print page: " + narrow + " page=" + page.getSize());
			java.awt.Rectangle settingsBounds = named(page, "officeBackstagePrintSettings").getBounds();
			assertTrue(pane.getY() >= settingsBounds.y + settingsBounds.height,
					"a narrow page must place the preview under the settings: pane=" + pane.getBounds());

			layoutTree(page, 900, 420);
			assertTrue(pane.getX() >= settingsBounds.width,
					"a wide page must keep the preview beside the settings: pane=" + pane.getBounds());
		});
	}

	/** Issue #1293: the page is usable before the printer list and the print document exist. */
	@Test
	void printersLoadingShowsTheLoadingTextAndKeepsPrintingDisabled() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			BackstagePrintPage page = new BackstagePrintPage(settings(List.of()), printButton(actions),
					new JPanel(), actions, LABELS::get);

			page.showPrintersLoading();

			assertTrue(texts(page).contains("Looking for printers..."), texts(page).toString());
			assertFalse(page.getPrinterCombo().isEnabled(), "the printer field waits for the list");
			assertFalse(page.getPrintButton().isEnabled(), "printing needs a printer");
			assertEquals(0, actions.printCalls.get());
		});
	}

	@Test
	void printersArrivingAfterThePageIsShownFillTheFieldAndEnablePrinting() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			BackstagePrintPage page = new BackstagePrintPage(settings(List.of()), printButton(actions),
					new JPanel(), actions, LABELS::get);
			page.showPrintersLoading();

			page.setPrinters(List.of("Printer One", "Printer Two"), "Printer Two");

			assertTrue(page.getPrinterCombo().isEnabled());
			assertTrue(page.getPrintButton().isEnabled());
			assertEquals("Printer Two", page.getPrinterCombo().getSelectedItem());
			assertFalse(texts(page).contains("Looking for printers..."), texts(page).toString());
			assertFalse(texts(page).contains("No printers available"), texts(page).toString());
		});
	}

	@Test
	void thePreviewPlaceholderIsReplacedInPlaceWhenTheDocumentIsReady() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			JLabel placeholder = new JLabel("Preparing the preview...");
			BackstagePrintPage page = new BackstagePrintPage(settings(List.of("Printer One")),
					printButton(actions), placeholder, actions, LABELS::get);

			JPanel canvas = new JPanel();
			canvas.setName("officeBackstagePrintPreviewCanvas");
			page.setPreviewComponent(canvas);

			assertSame(canvas, page.getPreviewComponent());
			assertFalse(UiComponentWalker.flatten(page).contains(placeholder),
					"the placeholder leaves the page once the preview exists");
			layoutTree(page, 900, 420);
			assertTrue(canvas.getWidth() > 0 && canvas.getHeight() > 0,
					"the preview fills the pane: " + canvas.getBounds());
		});
	}

	private static void layoutTree(Container root, int width, int height) {
		root.setSize(width, height);
		layoutDescendants(root);
	}

	private static void layoutDescendants(Container container) {
		container.doLayout();
		for (Component child : container.getComponents())
			if (child instanceof Container nested) layoutDescendants(nested);
	}

	@Test
	void withPrintersThePrintButtonIsEnabledAndValuesAreShown() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			BackstagePrintPage page = new BackstagePrintPage(settings(List.of("HP LaserJet", "Microsoft Print to PDF")),
					printButton(actions), new JPanel(), actions, LABELS::get);
			assertTrue(page.getPrintButton().isEnabled());
			assertTrue(page.getPrinterCombo().isEnabled());
			List<String> visible = texts(page);
			assertTrue(visible.contains("HP LaserJet"), visible.toString());
			assertTrue(visible.contains("A4"), visible.toString());
			assertTrue(visible.contains("Portrait"), visible.toString());
		});
	}

	@Test
	void printButtonInvokesOnlyTheSuppliedPrintCommand() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			BackstagePrintPage page = new BackstagePrintPage(settings(List.of("Printer One")),
					printButton(actions), new JPanel(), actions, LABELS::get);
			page.getPrintButton().doClick();
			assertEquals(1, actions.printCalls.get());
			assertEquals(0, actions.setupCalls.get());
			assertEquals(0, actions.settingsCalls.get());
			assertTrue(actions.documentModified.get());
		});
	}

	@Test
	void pageSetupLinkOpensTheSuppliedDialogAction() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			BackstagePrintPage page = new BackstagePrintPage(settings(List.of("Printer One")),
					printButton(actions), new JPanel(), actions, LABELS::get);
			page.getPageSetupLink().doClick();
			assertEquals(1, actions.setupCalls.get());
			assertEquals(0, actions.printCalls.get());
		});
	}

	@Test
	void openingThePageCreatesNoPrintJobAndDoesNotModifyTheDocument() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			new BackstagePrintPage(settings(List.of("Printer One")), printButton(actions), new JPanel(), actions, LABELS::get);
			assertEquals(0, actions.printCalls.get(), "opening must not create a print job");
			assertEquals(0, actions.settingsCalls.get(), "opening must not change settings");
			assertEquals(0, actions.setupCalls.get(), "opening must not show a dialog");
			assertFalse(actions.documentModified.get(), "opening must not set the modified flag");
		});
	}

	@Test
	void changingSettingsRefreshesPreviewWithoutMutatingProjectData() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecordingActions actions = actions();
			BackstagePrintPage page = new BackstagePrintPage(settings(List.of("Printer One")),
					printButton(actions), new JPanel(), actions, LABELS::get);
			page.getOrientationCombo().setSelectedItem("Landscape");
			page.getPaperSizeCombo().setSelectedItem("Letter");
			page.getRangeCombo().setSelectedItem("Selection");
			fireAction(page.getPrinterCombo());
			page.getCopiesField().setValue(4);

			assertTrue(actions.settingsCalls.get() >= 5, "each setting change refreshes the preview: " + actions.settingsCalls);
			assertEquals(0, actions.printCalls.get(), "changing settings must not print");
			assertEquals(0, actions.setupCalls.get(), "changing settings must not open a dialog");
			assertFalse(actions.documentModified.get(), "changing settings must not mutate project data");
			assertEquals("Landscape", page.getOrientationCombo().getSelectedItem());
			assertEquals(4, page.getCopiesField().getValue());
		});
	}

	@Test
	void printPageIsWiredIntoTheFullBackstageAndOtherPagesSurvive() throws Exception {
		MicroProjectTheme.installLight();
		SwingUtilities.invokeAndWait(() -> {
			JComponent backstage = openBackstage();
			((AbstractButton) named(backstage, "officeBackstageNav-print")).doClick(0);
			assertNotNull(named(backstage, "officeBackstagePage-print"));
			assertNotNull(named(backstage, "officeBackstagePrintPage"));
			assertNotNull(named(backstage, "officeBackstagePrintSettings"));
			assertNotNull(named(backstage, "officeBackstagePrintPreviewPane"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonPrint"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonPrintPreview"));
			assertTrue(UiComponentWalker.flatten(backstage).stream()
					.noneMatch(component -> "officeBackstageCommand-RibbonPDF".equals(component.getName())),
					"PDF export moved to the Export page and must not appear on Print");
			assertNotNull(named(backstage, "officeBackstagePrintPageSetup"));

			((AbstractButton) named(backstage, "officeBackstageNav-open")).doClick(0);
			assertNotNull(named(backstage, "officeBackstageOpenPage"));
			((AbstractButton) named(backstage, "officeBackstageNav-info")).doClick(0);
			assertNotNull(named(backstage, "officeBackstageInfoPage"));
			((AbstractButton) named(backstage, "officeBackstageNav-new")).doClick(0);
			assertNotNull(named(backstage, "officeBackstagePage-new"));
		});
	}

	private static void fireAction(JButton button) {
		for (ActionListener listener : button.getActionListeners()) listener.actionPerformed(new ActionEvent(button, 0, "change"));
	}

	private static void fireAction(JComponent component) {
		if (component instanceof JButton button) {
			fireAction(button);
			return;
		}
		if (component instanceof javax.swing.JComboBox<?> combo)
			for (ActionListener listener : combo.getActionListeners())
				listener.actionPerformed(new ActionEvent(combo, ActionEvent.ACTION_PERFORMED, "change"));
	}

	private static List<String> texts(Component root) {
		List<String> result = new ArrayList<>();
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof JLabel label) result.add(label.getText());
			if (component instanceof AbstractButton button) result.add(button.getText());
			if (component instanceof JComboBox<?> combo && combo.getSelectedItem() != null)
				result.add(String.valueOf(combo.getSelectedItem()));
		}
		return result;
	}

	private static JComponent named(Component root, String name) {
		return UiComponentWalker.flatten(root).stream()
				.filter(JComponent.class::isInstance).map(JComponent.class::cast)
				.filter(component -> name.equals(component.getName())).findFirst()
				.orElseThrow(() -> new AssertionError("missing component: " + name));
	}

	private static JComponent openBackstage() {
		SwingRibbonFactory factory = new SwingRibbonFactory(
				new MenuRibbonCommandSource(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
						MenuDefinitionSupport.ribbonBundles(java.util.Locale.ROOT))),
				MenuDefinitionSupport.ribbonBundles(java.util.Locale.ROOT));
		JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		List<JComponent> shown = new ArrayList<>();
		ribbon.setBackstageHost(new RibbonBackstageHost() {
			@Override public void show(JComponent backstageView, Runnable dismiss) { shown.add(backstageView); }
			@Override public void hide() { }
		});
		String fileTitle = MenuDefinitionSupport.menuBundle(java.util.Locale.ROOT).getString("FileRibbonTask.title");
		UiComponentWalker.flatten(host).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> fileTitle.equals(button.getText()) && button.getName() != null
						&& button.getName().startsWith("microProjectRibbonTab-")).findFirst().orElseThrow().doClick();
		return shown.getFirst();
	}
}
