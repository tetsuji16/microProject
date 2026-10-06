/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.microproject.ui.ribbon.RibbonBackstageHost;

/** Places Backstage over the ribbon and workspace while leaving Office title chrome interactive. */
final class OfficeBackstageHost implements RibbonBackstageHost {
	private final JComponent header;
	private final JLayeredPane layeredPane;
	private JPanel overlay;
	private ComponentAdapter resizeListener;

	OfficeBackstageHost(JFrame frame, JComponent header) {
		this.header = header;
		this.layeredPane = frame.getRootPane().getLayeredPane();
	}

	@Override
	public void show(JComponent backstageView, Runnable dismiss) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> show(backstageView, dismiss));
			return;
		}
		hide();
		overlay = new JPanel(null);
		overlay.setName("officeBackstageOverlay");
		overlay.setOpaque(true);
		overlay.setBackground(backstageView.getBackground());
		overlay.add(backstageView);
		overlay.putClientProperty("microproject.backstage.view", backstageView);
		resizeListener = new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent event) { layoutOverlay(); }
			@Override public void componentMoved(ComponentEvent event) { layoutOverlay(); }
		};
		layeredPane.addComponentListener(resizeListener);
		layeredPane.add(overlay, JLayeredPane.MODAL_LAYER);
		layoutOverlay();
		installEscape(backstageView, dismiss);
		Object focusTarget = backstageView.getClientProperty("microproject.backstage.initialFocus");
		if (focusTarget instanceof Component component) component.requestFocusInWindow();
		else backstageView.requestFocusInWindow();
	}

	private void layoutOverlay() {
		if (overlay == null) return;
		int top = 0;
		if (header.isShowing() && layeredPane.isShowing()) {
			top = SwingUtilities.convertPoint(header, 0, header.getHeight(), layeredPane).y;
		} else {
			top = Math.max(0, header.getHeight());
		}
		top = Math.min(top, layeredPane.getHeight());
		overlay.setBounds(0, top, layeredPane.getWidth(), layeredPane.getHeight() - top);
		Component[] children = overlay.getComponents();
		if (children.length > 0) children[0].setBounds(0, 0, overlay.getWidth(), overlay.getHeight());
		overlay.revalidate();
		overlay.repaint();
	}

	private void installEscape(JComponent backstageView, Runnable dismiss) {
		backstageView.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeBackstage");
		backstageView.getActionMap().put("closeBackstage", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent event) { dismiss.run(); }
		});
	}

	@Override
	public void hide() {
		if (overlay == null) return;
		layeredPane.removeComponentListener(resizeListener);
		layeredPane.remove(overlay);
		overlay = null;
		resizeListener = null;
		layeredPane.revalidate();
		layeredPane.repaint();
	}
}
