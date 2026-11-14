/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JComponent;

/**
 * The Alt key sequence of the ribbon: Alt shows the tab KeyTips, a tab letter selects the
 * tab and shows the KeyTips of its controls, and the letters of a control run it. Escape
 * goes back one level. The session holds no Swing state of its own, so it can be driven
 * without a window.
 */
final class RibbonKeyTipSession {
	enum Level { INACTIVE, TABS, CONTROLS }

	/** What a KeyTip addresses: the component the badge hangs under and the action it runs. */
	record Target(String id, JComponent anchor, Runnable activate) {
		Target {
			Objects.requireNonNull(id);
			Objects.requireNonNull(activate);
		}
	}

	private final Supplier<Map<String, Target>> tabTargets;
	private final Function<String, Map<String, Target>> controlTargets;
	private final Runnable onChange;
	private Level level = Level.INACTIVE;
	private Map<String, Target> targets = Map.of();
	private String typed = "";

	/**
	 * @param tabTargets the KeyTips of the visible tabs; evaluated when the session starts
	 * @param controlTargets the KeyTips of the controls of a tab; evaluated after the tab was selected
	 * @param onChange called whenever what the badges show changed
	 */
	RibbonKeyTipSession(Supplier<Map<String, Target>> tabTargets,
		Function<String, Map<String, Target>> controlTargets, Runnable onChange) {
		this.tabTargets = Objects.requireNonNull(tabTargets);
		this.controlTargets = Objects.requireNonNull(controlTargets);
		this.onChange = Objects.requireNonNull(onChange);
	}

	Level getLevel() {
		return level;
	}

	boolean isActive() {
		return level != Level.INACTIVE;
	}

	/** The KeyTips the badges show now: the ones that still match what was typed. */
	Map<String, Target> visibleTargets() {
		Map<String, Target> visible = new LinkedHashMap<>();
		targets.forEach((tip, target) -> {
			if (tip.startsWith(typed)) {
				visible.put(tip, target);
			}
		});
		return visible;
	}

	String getTyped() {
		return typed;
	}

	/** Alt was pressed and released on its own: starts the sequence, or ends it when it runs. */
	void toggle() {
		if (isActive()) {
			cancel();
			return;
		}
		Map<String, Target> tabs = normalize(tabTargets.get());
		if (tabs.isEmpty()) {
			return;
		}
		enter(Level.TABS, tabs);
	}

	void cancel() {
		if (isActive()) {
			enter(Level.INACTIVE, Map.of());
		}
	}

	/** Escape: with letters typed clears them, else goes back one level. */
	void back() {
		if (!typed.isEmpty()) {
			typed = "";
			onChange.run();
		} else if (level == Level.CONTROLS) {
			enter(Level.TABS, normalize(tabTargets.get()));
		} else {
			cancel();
		}
	}

	/**
	 * Feeds one letter or digit. Returns true when the sequence consumed it, which is every
	 * key while it is active; a key that matches nothing is ignored.
	 */
	boolean type(char key) {
		if (!isActive()) {
			return false;
		}
		String candidate = typed + Character.toUpperCase(key);
		Target exact = targets.get(candidate);
		boolean longer = targets.keySet().stream().anyMatch(tip -> tip.length() > candidate.length() && tip.startsWith(candidate));
		if (exact == null && !longer) {
			return true;
		}
		if (longer) {
			typed = candidate;
			onChange.run();
		} else {
			run(exact);
		}
		return true;
	}

	private void run(Target target) {
		if (level == Level.TABS) {
			target.activate().run();
			Map<String, Target> controls = normalize(controlTargets.apply(target.id()));
			if (controls.isEmpty()) {
				cancel();
			} else {
				enter(Level.CONTROLS, controls);
			}
		} else {
			cancel();
			target.activate().run();
		}
	}

	private void enter(Level next, Map<String, Target> nextTargets) {
		level = next;
		targets = nextTargets;
		typed = "";
		onChange.run();
	}

	private static Map<String, Target> normalize(Map<String, Target> tips) {
		Map<String, Target> normalized = new LinkedHashMap<>();
		tips.forEach((tip, target) -> normalized.putIfAbsent(tip.toUpperCase(Locale.ROOT), target));
		return normalized;
	}
}
