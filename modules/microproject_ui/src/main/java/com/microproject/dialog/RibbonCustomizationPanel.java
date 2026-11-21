/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.microproject.ui.ribbon.RibbonCustomization;
import com.microproject.ui.ribbon.RibbonCustomizationCatalog;
import com.microproject.ui.ribbon.RibbonCustomizationDraft;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;

/**
 * The Customize Ribbon page of the preferences dialog: canonical commands on the left, the tabs, groups and commands
 * of the ribbon on the right. Edits change a working copy ({@link RibbonCustomizationDraft}); nothing is stored until
 * {@link #saveIfChanged()} runs when the dialog is applied.
 */
final class RibbonCustomizationPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private enum Kind { FILE_TAB, DEFAULT_TAB, DEFAULT_GROUP, DEFAULT_COMMAND, CUSTOM_TAB, CUSTOM_GROUP, CUSTOM_COMMAND }

	/** One row of the ribbon tree. {@code index} is the position of a custom command inside its group. */
	private record Entry(Kind kind, String tabId, String groupId, int index, String commandId, String label) {
		String key() { return kind + "|" + tabId + "|" + groupId + "|" + index; }

		@Override
		public String toString() { return label; }
	}

	private final RibbonCustomization saved;
	private final RibbonCustomizationDraft draft;
	private final DefaultListModel<RibbonCustomizationCatalog.Command> commandModel = new DefaultListModel<>();
	private final JList<RibbonCustomizationCatalog.Command> commandList;
	private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
	private final JTree tree = new JTree(treeModel);
	private final Set<String> expandedKeys = new HashSet<>();
	private final JButton add;
	private final JButton remove;
	private final JButton newTab;
	private final JButton newGroup;
	private final JButton rename;
	private final JButton up;
	private final JButton down;
	private final JButton reset;
	private final JMenuItem resetTabItem;
	private final JMenuItem resetAllItem;
	private boolean rebuilding;
	private JPanel transferRow;

	RibbonCustomizationPanel() {
		super(new GridBagLayout());
		setName("ribbonCustomizationPanel");
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		saved = RibbonDisplayPreferences.loadRibbonCustomization();
		draft = new RibbonCustomizationDraft(saved);
		RibbonCustomizationCatalog.commands().forEach(commandModel::addElement);
		commandList = CustomizeSupport.commandList("ribbonCustomizeCommands",
			UsabilityStrings.text("customize.commandList"), commandModel);
		tree.setName("ribbonCustomizeTree");
		tree.getAccessibleContext().setAccessibleName(UsabilityStrings.text("ribbonCustomize.tree"));
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setRowHeight(0);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new RibbonRowRenderer());
		add = CustomizeSupport.button("ribbonCustomizeAdd", "customize.add", "customize.add.mnemonic", event -> addCommand());
		remove = CustomizeSupport.button("ribbonCustomizeRemove", "customize.remove", "customize.remove.mnemonic",
			event -> removeSelected());
		newTab = CustomizeSupport.button("ribbonCustomizeNewTab", "ribbonCustomize.newTab",
			"ribbonCustomize.newTab.mnemonic", event -> addTab());
		newGroup = CustomizeSupport.button("ribbonCustomizeNewGroup", "ribbonCustomize.newGroup",
			"ribbonCustomize.newGroup.mnemonic", event -> addGroup());
		rename = CustomizeSupport.button("ribbonCustomizeRename", "ribbonCustomize.rename",
			"ribbonCustomize.rename.mnemonic", event -> renameSelected());
		up = CustomizeSupport.button("ribbonCustomizeUp", "customize.up", "customize.up.mnemonic", event -> move(-1));
		down = CustomizeSupport.button("ribbonCustomizeDown", "customize.down", "customize.down.mnemonic", event -> move(1));
		reset = CustomizeSupport.button("ribbonCustomizeReset", "customize.reset", "customize.reset.mnemonic", null);
		reset.setText(reset.getText() + " ▾");
		reset.setEnabled(true);
		resetTabItem = new JMenuItem(UsabilityStrings.text("ribbonCustomize.resetTab"));
		resetTabItem.setName("ribbonCustomizeResetTab");
		resetTabItem.getAccessibleContext().setAccessibleName(resetTabItem.getText());
		resetTabItem.addActionListener(event -> confirmReset(false));
		resetAllItem = new JMenuItem(UsabilityStrings.text("ribbonCustomize.resetAll"));
		resetAllItem.setName("ribbonCustomizeResetAll");
		resetAllItem.getAccessibleContext().setAccessibleName(resetAllItem.getText());
		resetAllItem.addActionListener(event -> confirmReset(true));
		JPopupMenu resetMenu = new JPopupMenu();
		resetMenu.setName("ribbonCustomizeResetMenu");
		resetMenu.add(resetTabItem);
		resetMenu.add(resetAllItem);
		reset.addActionListener(event -> {
			resetTabItem.setEnabled(selected() != null);
			resetMenu.show(reset, 0, reset.getHeight());
		});
		commandList.addListSelectionListener(event -> updateButtons());
		tree.addTreeSelectionListener(event -> updateButtons());
		tree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
			@Override public void treeExpanded(javax.swing.event.TreeExpansionEvent event) { track(event.getPath(), true); }
			@Override public void treeCollapsed(javax.swing.event.TreeExpansionEvent event) { track(event.getPath(), false); }
		});
		tree.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent event) { toggleVisibilityAt(event); }
		});
		tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleTabVisibility");
		tree.getActionMap().put("toggleTabVisibility", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(java.awt.event.ActionEvent event) { toggleSelectedVisibility(); }
		});
		layoutControls();
		rebuild(null);
	}

	private void layoutControls() {
		JLabel commandsLabel = CustomizeSupport.label("customize.commandList", commandList);
		JLabel treeLabel = CustomizeSupport.label("ribbonCustomize.tree", tree);
		place(commandsLabel, 0, 0, 0, 0, 1);
		place(treeLabel, 2, 0, 0, 0, 1);
		place(CustomizeSupport.scroll(commandList, "ribbonCustomizeCommandsScroll"), 0, 1, 1, 1, 1);
		place(CustomizeSupport.buttonColumn(add, remove), 1, 1, 0, 1, 1);
		place(CustomizeSupport.scroll(tree, "ribbonCustomizeTreeScroll"), 2, 1, 1, 1, 1);
		place(CustomizeSupport.buttonColumn(up, down), 3, 1, 0, 1, 1);
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		row.add(newTab);
		row.add(newGroup);
		row.add(rename);
		row.add(reset);
		transferRow = row;
		place(row, 2, 2, 0, 0, 2);
	}

	/** Adds the Import/Export dropdown shared with the Quick Access Toolbar page. */
	void attachTransfer(CustomizationTransfer transfer) {
		transferRow.add(transfer.createButton("ribbonCustomize"));
		transferRow.revalidate();
	}

	/** The working copy as it would be stored on Apply. */
	RibbonCustomization workingCustomization() {
		return draft.toCustomization();
	}

	/** Replaces the working copy, for example with an imported file. */
	void replaceWorking(RibbonCustomization customization) {
		draft.replaceWith(customization);
		rebuild(null);
	}

	private void place(Component component, int x, int y, double weightx, double weighty, int width) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.gridwidth = width;
		constraints.weightx = weightx;
		constraints.weighty = weighty;
		constraints.insets = new Insets(2, 4, 2, 4);
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = weighty > 0 ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
		add(component, constraints);
	}

	// ----- tree -----

	private void rebuild(String selectKey) {
		rebuilding = true;
		try {
			root.removeAllChildren();
			String selectedKey = selectKey != null ? selectKey : selected() == null ? null : selected().key();
			root.add(tabNode(RibbonCustomization.FILE_TAB_ID, Kind.FILE_TAB));
			for (String tabId : draft.defaultTabOrder()) root.add(tabNode(tabId, Kind.DEFAULT_TAB));
			for (RibbonCustomizationDraft.Tab tab : draft.customTabs()) {
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(
					new Entry(Kind.CUSTOM_TAB, tab.id(), null, -1, null, tab.name()));
				root.add(node);
				addCustomGroups(node, tab.id(), tab.id());
			}
			treeModel.reload();
			java.util.Enumeration<?> nodes = root.depthFirstEnumeration();
			List<DefaultMutableTreeNode> all = new java.util.ArrayList<>();
			while (nodes.hasMoreElements()) all.add((DefaultMutableTreeNode) nodes.nextElement());
			for (DefaultMutableTreeNode node : all) {
				if (isExpandedInDraft(node)) tree.expandPath(new TreePath(node.getPath()));
			}
			DefaultMutableTreeNode toSelect = selectedKey == null ? null : findByKey(all, selectedKey);
			if (toSelect == null && root.getChildCount() > 0) toSelect = (DefaultMutableTreeNode) root.getChildAt(0);
			if (toSelect != null) {
				TreePath path = new TreePath(toSelect.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
			}
		} finally {
			rebuilding = false;
		}
		updateButtons();
	}

	private static DefaultMutableTreeNode findByKey(List<DefaultMutableTreeNode> nodes, String key) {
		for (DefaultMutableTreeNode node : nodes) {
			if (node.getUserObject() instanceof Entry entry && entry.key().equals(key)) return node;
		}
		return null;
	}

	/** A node is opened again only when it and all its ancestors were open before the rebuild. */
	private boolean isExpandedInDraft(DefaultMutableTreeNode node) {
		if (!(node.getUserObject() instanceof Entry entry) || !expandedKeys.contains(entry.key())) return false;
		return !(node.getParent() instanceof DefaultMutableTreeNode parent) || parent == root || isExpandedInDraft(parent);
	}

	private DefaultMutableTreeNode tabNode(String tabId, Kind kind) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(
			new Entry(kind, tabId, null, -1, null, RibbonCustomizationCatalog.tabTitle(tabId)));
		for (RibbonCustomizationCatalog.DefaultGroup group : RibbonCustomizationCatalog.defaultGroups(tabId)) {
			DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(
				new Entry(Kind.DEFAULT_GROUP, tabId, group.id(), -1, null, group.title()));
			for (String commandId : group.commandIds()) {
				groupNode.add(new DefaultMutableTreeNode(new Entry(Kind.DEFAULT_COMMAND, tabId, group.id(),
					groupNode.getChildCount(), commandId, RibbonCustomizationCatalog.commandName(commandId))));
			}
			node.add(groupNode);
		}
		addCustomGroups(node, tabId, tabId);
		return node;
	}

	private void addCustomGroups(DefaultMutableTreeNode tabNode, String tabId, String groupsOf) {
		for (RibbonCustomizationDraft.Group group : draft.groups(groupsOf)) {
			DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(
				new Entry(Kind.CUSTOM_GROUP, tabId, group.id(), -1, null, group.name()));
			List<String> commands = group.commandIds();
			for (int index = 0; index < commands.size(); index++) {
				groupNode.add(new DefaultMutableTreeNode(new Entry(Kind.CUSTOM_COMMAND, tabId, group.id(), index,
					commands.get(index), RibbonCustomizationCatalog.commandName(commands.get(index)))));
			}
			tabNode.add(groupNode);
		}
	}

	private void track(TreePath path, boolean expanded) {
		if (rebuilding || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)
			|| !(node.getUserObject() instanceof Entry entry)) return;
		if (expanded) expandedKeys.add(entry.key());
		else expandedKeys.remove(entry.key());
	}

	private Entry selected() {
		TreePath path = tree.getSelectionPath();
		return path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode node
			&& node.getUserObject() instanceof Entry entry ? entry : null;
	}

	/** Expands the tab or group so that a new child is visible. */
	private void keepExpanded(String tabId, String groupId) {
		expandedKeys.add(tabKey(tabId));
		if (groupId != null) expandedKeys.add(groupKey(tabId, groupId));
	}

	private String tabKey(String tabId) {
		Kind kind = draft.isFileTab(tabId) ? Kind.FILE_TAB : draft.isDefaultTab(tabId) ? Kind.DEFAULT_TAB : Kind.CUSTOM_TAB;
		return new Entry(kind, tabId, null, -1, null, "").key();
	}

	private static String groupKey(String tabId, String groupId) {
		return new Entry(Kind.CUSTOM_GROUP, tabId, groupId, -1, null, "").key();
	}

	// ----- actions -----

	private void addCommand() {
		Entry entry = selected();
		RibbonCustomizationCatalog.Command command = commandList.getSelectedValue();
		if (entry == null || command == null) return;
		if (entry.kind() != Kind.CUSTOM_GROUP && entry.kind() != Kind.CUSTOM_COMMAND) return;
		if (!draft.addCommand(entry.groupId(), command.id())) return;
		keepExpanded(entry.tabId(), entry.groupId());
		int index = draft.group(entry.groupId()).commandIds().size() - 1;
		rebuild(new Entry(Kind.CUSTOM_COMMAND, entry.tabId(), entry.groupId(), index, null, "").key());
	}

	private void removeSelected() {
		Entry entry = selected();
		if (entry == null) return;
		// After a removal the removed row is gone; select its parent.
		String next;
		switch (entry.kind()) {
			case CUSTOM_TAB -> { draft.removeTab(entry.tabId()); next = tabKey(RibbonCustomization.FILE_TAB_ID); }
			case CUSTOM_GROUP -> { draft.removeGroup(entry.groupId()); next = tabKey(entry.tabId()); }
			case CUSTOM_COMMAND -> {
				draft.removeCommand(entry.groupId(), entry.index());
				next = groupKey(entry.tabId(), entry.groupId());
			}
			default -> { return; }
		}
		rebuild(next);
	}

	private void addTab() {
		RibbonCustomizationDraft.Tab tab = draft.addTab(UsabilityStrings.text("ribbonCustomize.newTabName"));
		RibbonCustomizationDraft.Group group = draft.addGroup(tab.id(), UsabilityStrings.text("ribbonCustomize.newGroupName"));
		keepExpanded(tab.id(), group.id());
		rebuild(tabKey(tab.id()));
	}

	private void addGroup() {
		Entry entry = selected();
		if (entry == null || draft.isFileTab(entry.tabId())) return;
		RibbonCustomizationDraft.Group group = draft.addGroup(entry.tabId(), UsabilityStrings.text("ribbonCustomize.newGroupName"));
		if (group == null) return;
		keepExpanded(entry.tabId(), group.id());
		rebuild(groupKey(entry.tabId(), group.id()));
	}

	private void renameSelected() {
		Entry entry = selected();
		if (entry == null || (entry.kind() != Kind.CUSTOM_TAB && entry.kind() != Kind.CUSTOM_GROUP)) return;
		Object value = JOptionPane.showInputDialog(this, UsabilityStrings.text("ribbonCustomize.renamePrompt"),
			UsabilityStrings.text("ribbonCustomize.rename.title"), JOptionPane.QUESTION_MESSAGE, null, null, entry.label());
		if (value == null || value.toString().isBlank()) return;
		if (entry.kind() == Kind.CUSTOM_TAB) draft.renameTab(entry.tabId(), value.toString());
		else draft.renameGroup(entry.groupId(), value.toString());
		rebuild(entry.key());
	}

	private void move(int delta) {
		Entry entry = selected();
		if (entry == null) return;
		String key = entry.key();
		boolean moved = switch (entry.kind()) {
			case DEFAULT_TAB, CUSTOM_TAB -> draft.moveTab(entry.tabId(), delta);
			case CUSTOM_GROUP -> draft.moveGroup(entry.groupId(), delta);
			case CUSTOM_COMMAND -> {
				boolean result = draft.moveCommand(entry.groupId(), entry.index(), delta);
				if (result) key = new Entry(Kind.CUSTOM_COMMAND, entry.tabId(), entry.groupId(), entry.index() + delta, null, "").key();
				yield result;
			}
			default -> false;
		};
		if (moved) rebuild(key);
	}

	private void confirmReset(boolean all) {
		Entry entry = selected();
		if (!all && entry == null) return;
		int result = JOptionPane.showConfirmDialog(this,
			UsabilityStrings.text(all ? "ribbonCustomize.confirmResetAll" : "ribbonCustomize.confirmResetTab"),
			UsabilityStrings.text("ribbonCustomize.reset.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (result != JOptionPane.OK_OPTION) return;
		if (all) draft.resetAll();
		else draft.resetTab(entry.tabId());
		rebuild(entry == null || draft.customTab(entry.tabId()) == null && !draft.isDefaultTab(entry.tabId()) ? null
			: tabKey(entry.tabId()));
	}

	// ----- visibility checkbox -----

	private void toggleVisibilityAt(MouseEvent event) {
		TreePath path = tree.getPathForLocation(event.getX(), event.getY());
		if (path == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)
			|| !(node.getUserObject() instanceof Entry entry) || entry.kind() != Kind.DEFAULT_TAB) return;
		java.awt.Rectangle bounds = tree.getPathBounds(path);
		int checkWidth = new JCheckBox().getPreferredSize().width;
		if (bounds != null && event.getX() >= bounds.x && event.getX() < bounds.x + checkWidth) toggleVisibility(entry);
	}

	private void toggleSelectedVisibility() {
		Entry entry = selected();
		if (entry != null && entry.kind() == Kind.DEFAULT_TAB) toggleVisibility(entry);
	}

	private void toggleVisibility(Entry entry) {
		draft.setHidden(entry.tabId(), !draft.isHidden(entry.tabId()));
		rebuild(entry.key());
	}

	// ----- button state -----

	private void updateButtons() {
		if (rebuilding) return;
		Entry entry = selected();
		Kind kind = entry == null ? null : entry.kind();
		boolean customContainer = kind == Kind.CUSTOM_GROUP || kind == Kind.CUSTOM_COMMAND;
		RibbonCustomizationCatalog.Command command = commandList.getSelectedValue();
		add.setEnabled(customContainer && command != null && !hasCommand(entry, command.id()));
		remove.setEnabled(kind == Kind.CUSTOM_TAB || kind == Kind.CUSTOM_GROUP || kind == Kind.CUSTOM_COMMAND);
		newTab.setEnabled(true);
		newGroup.setEnabled(entry != null && kind != Kind.FILE_TAB && !draft.isFileTab(entry.tabId()));
		rename.setEnabled(kind == Kind.CUSTOM_TAB || kind == Kind.CUSTOM_GROUP);
		up.setEnabled(canMove(entry, -1));
		down.setEnabled(canMove(entry, 1));
	}

	private boolean hasCommand(Entry entry, String commandId) {
		RibbonCustomizationDraft.Group group = draft.group(entry.groupId());
		return group != null && group.commandIds().contains(commandId);
	}

	private boolean canMove(Entry entry, int delta) {
		if (entry == null) return false;
		int index;
		int size;
		switch (entry.kind()) {
			case DEFAULT_TAB -> {
				index = draft.defaultTabOrder().indexOf(entry.tabId());
				size = draft.defaultTabOrder().size();
			}
			case CUSTOM_TAB -> {
				index = -1;
				size = draft.customTabs().size();
				for (int i = 0; i < size; i++) if (draft.customTabs().get(i).id().equals(entry.tabId())) index = i;
			}
			case CUSTOM_GROUP -> {
				List<RibbonCustomizationDraft.Group> groups = draft.groups(entry.tabId());
				size = groups.size();
				index = -1;
				for (int i = 0; i < size; i++) if (groups.get(i).id().equals(entry.groupId())) index = i;
			}
			case CUSTOM_COMMAND -> {
				index = entry.index();
				size = draft.group(entry.groupId()).commandIds().size();
			}
			default -> {
				return false;
			}
		}
		int target = index + delta;
		return index >= 0 && target >= 0 && target < size;
	}

	// ----- saving -----

	/** Whether the working copy can be stored; the stored text has a size limit. */
	boolean canSave() {
		return RibbonDisplayPreferences.isStorable(draft.toCustomization());
	}

	/** Stores the working copy when it differs from the saved customization; returns whether it was stored. */
	boolean saveIfChanged() {
		RibbonCustomization working = draft.toCustomization();
		if (working.equals(saved)) return false;
		RibbonDisplayPreferences.saveRibbonCustomization(working);
		return true;
	}

	// ----- rendering -----

	private final class RibbonRowRenderer implements TreeCellRenderer {
		private final JCheckBox tabBox = new JCheckBox();
		private final javax.swing.tree.DefaultTreeCellRenderer plain = new javax.swing.tree.DefaultTreeCellRenderer();

		@Override
		public Component getTreeCellRendererComponent(JTree owner, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
			Entry entry = value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof Entry e ? e : null;
			if (entry != null && (entry.kind() == Kind.DEFAULT_TAB || entry.kind() == Kind.FILE_TAB)) {
				tabBox.setText(entry.label());
				tabBox.setSelected(!draft.isHidden(entry.tabId()));
				tabBox.setEnabled(entry.kind() == Kind.DEFAULT_TAB);
				tabBox.setOpaque(selected);
				tabBox.setBackground(selected ? UIManagerColors.selectionBackground() : owner.getBackground());
				tabBox.setForeground(selected ? UIManagerColors.selectionForeground() : owner.getForeground());
				return tabBox;
			}
			return plain.getTreeCellRendererComponent(owner, value, selected, expanded, leaf, row, hasFocus);
		}
	}

	/** Colours for selected rows, read at render time so that a theme switch applies. */
	private static final class UIManagerColors {
		static java.awt.Color selectionBackground() { return javax.swing.UIManager.getColor("Tree.selectionBackground"); }

		static java.awt.Color selectionForeground() { return javax.swing.UIManager.getColor("Tree.selectionForeground"); }
	}
}
