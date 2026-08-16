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
package com.microproject.reports.view;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.microproject.help.HelpUtil;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.model.cache.GeneralFilteredIterator;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.views.BaseView;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.ReportDefinition;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.filtering.PredicatedNodeFilterIterator;
import com.microproject.pm.task.Project;
import com.microproject.reports.adapter.DataSource;
import com.microproject.reports.adapter.DataSourceProvider;
import com.microproject.reports.adapter.ReportUtil;
import com.microproject.reports.adapter.ReportViewer;
import com.microproject.strings.Messages;
import com.microproject.undo.UndoController;
import com.microproject.workspace.WorkspaceSetting;

/**
 *
 */
public class ReportView extends JPanel implements BaseView, CacheListener {
	private static final long serialVersionUID = 5457040745964404658L;
	private static final Logger logger = Logger.getLogger(ReportView.class.getName());
	protected JPanel report;
	protected Project project;
	private ReportViewer viewer = null;
	private DocumentFrame documentFrame;
	private boolean xmlFile = true;
	private ReportDefinition reportDefinition = null;
	JLabel reportLabel;
	JComboBox reportChoice;
	JLabel columnsLabel;
	JComboBox columnsChoice;
	CoordinatesConverter coord;
	SpreadSheetFieldArray fieldArray = null;
	BorderLayout layout = new java.awt.BorderLayout();
	NodeModelCache cache;
	private boolean initializing;
	boolean dirty = true;
	NodeModelCache taskCache = null;
	NodeModelCache resourceCache = null;
	private String viewName = DataSourceProvider.TASK_REPORT_VIEW;// initial report is task based
	private Consumer<Object> transformerClosure;
	private Float pendingZoomRatio;
	/**
	 * 
	 */
	public ReportView(DocumentFrame documentFrame) {
		super();
		this.documentFrame = documentFrame;
		this.project = documentFrame.getProject();
		HelpUtil.addDocHelp(this,"Report_View");
		transformerClosure=documentFrame.addTransformerInitializationClosure();
	}

	public void cleanUp() {
	    if (cache != null)
	    	cache.removeNodeModelListener(this);
		report = null;
		project = null;
		viewer = null;
		documentFrame = null;
		reportDefinition = null;
		reportLabel = null;
		reportChoice = null;
		columnsLabel = null;
		columnsChoice = null;
		coord = null;
		fieldArray = null;
		layout = null;
		cache = null;
		taskCache = null;
		resourceCache = null;
		viewName = null;
	}
	private NodeModelCache newFilteredCache(ReferenceNodeModelCache cache, String viewName) {
		NodeModelCache c = NodeModelCacheFactory.getInstance().createFilteredCache(cache,viewName,transformerClosure);
		c.update();
		return c;
	}
	

	private NodeModel updateCacheForView(String viewName) {
		if (viewName.equals(DataSourceProvider.TASK_REPORT_VIEW)) {
			if (taskCache == null)
				taskCache = newFilteredCache((ReferenceNodeModelCache)documentFrame.getTaskNodeModelCache(),viewName);
			cache = taskCache;
		} else if (viewName.equals(DataSourceProvider.RESOURCE_REPORT_VIEW)) {
			if (resourceCache == null)
				resourceCache = newFilteredCache((ReferenceNodeModelCache)documentFrame.getResourceNodeModelCache(),viewName);
			cache = resourceCache;
		} else if (viewName.equals(DataSourceProvider.PROJECT_REPORT_VIEW)) {
			cache = null;
			return GraphicManager.getInstance(this).getProjectFactory().getPortfolio().getNodeModel();
		}
	    return cache.getModel();

	}
	
	private void showReport() {
		documentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			makeViewer();
		} catch (JRException e) {
			logger.log(Level.SEVERE, "Failed to render report", e);
		}
		documentFrame.setCursor(Cursor.getDefaultCursor());
		
	}
	private void makeViewer() throws JRException{
		if (!dirty)
			return;
		
		documentFrame.showWaitCursor(true);
		Float zoomToApply = pendingZoomRatio;
		if (zoomToApply == null && viewer != null) {
			zoomToApply = Float.valueOf(viewer.getZoomRatio());
		}

		if (cache != null) { // remove old listener
		    cache.removeNodeModelListener(this);
		}
				   
        DataSource dataSource;

        SpreadSheetFieldArray fa = null;
        if (fieldArray != null) {
        	fa =(SpreadSheetFieldArray) fieldArray.clone();
        	CollectionUtils.filter(fa,new Predicate() {
        		public boolean evaluate(Object arg0) {
        			return !((Field)arg0).isGraphical(); // get rid of fields that can't be shown
        		}});
        }
        JasperReport report = ReportUtil.getReport(reportDefinition, coord.getProjectTimeIterator(), fa);

        viewName = DataSourceProvider.getViewName(report);
        //System.out.println("viewName="+viewName);
        documentFrame.setComboBoxesViewName(viewName); 

        NodeModel model = null;
        PredicatedNodeFilterIterator iterator;
        if (viewName == DataSourceProvider.REPORT_VIEW) { // special case to just use project
        	cache = null;
			ArrayList list = new ArrayList();
			list.add(project);
        	iterator = GeneralFilteredIterator.instance(list.iterator());
        } else {
        	model = updateCacheForView(viewName);
            if (cache == null){
            	iterator = GeneralFilteredIterator.instance(model.iterator());
            	//for (Iterator i=GeneralFilteredIterator.instance(model.iterator());i.hasNext();) System.out.println("Report model iterator: "+i.next());
            }else{ 
            	iterator = GeneralFilteredIterator.instance(cache.getIterator());
            	//for (Iterator i=GeneralFilteredIterator.instance(cache.getIterator());i.hasNext();) System.out.println("Report cache iterator: "+i.next());
            }
        }
        dataSource = DataSourceProvider.createDataSource(report,project,iterator,model);

        
        // projet name is used as report's title
        // and passed as a parameter
        HashMap params = new HashMap();
        params.put("projectName", project.getName()); //$NON-NLS-1$
        
		JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, dataSource);
		if (viewer != null) {
			viewer.changeReport(jasperPrint);
		} else {
			viewer =  new ReportViewer(jasperPrint);
			add(viewer,BorderLayout.CENTER);
		}
		if (zoomToApply != null) {
			viewer.setZoomRatio(zoomToApply.floatValue());
			pendingZoomRatio = null;
		}
		// add new listener
		if (cache != null) {
		    cache.addNodeModelListener(this);
		}
		dirty = false;
		
		documentFrame.showWaitCursor(false);

	}
	private Float getCurrentZoomRatio() {
		return (viewer != null) ? Float.valueOf(viewer.getZoomRatio()) : pendingZoomRatio;
	}
	
	private JPanel header() {
		JPanel panel = new JPanel();
		panel.add(reportLabel);
		panel.add(reportChoice);
		panel.add(columnsLabel);
		panel.add(columnsChoice);
		return panel;
	}
	
	private void initColumns() {
		String ssFields = reportDefinition.getMainSpreadsheetCategory();
		if (ssFields == null || ssFields.equals("assignmentSpreadsheet")) { //$NON-NLS-1$
			columnsChoice.setVisible(false);
			columnsLabel.setVisible(false);
			return;
		}
		columnsChoice.setVisible(true);
		columnsLabel.setVisible(true);
		DefaultComboBoxModel model = new DefaultComboBoxModel(Dictionary.getAll(ssFields));
		columnsChoice.setModel(model);
		columnsChoice.setSelectedItem(fieldArray);
	}
	public void init(CoordinatesConverter coord)  {
		initializing = true;
		this.coord = coord;
		setLayout(layout);
		reportLabel = new JLabel(Messages.getString("ReportView.Report")); //$NON-NLS-1$
		reportChoice = new JComboBox(ReportUtil.getReportDefinitions());
		reportChoice.setSelectedIndex(0);
		reportDefinition = (ReportDefinition) reportChoice.getSelectedItem();
		fieldArray = reportDefinition.getMainFieldArray();
		reportChoice.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				reportDefinition = (ReportDefinition) ((JComboBox)arg0.getSource()).getSelectedItem();
				fieldArray = reportDefinition.getMainFieldArray();
				dirty = true;
				showReport();
				initColumns();
			}});
		
		columnsLabel= new JLabel(Messages.getString("ReportView.Columns")); //$NON-NLS-1$
		columnsChoice = new JComboBox();
		initColumns();
		columnsChoice.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				fieldArray = (SpreadSheetFieldArray) ((JComboBox)arg0.getSource()).getSelectedItem();
				dirty = true;
				showReport();
			}});
		add(header(), BorderLayout.PAGE_START);
		showReport();
		initializing = false;
	}

	public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent){
		if (initializing)
			return;
		dirty = true;
		if (!isShowing())
			return;
		if (!isVisible()) { // set it as dirty and recalculate when shown because it's expensive to recalc report if not shown
			return;
		}
		showReport();
	}
	

	public UndoController getUndoController() {
		return null;
	}

	public void zoomIn() {
		if (viewer != null)
			viewer.zoomIn();
	}

	public void zoomOut() {
		if (viewer != null)
			viewer.zoomOut();
	}
	public boolean canZoomIn() {
		return true;
	}
	public boolean canZoomOut() {
		return true;
	}
	public int getScale() {
		return -1;
	}
	public SpreadSheet getSpreadSheet() {
		return null;
	}
	public boolean hasNormalMinWidth() {
		return true;
	}
	public String getViewName() {
		return viewName;
	}
	public boolean showsTasks() {
		return false;
	}
	public boolean showsResources() {
		return false;
	}
	/**
	 * Because it can be expensive to recalc a report, if the report is not visible, it is only recalced when made visible
	 */
	public void onActivate(boolean activate) {
		if (activate)
			showReport();
	}
	public boolean isPrintable() {
		return false;
	}

	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace)w;
		pendingZoomRatio = ws.zoomRatio;
		if (ws.reportName != null) {
			ReportDefinition def = ReportUtil.getFromName(ws.reportName);
			if (def != null)
				reportChoice.setSelectedItem(def);
		}
		if (ws.fieldArrayName != null) {
			SpreadSheetFieldArray s = (SpreadSheetFieldArray) Dictionary.get(reportDefinition.getMainSpreadsheetCategory(),ws.fieldArrayName);
			if (s != null)
				columnsChoice.setSelectedItem(s);
		}
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		if (reportDefinition != null)
			ws.reportName = reportDefinition.getName();
		if (fieldArray != null)
			ws.fieldArrayName = fieldArray.toString();
		ws.zoomRatio = getCurrentZoomRatio();
		return ws;
	}

	public static class Workspace implements WorkspaceSetting  {
		private static final long serialVersionUID = -7768176701769503845L;
		String fieldArrayName = null;
		String reportName = null;
		Float zoomRatio = null;
		public String getFieldArrayName() {
			return fieldArrayName;
		}
		public void setFieldArrayName(String fieldArrayName) {
			this.fieldArrayName = fieldArrayName;
		}
		public String getReportName() {
			return reportName;
		}
		public void setReportName(String reportName) {
			this.reportName = reportName;
		}
		public Float getZoomRatio() {
			return zoomRatio;
		}
		public void setZoomRatio(Float zoomRatio) {
			this.zoomRatio = zoomRatio;
		}
	}

	public boolean canScrollToTask() {
		return false;
	}

	public void scrollToTask() {
	}
	
	public NodeModelCache getCache(){
		return cache;
	}
	
	
}

