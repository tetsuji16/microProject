/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.common.CommandButtonDisplayState;
import org.pushingpixels.flamingo.api.common.JCommandButton;
import org.pushingpixels.flamingo.api.common.JCommandToggleButton;
import org.pushingpixels.flamingo.api.common.RichTooltip;
import org.pushingpixels.flamingo.api.common.icon.ImageWrapperResizableIcon;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;
import org.pushingpixels.flamingo.api.ribbon.JRibbon;
import org.pushingpixels.flamingo.api.ribbon.JRibbonBand;
import org.pushingpixels.flamingo.api.ribbon.JRibbonComponent;
import org.pushingpixels.flamingo.api.ribbon.RibbonContextualTaskGroup;
import org.pushingpixels.flamingo.api.ribbon.RibbonElementPriority;
import org.pushingpixels.flamingo.api.ribbon.RibbonTask;
import org.pushingpixels.flamingo.api.ribbon.resize.CoreRibbonResizePolicies;
import org.pushingpixels.flamingo.api.ribbon.resize.IconRibbonBandResizePolicy;
import org.pushingpixels.flamingo.api.ribbon.resize.RibbonBandResizePolicy;

import com.microproject.pm.graphic.IconManager;
import com.microproject.ribbon.RibbonCommandSource;
import com.microproject.ribbon.SwingRibbonModel;
import com.microproject.util.FlatUiSupport;

/** Native Flamingo rendering and host controls for the resource-backed ribbon model. */
final class FlamingoRibbonController extends JPanel implements RibbonController {
	static final KeyStroke AUTO_HIDE_REVEAL_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_ALT, 0);
	static final String AUTO_HIDE_REVEAL_ACTION = "microproject.ribbon.revealAutoHidden";
	private final JRibbon ribbon = new CompactJRibbon();
	private final RibbonCommandSource commands;
	private final List<Consumer<RibbonDisplayMode>> displayModeListeners = new ArrayList<>();
	private final Map<String, RibbonTask> tasksById = new LinkedHashMap<>();
	private final Map<String, RibbonContextualTaskGroup> contextualGroupsById = new LinkedHashMap<>();
	private final java.util.Set<String> visibleContextualTabIds = new java.util.LinkedHashSet<>();
	private final List<JComponent> registeredControls = new ArrayList<>();
	private final List<ActionStateBinding> actionStateBindings = new ArrayList<>();
	private boolean actionStateListenersAttached = true;
	private boolean autoHideRevealed;
	private JRootPane shortcutRoot;
	private boolean ownsAutoHideRevealShortcut;
	private RibbonDisplayMode displayMode = RibbonDisplayMode.ALWAYS_SHOW;

	FlamingoRibbonController(SwingRibbonModel model, RibbonCommandSource commands) {
		super(new BorderLayout());
		this.commands = Objects.requireNonNull(commands);
		setOpaque(true);
		setBackground(FlatUiSupport.ribbonChromeBackground());
		ribbon.setOpaque(true);
		ribbon.setBackground(FlatUiSupport.ribbonChromeBackground());
		add(ribbon, BorderLayout.CENTER);
		build(model);
		styleRibbonSurface(ribbon);
		styleRibbonTaskTabs(ribbon);
	}

	private void build(SwingRibbonModel model) {
		Map<String, List<RibbonTask>> groups = new LinkedHashMap<>();
		Map<String, String> groupTitles = new LinkedHashMap<>();
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			List<JRibbonBand> bands = new ArrayList<>();
			for (SwingRibbonModel.RibbonBand band : tab.getBands()) bands.add(createBand(band));
			RibbonTask task = new RibbonTask(tab.getTitle(), bands.toArray(org.pushingpixels.flamingo.api.ribbon.AbstractRibbonBand<?>[]::new));
			if (tab.getAccessKey() != null) task.setKeyTip(tab.getAccessKey());
			tasksById.put(tab.getId(), task);
			if (tab.isContextual()) {
				groups.computeIfAbsent(tab.getId(), ignored -> new ArrayList<>()).add(task);
				groupTitles.put(tab.getId(), tab.getTitle());
			} else ribbon.addTask(task);
		}
		int hueIndex = 0;
		for (Map.Entry<String, List<RibbonTask>> entry : groups.entrySet()) {
			Color hue = Color.getHSBColor((hueIndex++ * 0.19f) % 1f, 0.32f, 0.86f);
			RibbonContextualTaskGroup group = new RibbonContextualTaskGroup(groupTitles.get(entry.getKey()), hue,
				entry.getValue().toArray(RibbonTask[]::new));
			contextualGroupsById.put(entry.getKey(), group);
			ribbon.addContextualTaskGroup(group);
		}
	}

	private JRibbonBand createBand(SwingRibbonModel.RibbonBand specification) {
		if (specification.isCustomBand()) {
			JRibbonBand band = new JRibbonBand(specification.getTitle(), icon(specification.getId(), 16));
			styleBandBoundary(band);
			JComponent component = specification.getCustomBandProvider().createComponent();
			if (component != null) band.addRibbonComponent(new JRibbonComponent(component));
			band.setResizePolicies(resizePolicies(band));
			return band;
		}
		JRibbonBand band = new JRibbonBand(specification.getTitle(), icon(specification.getId(), 16));
		styleBandBoundary(band);
		for (SwingRibbonModel.RibbonButton spec : specification.getButtons()) {
			Action action = commands.createAction(spec.getId());
			ResizableIcon icon = spec.getIconKey() == null ? null : icon(spec.getIconKey(), spec.getButtonSize() == SwingRibbonModel.ButtonSize.LARGE ? 32 : 16);
			String initialActionName = actionName(action, spec.getId());
			String buttonText = spec.getText();
			AbstractCommandButton button = spec.isToggle()
				? new JCommandToggleButton(buttonText, icon)
				: new JCommandButton(buttonText, icon);
			button.setName(spec.getId());
			button.setDisplayState(displayState(spec.getButtonSize()));
			setRichTooltip(button, Objects.toString(action.getValue(Action.SHORT_DESCRIPTION), actionName(action, spec.getId())));
			button.setEnabled(action.isEnabled());
			button.getActionModel().setSelected(Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY)));
			button.addActionListener(event -> action.actionPerformed(new ActionEvent(event.getSource(),
				ActionEvent.ACTION_PERFORMED, spec.getId(), event.getWhen(), event.getModifiers())));
			String keyTip = Objects.toString(action.getValue("ActionKeyTip"), "");
			if (!keyTip.isBlank()) button.setActionKeyTip(keyTip);
			PropertyChangeListener stateSync = event -> {
				if ("enabled".equals(event.getPropertyName())) button.setEnabled(action.isEnabled());
				if (Action.NAME.equals(event.getPropertyName())) {
					String nextName = Objects.toString(event.getNewValue(), spec.getId());
					button.setText(nextName.equals(initialActionName) ? spec.getText() : nextName);
				}
				if (Action.SELECTED_KEY.equals(event.getPropertyName()))
					button.getActionModel().setSelected(Boolean.TRUE.equals(event.getNewValue()));
				if (Action.SHORT_DESCRIPTION.equals(event.getPropertyName())) setRichTooltip(button, Objects.toString(event.getNewValue(), ""));
			};
			action.addPropertyChangeListener(stateSync);
			actionStateBindings.add(new ActionStateBinding(action, stateSync));
			commands.registerCommandControl(spec.getId(), button);
			registeredControls.add(button);
			band.addCommandButton(button, priority(spec.getPriority()));
		}
		band.setResizePolicies(resizePolicies(band));
		return band;
	}

	private static void styleBandBoundary(JRibbonBand band) {
		band.setOpaque(true);
		band.setBackground(FlatUiSupport.ribbonChromeBackground());
		band.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 0, 1, FlatUiSupport.ribbonBandSeparatorColor()),
			BorderFactory.createEmptyBorder(2, 2, 2, 2)));
	}

	static void styleRibbonSurface(Container root) {
		Color panelBackground = UIManager.getColor("Panel.background");
		Color surface = FlatUiSupport.ribbonChromeBackground();
		if (root instanceof JComponent component && component.isOpaque()
			&& Objects.equals(component.getBackground(), panelBackground)) component.setBackground(surface);
		for (Component child : root.getComponents())
			if (child instanceof Container container) styleRibbonSurface(container);
	}

	static void styleRibbonTaskTabs(JRibbon ribbon) {
		if (!(ribbon.getUI() instanceof org.pushingpixels.flamingo.internal.ui.ribbon.BasicRibbonUI ui))
			return;
		for (org.pushingpixels.flamingo.internal.ui.ribbon.JRibbonTaskToggleButton tab : ui.getTaskToggleButtons().values()) {
			tab.setOpaque(false);
			tab.setBackground(FlatUiSupport.ribbonChromeBackground());
			tab.setUI(new OfficeRibbonTaskTabUI());
			tab.setBorder(BorderFactory.createEmptyBorder(4, 10, 7, 10));
		}
	}

	private static List<RibbonBandResizePolicy> resizePolicies(JRibbonBand band) {
		List<RibbonBandResizePolicy> candidates = CoreRibbonResizePolicies.getCorePoliciesPermissive(band);
		Insets insets = band.getInsets();
		int height = band.getControlPanel().getPreferredSize().height + band.getUI().getBandTitleHeight()
			+ insets.top + insets.bottom;
		List<RibbonBandResizePolicy> compatible = new ArrayList<>();
		for (RibbonBandResizePolicy candidate : candidates) {
			int candidateWidth = candidate.getPreferredWidth(height, 4);
			if (candidate instanceof IconRibbonBandResizePolicy) {
				while (!compatible.isEmpty()
					&& compatible.getLast().getPreferredWidth(height, 4) < candidateWidth) compatible.removeLast();
				compatible.add(candidate);
			} else if (compatible.isEmpty()
				|| candidateWidth <= compatible.getLast().getPreferredWidth(height, 4)) {
				compatible.add(candidate);
			}
		}
		return compatible;
	}

	private static String actionName(Action action, String fallback) {
		return Objects.toString(action.getValue(Action.NAME), fallback);
	}

	private static void setRichTooltip(AbstractCommandButton button, String text) {
		if (text == null || text.isBlank()) return;
		RichTooltip tooltip = new RichTooltip(text, text);
		tooltip.addDescriptionSection(text);
		button.setActionRichTooltip(tooltip);
	}

	private static RibbonElementPriority priority(SwingRibbonModel.ButtonPriority priority) {
		return switch (priority) {
			case TOP -> RibbonElementPriority.TOP;
			case MEDIUM -> RibbonElementPriority.MEDIUM;
			case LOW -> RibbonElementPriority.LOW;
		};
	}

	private static CommandButtonDisplayState displayState(SwingRibbonModel.ButtonSize size) {
		return switch (size) {
			case LARGE -> CommandButtonDisplayState.BIG;
			case MEDIUM -> CommandButtonDisplayState.MEDIUM;
			case SMALL -> CommandButtonDisplayState.SMALL;
		};
	}

	private static ResizableIcon icon(String key, int size) {
		ResizableIcon resolved;
		try {
			ImageIcon tinted = IconManager.getRibbonIconTinted(key, size, size, FlatUiSupport.ribbonIconColor());
			resolved = tinted == null ? IconManager.getRibbonIcon(key, size, size)
				: ImageWrapperResizableIcon.getIcon(tinted.getImage(), new Dimension(size, size));
		} catch (MissingResourceException ignored) {
			resolved = null;
		}
		return resolved == null ? new EmptyResizableIcon(size) : resolved;
	}

	@Override public void setRibbonDisplayMode(RibbonDisplayMode mode) {
		RibbonDisplayMode next = Objects.requireNonNull(mode);
		if (displayMode == next) return;
		displayMode = next;
		autoHideRevealed = false;
		applyDisplayMode();
		updateAutoHideRevealShortcut();
		displayModeListeners.forEach(listener -> listener.accept(next));
	}
	@Override public RibbonDisplayMode getRibbonDisplayMode() { return displayMode; }
	@Override public boolean isCommandSurfaceVisible() {
		return displayMode == RibbonDisplayMode.ALWAYS_SHOW || autoHideRevealed;
	}
	@Override public void toggleRibbonCollapseMode() {
		setRibbonDisplayMode(displayMode == RibbonDisplayMode.ALWAYS_SHOW ? RibbonDisplayMode.TABS_ONLY : RibbonDisplayMode.ALWAYS_SHOW);
	}

	void revealAutoHiddenRibbon() {
		if (displayMode != RibbonDisplayMode.AUTO_HIDE || autoHideRevealed) return;
		autoHideRevealed = true;
		applyDisplayMode();
	}
	@Override public void addRibbonDisplayModeListener(Consumer<RibbonDisplayMode> listener) { displayModeListeners.add(Objects.requireNonNull(listener)); }
	@Override public void showProjectTab() {
		RibbonTask task = tasksById.get("ProjectRibbonTask");
		if (task != null) ribbon.setSelectedTask(task);
	}
	@Override public void setVisibleContextualTabs(Collection<String> tabIds) {
		visibleContextualTabIds.clear();
		visibleContextualTabIds.addAll(tabIds);
		applyContextualVisibility();
	}

	@Override public void addNotify() {
		super.addNotify();
		if (!actionStateListenersAttached) {
			for (ActionStateBinding binding : actionStateBindings) binding.action().addPropertyChangeListener(binding.listener());
			actionStateListenersAttached = true;
		}
		for (JComponent control : registeredControls) {
			String commandId = control.getName();
			if (commandId != null) commands.registerCommandControl(commandId, control);
		}
		updateAutoHideRevealShortcut();
		applyContextualVisibility();
	}

	private void applyDisplayMode() {
		boolean visible = displayMode != RibbonDisplayMode.AUTO_HIDE || autoHideRevealed;
		setVisible(visible);
		ribbon.setMinimized(displayMode == RibbonDisplayMode.TABS_ONLY
			|| (displayMode == RibbonDisplayMode.AUTO_HIDE && !autoHideRevealed));
		revalidate();
		repaint();
	}

	private void updateAutoHideRevealShortcut() {
		if (displayMode == RibbonDisplayMode.AUTO_HIDE) {
			JRootPane root = SwingUtilities.getRootPane(this);
			if (root == null || root == shortcutRoot) return;
			removeAutoHideRevealShortcut();
			InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			if (inputMap.get(AUTO_HIDE_REVEAL_KEY) != null) return;
			ActionMap actionMap = root.getActionMap();
			inputMap.put(AUTO_HIDE_REVEAL_KEY, AUTO_HIDE_REVEAL_ACTION);
			actionMap.put(AUTO_HIDE_REVEAL_ACTION, new AbstractAction() {
				@Override public void actionPerformed(ActionEvent event) { revealAutoHiddenRibbon(); }
			});
			shortcutRoot = root;
			ownsAutoHideRevealShortcut = true;
		} else {
			removeAutoHideRevealShortcut();
		}
	}

	private void removeAutoHideRevealShortcut() {
		if (shortcutRoot == null || !ownsAutoHideRevealShortcut) return;
		InputMap inputMap = shortcutRoot.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		if (AUTO_HIDE_REVEAL_ACTION.equals(inputMap.get(AUTO_HIDE_REVEAL_KEY)))
			inputMap.remove(AUTO_HIDE_REVEAL_KEY);
		shortcutRoot.getActionMap().remove(AUTO_HIDE_REVEAL_ACTION);
		shortcutRoot = null;
		ownsAutoHideRevealShortcut = false;
	}

	private void applyContextualVisibility() {
		if (javax.swing.SwingUtilities.getWindowAncestor(this) == null) return;
		for (Map.Entry<String, RibbonContextualTaskGroup> group : contextualGroupsById.entrySet())
			ribbon.setVisible(group.getValue(), visibleContextualTabIds.contains(group.getKey()));
	}
	@Override public void setContextualTabTitles(Map<String, String> titles) {
		for (Map.Entry<String, RibbonContextualTaskGroup> group : contextualGroupsById.entrySet()) {
			String title = titles.get(group.getKey());
			if (title != null) group.getValue().setTitle(title);
		}
		for (Map.Entry<String, RibbonTask> task : tasksById.entrySet()) {
			String title = titles.get(task.getKey());
			if (title != null) task.getValue().setTitle(title);
		}
	}

	boolean isContextualTabVisible(String tabId) {
		return visibleContextualTabIds.contains(tabId);
	}

	@Override public void removeNotify() {
		removeAutoHideRevealShortcut();
		commands.unregisterCommandControls(registeredControls);
		if (actionStateListenersAttached) {
			for (ActionStateBinding binding : actionStateBindings) binding.action().removePropertyChangeListener(binding.listener());
			actionStateListenersAttached = false;
		}
		super.removeNotify();
	}

	private record ActionStateBinding(Action action, PropertyChangeListener listener) { }

	private static final class EmptyResizableIcon implements ResizableIcon {
		private int size;
		private EmptyResizableIcon(int size) { this.size = size; }
		@Override public void setDimension(Dimension dimension) { size = Math.max(dimension.width, dimension.height); }
		@Override public int getIconWidth() { return size; }
		@Override public int getIconHeight() { return size; }
		@Override public void paintIcon(java.awt.Component component, java.awt.Graphics graphics, int x, int y) { }
	}
}
