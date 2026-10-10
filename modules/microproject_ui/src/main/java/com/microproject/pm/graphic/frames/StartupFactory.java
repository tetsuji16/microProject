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
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;


import com.microproject.dialog.UpdateChecker;
import com.microproject.ui.util.DesktopBrowserLauncher;
import com.microproject.ui.util.SwingAlertPresenter;
import com.microproject.ui.util.SwingUiDispatcher;
import com.microproject.ui.util.SwingJobQueueUiProvider;
import com.microproject.pm.graphic.laf.LafManagerImpl;
import com.microproject.session.SessionFactory;
import com.microproject.util.Alert;
import com.microproject.util.BrowserControl;
import com.microproject.util.UiDispatch;
import com.microproject.job.JobQueueUiServices;
import com.microproject.grouping.core.transform.TransformParameterDialogServices;
import com.microproject.dialog.TransformParameterDialog;
import com.microproject.util.DebugUtils;
import com.microproject.util.Environment;

@SuppressWarnings("deprecation")
public abstract class StartupFactory {
	private static final Logger logger = Logger.getLogger(StartupFactory.class.getName());
	static {
		Alert.setPresenter(new SwingAlertPresenter());
		BrowserControl.setBrowserLauncher(new DesktopBrowserLauncher());
		UiDispatch.setDispatcher(new SwingUiDispatcher());
		JobQueueUiServices.setProvider(new SwingJobQueueUiProvider());
		TransformParameterDialogServices.setProvider(() -> {
			TransformParameterDialog dialog = new TransformParameterDialog();
			return dialog::accept;
		});
	}
	protected String[] projectUrls=null;
	protected HashMap<String, Object> opts = null;

	protected StartupFactory() {
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

		return g;
	}

	public GraphicManager instanceFromExistingSession(Container container) {


		System.gc(); // hope to avoid out of memory problems

		DebugUtils.isMemoryOk(true);


		long t=System.currentTimeMillis();
		final GraphicManager graphicManager = new GraphicManager(container);
		graphicManager.setRestartAction(() -> restart(graphicManager));
		SessionFactory.getInstance().setJobQueue(graphicManager.getJobQueue());
		//if (Environment.isNewLook())
			graphicManager.initLookAndFeel();
		Runnable initialize = () -> {
			graphicManager.beginInitialization();
			try {
				graphicManager.initView();
			} finally {
				graphicManager.finishInitialization();
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			initialize.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(initialize);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while restoring the application window", exception);
			} catch (java.lang.reflect.InvocationTargetException exception) {
				throw new IllegalStateException("Failed to restore the application window", exception.getCause());
			}
		}
//		graphicManager.invalidate();
		return graphicManager;
	}


	public GraphicManager instanceFromNewSession(Container container,  final boolean doWelcome) {
		long t=System.currentTimeMillis();
		Environment.setClientSide(true);

		// System.setSecurityManager(null); // DISABLED for Java 17+ compatibility
		Thread loadConfigThread = new Thread(() -> {
			long configLoadStartTime = System.currentTimeMillis();
			doLoadConfig();
		}, "loadConfig");
		loadConfigThread.start();

		GraphicManager graphicManager = null;
		//String projectUrl[]=null;
		try {
			GraphicManager newGraphicManager = new GraphicManager(container);
			newGraphicManager.setRestartAction(() -> restart(newGraphicManager));
			graphicManager = newGraphicManager;
		} catch (HeadlessException e) {
			logger.log(Level.SEVERE, "Failed to create GraphicManager", e);
		}
		Environment.setUser(new com.microproject.company.DefaultUser());
		Environment.setStandAlone(true);
		graphicManager.setConnected(true);
		//if (Environment.isNewLook())
			graphicManager.initLookAndFeel();

		SessionFactory.getInstance().setJobQueue(graphicManager.getJobQueue());

		try {
			loadConfigThread.join();
		} catch (InterruptedException e1) {
			Thread.currentThread().interrupt();
			logger.log(Level.WARNING, "Interrupted while waiting for config load", e1);
		}

		t=System.currentTimeMillis();

		final GraphicManager gm = graphicManager;
		graphicManager.beginInitialization();
		try{

			graphicManager.initView();
			doStartupAction(gm,(projectUrls==null&&gm.getLastFileName()!=null)?new String[]{gm.getLastFileName()}:projectUrls,doWelcome);

			doPostInitView(gm.getContainer());
			UpdateChecker.checkInBackground(gm.getPreferences());
			
			
			
//			final Container cc=container;
//			SwingUtilities.invokeLater(new Runnable() {
//
//			    @Override
//			    public void run() {
//					//cc.setVisible(true);
		}finally{
			graphicManager.finishInitialization();
			activateStartupWindow(gm.getContainer());
		}
        return graphicManager;
	}

	/**
	 * Showing a frame does not guarantee that Windows activates it after the
	 * startup bootstrap.  Restore the native shell's foreground/focus state so
	 * the next physical Robot or user click is delivered to the ribbon window.
	 */
	private static void activateStartupWindow(Container container) {
		if (!(container instanceof Window window)) return;
		Runnable activate = () -> {
			if (!window.isDisplayable() || !window.isVisible()) return;
			window.setFocusableWindowState(true);
			if (!window.isAlwaysOnTop()) window.setAlwaysOnTop(true);
			window.toFront();
			window.requestFocus();
			window.requestFocusInWindow();
		};
		window.addWindowListener(new WindowAdapter() {
			@Override public void windowOpened(WindowEvent event) { activate.run(); }
		});
		SwingUtilities.invokeLater(activate);
		// Windows can reject the first foreground request while the launcher is
		// still handing off focus. Retry briefly without listening to
		// windowActivated (that event is emitted by requestFocus itself and would
		// recurse through DefaultFrameManager).
		javax.swing.Timer retry = new javax.swing.Timer(100, null);
		final int[] attempts = {0};
		retry.addActionListener(event -> {
			if (!window.isDisplayable() || !window.isVisible() || window.isActive() || ++attempts[0] >= 10) {
				((javax.swing.Timer) event.getSource()).stop();
				if (window.isDisplayable() && window.isVisible() && window.isAlwaysOnTop())
					window.setAlwaysOnTop(false);
				return;
			}
			activate.run();
		});
		retry.setInitialDelay(100);
		retry.start();
	}

	public void doLoadConfig() {
		com.microproject.init.Init.initialize();
	}
	public void doPostInitView(Container container) {
	}

	public void doStartupAction(final GraphicManager gm, final String[] projectUrls, final boolean welcome) {
		if (Environment.isClientSide()) {
			if (projectUrls!=null && projectUrls.length > 0) {
				// A desktop invocation may contain several file names.  Route all of
				// them through the same serial local-file flow as File/Open so every
				// requested project obtains its own registered document window.
				gm.openLocalProjectsSequentially(projectUrls);
			}else{
				SwingUtilities.invokeLater(() -> {
					if (gm.offerRecoveryAtStartup()) {
						return;
					}
					if (welcome&&!Environment.isPlugin()) {
						if (!Environment.isProjectLibre() && !LafManagerImpl.isLafOk()) // for startup glitch - we don't want people to work until restarting.
							return;
						gm.doWelcomeDialog();
					}
					if (Environment.isPlugin()) gm.doNewProjectNoDialog(opts);
				});

			}
		}

	}


}

