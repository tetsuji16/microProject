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
package com.microproject.pm.graphic.views;

import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JComponent;

import com.microproject.dialog.AbstractDialog;
import com.microproject.dialog.ButtonPanel;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.task.Portfolio;
import com.microproject.strings.Messages;

public class ProjectsDialog extends AbstractDialog{
	private static ProjectsDialog instance = null;
	private GraphicManager graphicManager =null;
	private ProjectView projectView;
	public static void show(GraphicManager graphicManager) {
		if (instance == null) {
			instance = new ProjectsDialog(graphicManager);
			instance.pack();
			instance.setModal(false);
    	}
		instance.setLocationRelativeTo(graphicManager.getFrame());
		instance.setVisible(true);
	}
	private ProjectsDialog(GraphicManager graphicManager) {
		super(graphicManager.getFrame(), Messages.getString("File.projects"), false);
		this.graphicManager = graphicManager;
	}
	@Override
	public JComponent createContentPanel() {
		Portfolio portfolio = graphicManager.getProjectFactory().getPortfolio();
		projectView = new ProjectView(portfolio.getNodeModel(), portfolio);
		instance.setPreferredSize(new Dimension(800,250));
		return projectView;
	}
	public ButtonPanel createButtonPanel() {
		return null;
	}

}

