/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
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
		templateChoice = new JComboBox<>(new String[] { "Basic project", "Software delivery", "Construction" });
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

