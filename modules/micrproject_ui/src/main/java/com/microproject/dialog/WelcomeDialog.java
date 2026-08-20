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
package com.microproject.dialog;

import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.IconManager;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;
import com.microproject.application.RecentProjectStore;

public final class WelcomeDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;

	public static class Form {
		boolean createProject = true;
		boolean openProject = false;
		boolean manageResources = false;
		String recentPath;
		String templateId;
		public final boolean isCreateProject() {
			return createProject;
		}
		public final void setCreateProject(boolean createProject) {
			this.createProject = createProject;
		}
		public final boolean isManageResources() {
			return manageResources;
		}
		public final void setManageResources(boolean manageResources) {
			this.manageResources = manageResources;
		}
		public final boolean isOpenProject() {
			return openProject;
		}
		public final void setOpenProject(boolean openProject) {
			this.openProject = openProject;
		}
		public String getRecentPath() { return recentPath; }
		public String getTemplateId() { return templateId; }
	}
	private Form form;
	private MenuManager menuManager;
	private final boolean focusRecentProjects;
	// use property utils to copy to project like struts

	ButtonGroup radioGroup;
	JButton createProject;
	JButton openProject;
	JButton manageResources;
	JList<RecentProjectStore.Entry> recentProjects;
	JComboBox<String> templateChoice;
	JCheckBox restoreSession;
	private final RecentProjectStore recentStore = new RecentProjectStore();

	protected boolean bind(boolean get) {
		if (form == null)
			return false;
		if (!get) {
			form.setCreateProject(createProject.isSelected());
			form.setOpenProject(openProject.isSelected());
			form.setManageResources(manageResources.isSelected());
		}
		return true;
	}
	public static WelcomeDialog getInstance(Frame owner, MenuManager menuManager) {
		return new WelcomeDialog(owner,menuManager,false);
	}
	public static WelcomeDialog getRecentProjectsInstance(Frame owner, MenuManager menuManager) {
		return new WelcomeDialog(owner,menuManager,true);
	}

	private WelcomeDialog(Frame owner, MenuManager menuManager, boolean focusRecentProjects) {
		super(owner, Messages.getContextString("Text.welcomeToPod"), true); //$NON-NLS-1$
		this.menuManager = menuManager;
		this.focusRecentProjects = focusRecentProjects;
		form = new Form();
	}

	// Component Creation and Initialization **********************************

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		createProject = new JButton(Messages.getString("Text.createProject"),IconManager.getIcon("menu24.new"));
		openProject = new JButton(Messages.getString("Text.openProject"),IconManager.getIcon("menu24.open"));
		manageResources = new JButton(Messages.getString("Text.manageResources"),IconManager.getIcon("view.resources"));
		recentProjects = new JList<>(recentStore.entries().toArray(RecentProjectStore.Entry[]::new));
		recentProjects.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
				RecentProjectStore.Entry entry = (RecentProjectStore.Entry) value;
				String prefix = entry.pinned() ? "★ " : "";
				String suffix = entry.exists() ? "" : "  (" + UsabilityStrings.text("welcome.missing") + ")";
				return super.getListCellRendererComponent(list, prefix + entry.path().getFileName() + " — " + entry.path().getParent() + suffix, index, selected, focus);
			}
		});
		recentProjects.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) openSelectedRecentProject();
			}
		});
		recentProjects.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_ENTER) {
					openSelectedRecentProject();
					event.consume();
				}
			}
		});
		templateChoice = new JComboBox<>(new String[] {
			UsabilityStrings.text("welcome.template.basic"),
			UsabilityStrings.text("welcome.template.software"),
			UsabilityStrings.text("welcome.template.construction")
		});
		restoreSession = new JCheckBox(UsabilityStrings.text("welcome.restore"), recentStore.isRestoreSessionEnabled());
		restoreSession.addActionListener(event -> recentStore.setRestoreSessionEnabled(restoreSession.isSelected()));
		
		HelpUtil.addDocHelp(createProject,"Creating_a_Project");
		HelpUtil.addDocHelp(manageResources,"Managing_your_resource_pool");
		
		createProject.setSelected(true);
		
		ActionListener buttonListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createProject.setSelected(false);
				openProject.setSelected(false);
				manageResources.setSelected(false);
				((JButton)e.getSource()).setSelected(true);
				onOk();
			}};
			
		createProject.addActionListener(buttonListener);
		openProject.addActionListener(buttonListener);
		manageResources.addActionListener(buttonListener);
		
		bind(true);
	}

	// Building *************************************************************

	/**
	 * Builds the panel. Initializes and configures components first, then
	 * creates a FormLayout, configures the layout, creates a builder, sets a
	 * border, and finally adds the components.
	 * 
	 * @return the built panel
	 */

	public JComponent createContentPanel() {
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		initControls();
		JPanel builder = new JPanel(new BorderLayout(12, 12));
		builder.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
		builder.add(new JLabel(Messages.getString("WelcomeDialog.WhatWouldYouLikeToDo")), BorderLayout.NORTH);
		JPanel start = new JPanel(new GridLayout(0, 1, 6, 6)); start.add(createProject); start.add(openProject);
		if (Environment.isAdministrator()) start.add(manageResources);
		JPanel template = new JPanel(new BorderLayout(4, 4)); template.add(new JLabel(UsabilityStrings.text("welcome.template")), BorderLayout.NORTH); template.add(templateChoice, BorderLayout.CENTER);
		JButton createTemplate = new JButton(UsabilityStrings.text("welcome.createTemplate"));
		createTemplate.addActionListener(event -> { createProject.setSelected(false); openProject.setSelected(false); manageResources.setSelected(false); form.templateId = switch (templateChoice.getSelectedIndex()) { case 1 -> "software"; case 2 -> "construction"; default -> "basic"; }; onOk(); });
		template.add(createTemplate, BorderLayout.SOUTH); start.add(template); start.add(restoreSession);
		builder.add(start, BorderLayout.WEST);
		JPanel recent = new JPanel(new BorderLayout(4, 4)); recent.add(new JLabel(UsabilityStrings.text("welcome.recent")), BorderLayout.NORTH);
		recentProjects.setVisibleRowCount(9); recent.add(new JScrollPane(recentProjects), BorderLayout.CENTER);
		JPanel recentButtons = new JPanel(); JButton openRecent = new JButton(UsabilityStrings.text("welcome.openSelected"));
		openRecent.addActionListener(event -> openSelectedRecentProject());
		JButton pin = new JButton(UsabilityStrings.text("welcome.pin")); pin.addActionListener(event -> { RecentProjectStore.Entry entry = recentProjects.getSelectedValue(); if (entry != null) { recentStore.setPinned(entry.path(), !entry.pinned()); recentProjects.setListData(recentStore.entries().toArray(RecentProjectStore.Entry[]::new)); } });
		JButton remove = new JButton(UsabilityStrings.text("welcome.remove")); remove.addActionListener(event -> { RecentProjectStore.Entry entry = recentProjects.getSelectedValue(); if (entry != null) { recentStore.remove(entry.path()); recentProjects.setListData(recentStore.entries().toArray(RecentProjectStore.Entry[]::new)); } });
		recentButtons.add(openRecent); recentButtons.add(pin); recentButtons.add(remove); recent.add(recentButtons, BorderLayout.SOUTH); recent.setPreferredSize(new Dimension(560, 260));
		builder.add(recent, BorderLayout.CENTER);
		if (focusRecentProjects) SwingUtilities.invokeLater(() -> {
			if (recentProjects.isSelectionEmpty() && recentProjects.getModel().getSize() > 0)
				recentProjects.setSelectedIndex(0);
			recentProjects.requestFocusInWindow();
		});
		requestFocusInWindow();
		return builder;
	}

	private void openSelectedRecentProject() {
		RecentProjectStore.Entry entry = recentProjects.getSelectedValue();
		if (entry == null || !entry.exists()) return;
		createProject.setSelected(false);
		openProject.setSelected(false);
		manageResources.setSelected(false);
		form.recentPath = entry.path().toString();
		onOk();
	}

	/**
	 * @return Returns the form.
	 */
	public Form getForm() {
		return form;
	}
	
	public Object getBean() {
		return form;
	}
	@Override
	protected boolean hasOkAndCancelButtons() {
		return false;
	}
}
