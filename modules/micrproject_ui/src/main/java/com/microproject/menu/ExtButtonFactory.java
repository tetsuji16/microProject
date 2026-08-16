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

import java.awt.Color;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

import com.microproject.menu.resource.ButtonFactory;
import com.microproject.menu.resource.MissingListenerException;
import com.microproject.menu.resource.ResourceFormatException;

import com.microproject.help.HelpUtil;

/**
 * 
 */
public class ExtButtonFactory extends ButtonFactory {
	public final static String TYPE_SUFFIX = ".type";
	private final static String SELECTED_SUFFIX = ".selected";
	public final static String VISIBLE_SUFFIX = ".visible";
	public final static String DOC_SUFFIX = ".doc";
	public static Color BACKGROUND_COLOR = null;
	public AbstractButton createJToolbarButton(String name) throws MissingResourceException, ResourceFormatException, MissingListenerException {
		String type = null;
		AbstractButton result = null;
		try {
			type = getString(name + TYPE_SUFFIX);
		} catch (MissingResourceException e) {
		}
		if (type != null && type.equals("TOGGLE")) {
			result = new JToggleButton();
			if (BACKGROUND_COLOR == null)
				BACKGROUND_COLOR = result.getBackground();
			initializeButton(result, name);

			// is the button selected?
			try {
				result.setSelected(getBoolean(name + SELECTED_SUFFIX));
			} catch (MissingResourceException e) {
			}


		}
		if (result == null)
			result = super.createJToolbarButton(name);
		String help = getStringOrNull(name + DOC_SUFFIX);
		if (help != null)
			HelpUtil.addDocHelp(result,help);
		return result;
		
	}

	/**
	 * @param rb
	 * @param am
	 */
	public ExtButtonFactory(ProjectMenuActionMap am, ResourceBundle...rb) {
		super(new BatikActionMapAdapter(am), rb);
	}

	public JComponent createJComboBox(String name)
			throws MissingResourceException, ResourceFormatException,
			MissingListenerException {
		JComboBox result = new JComboBox();
		return result;
	}

}

