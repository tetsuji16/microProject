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

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.workspace.FrameHolder;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.ui.ribbon.ModernRibbonPanel;
import com.microproject.ui.shell.WindowShellInstaller;
import com.microproject.util.Environment;

public class MainRibbonFrame extends JFrame implements FrameHolder{
	private static final long serialVersionUID = -5161903673269959353L;
	protected GraphicManager graphicManager;
	private JPanel ribbonPanel;
	private JPanel backstageGlassPane;
	private Runnable windowCloseAction;

	public MainRibbonFrame(String name, String projectUrl, String server) throws HeadlessException {
		super(name);
		setIconImage(IconManager.getImage("application.icon"));
		init();
	}

	public GraphicManager getGraphicManager() {
		return graphicManager;
	}

	public void init() {
		// Install before the frame becomes displayable. FlatLaf then delegates the
		// border, caption drag, system menu, and window controls to Windows.
		WindowShellInstaller.installOfficeRibbonShell(this);
		installBackstageHost();
		if (Environment.isMac()) setPreferredSize(new Dimension(1280, 768));
		else setPreferredSize(new Dimension(1024, 768));
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE/*DISPOSE_ON_CLOSE*/);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (windowCloseAction != null)
					windowCloseAction.run();
				else if (graphicManager != null)
					graphicManager.closeApplication();
			}
		});
	}

	private void installBackstageHost() {
		backstageGlassPane = new JPanel(new java.awt.BorderLayout());
		backstageGlassPane.setOpaque(true);
		backstageGlassPane.setBackground(com.microproject.util.FlatUiSupport.panelBackground());
		backstageGlassPane.setVisible(false);
		setGlassPane(backstageGlassPane);
		getRootPane().putClientProperty(ModernRibbonPanel.BACKSTAGE_HOST_PROPERTY,
			new ModernRibbonPanel.BackstageHost() {
				@Override public void showBackstage(JComponent content) {
					backstageGlassPane.removeAll();
					javax.swing.JButton close = new javax.swing.JButton("←");
					close.setToolTipText("Back");
					close.addActionListener(event -> hideBackstage());
					JPanel header = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 12, 8));
					header.setOpaque(false);
					header.add(close);
					backstageGlassPane.add(header, java.awt.BorderLayout.NORTH);
					backstageGlassPane.add(content, java.awt.BorderLayout.CENTER);
					backstageGlassPane.setVisible(true);
					backstageGlassPane.revalidate();
					backstageGlassPane.repaint();
				}

				@Override public void hideBackstage() {
					backstageGlassPane.setVisible(false);
					backstageGlassPane.removeAll();
				}
			});
	}

	/** Installs a document-scoped close action for secondary windows. */
	public void setWindowCloseAction(Runnable windowCloseAction) {
		this.windowCloseAction = windowCloseAction;
	}

	public void setGraphicManager(GraphicManager graphicManager) {
		this.graphicManager = graphicManager;
		getRootPane().setTransferHandler(new ProjectFileDropTransferHandler(graphicManager));
	}

	public JPanel getRibbonPanel() {
		return ribbonPanel;
	}

	public void setRibbonPanel(JPanel ribbonPanel) {
		if (this.ribbonPanel != null) {
			getContentPane().remove(this.ribbonPanel);
		}
		this.ribbonPanel = ribbonPanel;
		if (ribbonPanel != null) {
			getContentPane().add(ribbonPanel, java.awt.BorderLayout.NORTH);
		}
		getContentPane().revalidate();
		getContentPane().repaint();
		if (graphicManager != null) graphicManager.updateRibbonContext(graphicManager.getTopViewId());
	}

	/** Updates view-specific tabs without rebuilding or re-registering commands. */
	public void setVisibleContextualRibbonTabs(java.util.Collection<String> tabIds) {
		if (ribbonPanel == null) return;
		Object value = ribbonPanel.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		if (value instanceof ModernRibbonPanel panel) panel.setVisibleContextualTabs(tabIds);
	}

	public void setContextualRibbonTabTitles(java.util.Map<String, String> titles) {
		if (ribbonPanel == null) return;
		Object value = ribbonPanel.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		if (value instanceof ModernRibbonPanel panel) panel.setContextualTabTitles(titles);
	}

	public FrameManager getFrameManager() {
		return graphicManager.getFrameManager();
	}

	public void setVisible(boolean visible){
		super.setVisible(visible);
	}
	


}
