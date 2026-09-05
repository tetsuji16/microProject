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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.RootPaneContainer;
import javax.swing.border.LineBorder;
import javax.swing.ListCellRenderer;

import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.session.LoadOptions;
import com.microproject.strings.Messages;
import com.microproject.ui.shell.ProjectLibreShell;
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
	private JPanel arrangedFrames;
	/** Independent desktop hosts for documents after the primary project. */
	private final Map<NamedFrame, JFrame> documentWindows = new LinkedHashMap<NamedFrame, JFrame>();
	/** The document retained in the application's primary ribbon frame. */
	private NamedFrame primaryFrame;
	private WindowArrangement currentArrangement = WindowArrangement.SINGLE;
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
		for (JFrame documentWindow : documentWindows.values())
			documentWindow.dispose();
		documentWindows.clear();
		container = null;
		emptyPanel = null;
		previous = null;
		primaryFrame = null;
		workspace = null;
		graphicManager = null;
	}

	/** Whether this workspace still owns a live UI container. */
	public boolean isActive() {
		return container != null && graphicManager != null;
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
			projectComboBox.setName("openProjectSelector");
			projectComboBox.setToolTipText(Messages.getString("DefaultFrameManager.Project")); //$NON-NLS-1$
			projectComboBox.setMinimumSize(new Dimension(100,28));
			projectComboBox.setMaximumSize(new Dimension(300,28));
			projectComboBox.setPreferredSize(new Dimension(140,28));
			projectComboBox.setRenderer(new ProjectWindowRenderer());
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
			projectComboPanel.add(createArrangementButton());

		}
		return projectComboBox;
	}

	private JButton createArrangementButton() {
		JButton button = new JButton("Windows...");
		button.setName("windowArrangementMenu");
		button.setMargin(new Insets(2, 7, 2, 7));
		button.setToolTipText("Switch project windows or arrange open projects");
		JPopupMenu popup = new JPopupMenu();
		addArrangementItem(popup, "Arrange All (tile)", WindowArrangement.TILE);
		addArrangementItem(popup, "Arrange Horizontally", WindowArrangement.HORIZONTAL);
		addArrangementItem(popup, "Arrange Vertically", WindowArrangement.VERTICAL);
		addArrangementItem(popup, "Cascade Windows", WindowArrangement.CASCADE);
		popup.addSeparator();
		JMenuItem single = new JMenuItem("Show Active Project Only");
		single.setName("showActiveProjectOnly");
		single.addActionListener(event -> arrangeAll(WindowArrangement.SINGLE));
		popup.add(single);
		button.addActionListener(event -> popup.show(button, 0, button.getHeight()));
		return button;
	}

	private void addArrangementItem(JPopupMenu popup, String label, WindowArrangement arrangement) {
		JMenuItem item = new JMenuItem(label);
		item.setName("arrange" + arrangement.name());
		item.addActionListener(event -> arrangeAll(arrangement));
		popup.add(item);
	}

	private final class ProjectWindowRenderer extends JLabel implements ListCellRenderer<Object> {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
				boolean selected, boolean cellHasFocus) {
			setOpaque(true);
			setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
			setBackground(selected ? list.getSelectionBackground() : list.getBackground());
			setForeground(selected ? list.getSelectionForeground() : list.getForeground());
			NamedFrame frame = value instanceof NamedFrame namedFrame ? namedFrame : null;
			setText(describeWindow(frame));
			setToolTipText(describeWindowTooltip(frame));
			return this;
		}
	}

	private String describeWindow(NamedFrame frame) {
		if (frame == null)
			return "";
		StringBuilder description = new StringBuilder();
		if (frame == previous)
			description.append("● ");
		if (frame instanceof DocumentFrame documentFrame && documentFrame.getProject() != null) {
			Project project = documentFrame.getProject();
			String name = project.getName();
			if (name == null || name.isBlank())
				name = documentFrame.getTitle();
			description.append(Messages.getContextString("Text.ApplicationTitle"));
			description.append(" - ").append(name == null ? "(unnamed project)" : name);
			if (project.isMaster())
				description.append(" [Master]");
			if (project.isReadOnly())
				description.append(" [Read-only]");
			if (project.needsSaving())
				description.append(" *");
		} else {
			description.append(frame.getTitle() == null ? frame.getId() : frame.getTitle());
		}
		return description.toString();
	}

	private String describeWindowTooltip(NamedFrame frame) {
		String title = describeWindow(frame);
		if (frame instanceof DocumentFrame documentFrame && documentFrame.getProject() != null) {
			String fileName = documentFrame.getProject().getFileName();
			if (fileName != null && !fileName.isBlank())
				return title + " — " + canonicalWindowPath(fileName);
		}
		return title;
	}

	private static String canonicalWindowPath(String fileName) {
		try {
			return new java.io.File(fileName).getCanonicalPath();
		} catch (java.io.IOException ignored) {
			return new java.io.File(fileName).getAbsolutePath();
		}
	}



	public void activateFrame(NamedFrame frame) {
		JFrame documentWindow = documentWindows.get(frame);
		if (documentWindow != null) {
			if (previous != null && previous != frame)
				previous.setActive(false);
			previous = frame;
			frame.setActive(true);
			documentWindow.setVisible(true);
			documentWindow.toFront();
			documentWindow.requestFocus();
			refreshContainer();
			updateDesktopWindowTitles();
			return;
		}
		leaveArrangeAll();
		getProjectComboBox().setSelectedItem(frame);
		if (previous != null) {
			if (documentWindows.containsKey(previous)) {
				// A secondary desktop host stays visible when another document is
				// activated.  Hiding its child component would leave the window blank.
				previous.setActive(false);
			} else {
				container.remove(previous);
				previous.setActive(false);
				previous.setVisible(false);
			}
		} else {
			if (container != null)
				container.remove(emptyPanel);
		}
		previous = frame;
		currentArrangement = WindowArrangement.SINGLE;
		if (frame == null) { // happens when closing all
			refreshContainer();
			return;
		}
		container.add(frame,"Center");
		frame.setActive(true);
		frame.setVisible(true);
		refreshContainer();
		updateDesktopWindowTitles();


	}

	/** Displays open project frames using the requested MS Project-style layout. */
	@Override
	public void arrangeAll(WindowArrangement arrangement) {
		if (!documentWindows.isEmpty() && !GraphicsEnvironment.isHeadless()) {
			arrangeDesktopWindows(arrangement);
			return;
		}
		int count = getProjectComboBox().getItemCount();
		if (container == null || count == 0)
			return;
		if (arrangement == null || arrangement == WindowArrangement.SINGLE || count == 1) {
			NamedFrame active = previous != null ? previous : (NamedFrame) getProjectComboBox().getItemAt(0);
			activateFrame(active);
			return;
		}
		leaveArrangeAll();
		if (previous != null)
			container.remove(previous);
		int columns;
		int rows;
		switch (arrangement) {
		case HORIZONTAL -> { rows = 1; columns = count; }
		case VERTICAL -> { rows = count; columns = 1; }
		case TILE -> {
			columns = (int) Math.ceil(Math.sqrt(count));
			rows = (int) Math.ceil((double) count / columns);
		}
		case CASCADE -> {
			arrangedFrames = new CascadePanel();
			addArrangedFrames(count);
			currentArrangement = arrangement;
			container.add(arrangedFrames, "Center");
			refreshContainer();
			return;
		}
		default -> throw new IllegalStateException("Unsupported arrangement: " + arrangement);
		}
		arrangedFrames = new JPanel(new GridLayout(rows, columns, 6, 6));
		addArrangedFrames(count);
		currentArrangement = arrangement;
		container.add(arrangedFrames, "Center");
		refreshContainer();
	}

	private void arrangeDesktopWindows(WindowArrangement arrangement) {
		java.util.List<Window> windows = new java.util.ArrayList<Window>();
		Window primary = container instanceof Window ? (Window) container
				: javax.swing.SwingUtilities.getWindowAncestor(container);
		if (primary != null && primary.isDisplayable()) windows.add(primary);
		windows.addAll(documentWindows.values());
		if (windows.size() < 2) return;
		Rectangle area = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		if (arrangement == null || arrangement == WindowArrangement.SINGLE) {
			activateFrame(previous);
			return;
		}
		int count = windows.size(); int rows; int columns;
		switch (arrangement) {
		case HORIZONTAL -> { rows = 1; columns = count; }
		case VERTICAL -> { rows = count; columns = 1; }
		case TILE -> { columns = (int)Math.ceil(Math.sqrt(count)); rows = (int)Math.ceil((double)count / columns); }
		case CASCADE -> {
			int offset = Math.max(24, Math.min(48, Math.min(area.width, area.height) / 12));
			for (int i = 0; i < count; i++) windows.get(i).setBounds(area.x + i * offset, area.y + i * offset,
					area.width - offset * (count - 1), area.height - offset * (count - 1));
			currentArrangement = arrangement; return;
		}
		default -> throw new IllegalStateException("Unsupported arrangement: " + arrangement);
		}
		int width = area.width / columns; int height = area.height / rows;
		for (int i = 0; i < count; i++) windows.get(i).setBounds(area.x + (i % columns) * width,
				area.y + (i / columns) * height, width, height);
		currentArrangement = arrangement;
	}

	private void addArrangedFrames(int count) {
		for (int index = 0; index < count; index++) {
			NamedFrame frame = (NamedFrame) getProjectComboBox().getItemAt(index);
			frame.setActive(frame == previous);
			frame.setVisible(true);
			arrangedFrames.add(frame);
		}
	}

	private final class CascadePanel extends JPanel {
		private static final long serialVersionUID = 1L;
		CascadePanel() { setLayout(null); }
		@Override
		public void doLayout() {
			int offset = Math.max(24, Math.min(48, Math.min(getWidth(), getHeight()) / 12));
			int width = Math.max(1, getWidth() - offset * Math.max(0, getComponentCount() - 1));
			int height = Math.max(1, getHeight() - offset * Math.max(0, getComponentCount() - 1));
			for (int index = 0; index < getComponentCount(); index++)
				getComponent(index).setBounds(index * offset, index * offset, width, height);
		}
	}

	private void leaveArrangeAll() {
		if (arrangedFrames == null || container == null)
			return;
		container.remove(arrangedFrames);
		// Removing a component from a Swing container does not reset its visible
		// flag.  Clear it before returning to the single active-project view so
		// that the inactive project is neither painted nor reported as visible.
		for (Component component : arrangedFrames.getComponents())
			component.setVisible(false);
		arrangedFrames.removeAll();
		arrangedFrames = null;
	}

	private void refreshContainer() {
		if (container instanceof JComponent component)
			component.revalidate();
		else if (container != null) {
			container.invalidate();
			container.validate();
		}
		if (container != null)
			container.repaint();
		if (projectComboBox != null)
			projectComboBox.setToolTipText(describeWindowTooltip((NamedFrame) projectComboBox.getSelectedItem()));
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
		if (primaryFrame == null)
			primaryFrame = frame;
		if (usesIndependentWindows() && frame instanceof DocumentFrame
				&& getProjectComboBox().getItemCount() > 1)
			createDocumentWindow(frame);
		activateFrame(frame);
		projectComboPanel.setVisible(true);

	}

	private boolean usesIndependentWindows() {
		return !GraphicsEnvironment.isHeadless()
				&& (container instanceof MainRibbonFrame
						|| javax.swing.SwingUtilities.getWindowAncestor(container) instanceof MainRibbonFrame);
	}

	private void createDocumentWindow(NamedFrame frame) {
		MainRibbonFrame window = new MainRibbonFrame(describeWindow(frame), null, null);
		window.setGraphicManager(graphicManager);
		window.setWindowCloseAction(() -> graphicManager.closeDocumentWindow((DocumentFrame) frame));
		ProjectLibreShell.installRibbonShell(window, graphicManager.getMenuManager(), graphicManager::showHelpDialog);
		window.setSize(900, 650);
		window.setLocationByPlatform(true);
		window.getContentPane().add(frame, java.awt.BorderLayout.CENTER);
		window.addWindowFocusListener(new WindowAdapter() {
			@Override public void windowGainedFocus(WindowEvent event) { graphicManager.activateDocumentWindow((DocumentFrame)frame); }
		});
		documentWindows.put(frame, window);
		window.setVisible(true);
	}

	private void promoteSecondaryWindowIfNeeded(NamedFrame removedFrame) {
		if (documentWindows.containsKey(removedFrame) || documentWindows.isEmpty())
			return;
		Map.Entry<NamedFrame, JFrame> replacement = documentWindows.entrySet().iterator().next();
		JFrame replacementWindow = replacement.getValue();
		replacementWindow.getContentPane().remove(replacement.getKey());
		documentWindows.remove(replacement.getKey());
		replacementWindow.dispose();
		primaryFrame = replacement.getKey();
	}

	private void updateDesktopWindowTitles() {
		Window primaryWindow = container instanceof Window ? (Window) container
				: javax.swing.SwingUtilities.getWindowAncestor(container);
		if (primaryWindow instanceof JFrame && primaryFrame != null)
			((JFrame) primaryWindow).setTitle(describeWindow(primaryFrame));
		for (Map.Entry<NamedFrame, JFrame> entry : documentWindows.entrySet())
			entry.getValue().setTitle(describeWindow(entry.getKey()));
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
		leaveArrangeAll();
		boolean wasPrevious = previous == frame;
		promoteSecondaryWindowIfNeeded(frame);
		getProjectComboBox().removeItem(frame);
		JFrame documentWindow = documentWindows.remove(frame);
		if (documentWindow != null) documentWindow.dispose();
		else container.remove(frame);
		((DocumentFrame)frame).cleanUp();
		if (wasPrevious)
			previous = null;
		if (frame == primaryFrame && documentWindows.isEmpty())
			primaryFrame = null;
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
		for (Map.Entry<NamedFrame, JFrame> entry : documentWindows.entrySet())
			entry.getValue().setTitle(describeWindow(entry.getKey()));
		updateDesktopWindowTitles();
	}

	public void setTabTitle(NamedFrame frame, String tabTitle) {
		JFrame documentWindow = documentWindows.get(frame);
		if (documentWindow != null)
			documentWindow.setTitle(describeWindow(frame));
		updateDesktopWindowTitles();
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

	public WindowArrangement getCurrentArrangement() {
		return currentArrangement;
	}

	final int getIndependentWindowCount() {
		return documentWindows.size();
	}

	final JFrame getIndependentWindow(NamedFrame frame) {
		return documentWindows.get(frame);
	}

}
