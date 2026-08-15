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
package com.microproject.pm.graphic.laf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.util.Environment;
import com.microproject.util.FlatLafSupport;
import com.microproject.util.FlatUiSupport;

public class LafManagerImpl implements LafManager {
    private static final Logger logger = Logger.getLogger(LafManagerImpl.class.getName());
    protected static LookAndFeel plaf = null; // for substance
    protected static GraphicManager graphicManager;
	private static Boolean lafOK = null;
	private static boolean dialogButtonArrowTraversalInstalled = false;
	private static final int BUTTON_AXIS_HORIZONTAL = 0;
	private static final int BUTTON_AXIS_VERTICAL = 1;
    public LafManagerImpl(GraphicManager graphicManager){
    	this.graphicManager=graphicManager;
    }

	public void clean(){
		if (plaf!=null){
			plaf.uninitialize();
			plaf = null;
		}
	}

    public static boolean isLafOk() {
    	if (lafOK == null) {
	    	UIDefaults d = UIManager.getDefaults();
	    	Object cl = d.get("ClassLoader");
	    	JPanel target = new JPanel();
	    	ClassLoader uiClassLoader =
	    		(cl != null) ? (ClassLoader)cl : target.getClass().getClassLoader();
	    	Class uiClass = d.getUIClass(target.getUIClassID(), uiClassLoader);
	    	lafOK = (uiClass != null);
    	}
    	return lafOK;
    }

    public LookAndFeel getPlaf() {
    	if (plaf == null) {
			try {
				FlatLafSupport.ensureInitialized();
				plaf = UIManager.getLookAndFeel();

			} catch (Exception e) {
				logger.log(Level.WARNING, "Failed to initialize look and feel", e);
			}
			if (graphicManager!=null) SwingUtilities.updateComponentTreeUI(graphicManager.getContainer());
    	}
    	return plaf;
    }
    public void initLookAndFeel() {
		if (plaf == null)
			getPlaf();
		installDialogButtonArrowTraversal();
    }

	private static void installDialogButtonArrowTraversal() {
		if (dialogButtonArrowTraversalInstalled)
			return;
		dialogButtonArrowTraversalInstalled = true;
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new java.awt.KeyEventDispatcher() {
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (e == null || e.getID() != KeyEvent.KEY_PRESSED)
					return false;
				int keyCode = e.getKeyCode();
				if (keyCode != KeyEvent.VK_RIGHT && keyCode != KeyEvent.VK_LEFT && keyCode != KeyEvent.VK_DOWN && keyCode != KeyEvent.VK_UP)
					return false;
				if (moveDialogButtonFocus(getButtonFocusDirection(keyCode), getButtonFocusAxis(keyCode))) {
					e.consume();
					return true;
				}
				return false;
			}
		});
	}

	private static int getButtonFocusDirection(int keyCode) {
		return keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_DOWN ? 1 : -1;
	}

	private static int getButtonFocusAxis(int keyCode) {
		return keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN ? BUTTON_AXIS_VERTICAL : BUTTON_AXIS_HORIZONTAL;
	}

	private static boolean moveDialogButtonFocus(int direction, int requestedAxis) {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (!(focusOwner instanceof JButton))
			return false;
		Window dialog = SwingUtilities.getWindowAncestor(focusOwner);
		if (!(dialog instanceof Dialog))
			return false;

		List buttons = new ArrayList();
		collectFocusableButtons(dialog, buttons);
		if (buttons.size() < 2)
			return false;
		int buttonAxis = getDialogButtonAxis(dialog, buttons);
		if (buttonAxis != requestedAxis)
			return false;
		sortButtons(dialog, buttons, buttonAxis);
		int current = buttons.indexOf(focusOwner);
		if (current < 0)
			return false;
		int target = current + direction;
		if (target < 0 || target >= buttons.size())
			return true;
		((Component)buttons.get(target)).requestFocusInWindow();
		return true;
	}

	private static void collectFocusableButtons(Container container, List buttons) {
		Component[] components = container.getComponents();
		for (int i = 0; i < components.length; i++) {
			Component component = components[i];
			if (component instanceof JButton && component.isVisible() && component.isEnabled() && component.isFocusable())
				buttons.add(component);
			if (component instanceof Container)
				collectFocusableButtons((Container)component, buttons);
		}
	}

	private static int getDialogButtonAxis(Container dialog, List buttons) {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (int i = 0; i < buttons.size(); i++) {
			Rectangle bounds = getDialogRelativeBounds(dialog, (Component)buttons.get(i));
			minX = Math.min(minX, bounds.x);
			minY = Math.min(minY, bounds.y);
			maxX = Math.max(maxX, bounds.x + bounds.width);
			maxY = Math.max(maxY, bounds.y + bounds.height);
		}
		return (maxY - minY) > (maxX - minX) ? BUTTON_AXIS_VERTICAL : BUTTON_AXIS_HORIZONTAL;
	}

	private static void sortButtons(final Container dialog, List buttons, final int axis) {
		Collections.sort(buttons, new Comparator() {
			public int compare(Object o1, Object o2) {
				Rectangle b1 = getDialogRelativeBounds(dialog, (Component)o1);
				Rectangle b2 = getDialogRelativeBounds(dialog, (Component)o2);
				if (axis == BUTTON_AXIS_VERTICAL) {
					int y = b1.y - b2.y;
					return y == 0 ? b1.x - b2.x : y;
				}
				int x = b1.x - b2.x;
				return x == 0 ? b1.y - b2.y : x;
			}
		});
	}

	private static Rectangle getDialogRelativeBounds(Container dialog, Component component) {
		return SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), dialog);
	}


	public void setColorTheme(String viewName ) {
	}

	public void changePalette(){
	}

	public boolean isChangePaletteAllowed(LookAndFeel lookAndFeel){
		return false;

	}

	public void paintComponent(Graphics g,Component component,boolean selected){
		if (Environment.isMac()){
			g.setColor(GraphicManager.getInstance().getLafManager().getUnselectedBackgroundColor());
			Rectangle bounds = component.getBounds();
			g.fillRect(0, 0,bounds.width,bounds.height);
		}
	}

	public void setUI(JTabbedPane component){
		FlatUiSupport.styleTabbedPane(component);
	}

	public void setColorScheme(JComponent component){
	}

	public void paintTimeScale(Graphics2D g2,int x,int y,int w,int h,Shape[] shapes){

	}


	public Color getSelectedBackgroundColor() {
		return FlatUiSupport.tableSelectionBackground();
	}
	public Color getUnselectedBackgroundColor() {
		return FlatUiSupport.surfaceBackground();
	}

	public void dumpUIValues() {
		String v[] = new String[] {
			"Label.background"
			,"Table.focusCellBackground"
			,"Menu.selectionBackground"
			,"Table.focusCellBackground"
			,"TabbedPane.darkShadow"
			,"Table.focusCellForeground"
			,"Table.selectionBackground"
			,"TableHeader.background"
			,"TextField.selectionBackground"
		};
		for (int i = 0; i < v.length; i++)
			logger.log(Level.FINE, "{0}={1}", new Object[] { v[i], UIManager.get(v[i]) });

	}

	public static void main(String [] x) {
		FlatLafSupport.initialize();
		logger.log(Level.INFO, "windows laf {0}", isWindowsLAF());
		logger.log(Level.INFO, "LAF : {0}", UIManager.getLookAndFeel().getClass().getName());
		outputSwingDefs();
	}
	public static void outputSwingDefs() {
		String lineSep = System.getProperty("line.separator");
		javax.swing.UIDefaults uid = javax.swing.UIManager.getDefaults();
		java.util.Enumeration uidKeys = uid.keys();

		while (uidKeys.hasMoreElements()) {
			Object aKey = uidKeys.nextElement();
			Object aValue = uid.get(aKey);
			String str = "KEY: " + aKey + ", VALUE: " + aValue + lineSep;
			logger.fine(str);
		}
	}

	public static boolean isWindowsLAF() {
		return UIManager.getLookAndFeel() != null
			&& "com.sun.java.swing.plaf.windows.WindowsLookAndFeel".equals(UIManager.getLookAndFeel().getClass().getName());
	}
	public boolean isToolbarOpaque() {
		return Environment.isNewLaf() || isWindowsLAF();
	}

}

