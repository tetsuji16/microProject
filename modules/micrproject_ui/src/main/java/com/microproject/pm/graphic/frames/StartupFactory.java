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
package com.microproject.pm.graphic.frames;

import java.awt.Container;
import java.awt.HeadlessException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;


import com.microproject.company.DefaultUser;
import com.microproject.configuration.Configuration;
import com.microproject.configuration.ConfigurationReader;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.Settings;
import com.microproject.util.ClassLoaderUtils;
import com.microproject.dialog.LoginDialog;
import com.microproject.dialog.LoginForm;
import com.microproject.dialog.UpdateChecker;
import com.microproject.pm.graphic.laf.LafManagerImpl;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.server.access.PartnerInfo;
import com.microproject.server.data.ProjectData;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.DebugUtils;
import com.microproject.util.Environment;
import com.microproject.util.VersionUtils;

@SuppressWarnings("deprecation")
public abstract class StartupFactory {
	private static final Logger logger = Logger.getLogger(StartupFactory.class.getName());
	public static final String defaultServerUrl = Settings.SITE_HOME;
	private static final int NUM_INVALID_LOGINS = 3;


	protected String serverUrl=null;
	protected String[] projectUrls=null;
	protected String login=null;
	protected String password=null;
	protected Map<String, String> credentials = new HashMap<String, String>();
	protected long projectId;
	protected HashMap<String, Object> opts = null;

	protected StartupFactory() {
//		System.out.println("---------- StartupFactory");
	}

	/**
	 * Used to test restoring of workspace to simulate applet restart
	 * @param old
	 * @return
	 */
	public GraphicManager restart(GraphicManager old) {
		RootPaneContainer con = (RootPaneContainer) old.getContainer();
		old.encodeWorkspace();
		old.cleanUp();
		con.getContentPane().removeAll();
		GraphicManager g = instanceFromExistingSession((Container) con);
//		g.decodeWorkspace();

//		System.out.println("restarted");
		return g;
	}

	public GraphicManager instanceFromExistingSession(Container container) {


		System.gc(); // hope to avoid out of memory problems

		DebugUtils.isMemoryOk(true);


		long t=System.currentTimeMillis();
//		System.out.println("---------- StartupFactory instanceFromExistingSession#1");
		final GraphicManager graphicManager = new GraphicManager(container);
		graphicManager.setStartupFactory(this);
		SessionFactory.getInstance().setJobQueue(graphicManager.getJobQueue());
		//if (Environment.isNewLook())
			graphicManager.initLookAndFeel();
//		System.out.println("---------- StartupFactory instanceFromExistingSession#1 done in "+(System.currentTimeMillis()-t)+" ms");
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				long t=System.currentTimeMillis();
//				System.out.println("---------- StartupFactory instanceFromExistingSession#2");
				graphicManager.initView();
//				System.out.println("---------- StartupFactory instanceFromExistingSession#2 done in "+(System.currentTimeMillis()-t)+" ms");
			}});
//		graphicManager.invalidate();
		return graphicManager;
	}


	public GraphicManager instanceFromNewSession(Container container,  final boolean doWelcome) {
		long t=System.currentTimeMillis();
//		System.out.println("---------- StartupFactory instanceFromNewSession#1 main");
		Environment.setClientSide(true);

		// System.setSecurityManager(null); // DISABLED for Java 17+ compatibility
		Thread loadConfigThread=new Thread("loadConfig"){
			public void run() {
				long t=System.currentTimeMillis();
//				System.out.println("---------- StartupFactory instanceFromNewSession#1 doLoadConfig");
				doLoadConfig();
//				System.out.println("---------- StartupFactory instanceFromNewSession#1 doLoadConfig done in "+(System.currentTimeMillis()-t)+" ms");
			}
		};
		loadConfigThread.start();

		GraphicManager graphicManager = null;
		//String projectUrl[]=null;
		try {
			graphicManager=new GraphicManager(/*projectUrl,*/serverUrl,container);
			graphicManager.setStartupFactory(this);
		} catch (HeadlessException e) {
			logger.log(Level.SEVERE, "Failed to create GraphicManager", e);
		}
		graphicManager.setConnected(false);

		if (!doLogin(graphicManager)) return null;
		markLoginSuccessful(graphicManager);
		//if (Environment.isNewLook())
			graphicManager.initLookAndFeel();

		SessionFactory.getInstance().setJobQueue(graphicManager.getJobQueue());

		PartnerInfo partnerInfo=null;
		if (!Environment.getStandAlone()) {
			Session session = SessionFactory.getInstance().getSession(false);
			try {
				partnerInfo=(PartnerInfo)SessionFactory.call(session,"retrievePartnerInfo",null,null);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Failed to retrieve partner info", e);
			}
		}

//		System.out.println("---------- StartupFactory instanceFromNewSession#1 main done in "+(System.currentTimeMillis()-t)+" ms");
		try {
			loadConfigThread.join();
		} catch (InterruptedException e1) {
			Thread.currentThread().interrupt();
			logger.log(Level.WARNING, "Interrupted while waiting for config load", e1);
		}

		t=System.currentTimeMillis();
//		System.out.println("---------- StartupFactory instanceFromNewSession#2");

		if (partnerInfo!=null){

			if (partnerInfo.getConfigurationXML() != null) {
				ConfigurationReader.readString(partnerInfo.getConfigurationXML(),Configuration.getInstance());
				Configuration.getInstance().setDonePopulating();
			}
			if (partnerInfo.getViewXML() != null) {
				ConfigurationReader.readString(partnerInfo.getViewXML(),Dictionary.getInstance());
			}
		}

		final GraphicManager gm = graphicManager;
		graphicManager.beginInitialization();
		try{

			graphicManager.initView();
			doStartupAction(gm,projectId,(projectUrls==null&&gm.getLastFileName()!=null)?new String[]{gm.getLastFileName()}:projectUrls,doWelcome,false);

			doPostInitView(gm.getContainer());
			UpdateChecker.checkInBackground(gm.getPreferences());
			
			
			
//			final Container cc=container;
//			SwingUtilities.invokeLater(new Runnable() {
//
//			    @Override
//			    public void run() {
//					//cc.setVisible(true);
//					gm.initView();
//					doStartupAction(gm,projectId,(projectUrls==null&&gm.getLastFileName()!=null)?new String[]{gm.getLastFileName()}:projectUrls,doWelcome,false);
//
//					doPostInitView(gm.getContainer());
//			    }
//			});


		}finally{
			graphicManager.finishInitialization();
		}
        return graphicManager;
	}

	public void doLoadConfig() {
		com.microproject.init.Init.initialize();
	}
	public void doPostInitView(Container container) {
	}

	/**
	 * Completes the command-state transition shared by standalone and server
	 * startup. A successful standalone login used to return before restoring this
	 * state, leaving New, Open and Import permanently disabled.
	 */
	static void markLoginSuccessful(GraphicManager graphicManager) {
		if (graphicManager != null)
			graphicManager.setConnected(true);
	}

	public boolean doLogin(GraphicManager graphicManager) {
		if (Environment.getStandAlone()){
//			graphicManager.getFrame().setVisible(true);
			Environment.setUser(new DefaultUser());
			return true;
		}
		credentials.put("serverUrl",serverUrl);
		getCredentials();
		Environment.setNewLook(true);

		int badLoginCount = 0;
		while (true) { // until a good login or exit because of too many bad
//			graphicManager.getFrame().setVisible(true);
			if (login==null||password==null || badLoginCount > 0){
				URL loginUrl=null;
				if (login==null||password==null){
					try {
						loginUrl=new URL(serverUrl+"/login");
					} catch (MalformedURLException e) {
						logger.log(Level.WARNING, "Invalid login server URL: " + serverUrl, e);
					}
				}
				LoginForm form = LoginDialog.doLogin(graphicManager.getFrame(),loginUrl); // it's actually a singleton
				if (form.isCancelled())
					System.exit(-1);
				if (form.isUseMenus())
					Environment.setNewLook(true);

				login=form.getLogin();
				password=form.getPassword();
			}

			if ("_SA".equals(login)||Environment.getStandAlone()) {// for testing purposes!
				Environment.setStandAlone(true);
				Environment.setUser(new DefaultUser());
				break;
			} else {
				credentials.put("login",login);
				credentials.put("password",password);


				SessionFactory.getInstance().setCredentials(credentials);
				try {
					Session session = SessionFactory.getInstance().getSession(false);
					logger.fine("logging in");
					final GraphicManager gm = graphicManager;
					SessionFactory.callNoEx(session,"login",new Class[]{Consumer.class},new Object[]{new Consumer<Object>() { public void accept(Object arg0) {
							Map<String,String> env=(Map<String,String>)arg0;
							if (env!=null){
								String serverVersion=env.get("serverVersion");
								checkServerVersion(serverVersion);
							}
						}
					}});
					if (!((Boolean)SessionFactory.callNoEx(session,"isLicensedToRunClient",null,null)).booleanValue()) {
						Alert.error(Messages.getString("Error.roleCantRunClient"));
						abort();
						return false;
					}

//					System.out.println("Application started with args: credentials=" + credentials.get("login") + " name " + session.getUser().getName() + " Roles " + session.getUser().getServerRoles());
					break;
				} catch (Exception e) {
					if (Session.EXPIRED.equals(e.getMessage())) {
						Alert.error(Messages.getString("Error.accountExpired"));
						abort();
						return false;

					}
					logger.log(Level.WARNING, "Login failed", e);
					badLoginCount++;
					SessionFactory.getInstance().clearSessions();

					if (badLoginCount == NUM_INVALID_LOGINS) {
						Alert.error(Messages.getString("Login.tooManyBad"));
						abort();
						return false;
					} else {
						Alert.error(Messages.getString("Login.error"));
					}
				}
			}
		}
		return true;
	}

	protected void checkServerVersion(String serverVersion){
		String thisVersion=null;
		if (serverVersion!=null){
			thisVersion=VersionUtils.getVersion();
			if (thisVersion!=null) thisVersion=VersionUtils.toAppletVersion(thisVersion);
			if(thisVersion==null||serverVersion.equals(thisVersion)) return; //ok
		}
		String jnlpUrl="";//https://www.projectlibre.com/web/jnlp/projectlibre.jnlp";
		if (Alert.okCancel(Messages.getString("Text.newPODVersion"))){
			try {
				Object basicService = ClassLoaderUtils.forName("javax.jnlp.ServiceManager").getMethod("lookup", new Class[]{String.class})
				.invoke(null, new Object[] {"javax.jnlp.BasicService"});
				ClassLoaderUtils.forName("javax.jnlp.BasicService").getMethod("showDocument", new Class[]{URL.class})
				.invoke(basicService, new Object[] {new URL(jnlpUrl)});
			} catch(Exception e) {
				//e.printStackTrace();
				// Not running in JavaWebStart or service is not supported.
				return;
				//Runtime.getRuntime().exec("javaws ");
			}
//			try {
//			BasicService basicService=(BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
//			basicService.showDocument(/*new URL(basicService.getCodeBase(),*/new URL(jnlpUrl));
//			}catch (UnavailableServiceException e) {
//			Runtime.getRuntime().exec("javaws ");
//			}
			System.exit(0);
		}
	}




/*
 * Returns null if shouldn't open, returns false if open read only, true if open writable
 *
 */	public static Boolean verifyOpenWritable(Long projectId) {
		if (projectId == null || projectId == 0)
			return null;
		if (ProjectFactory.getInstance().isResourcePoolOpenAndWritable()) {
			Alert.warn(Messages.getString("Warn.resourcePoolOpen"));
			return null;
		}

		String locker = getLockerName(projectId);
		boolean openAs = false;
		if (locker != null) {
			openAs = (JOptionPane.YES_OPTION == Alert.confirmYesNo(Messages.getStringWithParam("Warn.lockMessage",locker)));
			if (openAs == false)
				return null;
		}
		return !openAs;
	}
	public static String getLockerName(long projectId) {
		ProjectData projectData = (ProjectData)ProjectFactory.getProjectData(projectId);
		if (projectData == null) {
			return null;
		}
		logger.fine("Locked is " + projectData.isLocked() + "  Lock info: User is " + Environment.getUser().getUniqueId() + "  locker id is " + projectData.getLockedById() + " locker is " + projectData.getLockedByName());

		if (projectData != null && projectData.isLocked()) {

			if (Environment.getUser().getUniqueId() != projectData.getLockedById())
				return projectData.getLockedByName();
		}
		return null;
	}



	protected abstract void abort();
	protected void getCredentials() {
	}
	public void doStartupAction(final GraphicManager gm, final long projectId, final String[] projectUrls, final boolean welcome, boolean readOnly) {
		if (Environment.isClientSide()) {
			if (projectId > 0) {

				Boolean writable = null;
				if (readOnly)
					writable = Boolean.FALSE;
				else
					writable = verifyOpenWritable(projectId);
				if (writable == null)
					return;
				gm.loadDocument(projectId, true,!writable,new Consumer<Object>() { public void accept(Object arg0) {
						Project project=(Project)arg0;
						DocumentFrame frame=gm.getCurrentFrame();
						if (frame!=null&&frame.getProject().getUniqueId() != projectId) {
							gm.switchToProject(projectId);
						}
					}
				});
			}
			else if (projectUrls!=null && projectUrls.length > 0) {
				// A desktop invocation may contain several file names.  Route all of
				// them through the same serial local-file flow as File/Open so every
				// requested project obtains its own registered document window.
				gm.openLocalProjectsSequentially(projectUrls);
			}else{
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (gm.offerRecoveryAtStartup()) {
							return;
						}
						if (welcome&&!Environment.isPlugin()) {
							if (!Environment.isProjectLibre()) {
								if (Environment.isNeedToRestart())
									return;
								if (!LafManagerImpl.isLafOk()) // for startup glitch - we don't want people to work until restarting.
									return;
							}
							gm.doWelcomeDialog();
						}
						if (Environment.isPlugin()) gm.doNewProjectNoDialog(opts);
					}
				});

			}
		}

	}


}

