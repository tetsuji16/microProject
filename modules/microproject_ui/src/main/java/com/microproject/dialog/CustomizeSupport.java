/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.microproject.ui.ribbon.RibbonCustomizationCatalog;

/** Shared building blocks of the Customize Ribbon and Quick Access Toolbar pages. */
final class CustomizeSupport {
	private CustomizeSupport() { }

	/** A button with its accessible name and mnemonic taken from the bundle. */
	static JButton button(String name, String textKey, String mnemonicKey, ActionListener action) {
		JButton button = new JButton(UsabilityStrings.text(textKey));
		button.setName(name);
		button.getAccessibleContext().setAccessibleName(UsabilityStrings.text(textKey));
		String mnemonic = UsabilityStrings.text(mnemonicKey, "");
		if (!mnemonic.isEmpty()) button.setMnemonic(mnemonic.charAt(0));
		button.setEnabled(false);
		if (action != null) button.addActionListener(action);
		return button;
	}

	/** A single-selection list of canonical commands, named for assistive technology. */
	static JList<RibbonCustomizationCatalog.Command> commandList(String name, String accessibleName,
		javax.swing.DefaultListModel<RibbonCustomizationCatalog.Command> model) {
		JList<RibbonCustomizationCatalog.Command> list = new JList<>(model);
		list.setName(name);
		list.getAccessibleContext().setAccessibleName(accessibleName);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(14);
		return list;
	}

	static JScrollPane scroll(Component view, String name) {
		JScrollPane scroll = new JScrollPane(view);
		scroll.setName(name);
		scroll.setPreferredSize(new java.awt.Dimension(190, 300));
		// The page narrows with the window, so the lists must be allowed to give up width down to a still
		// readable column instead of pushing the buttons of the right-hand column out of view.
		scroll.setMinimumSize(new java.awt.Dimension(90, 96));
		return scroll;
	}

	/**
	 * A preferences page that narrows with its window instead of keeping its preferred width. A plain panel
	 * hosted by a scroll pane is always laid out at its preferred width, so in a narrow window, or at a large
	 * display scale where the same text needs more pixels, the right-hand column (the Up/Down buttons and the
	 * action row of the ribbon customization) moves behind a horizontal scroll bar and looks cut off.
	 */
	static class Page extends JPanel implements javax.swing.Scrollable {
		private static final long serialVersionUID = 1L;

		Page(java.awt.LayoutManager layout) {
			super(layout);
		}

		@Override public java.awt.Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

		@Override public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}

		@Override public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return orientation == javax.swing.SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
		}

		@Override public boolean getScrollableTracksViewportWidth() { return true; }

		@Override public boolean getScrollableTracksViewportHeight() { return false; }
	}

	static JLabel label(String textKey, Component target) {
		JLabel label = new JLabel(UsabilityStrings.text(textKey));
		if (target instanceof javax.swing.JComponent component) label.setLabelFor(component);
		return label;
	}

	/** A narrow column of buttons that sits between or beside the two lists. */
	static JPanel buttonColumn(JButton... buttons) {
		JPanel column = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
		for (JButton button : buttons) column.add(button);
		JPanel holder = new JPanel(new java.awt.BorderLayout());
		holder.add(column, java.awt.BorderLayout.NORTH);
		return holder;
	}
}
