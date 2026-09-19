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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoableEditSupport;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.pm.graphic.frames.DocumentSelectedEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;
import com.microproject.grouping.core.Node;
import com.microproject.field.Field;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.scheduling.ScheduleEventListener;
import com.microproject.pm.task.BelongsToDocument;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.util.DataUtils;


/**
 *
 */
@SuppressWarnings("deprecation")
public abstract class FieldDialog extends AbstractDialog  implements ObjectEvent.Listener,ScheduleEventListener,SelectionNodeListener, DocumentSelectedEvent.Listener {
	private static final Logger logger = Logger.getLogger(FieldDialog.class.getName());
	private boolean multipleObjects;
	private Class objectClass;
	private UndoableEditSupport undoableEditSupport;
	protected FieldDialog(Frame owner, String title, boolean modal, boolean multipleObjects/*,UndoableEditSupport undoableEditSupport*/) {
		super(owner,title,modal);
		this.multipleObjects = multipleObjects;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateAll();
			}});
	}

	protected List<FieldComponentMap> maps = new ArrayList<>();
	protected Object object;
	protected List<Object> collection = new ArrayList<>();
	private JComponent dirtyComponent;
	protected JComponent mainComponent = null;
	
	protected FieldComponentMap createMap() {
		FieldComponentMap map;
		if (multipleObjects)
			map = new FieldComponentMap(collection);
		else
			map = new FieldComponentMap(object);
			
		maps.add(map);
		return map;
	}

	protected Object getObject() {
		return object;
	}
	protected Collection<Object> getCollection() {
		return collection;
	}

	protected Object getFirstObject() {
		if (collection == null)
			return object;
		Iterator<Object> i = collection.iterator();
		if (i.hasNext())
			return i.next();
		return null;
	}

	protected void onCancel() {
		updateAll();
		super.onCancel();
	}


	public void setType(boolean task){
		objectClass=(task)?Task.class:Resource.class;
		setTitle((task)?Messages.getString("FieldDialog.TaskInformation"):Messages.getString("FieldDialog.ResourceInformation")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void setObjectClass(Class objectClass) {
		this.objectClass = objectClass;
	}
	
	public Class getObjectClass() {
		return objectClass;
	}
	public void objectChanged(ObjectEvent objectEvent) {
		if (!isVisible()) return;
		if (multipleObjects && collection.contains(objectEvent.getObject())) {
			updateAll(); // if in list, need to update all
		} else if (objectEvent.getObject() == getObject()) {
			updateAll();
		}
	}
	public void scheduleChanged(ScheduleEvent scheduleEvent){
		if (!isVisible()) return;
		if (multipleObjects) {
			updateAll(); // if in list, need to update all
		} else if (getObject()!=null&&((Schedule)getObject()).isJustModified()) {
			updateAll();
		}
	}

	public void documentSelected(DocumentSelectedEvent evt) {
		logger.info(Messages.getString("FieldDialog.document") + evt.getCurrent()); //$NON-NLS-1$
	}

	public void selectionChanged(SelectionNodeEvent e) {
		if (!isVisible())
			return;
		Node selected;
		Object nodeObject;
		if (multipleObjects) {
			setCollection(e.getNodes());
		} else {
			selected = e.getCurrentNode();
			if (selected == null)
				return;
			nodeObject = selected.getImpl();
			nodeObject = DataUtils.extractObjectOfClass(nodeObject,objectClass);
			setObject(nodeObject);
			updateAll();
		}
	}

	public void setCollection(Collection nodeList) {
		collection.clear();
		DataUtils.extractObjectsOfClassFromNodeList(collection,nodeList,objectClass);
	}
	
	public void setObject(Object object) {
		if (object == this.object)
			return;
		if (this.object != null && this.object instanceof BelongsToDocument) {
			Document document=((BelongsToDocument)this.object).getDocument();
			document.removeObjectListener(this);
			if (document instanceof Project)
				((Project)document).removeScheduleListener(this);
		}
		this.object = object;
		if (object != null && object instanceof BelongsToDocument) {
			Document document=((BelongsToDocument)this.object).getDocument();
			document.addObjectListener(this);
			if (document instanceof Project)
				((Project)document).addScheduleListener(this);
		}
	}

	protected void desactivateListeners() {
		setObject(null);
	}

	public abstract JComponent createContentPanel();

	protected void updateAll() {
		setVisibleAndEnabledState();
		for (FieldComponentMap map : maps) {
			map.setObject(object);
			map.updateAll();
		}
	}
	
	protected void setVisibleAndEnabledState() {
		boolean showing = (object != null);
		if (mainComponent != null)
			mainComponent.setEnabled(showing);
	}

	public void setDirtyComponent(JComponent dirtyComponent) {
		this.dirtyComponent = dirtyComponent;
	}

/**
 * On pressing enter key, check any unvalidated component
 */	
	public void onOk() {
		if (dirtyComponent != null) {
			InputVerifier verifier = dirtyComponent.getInputVerifier();
			if (!verifier.shouldYieldFocus(dirtyComponent))
				return;
		}
		super.onOk();
	}

protected JComponent createFieldsPanel(FieldComponentMap map, Collection<Field> fields) {
	if (fields == null || fields.size() == 0)
		return null;
	 
	FormLayout layout = new FormLayout(
			"p, 3dlu, fill:160dlu:grow", //$NON-NLS-1$
			StringUtils.chomp(StringUtils.repeat("p,3dlu,", fields.size()))); // repeats and gets rid of last comma //$NON-NLS-1$
	DefaultFormBuilder builder = new DefaultFormBuilder(layout);
	map.append(builder,fields);
	return builder.getPanel();
}
}

