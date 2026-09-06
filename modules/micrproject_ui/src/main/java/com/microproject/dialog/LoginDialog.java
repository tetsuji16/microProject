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

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.IconManager;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.ClassLoaderUtils;
import com.microproject.util.SafeObjectInput;

public final class LoginDialog extends AbstractDialog {
	private static final Logger logger = Logger.getLogger(LoginDialog.class.getName());
	private static final long serialVersionUID = 1L;

	private LoginForm form;

	// use property utils to copy to project like struts
	JTextField login;
	JPasswordField password;
	JCheckBox storeCredentials;
	JCheckBox useMenus;
	
	private JFrame standaloneFrame = null;
	
	private static String getDialogTitle() {
		return Messages.getContextString("Text.ApplicationTitle"); //$NON-NLS-1$
	}
	public static LoginForm doLogin(Frame owner,URL serverUrl) {
		
		if (owner == null) {
			final JFrame standaloneFrame = new JFrame(getDialogTitle());
			standaloneFrame.setIconImage(IconManager.getImage("application.icon")); //$NON-NLS-1$
			standaloneFrame.addWindowListener(new WindowListener() {
				public void windowOpened(WindowEvent arg0) {}
				public void windowClosing(WindowEvent arg0) {}
				public void windowClosed(WindowEvent arg0) {}
				public void windowIconified(WindowEvent arg0) {}
				public void windowDeiconified(WindowEvent arg0) {}
				public void windowDeactivated(WindowEvent arg0) {}
				public void windowActivated(WindowEvent arg0) {
					Window w[] = standaloneFrame.getOwnedWindows();
					for (int i = 0; i < w.length; i++)
						w[i].toFront();
				}
			});
			owner = standaloneFrame;
		}

		LoginForm form=null;
		if (serverUrl!=null)
		try {
			Object ps=ClassLoaderUtils.getLocalClassLoader().loadClass("javax.jnlp.ServiceManager").getMethod("lookup",new Class[]{String.class}).invoke(null,new Object[]{"javax.jnlp.PersistenceService"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//			Object ps=Class.forName("javax.jnlp.ServiceManager").getMethod("lookup",new Class[]{String.class}).invoke(null,new Object[]{"javax.jnlp.PersistenceService"});
			Object contents=ps.getClass().getMethod("get",new Class[]{URL.class}).invoke(ps,new Object[]{serverUrl}); //$NON-NLS-1$
			try (var in = SafeObjectInput.create((InputStream)ClassLoaderUtils.getLocalClassLoader().loadClass("javax.jnlp.FileContents").getMethod("getInputStream", new Class[0]).invoke(contents, new Object[0]))) { //$NON-NLS-1$ //$NON-NLS-2$
//			ObjectInputStream in=new ObjectInputStream((InputStream)Class.forName("javax.jnlp.FileContents").getMethod("getInputStream",null).invoke(contents,null));
			form=(LoginForm)in.readObject();
			}
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to load login form from JNLP persistence service", e);
		}
		
		
		LoginDialog dlg = getInstance(owner,form);
		
		// make sure dialog shows
		dlg.requestFocus();
// Because setAlwaysOnTop is not in JDK 1.4, I added treatment as per http://www.codecomments.com/archive250-2004-12-347421.html		-HK 25/2/05
//		dlg.setAlwaysOnTop(true); // this is not in JDK 1.4!!!
		
		dlg.doModal();
		
		if (serverUrl!=null)
		try {			
			Object ps=Class.forName("javax.jnlp.ServiceManager").getMethod("lookup",new Class[]{String.class}).invoke(null,new Object[]{"javax.jnlp.PersistenceService"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (dlg.form.isStoreCredentials()){
				
				if (form==null) ps.getClass().getMethod("create",new Class[]{URL.class,long.class}).invoke(ps,new Object[]{serverUrl,Long.valueOf(1000)}); //$NON-NLS-1$
				Object contents=ps.getClass().getMethod("get",new Class[]{URL.class}).invoke(ps,new Object[]{serverUrl}); //$NON-NLS-1$
				ObjectOutputStream out=new ObjectOutputStream((OutputStream)Class.forName("javax.jnlp.FileContents").getMethod("getOutputStream",new Class[]{boolean.class}).invoke(contents,new Object[]{Boolean.TRUE})); //$NON-NLS-1$ //$NON-NLS-2$
				out.writeObject(dlg.form);
				out.close();
			} else if (form!=null) ps.getClass().getMethod("delete",new Class[]{URL.class}).invoke(ps,new Object[]{serverUrl}); //$NON-NLS-1$
			
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to store login form in JNLP persistence service", e);
		}
		
		
		
		return dlg.form;
	}
	
	public static LoginDialog getInstance(Frame owner,LoginForm form) {
		return new LoginDialog(owner,form);
	}

	private LoginDialog(Frame owner,LoginForm form) {
		super(owner, getDialogTitle(), true);
		this.form = (form==null)?new LoginForm():form;
		
		addWindowListener(new WindowListener() {
			public void windowOpened(WindowEvent arg0) {}
			public void windowClosing(WindowEvent arg0) {}
			public void windowClosed(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowActivated(WindowEvent arg0) {
				toFront();
			}
		});
	}

	// Component Creation and Initialization **********************************

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		login = new JTextField();
		password = new JPasswordField();
		storeCredentials=new JCheckBox();
		useMenus=new JCheckBox();
		bind(true);
	}

	protected boolean bind(boolean get) {
		if (get) {
			login.setText(form.getLogin());
			password.setText(form.getPassword());
			storeCredentials.setSelected(form.isStoreCredentials());
			useMenus.setSelected(form.isUseMenus());
		} else {
			if (login.getText().trim().length() == 0 || password.getPassword().length == 0) // prevent empty fields
				return false; 
			form.setLogin(login.getText());
			form.setPassword(new String(password.getPassword())); 
			form.setStoreCredentials(storeCredentials.isSelected());
			form.setUseMenus(useMenus.isSelected());
		}
		return true;
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
		initControls();
		FormLayout layout = new FormLayout("default, 3dlu, 120dlu:grow", // cols //$NON-NLS-1$
				FlatUiSupport.preferredFormRows(7)); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(Messages.getString("LoginDialog.Login"), login); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(Messages.getString("LoginDialog.Password"), password); //$NON-NLS-1$
		builder.nextLine(2);
//		builder.append(useMenus);
//		builder.append(Messages.getString("LoginDialog.UseOfficeLook")); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(storeCredentials);
		builder.append(Messages.getString("LoginDialog.RememberMe")); //$NON-NLS-1$
		
		return builder.getPanel();
	}
	/**
	 * @return Returns the form.
	 */
	public LoginForm getForm() {
		return form;
	}
	protected void onCancel() {
		getForm().setCancelled(true);
		super.onCancel();
	}
	
	

}
