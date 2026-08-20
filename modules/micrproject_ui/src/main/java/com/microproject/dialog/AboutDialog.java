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
package com.microproject.dialog;

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.main.Main;
import com.microproject.pm.graphic.IconManager;
import com.microproject.strings.Messages;
import com.microproject.util.BrowserControl;
import com.microproject.util.ClassLoaderUtils;
import com.microproject.util.Environment;
import com.microproject.util.UiLinkTargets;
import com.microproject.util.VersionUtils;

public final class AboutDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	public static AboutDialog getInstance(Frame owner) {
		return new AboutDialog(owner);
	}

	private AboutDialog(Frame owner) {
		super(owner, Messages.format("Format.words",
				Messages.getString("AboutDialog.About"), Messages.getContextString("Text.ApplicationTitle")), true); //$NON-NLS-1$
	}


	// Building *************************************************************

	/**
	 * Builds the panel. Initializes and configures components first, then
	 * creates a FormLayout, configures the layout, creates a builder, sets a
	 * border, and finally adds the components.
	 * 
	 * @return the built panel
	 */

	public JComponent createContentPanel() {
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		FormLayout layout = new FormLayout("default", // cols //$NON-NLS-1$
				"p, 3dlu,p, 3dlu, p, 3dlu, p, 10dlu,p"); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		JLabel logo = new JLabel(IconManager.getIcon("logo.microProject"));
		logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		logo.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent arg0) {
				BrowserControl.displayURL(UiLinkTargets.PROJECT_HOME);
			}});
		builder.append(logo); 
		builder.nextLine(2);
		builder.append(Messages.getContextString("Text.ShortTitle")); //$NON-NLS-1$
		builder.nextLine(2);
		String version=VersionUtils.getVersion();
		builder.append("Version "+(version==null?"Unknown":version)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$		
		builder.nextLine(2);
		builder.append(Messages.getString("AboutDialog.copyright")); //$NON-NLS-1$
//		if (Environment.isProjectLibre()) {
//			builder.nextLine(2);
//			builder.append(Main.getRunSinceMessage()); //$NON-NLS-1$
//		}
		return builder.getPanel();
	}
	protected boolean hasCloseButton() {
		return true;
	}

	protected boolean hasOkAndCancelButtons() {
		return false;
	}

}
