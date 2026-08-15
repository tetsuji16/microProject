/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.microproject.pm.graphic.gantt;

import com.microproject.util.DataUtils;
import java.util.function.Consumer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.apache.commons.collections.CollectionUtils;

import com.microproject.pm.graphic.graph.GraphInteractor;
import com.microproject.pm.graphic.graph.GraphModel;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.graph.GraphPopupMenu;
import com.microproject.graphic.configuration.BarStyle;
import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.grouping.core.transform.TransformList;
import com.microproject.grouping.core.transform.filtering.BaseFilter;
import com.microproject.strings.Messages;
import com.microproject.pm.task.Task;


/**
 *
 */
public class GanttPopupMenu extends GraphPopupMenu{
	private static final long serialVersionUID = -5006500626139949187L;
	private static final int[] STANDARD_COLORS = {
		0x5B9BD5, 0x4472C4, 0x70AD47, 0xFFC000,
		0xED7D31, 0xC00000, 0xA5A5A5, 0x7030A0
	};
	private static final String ANNOTATION_FIELD_RESOURCE_NAMES = "Field.resourceNames";
	private static final String ANNOTATION_FIELD_TASK_NAME = "Field.name";


	private class BarMenuAction extends JRadioButtonMenuItem implements ActionListener {
		private static final long serialVersionUID = 8168153384233811506L;
		BarStyle style;
    	
    	BarMenuAction(final BarStyle style) {
    		super(style.getName());
    		this.style = style;
    		setSelected(style.isActive());
    		addActionListener(this);
    	}
    	public void actionPerformed(ActionEvent arg0) {
    	    style.setActive(isSelected());
    	    ((GraphModel)interactor.getGraph().getModel()).updateAll(true);
    	}
    }
    
    private class AssignmentsMenuAction extends JRadioButtonMenuItem implements ActionListener {
		private static final long serialVersionUID = 3480838269288912755L;
		BaseFilter filter,filterOffline;
    	
    	AssignmentsMenuAction() {
    		super(Messages.getString("Gantt.Popup.showAssignments"));
    		filter=(BaseFilter)TransformList.getInstance("hidden_filters").getTransform("Filter.Gantt");
    		filterOffline=(BaseFilter)TransformList.getInstance("hidden_filters").getTransform("Filter.OfflineGantt");
    		setSelected(filter.isShowAssignments());
    		addActionListener(this);
    	}
        public void actionPerformed(ActionEvent e) {
            filter.setShowAssignments(isSelected());
            filterOffline.setShowAssignments(isSelected());
            ((GraphModel)interactor.getGraph().getModel()).getCache().update();
        }
    }

    private class ProgressLineMenuAction extends JRadioButtonMenuItem implements ActionListener {
		private static final long serialVersionUID = -7938597987478064286L;

		ProgressLineMenuAction() {
			super(Messages.getString("Gantt.Popup.showProgressLine"));
			if (interactor.getGraph() instanceof Gantt)
				setSelected(((Gantt)interactor.getGraph()).isProgressLineEnabled());
			addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			if (!(interactor.getGraph() instanceof Gantt))
				return;
			((Gantt)interactor.getGraph()).setProgressLineEnabled(isSelected());
			((GraphModel)interactor.getGraph().getModel()).updateAll(true);
		}
    }

    private class AnnotationTextMenuAction extends JRadioButtonMenuItem implements ActionListener {
		private static final long serialVersionUID = -6784371291922163170L;
		private final String fieldId;

		AnnotationTextMenuAction(String messageKey, String fieldId) {
			super(Messages.getString(messageKey));
			this.fieldId = fieldId;
			setSelected(fieldId.equals(getCurrentAnnotationFieldId()));
			addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			if (!isSelected())
				return;
			applyAnnotationField(fieldId);
		}
    }
   
    private class SplitModeMenuAction extends AbstractAction {
    	/**
		 * 
		 */
		private static final long serialVersionUID = -8615889754474230400L;
		SplitModeMenuAction() {
    		super(Messages.getString("Gantt.Popup.splitMode"));
    	}
        public void actionPerformed(ActionEvent e) {
            ((GanttInteractor)interactor).setSplitMode();
        }
    }
    
    
    public GanttPopupMenu(final GraphInteractor interactor) {
        super(interactor);
    }
    
	
/**
 * Because the styles may change, rebuild the menu each time
 *
 */
	protected void init() {
    	removeAll();
		Task selectedTask = interactor instanceof GanttInteractor ganttInteractor
				? ganttInteractor.getSelectedTask()
				: null;
		if (selectedTask != null && interactor.getGraph() instanceof Gantt gantt) {
			add(createFillColorMenu(gantt, selectedTask));
			add(new AbstractAction(Messages.getString("Gantt.FormatBar.title")) {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent event) {
					GanttBarFormatDialog.show(gantt, gantt, selectedTask);
				}
			});
			addSeparator();
		}
    	add(new SplitModeMenuAction());
    	add(new AssignmentsMenuAction());
    	add(new ProgressLineMenuAction());
        final JMenu bars=new JMenu(Messages.getString("Gantt.Popup.barStylesMenu"));
        final JMenu annotations=new JMenu(Messages.getString("Gantt.Popup.annotationStylesMenu"));
        final JMenu annotationText=new JMenu(Messages.getString("Gantt.Popup.annotationTextMenu"));
        final ButtonGroup annotationTextGroup = new ButtonGroup();
        JRadioButtonMenuItem resourceNamesItem = new AnnotationTextMenuAction("Gantt.Popup.annotationResourceNames", ANNOTATION_FIELD_RESOURCE_NAMES);
        JRadioButtonMenuItem taskNamesItem = new AnnotationTextMenuAction("Gantt.Popup.annotationTaskNames", ANNOTATION_FIELD_TASK_NAME);
        annotationTextGroup.add(resourceNamesItem);
        annotationTextGroup.add(taskNamesItem);
        annotationText.add(resourceNamesItem);
        annotationText.add(taskNamesItem);
        annotations.add(annotationText);
		DataUtils.forAllDo(interactor.getGraph().getBarStyles().getRows().iterator(), new Consumer<Object>() { public void accept(Object arg0) {
				BarStyle barStyle = (BarStyle)arg0;
				BarMenuAction menuAction =new BarMenuAction(barStyle); 
				if (barStyle.isLink()) // move the show links item to the main menu
					add(menuAction);
				else if (barStyle.isCalendar()) // move the show links item to the main menu
					add(menuAction);
				else if (barStyle.isHorizontalGrid()) // move the show links item to the main menu
					add(menuAction);
				else if (barStyle.isAnnotation())
					annotations.add(menuAction);
				else 
					bars.add(menuAction);
				
			}
		});
        add(bars);
        add(annotations);
    	
    }

	private JMenu createFillColorMenu(Gantt gantt, Task task) {
		JMenu menu = new JMenu(Messages.getString("Gantt.FormatBar.fillColor"));
		JMenuItem automatic = new JMenuItem(Messages.getString("Gantt.FormatBar.automatic"));
		automatic.addActionListener(event -> applyFillColor(gantt, task, null));
		menu.add(automatic);
		menu.addSeparator();
		for (int rgb : STANDARD_COLORS) {
			JMenuItem colorItem = new JMenuItem(String.format("#%06X", rgb), new ColorSwatchIcon(new Color(rgb)));
			colorItem.addActionListener(event -> applyFillColor(gantt, task, rgb));
			menu.add(colorItem);
		}
		menu.addSeparator();
		JMenuItem moreColors = new JMenuItem(Messages.getString("Gantt.FormatBar.moreColors"));
		moreColors.addActionListener(event -> {
			BarFormat current = gantt.getBarFormat(task);
			Integer currentRgb = task.isMilestone() ? current.getStartRgb() : current.getMiddleRgb();
			Color chosen = GanttBarFormatDialog.chooseColor(gantt, currentRgb);
			if (chosen != null)
				applyFillColor(gantt, task, chosen.getRGB() & 0x00FFFFFF);
		});
		menu.add(moreColors);
		return menu;
	}

	private void applyFillColor(Gantt gantt, Task task, Integer rgb) {
		BarFormat current = gantt.getBarFormat(task);
		gantt.applyBarFormat(task, current.withFillRgb(rgb));
	}

	private static final class ColorSwatchIcon implements Icon {
		private final Color color;

		private ColorSwatchIcon(Color color) {
			this.color = color;
		}

		@Override
		public int getIconWidth() {
			return 16;
		}

		@Override
		public int getIconHeight() {
			return 12;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			graphics.setColor(color);
			graphics.fillRect(x, y, getIconWidth(), getIconHeight());
			graphics.setColor(Color.DARK_GRAY);
			graphics.drawRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
		}
	}

	private String getCurrentAnnotationFieldId() {
		if (interactor.getGraph() instanceof Gantt) {
			String fieldId = ((Gantt) interactor.getGraph()).getAnnotationFieldId();
			return fieldId == null ? ANNOTATION_FIELD_RESOURCE_NAMES : fieldId;
		}
		Object firstAnnotationField = CollectionUtils.find(interactor.getGraph().getBarStyles().getRows(), new org.apache.commons.collections.Predicate() {
			public boolean evaluate(Object object) {
				return object instanceof BarStyle && ((BarStyle)object).isAnnotation();
			}
		});
		if (!(firstAnnotationField instanceof BarStyle))
			return ANNOTATION_FIELD_RESOURCE_NAMES;
		String fieldId = ((BarStyle)firstAnnotationField).getBarFormat().getFieldId();
		return fieldId == null ? ANNOTATION_FIELD_RESOURCE_NAMES : fieldId;
	}

	private void applyAnnotationField(final String fieldId) {
		if (interactor.getGraph() instanceof Gantt) {
			((Gantt) interactor.getGraph()).setAnnotationFieldId(fieldId);
		}
		((GraphModel)interactor.getGraph().getModel()).updateAll(true);
	}

}

