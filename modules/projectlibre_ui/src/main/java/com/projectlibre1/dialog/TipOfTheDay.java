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
 * specific language governing rights and limitations under the License. 
 * The Original Code is ProjectLibre. The Original Developer is the Initial Developer 
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
package com.projectlibre1.dialog;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.projectlibre1.strings.Messages;
import com.projectlibre1.pm.graphic.IconManager;
import com.projectlibre1.util.FlatUiSupport;
import com.projectlibre1.util.PopupDialogSupport;

public class TipOfTheDay {
	private static final String TIP_NUMBER_KEY = "tipNumber";
	private static final String SHOW_ON_STARTUP_KEY = "showTipsOnStartup";
	private static final Pattern TIP_KEY_PATTERN = Pattern.compile("^tip\\.(\\d+)\\.(name|description)$");
	private static final List<TipEntry> TIP_ENTRIES = loadTips(Messages.getTipProperties());

	private static boolean showTip = loadShowTipPreference();

	private TipOfTheDay() {
	}

	public static void showDialog(Component owner, boolean forceShow) {
		if (TIP_ENTRIES.isEmpty()) {
			return;
		}
		if (!forceShow && !showTip) {
			return;
		}

		int storedTip = readStoredTipNumber();
		TipDialog dialog = new TipDialog(owner, storedTip);
		dialog.setVisible(true);
		dialog.dispose();

		Preferences preferences = Preferences.userNodeForPackage(TipOfTheDay.class);
		preferences.putInt(TIP_NUMBER_KEY, dialog.getNextTipNumber());
		preferences.putBoolean(SHOW_ON_STARTUP_KEY, showTip);
	}

	private static boolean loadShowTipPreference() {
		return Preferences.userNodeForPackage(TipOfTheDay.class).getBoolean(SHOW_ON_STARTUP_KEY, true);
	}

	private static int readStoredTipNumber() {
		int tipNumber = Preferences.userNodeForPackage(TipOfTheDay.class).getInt(TIP_NUMBER_KEY, 1);
		if (tipNumber < 1 || tipNumber > TIP_ENTRIES.size()) {
			return 1;
		}
		return tipNumber;
	}

	private static List<TipEntry> loadTips(Properties properties) {
		List<TipEntry> tips = new ArrayList<TipEntry>();
		if (properties == null || properties.isEmpty()) {
			return tips;
		}

		List<Integer> tipNumbers = new ArrayList<Integer>();
		for (Object keyObject : properties.keySet()) {
			String key = String.valueOf(keyObject);
			Matcher matcher = TIP_KEY_PATTERN.matcher(key);
			if (matcher.matches()) {
				int tipNumber = Integer.parseInt(matcher.group(1));
				if (!tipNumbers.contains(Integer.valueOf(tipNumber))) {
					tipNumbers.add(Integer.valueOf(tipNumber));
				}
			}
		}
		tipNumbers.sort(Comparator.naturalOrder());

		for (Integer tipNumber : tipNumbers) {
			String name = trimToNull(properties.getProperty("tip." + tipNumber + ".name"));
			String description = trimToNull(properties.getProperty("tip." + tipNumber + ".description"));
			if (name == null && description == null) {
				continue;
			}
			tips.add(new TipEntry(name, description));
		}
		return tips;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static final class TipEntry {
		private final String name;
		private final String description;

		private TipEntry(String name, String description) {
			this.name = name;
			this.description = description;
		}
	}

	private static final class TipDialog extends JDialog {
		private static final long serialVersionUID = 1L;
		private static final int HORIZONTAL_PADDING = 18;
		private static final int VERTICAL_PADDING = 16;

		private final List<TipEntry> tips;
		private final JLabel tipIconLabel = new JLabel();
		private final JLabel didYouKnowLabel = new JLabel(Messages.getString("TipOfTheDay.didYouKnowText"));
		private final JLabel tipCounterLabel = new JLabel();
		private final JLabel tipTitleLabel = new JLabel();
		private final JTextArea tipDescriptionArea = new JTextArea();
		private final JCheckBox showOnStartupCheckBox;
		private JButton previousButton;
		private JButton nextButton;
		private JButton closeButton;
		private int currentIndex;
		private int nextTipNumber;

		private TipDialog(Component owner, int initialTipNumber) {
			super(resolveOwner(owner), Messages.getString("TipOfTheDay.dialogTitle"), ModalityType.APPLICATION_MODAL);
			this.tips = TIP_ENTRIES;
			this.currentIndex = Math.max(0, Math.min(initialTipNumber - 1, tips.size() - 1));
			this.nextTipNumber = 1;

			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			setResizable(true);
			FlatUiSupport.applyMinimumSize(this, new Dimension(620, 420));
			setSize(new Dimension(760, 500));
			setLocationRelativeTo(owner);
			setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));

			this.showOnStartupCheckBox = new JCheckBox(Messages.getString("TipOfTheDay.showOnStartupText"), showTip);
			this.showOnStartupCheckBox.addActionListener(e -> showTip = this.showOnStartupCheckBox.isSelected());

			setContentPane(buildContentPane());
			refreshTipContent();

			installEscapeBindingHook();
		}

		private JPanel buildContentPane() {
			JPanel content = new JPanel(new BorderLayout(0, 0));
			content.setBorder(BorderFactory.createEmptyBorder(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING));
			content.setBackground(FlatUiSupport.dialogBackground());
			content.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));

			JPanel header = createHeaderPanel();
			JPanel card = createTipCard();
			JPanel footer = createFooterPanel();

			content.add(header, BorderLayout.NORTH);
			content.add(card, BorderLayout.CENTER);
			content.add(footer, BorderLayout.SOUTH);
			return content;
		}

		private JPanel createHeaderPanel() {
			HeaderPanel header = new HeaderPanel();
			header.setLayout(new BorderLayout(14, 0));
			header.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

			Icon hintIcon = IconManager.getIcon("ribbon.hint");
			tipIconLabel.setIcon(hintIcon);
			tipIconLabel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
			tipIconLabel.setVerticalAlignment(JLabel.TOP);

			JPanel textColumn = new JPanel();
			textColumn.setOpaque(false);
			textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));

			didYouKnowLabel.setFont(didYouKnowLabel.getFont().deriveFont(Font.BOLD, didYouKnowLabel.getFont().getSize2D() + 1f));
			didYouKnowLabel.setForeground(FlatUiSupport.labelForeground());
			didYouKnowLabel.setAlignmentX(0f);

			tipTitleLabel.setFont(tipTitleLabel.getFont().deriveFont(Font.BOLD, tipTitleLabel.getFont().getSize2D() + 4f));
			tipTitleLabel.setForeground(FlatUiSupport.labelForeground());
			tipTitleLabel.setAlignmentX(0f);

			tipCounterLabel.setFont(tipCounterLabel.getFont().deriveFont(Font.PLAIN, tipCounterLabel.getFont().getSize2D() - 1f));
			tipCounterLabel.setForeground(FlatUiSupport.disabledForeground());
			tipCounterLabel.setAlignmentX(0f);

			textColumn.add(didYouKnowLabel);
			textColumn.add(Box.createVerticalStrut(6));
			textColumn.add(tipTitleLabel);
			textColumn.add(Box.createVerticalStrut(4));
			textColumn.add(tipCounterLabel);

			header.add(tipIconLabel, BorderLayout.WEST);
			header.add(textColumn, BorderLayout.CENTER);
			return header;
		}

		private JPanel createTipCard() {
			CardPanel card = new CardPanel();
			card.setLayout(new BorderLayout(0, 0));
			card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

			tipDescriptionArea.setEditable(false);
			tipDescriptionArea.setOpaque(true);
			tipDescriptionArea.setBackground(FlatUiSupport.dialogSurfaceBackground());
			tipDescriptionArea.setForeground(FlatUiSupport.labelForeground());
			tipDescriptionArea.setFont(FlatUiSupport.uiFont().deriveFont(Font.PLAIN, FlatUiSupport.uiFont().getSize2D() + 1f));
			tipDescriptionArea.setLineWrap(true);
			tipDescriptionArea.setWrapStyleWord(true);
			tipDescriptionArea.setFocusable(false);
			tipDescriptionArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			tipDescriptionArea.setMargin(new Insets(0, 0, 0, 0));
			tipDescriptionArea.setColumns(40);
			tipDescriptionArea.setRows(10);

			JScrollPane scrollPane = new JScrollPane(tipDescriptionArea);
			scrollPane.setBorder(BorderFactory.createEmptyBorder());
			scrollPane.setOpaque(false);
			scrollPane.getViewport().setOpaque(true);
			scrollPane.getViewport().setBackground(FlatUiSupport.dialogSurfaceBackground());
			scrollPane.setPreferredSize(new Dimension(600, 240));

			JPanel textShell = new JPanel(new BorderLayout(0, 12));
			textShell.setOpaque(false);
			textShell.add(scrollPane, BorderLayout.CENTER);

			card.add(textShell, BorderLayout.CENTER);
			return card;
		}

		private JPanel createFooterPanel() {
			JPanel footer = new JPanel(new BorderLayout(12, 0));
			footer.setOpaque(false);
			footer.setBorder(BorderFactory.createEmptyBorder(14, 4, 0, 4));

			showOnStartupCheckBox.setOpaque(false);
			showOnStartupCheckBox.setFont(FlatUiSupport.uiFont());

			JPanel buttonBar = buildButtonBar();
			footer.add(showOnStartupCheckBox, BorderLayout.WEST);
			footer.add(buttonBar, BorderLayout.EAST);
			return footer;
		}

		private JPanel buildButtonBar() {
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
			buttons.setOpaque(false);

			previousButton = new JButton(Messages.getString("TipOfTheDay.previousTipText"));
			previousButton.addActionListener(this::showPreviousTip);
			FlatUiSupport.styleDialogButton(previousButton, false);

			nextButton = new JButton(Messages.getString("TipOfTheDay.nextTipText"));
			nextButton.addActionListener(this::showNextTip);
			FlatUiSupport.styleDialogButton(nextButton, true);

			closeButton = new JButton(Messages.getString("TipOfTheDay.closeText"));
			closeButton.addActionListener(e -> dispose());
			FlatUiSupport.styleDialogButton(closeButton, false);

			buttons.add(previousButton);
			buttons.add(nextButton);
			buttons.add(closeButton);
			getRootPane().setDefaultButton(nextButton);
			return buttons;
		}

		private void refreshTipContent() {
			TipEntry tip = tips.get(currentIndex);
			tipTitleLabel.setText(tip.name == null ? "" : tip.name);
			tipDescriptionArea.setText(tip.description == null ? "" : tip.description);
			tipDescriptionArea.setCaretPosition(0);
			tipCounterLabel.setText((currentIndex + 1) + " / " + tips.size());
			nextTipNumber = getNextVisibleTipIndex(currentIndex) + 1;
			getRootPane().setDefaultButton(nextButton);
		}

		private void showPreviousTip(ActionEvent event) {
			currentIndex = getPreviousVisibleTipIndex(currentIndex);
			refreshTipContent();
		}

		private void showNextTip(ActionEvent event) {
			currentIndex = getNextVisibleTipIndex(currentIndex);
			refreshTipContent();
		}

		private int getNextTipNumber() {
			return nextTipNumber;
		}

		private void installEscapeBindingHook() {
			AWTEventListener escapeBinder = new AWTEventListener() {
				public void eventDispatched(AWTEvent event) {
					if (!(event instanceof WindowEvent)) {
						return;
					}
					Window window = ((WindowEvent) event).getWindow();
					if (((WindowEvent) event).getID() != WindowEvent.WINDOW_OPENED || !(window instanceof JDialog)) {
						return;
					}
					PopupDialogSupport.bindEscapeToDispose((JDialog) window);
				}
			};
			Toolkit.getDefaultToolkit().addAWTEventListener(escapeBinder, AWTEvent.WINDOW_EVENT_MASK);
			addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					Toolkit.getDefaultToolkit().removeAWTEventListener(escapeBinder);
				}
			});
		}

		private int getNextVisibleTipIndex(int index) {
			return (index + 1) % tips.size();
		}

		private int getPreviousVisibleTipIndex(int index) {
			return (index - 1 + tips.size()) % tips.size();
		}

		private static final class HeaderPanel extends JPanel {
			private static final long serialVersionUID = 1L;

			private HeaderPanel() {
				setOpaque(false);
			}

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				try {
					FlatUiSupport.enableAntialiasing(g2);
					Color start = FlatUiSupport.blend(FlatUiSupport.dialogBackground(), FlatUiSupport.accentColor(), 0.93f);
					Color end = FlatUiSupport.dialogBackground();
					g2.setPaint(new java.awt.GradientPaint(0, 0, start, 0, getHeight(), end));
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
					g2.setColor(FlatUiSupport.blend(FlatUiSupport.borderColor(), end, 0.78f));
					g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
					g2.setColor(FlatUiSupport.accentColor());
					g2.fillRoundRect(0, 0, 6, getHeight(), 6, 6);
				} finally {
					g2.dispose();
				}
				super.paintComponent(g);
			}
		}

		private static final class CardPanel extends JPanel {
			private static final long serialVersionUID = 1L;

			private CardPanel() {
				setOpaque(false);
			}

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				try {
					FlatUiSupport.enableAntialiasing(g2);
					g2.setColor(FlatUiSupport.dialogSurfaceBackground());
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
					g2.setColor(FlatUiSupport.blend(FlatUiSupport.borderColor(), FlatUiSupport.dialogSurfaceBackground(), 0.76f));
					g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
				} finally {
					g2.dispose();
				}
				super.paintComponent(g);
			}
		}
	}

	private static Window resolveOwner(Component owner) {
		if (owner == null) {
			return null;
		}
		Window window = SwingUtilities.getWindowAncestor(owner);
		if (window != null) {
			return window;
		}
		return (owner instanceof Window) ? (Window) owner : null;
	}
}
