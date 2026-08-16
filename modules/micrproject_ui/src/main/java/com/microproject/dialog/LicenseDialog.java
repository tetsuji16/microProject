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

import java.awt.Dimension;
import java.awt.Frame;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.BrowserControl;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;

public final class LicenseDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	JEditorPane license = null;
	JEditorPane thirdParty = null;
	private boolean init = false;
	private static boolean validated = Preferences.userNodeForPackage(LicenseDialog.class).getBoolean("validatedLicense",false); //$NON-NLS-1$
	private static boolean resetData;
	public static boolean showDialog(Frame owner, boolean force) {
		resetData=!force;
		if (!Environment.isProjectLibre() && !force)
			return false;
		if (!validated || force) {
			LicenseDialog dlg = new LicenseDialog(owner);
			if (!validated)
				dlg.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // force user to click a button
			dlg.doModal();
			return true;
		}
		if (Environment.isProjectLibre())
			System.setProperty("projectlibre.validation", Preferences.userNodeForPackage(LicenseDialog.class).get("licenseValidationDate","0"));
		return false;
	}

	private LicenseDialog(Frame owner) {
		super(owner, buildDialogTitle(
			Messages.getContextString("Text.ApplicationTitle"),
			Messages.getString("LicenseDialog.License")), true); //$NON-NLS-1$ //$NON-NLS-2$
		if (!Environment.isProjectLibre())
			validated = true; // POD validation is on web
	}

	static String buildDialogTitle(String applicationTitle, String licenseTitle) {
		if (licenseTitle == null || licenseTitle.isBlank()) {
			return applicationTitle;
		}
		if (applicationTitle == null || applicationTitle.isBlank()
				|| licenseTitle.regionMatches(true, 0, applicationTitle, 0, applicationTitle.length())) {
			return licenseTitle;
		}
		return applicationTitle + " " + licenseTitle;
	}

	public static URL resolveThirdPartyLicenseUrl() {
		return com.microproject.util.UiLinkTargets.bundledThirdPartyLicenseUrl();
	}


	private JEditorPane createEditorPane(URL url,final int fallbackHeight) {
		JEditorPane pane = null;
		try {
			pane = new JEditorPane();
			configureReadableHtml(pane);
			pane.setPage(url);
		} catch (Exception e) {
			if (!validated) {
				Alert.error(Messages.getString("LicenseDialog.CouldNotLoadExiting")); //$NON-NLS-1$
				System.exit(-1);
			} else {
				Alert.error(Messages.getString("LicenseDialog.CouldNotLoadLater")); //$NON-NLS-1$
				return null;

			}
		}

		pane.setEditable(false);
		pane.setAutoscrolls(true);
		pane.setSize(new Dimension(600, Short.MAX_VALUE));
		Dimension preferredSize = pane.getPreferredSize();
		int preferredHeight = preferredSize.height > 0 ? preferredSize.height : fallbackHeight;
		pane.setPreferredSize(new Dimension(600, preferredHeight));
		pane.setBounds(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
		pane.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType()== HyperlinkEvent.EventType.ACTIVATED)
					BrowserControl.displayURL(e.getURL().toExternalForm());
			}});
		return pane;
	}

	static void configureReadableHtml(JEditorPane pane) {
		pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		pane.setFont(UIManager.getFont("Label.font"));
	}
	protected void initComponents() {
		if (init)
			return;
		init = true;
		if (Environment.isProjectLibre()) {
			license = createEditorPane(getClass().getClassLoader().getResource("license/index.html"),7500); //$NON-NLS-1$
			thirdParty = createEditorPane(resolveThirdPartyLicenseUrl(),1200); //$NON-NLS-1$
		} else {
			thirdParty = createEditorPane(resolveThirdPartyLicenseUrl(),1200); //$NON-NLS-1$
			
		}
		
		
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
		FormLayout layout = new FormLayout("700px", // cols //$NON-NLS-1$
				"600px"); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		if (!Environment.isProjectLibre()) {
			builder.append(new JScrollPane(thirdParty));
		} else {
			JTabbedPane tabbed= new JTabbedPane();
			FlatUiSupport.styleTabbedPane(tabbed);
			tabbed.addTab(Messages.getString("LicenseDialog.License"),new JScrollPane(license));
			tabbed.addTab(Messages.getString("LicenseDialog.ThirdParty"),new JScrollPane(thirdParty));
			builder.append(tabbed);
		}

		JComponent result =  builder.getPanel();
		return result;
	}


	@Override
	public ButtonPanel createButtonPanel() {
		ButtonPanel bp = super.createButtonPanel();
		if (!validated)
			ok.setText(Messages.getString("ButtonText.IAccept")); //$NON-NLS-1$
		return bp;
	}

	@Override
	protected boolean hasCloseButton() {
		return validated;
	}

	@Override
	protected void onCancel() {
		if (!validated)
			System.exit(-1);
		super.onCancel();
	}

	@Override
	public void onOk() {
		validated = true;
		if (resetData){//About dialog shouldn't reset data
			Preferences.userNodeForPackage(LicenseDialog.class).put("licenseValidationDate",System.currentTimeMillis()+"."+(new Random()).nextInt(1000)); //$NON-NLS-1$
			Preferences.userNodeForPackage(LicenseDialog.class).putBoolean("validatedLicense",validated); //$NON-NLS-1$
			System.setProperty("projectlibre.validation", Preferences.userNodeForPackage(LicenseDialog.class).get("licenseValidationDate","0"));
		}
		super.onOk();
	}

}

