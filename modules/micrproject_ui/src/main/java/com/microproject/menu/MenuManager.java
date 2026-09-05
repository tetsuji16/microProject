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

import com.microproject.ui.ribbon.SwingRibbonFactory;
import com.microproject.ui.ribbon.SwingRibbonModel;
import com.microproject.ui.ribbon.CustomRibbonBandGenerator;
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

