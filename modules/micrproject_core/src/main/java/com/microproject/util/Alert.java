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
package com.microproject.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.microproject.strings.Messages;

/**
 *
 */
public class Alert {
	private static final Logger logger = Logger.getLogger(Alert.class.getName());
	public static void warn(Object errorObject) {
		if (allowPopups())
			warn(errorObject,getFrame());
	}
	public static void warn(Object errorObject, Component parent) {
		logger.log(Level.WARNING, "warning message {0}", errorObject);

		if (allowPopups())
			PopupDialogSupport.showMessageDialog(parent,errorObject, Messages.getContextString("Title.ProjectLibreWarning"),JOptionPane.WARNING_MESSAGE);
	}

	public static void error(Object errorObject) {
		if (allowPopups())
			error(errorObject,getFrame());
	}
	public static void error(Object errorObject, Component parent) {
		logger.log(Level.SEVERE, "error message {0}", errorObject);

		if (allowPopups())
			PopupDialogSupport.showMessageDialog(parent,errorObject, Messages.getContextString("Title.ProjectLibreError"),JOptionPane.ERROR_MESSAGE);
	}
	public static int confirmYesNo(Object messageObject) {
		if (!allowPopups())
			return JOptionPane.NO_OPTION;
		return PopupDialogSupport.showConfirmDialog(getFrame(),
		        messageObject,
		        Messages.getContextString("Text.ApplicationTitle"),
	            JOptionPane.YES_NO_OPTION);
	}
	public static int confirm(Object messageObject) {
		if (!allowPopups())
			return JOptionPane.NO_OPTION;
		int result = PopupDialogSupport.showConfirmDialog(getFrame(),
		        messageObject,
		        Messages.getContextString("Text.ApplicationTitle"),
	            JOptionPane.YES_NO_CANCEL_OPTION,
	            JOptionPane.QUESTION_MESSAGE,
	            JOptionPane.CANCEL_OPTION);
		if (result == JOptionPane.CLOSED_OPTION)
			result = JOptionPane.CANCEL_OPTION;
		return result;
	}
	public static boolean okCancel(Object messageObject) {
		if (!allowPopups())
			return true;

		return JOptionPane.OK_OPTION == PopupDialogSupport.showConfirmDialog(getFrame(),
		        messageObject,
		        Messages.getContextString("Text.ApplicationTitle"),
	            JOptionPane.OK_CANCEL_OPTION,
	            JOptionPane.QUESTION_MESSAGE,
	            JOptionPane.CANCEL_OPTION);
	}

	public static String renameProject(final String name,Set projectNames,boolean saveAs){
		try {
			return (String)Class.forName(GRAPHIC_MANAGER).getMethod("doRenameProjectDialog",new Class[]{String.class,Set.class,boolean.class}).invoke(getGraphicManager(),new Object[]{name,projectNames,saveAs});
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to open rename project dialog", e);
			return null;
		}
	}


	private static final String GRAPHIC_MANAGER="com.microproject.pm.graphic.frames.GraphicManager";
	public static Frame getFrame(){
		try {
		    return (Frame)Class.forName(GRAPHIC_MANAGER).getMethod("getFrameInstance", new Class<?>[0]).invoke(null, new Object[0]);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to get main frame", e);
			return null;
		}
	}
	public static Object getGraphicManager(){
		try {
		    return Class.forName(GRAPHIC_MANAGER).getMethod("getInstance", new Class<?>[0]).invoke(null, new Object[0]);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to get GraphicManager", e);
			return null;
		}
	}
	public static boolean allowPopups() {
		return Environment.isClientSide() && !Environment.isBatchMode();
	}
	public static Object getGraphicManagerMethod(String method) {
		try {
			return Class.forName(GRAPHIC_MANAGER).getMethod(method, new Class<?>[0]).invoke(null, new Object[0]);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to invoke GraphicManager method: " + method, e);
			return null;
		}
	}
	public static void setGraphicManagerMethod(String method,Object value) {
		try {
			Class.forName(GRAPHIC_MANAGER).getMethod(method,new Class[] {Object.class}).invoke(null,new Object[] {value});
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to invoke GraphicManager setter: " + method, e);
		}
	}

	public static void warnWithOnceOption(Object object,String preference) {
		warnWithOnceOption(object,preference,null);
	}
	public static void warnWithOnceOption(Object object,String preference,Component parentComponent) {
		boolean warned =  Preferences.userNodeForPackage(Alert.class).getBoolean(preference,false);
		if (warned)
			return;
		JOptionPane pane = new JOptionPane(object);
		String title=Messages.getContextString("Text.ApplicationTitle");
		JDialog dialog = pane.createDialog(parentComponent,title);
		PopupDialogSupport.bindEscapeToOptionPane(dialog, pane, JOptionPane.CLOSED_OPTION);
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT));
		JCheckBox notAgain = new JCheckBox(Messages.getString("Text.doNotShowAgain"));
		p.add(notAgain);
		pane.add(p);
		Dimension d=dialog.getSize();
		d.height+=40; // for extra height of checkbox
		dialog.setSize(d);
		dialog.setVisible(true);
		if (notAgain.isSelected())
			Preferences.userNodeForPackage(Alert.class).putBoolean(preference,true);


	}

}
