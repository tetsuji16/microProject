/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.AlertPresenter;

/** Swing implementation for user alerts requested across application layers. */
public final class SwingAlertPresenter implements AlertPresenter {
	private static final Logger logger = Logger.getLogger(SwingAlertPresenter.class.getName());

	@Override
	public void warn(Object message, Object parent) {
		PopupDialogSupport.showMessageDialog(parent(parent), message,
				Messages.getContextString("Title.ProjectLibreWarning"), JOptionPane.WARNING_MESSAGE);
	}

	@Override
	public void error(Object message, Object parent) {
		PopupDialogSupport.showMessageDialog(parent(parent), message,
				Messages.getContextString("Title.ProjectLibreError"), JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public int confirmYesNo(Object message) {
		return PopupDialogSupport.showConfirmDialog(getFrame(), message,
				Messages.getContextString("Text.ApplicationTitle"), JOptionPane.YES_NO_OPTION);
	}

	@Override
	public int confirm(Object message) {
		int result = PopupDialogSupport.showConfirmDialog(getFrame(), message,
				Messages.getContextString("Text.ApplicationTitle"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, JOptionPane.CANCEL_OPTION);
		return result == JOptionPane.CLOSED_OPTION ? JOptionPane.CANCEL_OPTION : result;
	}

	@Override
	public boolean okCancel(Object message) {
		return JOptionPane.OK_OPTION == PopupDialogSupport.showConfirmDialog(getFrame(), message,
				Messages.getContextString("Text.ApplicationTitle"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, JOptionPane.CANCEL_OPTION);
	}

	@Override
	public String renameProject(String name, Set<String> projectNames, boolean saveAs) {
		try {
			GraphicManager manager = GraphicManager.getInstance();
			if (manager == null) {
				logger.warning("No active GraphicManager for rename project dialog");
				return null;
			}
			return manager.doRenameProjectDialog(name, projectNames, saveAs);
		} catch (RuntimeException e) {
			logger.log(Level.WARNING, "Failed to open rename project dialog", e);
			return null;
		}
	}

	@Override
	public void warnWithOnceOption(Object message, String preference, Object parent) {
		Preferences preferences = Preferences.userNodeForPackage(Alert.class);
		if (preferences.getBoolean(preference, false))
			return;
		JOptionPane optionPane = new JOptionPane(message);
		String title = Messages.getContextString("Text.ApplicationTitle");
		JDialog dialog = optionPane.createDialog(parent(parent), title);
		PopupDialogSupport.bindEscapeToOptionPane(dialog, optionPane, JOptionPane.CLOSED_OPTION);
		JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JCheckBox notAgain = new JCheckBox(Messages.getString("Text.doNotShowAgain"));
		options.add(notAgain);
		optionPane.add(options);
		Dimension size = dialog.getSize();
		size.height += 40;
		dialog.setSize(size);
		dialog.setVisible(true);
		if (notAgain.isSelected())
			preferences.putBoolean(preference, true);
	}

	private static Component parent(Object parent) {
		return parent instanceof Component component ? component : getFrame();
	}

	private static Frame getFrame() {
		try {
			Object documentFrame = GraphicManager.getDocumentFrameInstance();
			if (documentFrame instanceof Frame frame && frame.isShowing())
				return frame;
			Frame fallback = GraphicManager.getFrameInstance();
			if (fallback != null && fallback.isShowing())
				return fallback;
		} catch (RuntimeException e) {
			logger.log(Level.FINE, "No GraphicManager document frame available", e);
		}
		Window[] windows = Window.getWindows();
		for (int i = windows.length - 1; i >= 0; i--) {
			Window window = windows[i];
			if (window.isShowing() && window instanceof Frame frame)
				return frame;
		}
		return null;
	}
}
