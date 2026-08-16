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
package com.microproject.pm.graphic;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.microproject.menu.resource.JToolbarSeparator;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.help.HelpUtil;
import com.microproject.menu.HyperLinkToolTip;
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuTextKeys;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.Environment;

public class TabbedNavigation implements MenuActionConstants, Serializable {
	private static final long serialVersionUID = -270788624568075685L;
	private static final Logger logger = Logger.getLogger(TabbedNavigation.class.getName());
	ExtTabbedPane tabbedPane;
	MenuManager menuManager;
	List<Action> actions = new ArrayList<>();
	int oldSelected = -1;
	DocumentFrame currentFrame;
	private JToolBar currentBar = null;
	private static int eventNum = 0;
	private JPopupMenu trackingPopup = null;
	private int resourceTabCount = 0;
	private List<JButton> trackingButtons = new ArrayList<>();

	private class ExtTabbedPane extends JTabbedPane {
		private static final long serialVersionUID = 7993870683783896098L;
		ExtTabbedPane() {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setFont(FlatUiSupport.uiFont());
			setBorder(BorderFactory.createEmptyBorder());
			FlatUiSupport.styleTabbedPane(this);

			addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int i = getSelectedIndex();
					AbstractAction ac = null;
					if (oldSelected != -1) {
						JComponent old = (JComponent) tabbedPane.getComponentAt(oldSelected);
						if (old instanceof JToolBar)
							removeFilterToolBar((JToolBar)old);
						ac = (AbstractAction)actions.get(i);
						ac.actionPerformed(new ActionEvent(this,eventNum++,"click"));
						JComponent selectedComponent = (JComponent) tabbedPane.getSelectedComponent();
						if (selectedComponent instanceof JToolBar)
							addFilterToolBar((JToolBar)selectedComponent);
					}
					oldSelected = i;
				}

			});
			GraphicManager.getInstance().getLafManager().setUI(this);
		}

		public void updateUI() {
			// ignore it since i set it explicitly in ctor and would give exception when clickin on tab due to fade issue above
		}

	}
	public JComponent createContentPanel(MenuManager menuManager,JToolBar toolbar, int group, int tabPlacement, boolean addFilterButtons) {
		this.menuManager = menuManager;
		menuManager.add(this);
		boolean top = (tabPlacement == JTabbedPane.TOP);
		int height = top ? 45 : 15;
		FormLayout layout = new FormLayout("p:grow",  "fill:" + height + "dlu:grow");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		CellConstraints cc = new CellConstraints();
		builder.setBorder(BorderFactory.createEmptyBorder());
		tabbedPane= new ExtTabbedPane();
		tabbedPane.setTabPlacement(tabPlacement);
		int tabCount = 0;
		int groupCount = 0;
		for (int i=0; i < toolbar.getComponentCount(); i++) {
			Object obj = toolbar.getComponent(i);
			if (obj instanceof JToolbarSeparator)
				groupCount++;
			if (! (obj instanceof AbstractButton))
				continue;
			if (group != -1 && group != groupCount)
				continue;
			AbstractButton b = (AbstractButton) obj;
			Action action = b.getAction();
			if (action == menuManager.getActionFromId(ACTION_TRACKING_GANTT))
				continue;
			if (action == menuManager.getActionFromId(ACTION_PROJECTS))
				continue;
			JComponent component;
			if (top)
				component = createSubPanel(action, addFilterButtons);
			else
				component = dummy();
			component.setBorder(BorderFactory.createEmptyBorder());
			if (!Environment.isNewLaf()) component.setOpaque(false);
			String text = HyperLinkToolTip.extractTip(b.getToolTipText());
			tabbedPane.addTab(text,component);
			if (action == menuManager.getActionFromId(ACTION_RESOURCES))
				this.resourceTabCount = tabCount;
			tabbedPane.setToolTipTextAt(tabCount, text); // don't use version with F1
			actions.add(action);
			tabCount++;
		}
		builder.add(tabbedPane);
		JComponent c = builder.getPanel();
		c.setBorder(BorderFactory.createEmptyBorder());

		return c;
	}

	public void setAllButResourceDisabled(boolean disable) {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			if (i == resourceTabCount)
				continue;
			tabbedPane.setEnabledAt(i,!disable);
		}

	}
	private JPanel dummy() {
		JPanel dummy = new JPanel();
		dummy.setSize(dummy.getSize().width,0);

		return dummy;
	}


	private JComponent createSubPanel(Action action, boolean addFilterButtons) {
		String toolBarName = null;
		boolean taskMenu = false;
		if (action == menuManager.getActionFromId(ACTION_GANTT)) {
			toolBarName = "GanttToolBar";
			taskMenu = true;
		} else if (action == menuManager.getActionFromId(ACTION_TRACKING_GANTT)) {
			toolBarName = "TrackingGanttToolBar";
			taskMenu = true;
		} else if (action == menuManager.getActionFromId(ACTION_TASK_USAGE_DETAIL)) {
			toolBarName = "TaskUsageDetailToolBar";
		} else if (action == menuManager.getActionFromId(ACTION_RESOURCE_USAGE_DETAIL)) {
			toolBarName = "ResourceUsageDetailToolBar";
		} else if (action == menuManager.getActionFromId(ACTION_NETWORK))
			toolBarName = "NetworkToolBar";
		else if (action == menuManager.getActionFromId(ACTION_WBS))
			toolBarName = "WBSToolBar";
		else if (action == menuManager.getActionFromId(ACTION_RBS))
			toolBarName = "RBSToolBar";
		else if (action == menuManager.getActionFromId(ACTION_RESOURCES))
			toolBarName = "ResourceToolBar";
		else if (action == menuManager.getActionFromId(ACTION_REPORT))
			toolBarName = "ReportToolBar";
		if (toolBarName == null)
			return dummy();
		JToolBar toolBar = menuManager.getToolBar(toolBarName);
		toolBar.setFloatable(false);
		if (taskMenu) {
			toolBar.addSeparator(new Dimension(20, 20));
			final JComponent tracking = tracking();
			toolBar.add(tracking);

			HelpUtil.addDocHelp(tracking,"Tracking_Menu");

//			toolBar.add(menuManager.getMenu(MenuManager.SF_MENU));
		}
		toolBar.addSeparator(new Dimension(40, 20));
//		String viewName = menuManager.getStringFromAction(action);
//		if (addFilterButtons)
//			FilterToolBarManager.getInstance().addButtons(toolBar,menuManager,viewName);


		return toolBar;
	}

	private JComponent tracking() {
		final JButton p = new JButton(Messages.getString("Spreadsheet.Task.tracking"), IconManager.getIcon("print.down")) {
			public Point getToolTipLocation(MouseEvent event) { // the tip MUST be touching the button if html because you can click on links
				if (getToolTipText().startsWith("<html>"))
					return new Point(0, getHeight()-2);
				else
					return super.getToolTipLocation(event);
			}

			public JToolTip createToolTip() {
				if (getToolTipText().startsWith("<html>")) {
					JToolTip tip = new HyperLinkToolTip();
					tip.setComponent(this);
					return tip;
				} else {
					return super.createToolTip();
				}
			}

		};


		p.setHorizontalTextPosition(AbstractButton.LEADING);
		String name = "SFTracking";
		String s = menuManager.getString(name + MenuTextKeys.TOOLTIP_SUFFIX);
		if (s != null) {
			String help = menuManager.getStringOrNull(name+MenuTextKeys.HELP_SUFFIX);
			String demo = menuManager.getStringOrNull(name+MenuTextKeys.DEMO_SUFFIX);
			String doc = menuManager.getStringOrNull(name+MenuTextKeys.DOC_SUFFIX);
			if (doc != null)
				s = HyperLinkToolTip.helpTipText(s,help,demo, doc);
			p.setToolTipText(s);
		}
		p.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (trackingPopup == null)
				   trackingPopup = menuManager.getPopupMenu("SFTracking");
				trackingPopup.show(p,0,p.getHeight());
			}});

		trackingButtons.add(p);
		return p;

	}
	public void setActivatedView(String viewId, boolean enable) {
		if (!enable)
			return;
		int index = indexOfViewId(viewId);
		if (index != -1 && index != tabbedPane.getSelectedIndex()) {
			tabbedPane.setSelectedIndex(index);
		}
	}
	private void dumpTabNames() {
		for (int i=0; i < tabbedPane.getTabCount(); i++)
			logger.log(Level.FINE, "tab {0} title: {1} action {2}", new Object[] { i, tabbedPane.getTitleAt(i), actions.get(i).hashCode() });
	}

	private int indexOfViewId(String viewId) {
		Object action = menuManager.getActionFromId(viewId);
		for (int i=0; i < tabbedPane.getTabCount(); i++)
			if (actions.get(i) == action)
				return i;
		return -1;
	}


	private void addFilterToolBar(JToolBar bar) {
		if (currentFrame != null) {
			currentBar  = bar;
			currentFrame.getFilterToolBarManager().addButtons(bar);
		}
	}
	private void removeFilterToolBar(JToolBar bar) {
		if (currentFrame != null) {
			currentFrame.getFilterToolBarManager().removeButtons(bar);
		}
	}

	public void setCurrentFrame(DocumentFrame currentFrame) {
		if (this.currentFrame != null)
			removeFilterToolBar(currentBar);
		this.currentFrame = currentFrame;
		initFilterToolBar();
	}
	public void initFilterToolBar() {
		JComponent selectedComponent = (JComponent) tabbedPane.getSelectedComponent();
		if (selectedComponent instanceof JToolBar)
			addFilterToolBar((JToolBar)selectedComponent);
	}

	public void setTrackingEnabled(boolean enabled) {
		for (JButton but: trackingButtons) {
			but.setEnabled(enabled);
		}
	}
}

