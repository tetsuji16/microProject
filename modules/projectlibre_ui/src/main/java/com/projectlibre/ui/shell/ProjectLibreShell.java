package com.projectlibre.ui.shell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.pm.graphic.TabbedNavigation;
import com.projectlibre1.pm.graphic.frames.MainRibbonFrame;
import com.projectlibre1.pm.graphic.frames.workspace.DefaultFrameManager;
import com.projectlibre1.pm.graphic.frames.workspace.FrameManager;
import com.projectlibre1.pm.graphic.laf.LafManager;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.toolbar.FilterToolBarManager;
import com.projectlibre1.util.FlatUiSupport;

/**
 * Builds shell chrome while leaving action orchestration in GraphicManager.
 */
public final class ProjectLibreShell {
	public static final class ShellHandles {
		private final TabbedNavigation topTabs;
		private final FilterToolBarManager filterToolBarManager;
		private final JMenu projectListMenu;

		public ShellHandles(TabbedNavigation topTabs, FilterToolBarManager filterToolBarManager, JMenu projectListMenu) {
			this.topTabs = topTabs;
			this.filterToolBarManager = filterToolBarManager;
			this.projectListMenu = projectListMenu;
		}

		public TabbedNavigation getTopTabs() {
			return topTabs;
		}

		public FilterToolBarManager getFilterToolBarManager() {
			return filterToolBarManager;
		}

		public JMenu getProjectListMenu() {
			return projectListMenu;
		}
	}

	private ProjectLibreShell() {
	}

	public static ShellHandles installRibbonShell(MainRibbonFrame frame, MenuManager menuManager, Runnable helpAction) {
		JPanel ribbonPanel = menuManager.createRibbonPanel(MenuManager.STANDARD_RIBBON, helpAction);
		if (ribbonPanel == null) {
			ribbonPanel = new JPanel(new BorderLayout());
		}
		JPanel shell = new OfficeChromePanel(menuManager, ribbonPanel, helpAction);
		frame.setRibbonPanel(shell);
		return new ShellHandles(null, null, null);
	}

	public static ShellHandles installNewLookShell(
		Container contentPane,
		MenuManager menuManager,
		LafManager lafManager,
		FrameManager frameManager,
		boolean applet,
		boolean external,
		boolean standAlone,
		boolean mac,
		JMenuBarTarget menuBarTarget) {
		JToolBar toolBar = menuManager.getToolBar(MenuManager.BIG_TOOL_BAR);
		if (!lafManager.isToolbarOpaque()) {
			toolBar.setOpaque(false);
		}
		if (!applet) {
			menuManager.setActionVisible("FullScreen", false);
		}
		if (external) {
			menuManager.setActionVisible("TeamFilter", false);
		}

		toolBar.addSeparator(new Dimension(20, 20));
		toolBar.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)));
		toolBar.add(((DefaultFrameManager) frameManager).getProjectComboPanel());
		toolBar.add(Box.createRigidArea(new Dimension(20, 20)));
		toolBar.setBackground(FlatUiSupport.panelBackground());
		toolBar.setFloatable(false);
		toolBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JToolBar viewToolBar = menuManager.getToolBar(MenuManager.VIEW_TOOL_BAR_WITH_NO_SUB_VIEW_OPTION);
		TabbedNavigation topTabs = new TabbedNavigation();
		JComponent tabs = topTabs.createContentPanel(menuManager, viewToolBar, 0, JTabbedPane.TOP, true);
		JComponent bottom = new TabbedNavigation().createContentPanel(menuManager, viewToolBar, 1, JTabbedPane.BOTTOM, false);
		attachNewLookChrome(contentPane, toolBar, tabs, bottom, FlatUiSupport.panelBackground());

		JMenu projectListMenu = null;
		if (mac && menuBarTarget != null) {
			JMenuBar menu = menuManager.getMenu(standAlone ? MenuManager.MAC_STANDARD_MENU : MenuManager.SERVER_STANDARD_MENU);
			menuBarTarget.setMenuBar(menu);
			projectListMenu = (JMenu) menu.getComponent(5);
		}

		return new ShellHandles(topTabs, null, projectListMenu);
	}

	public static ShellHandles installClassicShell(
		Container contentPane,
		MenuManager menuManager,
		boolean standAlone,
		boolean mac,
		JMenuBarTarget menuBarTarget) {
		JToolBar toolBar = menuManager.getToolBar(mac ? MenuManager.MAC_STANDARD_TOOL_BAR : MenuManager.STANDARD_TOOL_BAR);
		FilterToolBarManager filterToolBarManager = FilterToolBarManager.create(menuManager);
		filterToolBarManager.addButtons(toolBar);
		contentPane.add(toolBar, BorderLayout.BEFORE_FIRST_LINE);
		JToolBar viewToolBar = menuManager.getToolBar(MenuManager.VIEW_TOOL_BAR);
		viewToolBar.setOrientation(JToolBar.VERTICAL);
		viewToolBar.setRollover(true);
		contentPane.add(viewToolBar, BorderLayout.WEST);

		JMenuBar menu = menuManager.getMenu(standAlone ? (mac ? MenuManager.MAC_STANDARD_MENU : MenuManager.STANDARD_MENU) : MenuManager.SERVER_STANDARD_MENU);
		JMenu projectListMenu;
		if (!mac) {
			((JComponent) menu).setBorder(javax.swing.BorderFactory.createEmptyBorder());
			var logo = menu.getComponent(0);
			if (logo instanceof javax.swing.JMenuItem item) {
				item.setBorder(javax.swing.BorderFactory.createEmptyBorder());
				item.setMaximumSize(new Dimension(124, 52));
				item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
		}
		if (menuBarTarget != null) {
			menuBarTarget.setMenuBar(menu);
		}
		projectListMenu = (JMenu) menu.getComponent(mac ? 5 : 6);
		return new ShellHandles(null, filterToolBarManager, projectListMenu);
	}

	public static void attachNewLookChrome(Container contentPane, JToolBar toolBar, JComponent tabs, JComponent bottom, Color background) {
		Objects.requireNonNull(contentPane);
		Objects.requireNonNull(toolBar);
		Objects.requireNonNull(tabs);
		Objects.requireNonNull(bottom);
		Box top = new Box(BoxLayout.Y_AXIS);
		toolBar.setAlignmentX(0.0f);
		tabs.setAlignmentX(0.0f);
		top.add(toolBar);
		top.add(tabs);
		contentPane.add(top, BorderLayout.BEFORE_FIRST_LINE);
		contentPane.add(bottom, BorderLayout.AFTER_LAST_LINE);
		contentPane.setBackground(background);
	}

	public static boolean showRestartMessageIfNeeded(Container contentPane, boolean needToRestart) {
		if (!needToRestart) {
			return false;
		}
		contentPane.add(new JLabel(Messages.getString("Error.restart")), BorderLayout.CENTER);
		return true;
	}

	@FunctionalInterface
	public interface JMenuBarTarget {
		void setMenuBar(JMenuBar menuBar);
	}
}
