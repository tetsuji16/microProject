/*******************************************************************************
 * MIT License
 *
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
package com.microproject.ui.theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.UIResource;

import com.microproject.pm.graphic.IconManager;
import com.microproject.util.FlatLafSupport;
import com.microproject.util.FlatUiSupport;

/**
 * Switches the dark/light theme of all open windows at run time.
 *
 * <p>{@link #applyTheme(boolean)} re-installs the look and feel and the
 * microProject tokens, refreshes every window, re-maps explicitly assigned
 * theme colors on existing components, and finally notifies listeners so that
 * components holding cached colors or icons can refresh them.
 */
public final class ThemeManager {
	private static final Logger logger = Logger.getLogger(ThemeManager.class.getName());
	private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

	private ThemeManager() {
	}

	/** Registers a listener that runs on the EDT after every theme switch. Remove it explicitly. */
	public static void addThemeListener(Runnable listener) {
		if (listener != null)
			listeners.add(listener);
	}

	public static void removeThemeListener(Runnable listener) {
		listeners.remove(listener);
	}

	/**
	 * Runs {@code listener} on theme switches only while {@code component} is
	 * displayable; it is unregistered again when the component is removed, so
	 * no leak is possible.
	 */
	public static void bindToComponent(JComponent component, Runnable listener) {
		component.addHierarchyListener(event -> {
			if ((event.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) == 0)
				return;
			if (component.isDisplayable()) {
				if (!listeners.contains(listener))
					listeners.add(listener);
			} else {
				listeners.remove(listener);
			}
		});
		if (component.isDisplayable())
			addThemeListener(listener);
	}

	public static boolean isDark() {
		return FlatUiSupport.isDarkTheme();
	}


	/** Applies the theme to the whole application. Safe to call from any thread. */
	public static void applyTheme(boolean dark) {
		if (SwingUtilities.isEventDispatchThread()) {
			doApply(dark);
			return;
		}
		try {
			SwingUtilities.invokeAndWait(() -> doApply(dark));
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} catch (InvocationTargetException ex) {
			logger.log(Level.WARNING, "Theme switch failed", ex.getCause());
		}
	}

	private static void doApply(boolean dark) {
		Map<Object, Color> before = snapshotColors();
		FlatLafSupport.reapplyLookAndFeel(dark);
		IconManager.clearCaches();
		Map<Integer, Color> remap = buildRemap(before);
		FlatUiSupport.updateAllWindowTrees();
		Window[] windows = Window.getWindows();
		for (Window window : windows)
			remapTree(window, remap);
		for (Runnable listener : new ArrayList<>(listeners)) {
			try {
				listener.run();
			} catch (RuntimeException ex) {
				logger.log(Level.WARNING, "Theme listener failed", ex);
			}
		}
		for (Window window : windows) {
			window.invalidate();
			window.validate();
			window.repaint();
		}
	}

	private static Map<Object, Color> snapshotColors() {
		Map<Object, Color> colors = new HashMap<>();
		for (Object key : new ArrayList<>(Collections.list(UIManager.getDefaults().keys()))) {
			if (!(key instanceof String name))
				continue;
			Color color = UIManager.getColor(name);
			if (color != null)
				colors.put(name, color);
		}
		return colors;
	}

	private static Map<Integer, Color> buildRemap(Map<Object, Color> before) {
		Map<Integer, Color> remap = new HashMap<>();
		Set<Object> ordered = new LinkedHashSet<>();
		for (Object key : before.keySet())
			if (key.toString().startsWith("MicroProject."))
				ordered.add(key);
		ordered.addAll(before.keySet());
		for (Object key : ordered) {
			Color old = before.get(key);
			Color now = UIManager.getColor(key);
			if (now == null || old.equals(now))
				continue;
			boolean neutral = (old.getRGB() & 0xFFFFFF) == 0xFFFFFF || (old.getRGB() & 0xFFFFFF) == 0;
			if (neutral && !key.toString().startsWith("MicroProject."))
				continue; // plain white/black are too ambiguous to re-map
			remap.putIfAbsent(old.getRGB(), new Color(now.getRGB(), true));
		}
		return remap;
	}

	private static void remapTree(Component component, Map<Integer, Color> remap) {
		if (component == null)
			return;
		if (component.isBackgroundSet() && !(component.getBackground() instanceof UIResource)) {
			Color mapped = remap.get(component.getBackground().getRGB());
			if (mapped != null)
				component.setBackground(mapped);
		}
		if (component.isForegroundSet() && !(component.getForeground() instanceof UIResource)) {
			Color mapped = remap.get(component.getForeground().getRGB());
			if (mapped != null)
				component.setForeground(mapped);
		}
		if (component instanceof JComponent jc) {
			Border border = jc.getBorder();
			Border mapped = remapBorder(border, remap);
			if (mapped != border)
				jc.setBorder(mapped);
		}
		if (component instanceof JMenu menu)
			remapTree(menu.getPopupMenu(), remap);
		if (component instanceof Container container)
			for (Component child : container.getComponents())
				remapTree(child, remap);
		if (component instanceof Window window)
			for (Window owned : window.getOwnedWindows())
				remapTree(owned, remap);
	}

	private static Border remapBorder(Border border, Map<Integer, Color> remap) {
		if (border == null || border instanceof UIResource)
			return border;
		if (border instanceof CompoundBorder compound) {
			Border outside = remapBorder(compound.getOutsideBorder(), remap);
			Border inside = remapBorder(compound.getInsideBorder(), remap);
			return outside == compound.getOutsideBorder() && inside == compound.getInsideBorder()
				? border : new CompoundBorder(outside, inside);
		}
		if (border.getClass() == MatteBorder.class) {
			MatteBorder matte = (MatteBorder) border;
			Color mapped = matte.getMatteColor() == null ? null : remap.get(matte.getMatteColor().getRGB());
			if (mapped != null) {
				Insets insets = matte.getBorderInsets();
				return new MatteBorder(insets, mapped);
			}
		} else if (border.getClass() == LineBorder.class) {
			LineBorder line = (LineBorder) border;
			Color mapped = remap.get(line.getLineColor().getRGB());
			if (mapped != null)
				return new LineBorder(mapped, line.getThickness(), line.getRoundedCorners());
		}
		return border;
	}
}
