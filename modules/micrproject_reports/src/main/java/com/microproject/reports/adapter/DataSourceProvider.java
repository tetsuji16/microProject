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
package com.microproject.reports.adapter;

import java.util.Collection;

import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataSourceProvider;
import net.sf.jasperreports.engine.JRElement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignFont;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JRDesignTextElement;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;

import org.apache.commons.collections.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microproject.configuration.Configuration;
import com.microproject.datatype.Duration;
import com.microproject.datatype.Money;
import com.microproject.datatype.Rate;
import com.microproject.field.Field;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.grouping.core.transform.filtering.PredicatedNodeFilterIterator;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.timescale.TimeInterval;
import com.microproject.timescale.TimeIterator;

/**
 *
 */
public class DataSourceProvider implements JRDataSourceProvider {
	private static final Logger logger = LoggerFactory.getLogger(DataSourceProvider.class);
	public static final int PROJECT = 0; // just the current project's fields
	public static final int PROJECTS_TREE = 1;
	public static final int PROJECTS_FLAT = 2;
	public static final int TASKS_TREE = 3;
	public static final int TASKS_FLAT = 4;
	public static final int RESOURCES_TREE = 5;
	public static final int RESOURCES_FLAT = 6;
	public static final int ASSIGNMENTS = 7;
	public static final int PREDECESSORS = 8;
	public static final int SUCCESSORS = 9;
	public static final int TASKS_ASSIGNMENTS_TREE = 10;
	public static final int RESOURCES_ASSIGNMENTS_TREE = 11;
	public static final int TASKS_ASSIGNMENTS_FLAT = 12;
	public static final int RESOURCES_ASSIGNMENTS_FLAT = 13;
	public static final int ASSIGNMENTS_PROJECT_BASED = 14;
	public static final int ASSIGNMENTS_RESOURCE_BASED = 15;
	
	
	public static final String REPORT_VIEW=Messages.getString("View.Report");
	
	public static final String PROJECT_REPORT_VIEW=Messages.getString("View.ProjectReport");
	public static final String TASK_REPORT_VIEW=Messages.getString("View.TaskReport");
	public static final String RESOURCE_REPORT_VIEW=Messages.getString("View.ResourceReport");
	
	public static final String COLLECTION_TYPE_PROPERTY="collectionType";
	public static final String OUTLINE_PROPERTY="outline";
	public static final String TIME_BASED="timeBased";
	private static final DataSourceProvider INSTANCE = new DataSourceProvider();
	private volatile JRField[] reportFields = null;
	public static DataSourceProvider getInstance() {
		return INSTANCE;
	}

	private static void applyFont(JRDesignTextElement textElement, JRDesignFont font) {
		textElement.setFontName(font.getFontName());
		textElement.setFontSize(font.getOwnFontsize());
		textElement.setBold(font.isOwnBold());
		textElement.setItalic(font.isOwnItalic());
		textElement.setUnderline(font.isOwnUnderline());
		textElement.setStrikeThrough(font.isOwnStrikeThrough());
		textElement.setPdfFontName(font.getPdfFontName());
		textElement.setPdfEncoding(font.getPdfEncoding());
		textElement.setPdfEmbedded(font.isOwnPdfEmbedded());
	}
	public boolean supportsGetFieldsOperation() {
		return true;
	}
	private synchronized void initFields() {
		if (reportFields != null)
			return;
		Collection<Field> allFields = Configuration.getAllFields();
		JRField[] initializedFields = new JRField[allFields.size()];
		int index = 0;
		for (Field field : allFields) {
			JRDesignField newOne = new JRDesignField();
			newOne.setName(field.getId());
			newOne.setDescription(field.getName());
			newOne.setValueClass(reportValueClass(field));
			initializedFields[index++] = newOne;
		}
		reportFields = initializedFields;
	}

	static Class<?> reportValueClass(Field field) {
		if (field.isRate() || field.isMoney())
			return Double.class;
		if (field.isDurationOrWork())
			return Long.class;
		return field.getDisplayType();
	}

	public JRField[] getFields(JasperReport arg0) throws JRException, UnsupportedOperationException {
		if (reportFields == null)
			initFields();
		return reportFields.clone();
	}

	

/**
 * JasperAssistant version
 */
	public JRDataSource create(JasperReport arg0) throws JRException {
		DataSource dataSource = new DataSource();
		return dataSource;
	}
	
	/**
	 * Parses the report {@code collectionType} property. Returns -1 for a null
	 * or non-numeric value instead of throwing NumberFormatException (issue #186).
	 */
	static int parseCollectionType(String value) {
		if (value == null) {
			return -1;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * get the view corresponding to a report.  This is used primarily to set filter and sort combos
	 * @param report
	 * @return
	 */
	public static String getViewName(JasperReport report) {
		String collectionType = report.getProperty(COLLECTION_TYPE_PROPERTY);
		int type = parseCollectionType(collectionType);
		switch (type) {
			case PROJECT: 
				return REPORT_VIEW;
			case PROJECTS_TREE:
			case PROJECTS_FLAT:
				return PROJECT_REPORT_VIEW;
			case TASKS_TREE:
			case TASKS_FLAT:
			case TASKS_ASSIGNMENTS_TREE:
			case TASKS_ASSIGNMENTS_FLAT:
			case ASSIGNMENTS_PROJECT_BASED:
				return TASK_REPORT_VIEW;
			case RESOURCES_TREE:
			case RESOURCES_FLAT:
			case RESOURCES_ASSIGNMENTS_TREE:
			case RESOURCES_ASSIGNMENTS_FLAT:
			case ASSIGNMENTS_RESOURCE_BASED:
				return RESOURCE_REPORT_VIEW;
			default:
				return null;
		}

	}
	
	@SuppressWarnings("unchecked")
	public static DataSource createDataSource(JasperReport report, Project project, PredicatedNodeFilterIterator cacheIterator, WalkersNodeModel walkersNodeModel) throws JRException {
		DataSource dataSource = new DataSource();
		boolean timeBased = report.getProperty(TIME_BASED) != null;
		String collectionType = report.getProperty(COLLECTION_TYPE_PROPERTY);
		if (collectionType == null)
			throw new JRException("must specify collectionType property in report definition");
		int type = parseCollectionType(collectionType);
		if (type < 0)
			throw new JRException("Invalid collectionType property in report definition: " + collectionType);
		int outline = 0;
		String outlineNumber = report.getProperty(OUTLINE_PROPERTY);
		if (outlineNumber != null) {
			try {
				outline = Integer.parseInt(outlineNumber);
			} catch (NumberFormatException e) {
				logger.warn("Ignoring malformed outline property {}", outlineNumber);
			}
		}
		
		
		Predicate predicate = null;
		boolean tree = false;
		switch (type) {
			case PROJECT: 
				break;
			case PROJECTS_TREE:
				predicate = Project.instanceofPredicate();
				tree = true;
				break;
			case PROJECTS_FLAT:
				predicate = Project.instanceofPredicate();
				break;
			case TASKS_TREE:
				predicate = Task.instanceofPredicate();
				tree = true;
				break;
			case TASKS_FLAT:
				predicate = Task.instanceofPredicate();
				break;
			case RESOURCES_TREE:
				predicate = ResourceImpl.instanceofPredicate();
				tree = true;
				break;
			case RESOURCES_FLAT:
				predicate = ResourceImpl.instanceofPredicate();
				break;
			case TASKS_ASSIGNMENTS_TREE:
				tree = true;
				break;
			case RESOURCES_ASSIGNMENTS_TREE:
				tree = true;
				break;
			case TASKS_ASSIGNMENTS_FLAT:
				break;
			case RESOURCES_ASSIGNMENTS_FLAT:
				break;
			case ASSIGNMENTS_PROJECT_BASED:
				predicate = Assignment.instanceofPredicate();
				break;
			case ASSIGNMENTS_RESOURCE_BASED:
				predicate = Assignment.instanceofPredicate();
				break;
			case ASSIGNMENTS:
			case PREDECESSORS:
			case SUCCESSORS:
				throw new JRException("Report collectionType " + type + " is not implemented for desktop reports");
		}
		dataSource.setTimeBased(timeBased);
		dataSource.setProject(project);
		dataSource.setIterator(cacheIterator);
		dataSource.setNodeModel(walkersNodeModel);
		dataSource.setNodeBased(tree);
		dataSource.setPredicate(predicate);
		return dataSource;
	}
	
//	private static NodeModel getResourceModel(Project project, int outlineNumber) {
//		NodeModel resourceModel = project.getResourcePool().getResourceOutline(outlineNumber);
//		if (resourceModel instanceof AssignmentNodeModel) {
//			((AssignmentNodeModel)resourceModel).addAssignments();
//		}
//		return resourceModel;
//	}
public void dispose(JRDataSource arg0) throws JRException {
		// No per-data-source resources are retained by this provider.
	}

	public static Object fieldValueConverterToPrimitiveType(Field field,Object fieldValue) {
		if (fieldValue == null)
			return null;
		if (field.isRate()) {
			return Double.valueOf(((Rate)fieldValue).getValue());
		} else if(field.isMoney()) {
			return Double.valueOf(((Money)fieldValue).doubleValue());
		} else if (field.isDurationOrWork()) {
			return Long.valueOf(((Duration)fieldValue).longValue());
		} else {
			return fieldValue;
		}
	}
	
	public static JasperDesign addTimescale(JasperDesign design, TimeIterator iterator, Class fieldType) {
		
		// extract field name
		String baseFieldName = design.getProperty("timeBasedField");
		
		// get column header start position
		JRBand pageHeader = design.getPageHeader();
		JRElement[] elements = pageHeader.getElements();
		int maxX = 0;
		int maxY = 0;
		for(int i = 0; i < elements.length; i++)
		{
			maxX = (elements[i].getX() + elements[i].getWidth() > maxX)?(elements[i].getX() + elements[i].getWidth()):maxX;
			maxY = (elements[i].getY() > maxY)?elements[i].getY():maxY;
		}

		try {
			while(iterator.hasNext()) {
				
				// build field name from start & end
				TimeInterval interval = iterator.next();
				String fieldName = "TIME";
				fieldName += interval.getStart();
				fieldName += "_";
				fieldName += interval.getEnd();
				fieldName += "_" + baseFieldName;
				logger.debug("time based field is {}", fieldName);
				// add fields
				JRDesignField field = new JRDesignField();
				field.setName(fieldName);
				field.setValueClass(fieldType);
				design.addField(field);
			
				logger.debug("column header is {}", interval.getText2());
				
				// add columns
				JRDesignBand columnHeader = (JRDesignBand) design.getColumnHeader();
	
				JRDesignStaticText staticText = new JRDesignStaticText();
				staticText.setX(maxX);
				staticText.setY(0);
				staticText.setWidth(80);
				staticText.setHeight(15);
			staticText.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
				JRDesignFont normalFont = new JRDesignFont(design.getDefaultStyle());
				normalFont.setFontName("Arial");
				normalFont.setFontSize(Float.valueOf(10));
				normalFont.setPdfFontName("Helvetica");
				applyFont(staticText, normalFont);
				staticText.setText(interval.getText1());
				staticText.setPrintWhenDetailOverflows(true);
				columnHeader.addElement(staticText);

				// add textFields
				JRDesignBand detailBand = (JRDesignBand) ((JRDesignSection) design.getDetailSection()).getBandsList().get(0);
	
				JRDesignTextField textField = new JRDesignTextField();
				textField.setX(maxX);
				textField.setY(0);
				textField.setWidth(80);
				textField.setHeight(15);
				textField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
				applyFont(textField, normalFont);
	//			textField.setFont((JRReportFont)fonts.get("normalFont"));
				JRDesignExpression expression = new JRDesignExpression();
				expression.setText("$F{" + fieldName + "}");
				textField.setExpression(expression);
				textField.setPrintWhenDetailOverflows(true);
				detailBand.addElement(textField);
				maxX += 80;
			}

		} catch (JRException e) {
			throw new IllegalStateException("Unable to add timescale columns to report design", e);
		}
		return design;
      
//        detailBand.addElement();
        
		
	}
}

