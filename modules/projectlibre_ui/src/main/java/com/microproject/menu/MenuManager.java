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
package com.microproject.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.pushingpixels.flamingo.api.common.JCommandToggleButton;

import com.projectlibre.ui.ribbon.SwingRibbonFactory;
import com.projectlibre.ui.ribbon.SwingRibbonModel;
import com.projectlibre.ui.ribbon.CustomRibbonBandGenerator;
import com.microproject.pm.graphic.TabbedNavigation;
import com.microproject.preference.ConfigurationFile;
import com.microproject.util.ClassLoaderUtils;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;

/**
 *
 */
public class MenuManager {
	private static final Logger logger = Logger.getLogger(MenuManager.class.getName());
	private static final String MENU_BUNDLE = "com.microproject.menu.menu";
	private static final String MENU_INTERNAL_BUNDLE = "com.microproject.menu.menuInternal";
	private static final String MENU_BUNDLE_CONF_DIR = "menu";
	public static final String STANDARD_MENU ="StandardMenuBar";
	public static final String MAC_STANDARD_MENU ="MacStandardMenuBar";
	public static final String SERVER_STANDARD_MENU ="ServerStandardMenuBar";
	public static final String SF_MENU ="SFMenuBar";
	public static final String STANDARD_TOOL_BAR ="StandardToolBar";
	public static final String MAC_STANDARD_TOOL_BAR ="MacStandardToolBar";
	public static final String FILE_TOOL_BAR ="FileToolBar";
	public static final String BIG_TOOL_BAR ="BigToolBar";
	public static final String VIEW_TOOL_BAR ="ViewToolBar";
	public static final String VIEW_TOOL_BAR_WITH_NO_SUB_VIEW_OPTION ="ViewToolBarNoSubView";
	public static final String RIBBON_VIEW_BAR ="RibbonViewToolBar";
	public static final String PRINT_PREVIEW_TOOL_BAR ="PrintPreviewToolBar";

	public static final String STANDARD_RIBBON = "StandardRibbon";

	//private static MenuManager instance = null;
	static ResourceBundle[] bundles;
	/*static*/ ExtMenuFactory menuFactory;
	ExtToolBarFactory toolBarFactory;
	SwingRibbonFactory ribbonFactory;

	private final Collection<TabbedNavigation> tabbedNavigations = new LinkedList<>();

	public void add(TabbedNavigation t) {
		tabbedNavigations.add(t);
	}
	private MenuManager(ProjectMenuActionMap rootActionMap) {
		ResourceBundle internalBundle=null,bundle=null;
				
		if (bundle==null){
			try{
				bundle=ConfigurationFile.getDirectoryBundle(MENU_BUNDLE_CONF_DIR);
			}catch(Exception e){
				logger.log(Level.FINE, "Failed to load menu bundle from config directory", e);
			}
			if (internalBundle==null) internalBundle =  ResourceBundle.getBundle(MENU_INTERNAL_BUNDLE,Locale.getDefault(),ClassLoaderUtils.getLocalClassLoader());
			if (bundle==null) bundle =  ResourceBundle.getBundle(MENU_BUNDLE,Locale.getDefault(),ClassLoaderUtils.getLocalClassLoader());
			bundles=new ResourceBundle[]{internalBundle,bundle};
		}
	menuFactory = new ExtMenuFactory(rootActionMap,bundles);
	toolBarFactory = new ExtToolBarFactory(rootActionMap,bundles);
	if (Environment.isRibbonUI()) ribbonFactory = new SwingRibbonFactory(toolBarFactory, bundles);
	}

	public static MenuManager getInstance(ProjectMenuActionMap rootActionMap) {
//		if (instance == null)
//			instance = new MenuManager(rootActionMap);
//		return instance;
		return new MenuManager(rootActionMap);
	}

	public JMenuBar getMenu(String name) {
		JMenuBar menuBar = menuFactory.createJMenuBar(name);
		menuBar.setOpaque(true);
		menuBar.setBackground(FlatUiSupport.ribbonChromeBackground());
		return menuBar;
	}

	public JPopupMenu getPopupMenu(String name) {
		return menuFactory.createJPopupMenuBar(name);
	}

	public static String getMenuString(String key) {
    	MissingResourceException exception=null;
    	String s=null;
    	for (ResourceBundle bundle : bundles){
    		try {
				s=bundle.getString(key);
				exception=null;
			} catch (MissingResourceException e) {
				exception=e;
				continue;
			}
    		if (s!=null) break;
    	}
    	if (exception!=null) throw exception;
    	return s;
	}
	public String getString(String key) {
		return menuFactory.getString(key);
	}

    public String getStringOrNull(String key) {
    	try {
    	   return getString(key);
    	} catch (MissingResourceException e) {
    		return null;
    	}
    }

	public String getActionStringFromId(String id) {
		String result = menuFactory.getActionStringFromId(id);
		if (result == null)
			logger.warning(() -> "Invalid item: " + id + " it must be a menu item in the menu properties, even if it's only shown in a toolbar");
		return result;
	}

    public Action getActionFromId(String id) {
		return menuFactory.getActionFromId(id);
    }
    public String getStringFromAction(Action action) {
		return menuFactory.getStringFromAction(action);
    }

	public JMenuItem getMenuItemFromId(String id) {
		return menuFactory.getMenuItemFromId(id);
	}
	public List<?> getToolButtonsFromId(String id) {
		ArrayList<Object> result = new ArrayList<>();
		List<?> toolbarButtons = toolBarFactory.getButtonsFromId(id);
		if (toolbarButtons != null)
			result.addAll(toolbarButtons);
		if (ribbonFactory != null) {
			List<?> ribbonButtons = ribbonFactory.getButtonsFromId(id);
			if (ribbonButtons != null)
				result.addAll(ribbonButtons);
		}
		return result.isEmpty() ? null : result;
	}

	public final ExtToolBarFactory getToolBarFactory() {
		return toolBarFactory;
	}
	public final SwingRibbonFactory getRibbonFactory() {
		return ribbonFactory;
	}
	public JToolBar getToolBar(String name) {
		JToolBar toolBar = toolBarFactory.createJToolBar(name);
		FlatUiSupport.styleToolBar(toolBar);
		return toolBar;
	}
	public void initComponent(String name, JComponent component) {
		toolBarFactory.initJComponent(name,component);
	}
	public SwingRibbonModel getRibbon(String name, CustomRibbonBandGenerator customBandsGenerator) {
		return ribbonFactory == null ? null : ribbonFactory.createModel(name, customBandsGenerator);
	}

	public JPanel createRibbonPanel(String name, Runnable helpAction) {
		return createRibbonPanel(name, null, helpAction);
	}

	public JPanel createRibbonPanel(String name, CustomRibbonBandGenerator customBandsGenerator, Runnable helpAction) {
		return ribbonFactory == null ? null : ribbonFactory.createPanel(name, customBandsGenerator, helpAction);
	}

	public void setActionEnabled(String id, boolean enable) {
		Action action = getActionFromId(id);
		if (action != null)
			action.setEnabled(enable);
		Collection<?> buttons = getToolButtonsFromId(id);
		if (buttons != null) {
			Iterator<?> i = buttons.iterator();
			while (i.hasNext()) {
				Object button = i.next();
				if (button instanceof AbstractButton)
					((AbstractButton)button).setEnabled(enable);
			}
		}
		JMenuItem menuItem = menuFactory.getMenuItemFromId(id);
		if (menuItem != null)
			menuItem.setEnabled(enable);
	}
	public void setActionVisible(String id, boolean enable) {
		Collection<?> buttons = getToolButtonsFromId(id);
		if (buttons != null) {
			Iterator<?> i = buttons.iterator();
			while (i.hasNext()) {
				Object button = i.next();
				if (button instanceof AbstractButton)
					((AbstractButton)button).setVisible(enable);
			}
		}
		JMenuItem menuItem = menuFactory.getMenuItemFromId(id);
		if (menuItem != null)
			menuItem.setVisible(enable);
	}
	public void setActionSelected(String id, boolean enable) {
		Action action = getActionFromId(id);
		if (action != null)
			action.putValue(Action.SELECTED_KEY, enable);
		Collection<?> buttons = getToolButtonsFromId(id);
		if (buttons != null) {
			Iterator<?> i = buttons.iterator();
			while (i.hasNext()) {
				Object button = i.next();
				if (button instanceof AbstractButton) {
					((AbstractButton)button).setSelected(enable);
					if (button instanceof JToggleButton) {
					//	button.setBackground(enable ? Color.GRAY : ExtButtonFactory.BACKGROUND_COLOR);
					}
				} else if (button instanceof JCommandToggleButton) {
					((JCommandToggleButton)button).getActionModel().setSelected(enable);
				}
			}
		}
		JMenuItem menuItem = menuFactory.getMenuItemFromId(id);
		if (menuItem != null)
			menuItem.setSelected(enable);
		for (TabbedNavigation tabbedNavigation : tabbedNavigations) {
			tabbedNavigation.setActivatedView(id, enable);
		}

	}
	public void setText(String id, String text) {
		Collection<?> buttons = getToolButtonsFromId(id);
		if (buttons != null) {
			Iterator<?> i = buttons.iterator();
			while (i.hasNext()) {
				Object button = i.next();
				if (button instanceof AbstractButton)
					((AbstractButton)button).setToolTipText(text);
			}
		}
		JMenuItem menuItem = menuFactory.getMenuItemFromId(id);
		if (menuItem != null)
			menuItem.setText(text);
	}
	
	
    public String getTextForId(String id) {
    	return menuFactory.getTextForId(id);
    }
    public String getFullTipText(String name) {
		String s = getStringOrNull(name + MenuTextKeys.TOOLTIP_SUFFIX);
		if (s != null) {
			String help = getStringOrNull(name+MenuTextKeys.HELP_SUFFIX);
			String demo = getStringOrNull(name+MenuTextKeys.DEMO_SUFFIX);
			String doc = getStringOrNull(name+MenuTextKeys.DOC_SUFFIX);

			if (doc != null)
				s = HyperLinkToolTip.helpTipText(s,help,demo, doc);
		}
		return s;
    }

}

