/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.microproject.ui.ribbon.RibbonCustomizationCatalog;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;

/**
 * The Quick Access Toolbar page of the preferences dialog: canonical commands on the left, the toolbar commands in
 * order on the right. Edits change a working copy; {@link #saveIfChanged()} stores them when the dialog is applied.
 */
final class QuickAccessCustomizationPanel extends CustomizeSupport.Page {
	private static final long serialVersionUID = 1L;

	private final List<String> saved;
	private final DefaultListModel<RibbonCustomizationCatalog.Command> available = new DefaultListModel<>();
	private final DefaultListModel<RibbonCustomizationCatalog.Command> chosen = new DefaultListModel<>();
	private final JList<RibbonCustomizationCatalog.Command> availableList;
	private final JList<RibbonCustomizationCatalog.Command> chosenList;
	private final JButton add;
	private final JButton remove;
	private final JButton up;
	private final JButton down;
	private final JButton reset;
	private JPanel transferRow;

	QuickAccessCustomizationPanel() {
		super(new GridBagLayout());
		setName("quickAccessCustomizationPanel");
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		saved = List.copyOf(RibbonDisplayPreferences.loadQuickAccessCommands());
		List<RibbonCustomizationCatalog.Command> commands = RibbonCustomizationCatalog.commands();
		commands.forEach(available::addElement);
		for (String id : saved) chosen.addElement(new RibbonCustomizationCatalog.Command(id,
			RibbonCustomizationCatalog.commandName(id)));
		availableList = CustomizeSupport.commandList("quickAccessAvailable",
			UsabilityStrings.text("customize.commandList"), available);
		chosenList = CustomizeSupport.commandList("quickAccessChosen",
			UsabilityStrings.text("quickAccess.chosenList"), chosen);
		add = CustomizeSupport.button("quickAccessAdd", "customize.add", "customize.add.mnemonic", event -> addSelected());
		remove = CustomizeSupport.button("quickAccessRemove", "customize.remove", "customize.remove.mnemonic",
			event -> removeSelected());
		up = CustomizeSupport.button("quickAccessUp", "customize.up", "customize.up.mnemonic", event -> move(-1));
		down = CustomizeSupport.button("quickAccessDown", "customize.down", "customize.down.mnemonic", event -> move(1));
		reset = CustomizeSupport.button("quickAccessReset", "customize.reset", "customize.reset.mnemonic",
			event -> confirmReset());
		reset.setEnabled(true);
		availableList.addListSelectionListener(event -> updateButtons());
		chosenList.addListSelectionListener(event -> updateButtons());
		layoutControls();
		updateButtons();
	}

	private void layoutControls() {
		JLabel availableLabel = CustomizeSupport.label("customize.commandList", availableList);
		JLabel chosenLabel = CustomizeSupport.label("quickAccess.chosenList", chosenList);
		// The labels and the reset row span their list and its button column, so a narrow page
		// shrinks the lists instead of scrolling the buttons out of view.
		place(availableLabel, 0, 0, 0, 0, 2);
		place(chosenLabel, 2, 0, 0, 0, 2);
		place(CustomizeSupport.scroll(availableList, "quickAccessAvailableScroll"), 0, 1, 1, 1, 1);
		place(CustomizeSupport.buttonColumn(add, remove), 1, 1, 0, 1, 1);
		place(CustomizeSupport.scroll(chosenList, "quickAccessChosenScroll"), 2, 1, 1, 1, 1);
		place(CustomizeSupport.buttonColumn(up, down), 3, 1, 0, 1, 1);
		JPanel resetRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
		resetRow.add(reset);
		transferRow = resetRow;
		place(resetRow, 2, 2, 0, 0, 2);
	}

	/** Adds the Import/Export dropdown shared with the Customize Ribbon page. */
	void attachTransfer(CustomizationTransfer transfer) {
		transferRow.add(transfer.createButton("quickAccess"));
		transferRow.revalidate();
	}

	/** The working toolbar commands as they would be stored on Apply. */
	List<String> workingCommands() {
		return chosenIds();
	}

	/** Replaces the working toolbar commands, for example with an imported file. */
	void replaceWorking(List<String> commandIds) {
		chosen.clear();
		for (String id : commandIds) {
			chosen.addElement(new RibbonCustomizationCatalog.Command(id, RibbonCustomizationCatalog.commandName(id)));
		}
		updateButtons();
	}

	private void place(java.awt.Component component, int x, int y, double weightx, double weighty, int width) {
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

	private void addSelected() {
		RibbonCustomizationCatalog.Command command = availableList.getSelectedValue();
		if (command == null || contains(command.id())) return;
		chosen.addElement(command);
		chosenList.setSelectedIndex(chosen.size() - 1);
		chosenList.ensureIndexIsVisible(chosen.size() - 1);
		updateButtons();
	}

	private void removeSelected() {
		int index = chosenList.getSelectedIndex();
		if (index < 0) return;
		chosen.remove(index);
		if (!chosen.isEmpty()) chosenList.setSelectedIndex(Math.min(index, chosen.size() - 1));
		updateButtons();
	}

	private void move(int delta) {
		int index = chosenList.getSelectedIndex();
		int target = index + delta;
		if (index < 0 || target < 0 || target >= chosen.size()) return;
		RibbonCustomizationCatalog.Command command = chosen.remove(index);
		chosen.add(target, command);
		chosenList.setSelectedIndex(target);
		chosenList.ensureIndexIsVisible(target);
		updateButtons();
	}

	private void confirmReset() {
		int result = JOptionPane.showConfirmDialog(this, UsabilityStrings.text("chrome.confirmResetQuickAccess"),
			UsabilityStrings.text("chrome.resetQuickAccess"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (result != JOptionPane.OK_OPTION) return;
		chosen.clear();
		for (String id : RibbonDisplayPreferences.defaultQuickAccessCommands()) {
			chosen.addElement(new RibbonCustomizationCatalog.Command(id, RibbonCustomizationCatalog.commandName(id)));
		}
		updateButtons();
	}

	private boolean contains(String commandId) {
		for (int index = 0; index < chosen.size(); index++) if (chosen.get(index).id().equals(commandId)) return true;
		return false;
	}

	private void updateButtons() {
		RibbonCustomizationCatalog.Command candidate = availableList.getSelectedValue();
		int index = chosenList.getSelectedIndex();
		add.setEnabled(candidate != null && !contains(candidate.id()));
		remove.setEnabled(index >= 0);
		up.setEnabled(index > 0);
		down.setEnabled(index >= 0 && index < chosen.size() - 1);
	}

	private List<String> chosenIds() {
		List<String> ids = new ArrayList<>(chosen.size());
		for (int index = 0; index < chosen.size(); index++) ids.add(chosen.get(index).id());
		return ids;
	}

	/** Stores the working copy when it differs from the saved toolbar; returns whether anything was stored. */
	boolean saveIfChanged() {
		List<String> working = chosenIds();
		if (working.equals(saved)) return false;
		RibbonDisplayPreferences.saveQuickAccessCommands(working);
		return true;
	}
}
