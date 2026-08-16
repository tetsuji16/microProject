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
package com.microproject.pm.graphic.frames.workspace;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.border.LineBorder;

import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.session.LoadOptions;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;
import com.microproject.workspace.WorkspaceSetting;

public class DefaultFrameManager implements FrameManager {
	private static final long serialVersionUID = -1835326043838730651L;
	public static final int SPLIT_EAST_WEST_SOUTH_NORTH = 0;
	//RootPaneContainer root;
	Container container;
	Container emptyPanel;

	JComboBox projectComboBox;
	JPanel projectComboPanel;
	NamedFrame previous = null;
	GraphicManager graphicManager;
	private FrameWorkspace workspace;
	public DefaultFrameManager(Container container, Container emptyPanel, GraphicManager graphicManager) {
		this.container = container;
		this.emptyPanel = emptyPanel;
		this.graphicManager = graphicManager;
		projectComboPanel = new JPanel();
		projectComboPanel.setVisible(false);
	//	projectComboPanel.add(new JLabel(Messages.getString("DefaultFrameManager.Project"))); //$NON-NLS-1$ //$NON-NLS-2$
		GraphicManager.getInstance().getLafManager().setColorScheme(projectComboPanel);
	}
	public void cleanUp() {
		Iterator i = getAllFrames().iterator();
		while (i.hasNext()) {
			((DocumentFrame)i.next()).cleanUp();
		}
		projectComboBox.removeAll();
		container = null;
		emptyPanel = null;
		previous = null;
		workspace = null;
		NamedFrame previous = null;
		graphicManager = null;
	}

	protected class FrameComboBoxModel extends DefaultComboBoxModel{
		public FrameComboBoxModel(){
			super();
		}
		public void update(){
			fireContentsChanged(this, -1, -1);
		}
		public void addElement(Object anObject) {
			super.addElement(anObject);
		}
	}

	private final JComboBox getProjectComboBox() {
		if (projectComboBox == null) {
			projectComboBox = new JComboBox(new FrameComboBoxModel());
			projectComboBox.setToolTipText(Messages.getString("DefaultFrameManager.Project")); //$NON-NLS-1$
			projectComboBox.setMinimumSize(new Dimension(100,28));
			projectComboBox.setMaximumSize(new Dimension(300,28));
			projectComboBox.setPreferredSize(new Dimension(140,28));
			projectComboPanel.setVisible(false);
//			projectComboBox.setBorder(BorderFactory.createMatteBorder(4, 4, 4, 4,Color.WHITE));
//			projectComboBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

			projectComboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					NamedFrame frame = (NamedFrame)projectComboBox.getSelectedItem();
					if (frame == null || frame == previous)
						return;
					activateFrame(frame);
					frame.fireNamedFrameActivated(new NamedFrameEvent(frame));
				}});
			projectComboPanel.add(projectComboBox);

		}
		return projectComboBox;
	}



	public void activateFrame(NamedFrame frame) {
		getProjectComboBox().setSelectedItem(frame);
		if (previous != null) {
			container.remove(previous);
			previous.setActive(false);
			previous.setVisible(false);
		} else {
			if (container != null)
				container.remove(emptyPanel);
		}
		previous = frame;
		if (frame == null) // happens when closing all
			return;
		container.add(frame,"Center");
		frame.setActive(true);
		frame.setVisible(true);


	}
	public NamedFrame getFrame(String id) {
		for (int i = 0; i < getProjectComboBox().getItemCount(); i++) {
			NamedFrame frame = (NamedFrame)getProjectComboBox().getItemAt(i);
			if (frame.getId().equals(id))
				return frame;
		}
		return null;
	}
	public void addFrame(NamedFrame frame) {
		getProjectComboBox().addItem(frame);
		frame.setManager(this);
		activateFrame(frame);
		projectComboPanel.setVisible(true);

	}

	public AbstractList getAllFrames() {
		LinkedList list = new LinkedList();
		for (int i = 0; i < getProjectComboBox().getItemCount(); i++) {
			NamedFrame frame = (NamedFrame)getProjectComboBox().getItemAt(i);
			list.add(frame);
		}
		return list;
	}


	public Component getSelectedFrame() {
		return (Component) getProjectComboBox().getSelectedItem();
	}

	public void removeFrame(NamedFrame frame) {
		if (frame == null) // in case of subproject for example, it didn't have its own frame
			return;
		getProjectComboBox().removeItem(frame);
		container.remove(frame);
		((DocumentFrame)frame).cleanUp();
		if (getProjectComboBox().getItemCount() == 0) {
			previous = null;
			container.add(emptyPanel,"Center");
			projectComboPanel.setVisible(false);

		} else {
			if (previous != null) {
				activateFrame(previous); // try to activate last activated
			} else {
				activateFrame((NamedFrame)getProjectComboBox().getItemAt(0)); // activate first one otherwise
			}
		}

	}


	public void showFrame(NamedFrame frame) {
		getProjectComboBox().setSelectedItem(frame);
	}
	public void update() {
		FrameComboBoxModel model=(FrameComboBoxModel)getProjectComboBox().getModel();
		model.update();
	}

	public void setTabTitle(NamedFrame frame, String tabTitle) {
	}
	public com.microproject.pm.graphic.frames.workspace.Workspace getWorkspace() {
		return new com.microproject.pm.graphic.frames.workspace.Workspace();
	}

	final Container getEmptyPanel() {
		return emptyPanel;
	}

//	final RootPaneContainer getRoot() {
//		return root;
//	}

	public void restoreWorkspace(WorkspaceSetting w, int context) {
		FrameWorkspace ws = (FrameWorkspace) w;
		workspace = ws;
		Iterator i = ws.list.iterator();
		while (i.hasNext()) {
			DocumentFrame.Workspace documentFrameWorkspace = (DocumentFrame.Workspace) i.next();
			long projectId = documentFrameWorkspace.getProjectId();
			Project project = ProjectFactory.getInstance().findFromId(projectId);
			if (project == null){
				LoadOptions opt=new LoadOptions();
				opt.setId(projectId);
				opt.setSync(true);
				ProjectFactory.getInstance().openProject(opt);
			}
			DocumentFrame documentFrame = graphicManager.addProjectFrame(project); // will add to combo
			documentFrame.restoreWorkspace(documentFrameWorkspace, context); // a little ugly, in that the worspace is used above to create the frame
		}
		getProjectComboBox().setSelectedIndex(ws.getSelectedIndex());
	}

	public WorkspaceSetting createWorkspace(int context) {
		FrameWorkspace ws = new FrameWorkspace();
		ws.list = new LinkedList();
		for (int i = 0; i < getProjectComboBox().getItemCount(); i++) {
			DocumentFrame frame = (DocumentFrame)getProjectComboBox().getItemAt(i);
			ws.list.add(frame.createWorkspace(context));
		}
		ws.selectedIndex = getProjectComboBox().getSelectedIndex();
		return ws;
	}

	public static class FrameWorkspace implements WorkspaceSetting { // named FrameWorkspace to avoid conflict
		private static final long serialVersionUID = -4029197146082617077L;
		LinkedList list;
		int selectedIndex;
		public final LinkedList getList() {
			return list;
		}
		public final void setList(LinkedList list) {
			this.list = list;
		}
		public final int getSelectedIndex() {
			return selectedIndex;
		}
		public final void setSelectedIndex(int selectedIndex) {
			this.selectedIndex = selectedIndex;
		}
	}
	final GraphicManager getGraphicManager() {
		return graphicManager;
	}
	public JPanel getProjectComboPanel() {
		return projectComboPanel;
	}

}

