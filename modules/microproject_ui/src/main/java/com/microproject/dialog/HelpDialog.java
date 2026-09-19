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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.strings.Messages;
import com.microproject.util.BrowserControl;
import com.microproject.util.UiLinkTargets;
import com.microproject.util.VersionUtils;

public final class HelpDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	private static final String helpUrl = UiLinkTargets.DOCUMENTATION_HOME;
	JButton link;
	JButton donate;
    JButton license;
	public static HelpDialog getInstance(Frame owner) {
		return new HelpDialog(owner);
	}

	private HelpDialog(Frame owner) {
		super(owner, Messages.format("Format.words",
				Messages.getString("HelpDialog.About"), Messages.getContextString("Text.ApplicationTitle")), true); //$NON-NLS-1$
	}

	protected void initComponents() {
		link = new JButton(Messages.getString("HelpDialog.GoToOnlineHelp")); //$NON-NLS-1$
		link.setEnabled(true);
		link.setToolTipText(helpUrl);
		link.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				BrowserControl.displayURL(helpUrl);
			}
		});
		donate = new JButton(UsabilityStrings.text("help.support"));
		donate.setToolTipText(UiLinkTargets.DONATE_HOME);
		donate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				BrowserControl.displayURL(UiLinkTargets.DONATE_HOME);
			}
		});

		license = new JButton(Messages.getString("HelpDialog.ShowLicense")); //$NON-NLS-1$
		license.setEnabled(true);
		license.setToolTipText(Messages.getString("HelpDialog.ShowLicense")); //$NON-NLS-1$
		license.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				LicenseDialog.showDialog(GraphicManager.getFrameInstance(),true);
			}
		});		


		super.initComponents();
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
		FormLayout layout = new FormLayout("250px,300px,250px" , // cols //$NON-NLS-1$
			
				"p, 6dlu, p, 6dlu, p, 6dlu, p, 1dlu, p, 1dlu, p, 1dlu, p, 1dlu, p, 6dlu, p, 6dlu, p, 6dlu, p, 10dlu, p, 6dlu, p, 6dlu, p"); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		JLabel logo = new JLabel(IconManager.getIcon("logo.microProject"));
		logo.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent arg0) {
				BrowserControl.displayURL(UiLinkTargets.PROJECT_HOME);
			}});
		builder.nextColumn();
		builder.append(logo); 
		builder.nextLine(2);
		builder.nextColumn();
		builder.append(link);
		builder.nextLine(2);
		builder.nextColumn();
		builder.append(donate);
		builder.nextLine(2);
		builder.nextLine(2);
		builder.nextColumn();
		builder.append(license);
		
		builder.nextLine(2);
		String version=VersionUtils.getVersion();
		builder.addLabel(Messages.getContextString("Text.ShortTitle")+" "+"Version "+(version==null?"Unknown":version),cc.xyw(1,  21, 3));
		builder.nextLine(2);
		builder.addLabel(Messages.getString("AboutDialog.copyright"),cc.xyw(1,  23, 3));

		
		return builder.getPanel();
	}
	protected boolean hasCloseButton() {
		return true;
	}

	protected boolean hasOkAndCancelButtons() {
		return false;
	}

}
