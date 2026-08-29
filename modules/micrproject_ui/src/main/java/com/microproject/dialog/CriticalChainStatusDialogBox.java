/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.graphic.views.CriticalChainBufferChartPanel;
import com.microproject.pm.graphic.views.CriticalChainGraphPanel;
import com.microproject.pm.task.Project;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.PopupDialogSupport;

/** Read-only CCPM result surface used by the Report and View ribbon commands. */
public final class CriticalChainStatusDialogBox extends JDialog {
	private static final long serialVersionUID = 1L;

	public enum Surface { BUFFER_STATUS, NETWORK }

	public static void show(Frame owner, Project project, Surface surface) {
		if (java.awt.GraphicsEnvironment.isHeadless()) return;
		new CriticalChainStatusDialogBox(owner, project, surface).setVisible(true);
	}

	private CriticalChainStatusDialogBox(Frame owner, Project project, Surface surface) {
		super(owner, UsabilityStrings.text(surface == Surface.NETWORK ? "ccpm.networkTitle" : "ccpm.bufferTitle"), true);
		FlatUiSupport.styleDialogRoot(getRootPane());
		PopupDialogSupport.bindEscapeToDispose(this);
		setLayout(new BorderLayout());

		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.findSettings(project);
		if (settings == null || !settings.isEnabled() || service.findBaseline(project) == null) {
			add(new JLabel(UsabilityStrings.text("ccpm.noAppliedPlan")), BorderLayout.CENTER);
		} else {
			JPanel content = new JPanel(new BorderLayout());
			content.add(new JLabel(UsabilityStrings.text("ccpm.loading")), BorderLayout.CENTER);
			add(content, BorderLayout.CENTER);
			loadAnalysis(service, project, surface, content);
		}

		JButton close = new JButton(UsabilityStrings.text("common.close"));
		close.addActionListener(event -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(close);
		add(buttons, BorderLayout.SOUTH);
		setPreferredSize(new Dimension(820, 510));
		pack();
		setLocationRelativeTo(owner);
	}

	private void loadAnalysis(CriticalChainService service, Project project, Surface surface, JPanel content) {
		new SwingWorker<CriticalChainService.Analysis, Void>() {
			@Override protected CriticalChainService.Analysis doInBackground() {
				return service.analysis(project);
			}
			@Override protected void done() {
				if (content == null) return;
				try {
					CriticalChainService.Analysis analysis = get();
					content.removeAll();
					if (analysis == null) content.add(new JLabel(UsabilityStrings.text("ccpm.noAppliedPlan")), BorderLayout.CENTER);
					else if (surface == Surface.NETWORK) {
						CriticalChainGraphPanel graph = new CriticalChainGraphPanel(project);
						graph.setAnalysis(analysis);
						content.add(new JScrollPane(graph), BorderLayout.CENTER);
					} else {
						CriticalChainBufferChartPanel chart = new CriticalChainBufferChartPanel(project);
						chart.setAnalysis(analysis, true);
						content.add(chart, BorderLayout.CENTER);
					}
				} catch (Exception exception) {
					content.removeAll();
					content.add(new JLabel(UsabilityStrings.text("ccpm.analysisFailed")), BorderLayout.CENTER);
				}
				content.revalidate();
				content.repaint();
			}
		}.execute();
	}
}
