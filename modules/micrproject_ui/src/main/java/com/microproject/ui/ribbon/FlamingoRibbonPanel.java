/*
 * MIT License
 *
 * Copyright (c) 2026 microProject
 */
package com.microproject.ui.ribbon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.pushingpixels.flamingo.api.ribbon.JRibbon;
import org.pushingpixels.flamingo.api.ribbon.RibbonContextualTaskGroup;
import org.pushingpixels.flamingo.api.ribbon.RibbonTask;

import com.microproject.menu.ExtRibbonFactory;
import com.microproject.pm.graphic.IconManager;
import com.microproject.util.FlatUiSupport;

/** Thin resource adapter around Flamingo's ribbon implementation. */
public final class FlamingoRibbonPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public static final String CONTROLLER_PROPERTY = "microproject.ribbon.controller";
	private static final Color CONTEXTUAL_HUE = new Color(238, 184, 79);

	private final JRibbon ribbon = new JRibbon();
	private final Map<String, RibbonTask> tasks = new LinkedHashMap<>();
	private final Map<String, RibbonContextualTaskGroup> contextualGroups = new LinkedHashMap<>();
	private Collection<String> visibleContextualTaskIds = java.util.List.of();

	public FlamingoRibbonPanel(ExtRibbonFactory factory, String ribbonId,
			CustomRibbonBandGenerator customBandsGenerator, Runnable helpAction) {
		super(new BorderLayout());
		setOpaque(true);
		setBackground(FlatUiSupport.ribbonChromeBackground());
		for (Object value : factory.getStringList(ribbonId)) {
			String taskId = value.toString();
			RibbonTask task = factory.createRibbonTask(taskId, customBandsGenerator);
			tasks.put(taskId, task);
			if (Boolean.parseBoolean(factory.getStringOrNull(taskId + ".contextual"))) {
				RibbonContextualTaskGroup group = new RibbonContextualTaskGroup(task.getTitle(), CONTEXTUAL_HUE, task);
				contextualGroups.put(taskId, group);
				ribbon.addContextualTaskGroup(group);
			} else {
				ribbon.addTask(task);
			}
		}
		for (var button : factory.createTaskBar(ribbonId)) ribbon.addTaskbarComponent(button);
		if (helpAction != null)
			ribbon.configureHelp(IconManager.getRibbonIcon("ribbon.help"), event -> helpAction.run());
		add(ribbon, BorderLayout.CENTER);
		putClientProperty(CONTROLLER_PROPERTY, this);
	}

	public JRibbon getRibbon() {
		return ribbon;
	}

	public void setVisibleContextualTabs(Collection<String> taskIds) {
		visibleContextualTaskIds = taskIds == null ? java.util.List.of() : java.util.List.copyOf(taskIds);
		applyContextualVisibility();
	}

	public boolean isContextualTabVisible(String taskId) {
		return contextualGroups.containsKey(taskId) && visibleContextualTaskIds.contains(taskId);
	}

	@Override
	public void addNotify() {
		super.addNotify();
		applyContextualVisibility();
	}

	private void applyContextualVisibility() {
		// Flamingo repaints its Window directly; defer until this standalone JRibbon
		// has actually been attached to one.
		if (SwingUtilities.getWindowAncestor(ribbon) == null) return;
		for (var entry : contextualGroups.entrySet())
			ribbon.setVisible(entry.getValue(), visibleContextualTaskIds.contains(entry.getKey()));
	}

	public void setContextualTabTitles(Map<String, String> titles) {
		for (var entry : titles.entrySet()) {
			RibbonTask task = tasks.get(entry.getKey());
			if (task != null) task.setTitle(entry.getValue());
			RibbonContextualTaskGroup group = contextualGroups.get(entry.getKey());
			if (group != null) group.setTitle(entry.getValue());
		}
		revalidate();
		repaint();
	}
}
