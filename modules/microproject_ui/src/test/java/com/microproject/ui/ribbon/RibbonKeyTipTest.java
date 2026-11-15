/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ribbon.SwingRibbonModel;

/** The Alt key sequence: tab KeyTips, then the KeyTips of the controls of the chosen tab. */
class RibbonKeyTipTest {
	@Test
	void tabLetterThenControlLetterRunsTheControlAndEndsTheSequence() {
		List<String> log = new ArrayList<>();
		RibbonKeyTipSession session = session(log);

		session.toggle();
		assertEquals(RibbonKeyTipSession.Level.TABS, session.getLevel());
		assertEquals(List.of("H", "R"), List.copyOf(session.visibleTargets().keySet()));

		assertTrue(session.type('h'));
		assertEquals(RibbonKeyTipSession.Level.CONTROLS, session.getLevel());
		assertEquals(List.of("tab H"), log);
		assertEquals(List.of("V", "FP"), List.copyOf(session.visibleTargets().keySet()));

		session.type('V');
		assertEquals(List.of("tab H", "control paste"), log);
		assertFalse(session.isActive());
	}

	@Test
	void multiLetterKeyTipsNarrowDownBeforeRunning() {
		List<String> log = new ArrayList<>();
		RibbonKeyTipSession session = session(log);
		session.toggle();
		session.type('H');

		session.type('F');
		assertEquals(List.of("tab H"), log, "F alone is only a prefix of FP");
		assertEquals(List.of("FP"), List.copyOf(session.visibleTargets().keySet()));
		session.type('P');
		assertEquals(List.of("tab H", "control painter"), log);
		assertFalse(session.isActive());
	}

	@Test
	void escapeGoesBackOneLevelAtATime() {
		RibbonKeyTipSession session = session(new ArrayList<>());
		session.toggle();
		session.type('H');
		session.type('F');
		session.back();
		assertEquals("", session.getTyped());
		assertEquals(RibbonKeyTipSession.Level.CONTROLS, session.getLevel());
		session.back();
		assertEquals(RibbonKeyTipSession.Level.TABS, session.getLevel());
		session.back();
		assertEquals(RibbonKeyTipSession.Level.INACTIVE, session.getLevel());
	}

	@Test
	void unknownLettersAreSwallowedWithoutLeavingTheLevel() {
		List<String> log = new ArrayList<>();
		RibbonKeyTipSession session = session(log);
		assertFalse(session.type('H'), "an inactive sequence leaves keys alone");
		session.toggle();
		assertTrue(session.type('Z'));
		assertEquals(RibbonKeyTipSession.Level.TABS, session.getLevel());
		assertTrue(log.isEmpty());
	}

	@Test
	void aTabWithoutControlKeyTipsEndsTheSequence() {
		RibbonKeyTipSession session = session(new ArrayList<>());
		session.toggle();
		session.type('R');
		assertFalse(session.isActive());
	}

	@Test
	void altPressedAndReleasedAloneStartsTheSequenceButAltWithAKeyDoesNot() {
		RibbonKeyTipSession session = session(new ArrayList<>());
		RibbonKeyTipInput input = new RibbonKeyTipInput(session);

		assertFalse(input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_ALT));
		assertFalse(input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_H));
		assertFalse(input.dispatch(KeyEvent.KEY_RELEASED, KeyEvent.VK_H));
		assertFalse(input.dispatch(KeyEvent.KEY_RELEASED, KeyEvent.VK_ALT));
		assertFalse(session.isActive(), "Alt+H is a shortcut, not a KeyTip sequence");

		input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_ALT);
		assertTrue(input.dispatch(KeyEvent.KEY_RELEASED, KeyEvent.VK_ALT));
		assertTrue(session.isActive());

		assertTrue(input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_ESCAPE));
		assertFalse(session.isActive());
	}

	@Test
	void otherKeysCancelTheSequenceAndStillReachTheComponent() {
		RibbonKeyTipSession session = session(new ArrayList<>());
		RibbonKeyTipInput input = new RibbonKeyTipInput(session);
		session.toggle();
		assertFalse(input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_DOWN));
		assertFalse(session.isActive());
	}

	@Test
	void ctrlShortcutAfterALoneAltCancelsTheSequenceAndReachesTheComponent() {
		List<String> runs = new ArrayList<>();
		RibbonKeyTipSession session = session(runs);
		RibbonKeyTipInput input = new RibbonKeyTipInput(session);
		input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_ALT);
		input.dispatch(KeyEvent.KEY_RELEASED, KeyEvent.VK_ALT);
		assertTrue(session.isActive());
		int ctrl = KeyEvent.CTRL_DOWN_MASK;
		assertFalse(input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_CONTROL, ctrl));
		assertFalse(session.isActive());
		assertFalse(input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_Y, ctrl));
		assertFalse(input.dispatch(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, ctrl));
		assertFalse(input.dispatch(KeyEvent.KEY_RELEASED, KeyEvent.VK_Y, ctrl));
		assertTrue(runs.isEmpty());
	}

	@Test
	void ctrlLetterWhileTheSequenceIsShownIsNotTakenAsAKeyTip() {
		List<String> runs = new ArrayList<>();
		RibbonKeyTipSession session = session(runs);
		RibbonKeyTipInput input = new RibbonKeyTipInput(session);
		session.toggle();
		assertFalse(input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
		assertFalse(session.isActive());
		assertTrue(runs.isEmpty());
	}

	@Test
	void altThenTabLetterThenControlLetterClicksTheRibbonControl() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JPanel host = ribbonHost();
			ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
			AtomicInteger clicks = new AtomicInteger();
			AbstractButton copy = buttonByKeyTip(host, "C");
			copy.addActionListener(event -> clicks.incrementAndGet());
			RibbonKeyTipInput input = ribbon.keyTipInput();

			input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_ALT);
			input.dispatch(KeyEvent.KEY_RELEASED, KeyEvent.VK_ALT);
			assertEquals(RibbonKeyTipSession.Level.TABS, ribbon.keyTipSession().getLevel());
			assertTrue(ribbon.keyTipSession().visibleTargets().containsKey("H"));

			input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_H);
			assertEquals(RibbonKeyTipSession.Level.CONTROLS, ribbon.keyTipSession().getLevel());
			assertTrue(ribbon.keyTipSession().visibleTargets().keySet().containsAll(List.of("V", "C", "X", "FP")));

			input.dispatch(KeyEvent.KEY_PRESSED, KeyEvent.VK_C);
			assertEquals(1, clicks.get());
			assertFalse(ribbon.keyTipSession().isActive());
		});
	}

	@Test
	void sameKeyTipInOneTabIsRejectedWhenTheModelIsBuilt() {
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonCopy.keytip", "V")));
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonCopy.keytip", "v")));
	}

	@Test
	void aKeyTipThatPrefixesAnotherInTheSameTabIsRejected() {
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonCopy.keytip", "F")));
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonCopy.keytip", "FPX")));
	}

	@Test
	void theSameKeyTipInDifferentTabsIsAllowed() {
		SwingRibbonModel model = model(Map.of("RibbonGantt.keytip", "V"));
		assertNotNull(model);
	}

	private static RibbonKeyTipSession session(List<String> log) {
		Map<String, RibbonKeyTipSession.Target> tabs = new LinkedHashMap<>();
		tabs.put("h", new RibbonKeyTipSession.Target("home", null, () -> log.add("tab H")));
		tabs.put("R", new RibbonKeyTipSession.Target("resource", null, () -> log.add("tab R")));
		Map<String, RibbonKeyTipSession.Target> controls = new LinkedHashMap<>();
		controls.put("V", new RibbonKeyTipSession.Target("paste", new JButton(), () -> log.add("control paste")));
		controls.put("FP", new RibbonKeyTipSession.Target("painter", new JButton(), () -> log.add("control painter")));
		return new RibbonKeyTipSession(() -> tabs, id -> "home".equals(id) ? controls : Map.of(), () -> { });
	}

	private static AbstractButton buttonByKeyTip(JPanel host, String keyTip) {
		return UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> keyTip.equals(button.getClientProperty(RibbonControlPopups.KEY_TIP_PROPERTY)))
			.findFirst().orElseThrow();
	}

	private static JPanel ribbonHost() {
		ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		return factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
	}

	private static SwingRibbonModel model(Map<String, String> overrides) {
		ResourceBundle[] base = MenuDefinitionSupport.ribbonBundles(Locale.ROOT);
		ResourceBundle[] bundles = new ResourceBundle[base.length + 1];
		bundles[0] = new ListResourceBundle() {
			@Override
			protected Object[][] getContents() {
				return overrides.entrySet().stream()
					.map(entry -> new Object[] { entry.getKey(), entry.getValue() })
					.toArray(Object[][]::new);
			}
		};
		System.arraycopy(base, 0, bundles, 1, base.length);
		return new SwingRibbonFactory(new MenuRibbonCommandSource(
			new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), bundles)), bundles)
			.createModel(MenuManager.STANDARD_RIBBON);
	}
}
