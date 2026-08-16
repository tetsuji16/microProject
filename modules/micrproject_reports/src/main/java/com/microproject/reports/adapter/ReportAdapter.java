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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRGroup;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignGroup;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignFont;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JRDesignTextElement;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignVariable;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.CalculationEnum;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.ResetTypeEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microproject.configuration.ReportColumns;
import com.microproject.configuration.ReportDefinition;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;

/**
 *
 */
public class ReportAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ReportAdapter.class);
	private static String GroupName = "group";

	private ReportDefinition reportDefinition;
	private JRDesignFont boldFont;
	private JRDesignFont italicFont;
	private JRDesignFont normalFont;
	private boolean hasAggregableField = false;
	
	private JasperDesign jasperDesign = new JasperDesign();
	
	public ReportAdapter(ReportDefinition reportDefinition) {
		this.reportDefinition = reportDefinition;
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
	
	private boolean isAggregable(Field field) {
		return (Field.SUM == field.getSummaryForGroup());
	}
	
	private int neededWidth(SpreadSheetFieldArray fields) {
		int width = 0;
		
		// LEGAL: 612x1008
		// BORDERS: 20x30x20x30
		
		Iterator iterator = fields.iterator();
		while(iterator.hasNext()) {
			Field field = (Field)iterator.next();
			width += field.getColumnWidth();
			
		}
		return width;
	}
	
	private void generateBaseDesign()  throws JRException {
		
		if(reportDefinition.isTimeBased()) {
			jasperDesign.setProperty(DataSourceProvider.TIME_BASED, "true");
		}
		
		jasperDesign.setProperty(DataSourceProvider.COLLECTION_TYPE_PROPERTY
				, Integer.valueOf(reportDefinition.getCollectionType()).toString());
		
		jasperDesign.setName(reportDefinition.getName());
		jasperDesign.setPageWidth(1008);
		jasperDesign.setPageHeight(612);
		jasperDesign.setColumnWidth(968);
		jasperDesign.setColumnSpacing(0);
		jasperDesign.setLeftMargin(20);
		jasperDesign.setRightMargin(20);
		jasperDesign.setTopMargin(30);
		jasperDesign.setBottomMargin(30);
		
			//Fonts
			normalFont = new JRDesignFont(jasperDesign.getDefaultStyle());
			normalFont.setFontName("Arial");
			normalFont.setFontSize(Float.valueOf(10));
			normalFont.setPdfFontName("Helvetica");
			normalFont.setPdfEncoding("Cp1252");
			normalFont.setPdfEmbedded(false);
			
			boldFont = new JRDesignFont(jasperDesign.getDefaultStyle());
			boldFont.setFontName("Arial");
			boldFont.setFontSize(Float.valueOf(12));
			boldFont.setBold(true);
			boldFont.setPdfFontName("Helvetica-Bold");
			boldFont.setPdfEncoding("Cp1252");
			boldFont.setPdfEmbedded(false);
			
			italicFont = new JRDesignFont(jasperDesign.getDefaultStyle());
			italicFont.setFontName("Arial");
			italicFont.setFontSize(Float.valueOf(12));
			italicFont.setItalic(true);
			italicFont.setPdfFontName("Helvetica-Oblique");
			italicFont.setPdfEncoding("Cp1252");
			italicFont.setPdfEmbedded(false);

			//Title
			JRDesignBand band = new JRDesignBand();
			band.setHeight(50);
			JRDesignLine line = new JRDesignLine();
			line.setX(0);
			line.setY(0);
			line.setWidth(968);
			line.setHeight(0);
			band.addElement(line);
			
			JRDesignStaticText text = new JRDesignStaticText();
			
			text.setX(0);
			text.setY(10);
			text.setWidth(968);
			text.setHeight(30);
			text.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
			JRDesignFont bigFont = new JRDesignFont(jasperDesign.getDefaultStyle());
			bigFont.setFontName("Arial");
			bigFont.setFontSize(Float.valueOf(22));
			bigFont.setPdfFontName("Helvetica");
			bigFont.setPdfEncoding("Cp1252");
			bigFont.setPdfEmbedded(false);
			applyFont(text, bigFont);
			text.setText(reportDefinition.getName());
			band.addElement(text);
			jasperDesign.setTitle(band);
			
	}
	
	public JRDesignTextField getPageFooter() {
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(0);
		textField.setY(10);
		textField.setWidth(968);
		textField.setHeight(15);
		textField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
		applyFont(textField, normalFont);
		JRDesignExpression expression = new JRDesignExpression();
		expression.setText("\"Page \" + String.valueOf($V{PAGE_NUMBER})"); //  + \" of\"");
		textField.setExpression(expression);
		return textField;
	}
	
	private String getFieldName(Field field, boolean asDuration) {
		String fieldName = field.getId();
		String returnFieldName = "";
		
		String mod = "";
		
		fieldName = fieldName.substring(new String("Field.").length());
		
		if( (!field.isMoney())) {
			if(!asDuration) {
				mod = "MODText" + mod;
				mod = mod + "FIELD";
			}
		}
		
		returnFieldName = mod + fieldName;
		
		return returnFieldName;
	}
	
	private Class getFieldClass(Field field, boolean asDuration) {
		if(field.isMoney()) {
			return Double.class;
		} else if(field.isDurationOrWork() && asDuration) {
			return Long.class;
		} else {
			return String.class;
		}
	}
	
	private String getFieldPattern(Field field) {
		if(field.isMoney()) {
			return "$ #,##0.00";
		} else {
			return null;
		}
	}
	
	private void addFields(ArrayList fields) throws JRException {
		Iterator iterator = fields.iterator();
		while(iterator.hasNext()) {
			Field field = (Field)iterator.next();
			
			JRDesignField designField = new JRDesignField();
			designField.setName(getFieldName(field, false));
			designField.setValueClass(getFieldClass(field, false));


//			System.out.println("field is " + designField.getName());
			try{ //TOTO try catch is to avoid problems with duplicate field. Find the cause.
				jasperDesign.addField(designField);
	
				if(isAggregable(field)) {
					hasAggregableField = true;
	
					if(field.isDurationOrWork()) {
						// add extra Long field (used for calculations)
						designField = new JRDesignField();
						designField.setName(getFieldName(field, true));
						designField.setValueClass(getFieldClass(field, true));
						jasperDesign.addField(designField);
					}
				}
			}catch(JRException e){
				logger.warn("Skipping duplicate or invalid report field {}", field.getId(), e);
			}
		}
	}
	
/**
 * Add fields which have sums.  If a group is passed in, assign the filds to the group so that the sums
 * are reset after each group. 
 * @param fields
 * @param group
 * @throws JRException
 */
	private void addAggregableFields(ArrayList fields, JRDesignGroup group) throws JRException {
		Iterator iterator = fields.iterator();
		while(iterator.hasNext()) {
			Field field = (Field)iterator.next();

			if(isAggregable(field)) {
				String fieldName = getFieldName(field, true);
				JRDesignVariable variable = new JRDesignVariable();
				variable.setName(fieldName + "Sum");
				variable.setValueClass(getFieldClass(field, true));
				variable.setCalculation(CalculationEnum.SUM);
				if (group == null) {
					variable.setResetType(ResetTypeEnum.REPORT);
				} else {
					variable.setResetType(ResetTypeEnum.GROUP);
					variable.setResetGroup(group);
					
				}
				
				JRDesignExpression expression = new JRDesignExpression();
				expression.setText("$F{" + fieldName + "}");
				variable.setExpression(expression);
				jasperDesign.addVariable(variable);
				
			}
		}
	}
	
	private JRDesignBand addFieldsHeader(JRDesignBand band, SpreadSheetFieldArray fields, boolean isSub) {
//		JRDesignRectangle rectangle = new JRDesignRectangle();
//		rectangle.setX(0);
//		rectangle.setY(5);
//		rectangle.setWidth(515);
//		rectangle.setHeight(15);
////		if(isSub) {
////			rectangle.setForecolor(new Color(0x99, 0x99, 0x99));
////			rectangle.setBackcolor(new Color(0x99, 0x99, 0x99));
////		} else {
////			rectangle.setForecolor(new Color(0x33, 0x33, 0x33));
////			rectangle.setBackcolor(new Color(0x33, 0x33, 0x33));
////		}
//		band.addElement(rectangle);
		
		// columns in page header
		Iterator iterator = fields.iterator();
		int x = 0;
		while(iterator.hasNext()) {
			Field field = (Field)iterator.next();
			JRDesignStaticText staticText = new JRDesignStaticText();
			staticText.setX(x);
			if(isSub) {
				staticText.setY(20);
			} else {
				staticText.setY(5);
			}
			staticText.setWidth(field.getColumnWidth());
			staticText.setHeight(15);
			staticText.setForecolor(Color.white);
			if(isSub) {
				staticText.setBackcolor(new Color(0x99, 0x99, 0x99));
			} else {
				staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
			}
			staticText.setMode(ModeEnum.OPAQUE);
			staticText.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
			applyFont(staticText, boldFont);
			staticText.setText(field.getName());
			band.addElement(staticText);
			
			x += field.getColumnWidth();
		}
		
		return band;
	}
	
	private JRDesignBand getFieldsHeader(SpreadSheetFieldArray fields, boolean isSub) throws JRException {
		//Page header
		JRDesignBand band = new JRDesignBand();
		band.setHeight(20);
		return addFieldsHeader(band, fields, isSub);
	}
	
	private JRDesignBand addDetail(JRDesignBand band, SpreadSheetFieldArray fields, JRGroup group) throws JRException {
		Iterator iterator = fields.iterator();
		int x = 0;
		if(null != group) {
			band.setHeight(40);
		} else {
			band.setHeight(15);
		}
		while(iterator.hasNext()) {
			Field field = (Field)iterator.next();
			JRDesignTextField textField = new JRDesignTextField();
			if(null != group) {
				textField.setEvaluationTime(EvaluationTimeEnum.GROUP);
				textField.setEvaluationGroup(group);
//				textField.setBackcolor(Color.black);
//				textField.setForecolor(Color.white);
				textField.setY(5);
				applyFont(textField, boldFont);
				textField.setHeight(15);
			}
			else {
				textField.setY(0);
				applyFont(textField, normalFont);
				textField.setHeight(12);
			}
			textField.setX(x);
			textField.setWidth(field.getColumnWidth());
			textField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);

			String fieldName = getFieldName(field, false);
			if(field.isMoney()) {
				// Double
				textField.setPattern(getFieldPattern(field));
			}

			JRDesignExpression expression = new JRDesignExpression();
			expression.setText("$F{" + fieldName + "}");

			textField.setExpression(expression);
			band.addElement(textField);

			x += field.getColumnWidth();
		}
		
		return band;
	}
	
	private JRDesignBand getDetail(SpreadSheetFieldArray fields, JRGroup group) throws JRException {
		//Detail
		JRDesignBand band = new JRDesignBand();
		return addDetail(band, fields, group);
	}
	
	private JRDesignBand getAggregatableFooter(SpreadSheetFieldArray fields) throws JRException {
		
		JRDesignBand band = new JRDesignBand();
		band.setHeight(40);
		Iterator iterator = fields.iterator();
		int x = 0;
		while(iterator.hasNext()) {
			Field field = (Field)iterator.next();
			if(isAggregable(field)) {
				JRDesignLine line = new JRDesignLine();
				line.setX(x);
				line.setY(0);
				line.setWidth(field.getColumnWidth());
				line.setHeight(0);
				band.addElement(line);
				line.setY(2);
				band.addElement(line);

				JRDesignTextField textField = new JRDesignTextField();
				textField.setX(x);
				textField.setY(4);
				textField.setWidth(field.getColumnWidth());
				textField.setHeight(12);
				textField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
				applyFont(textField, normalFont);
				JRDesignExpression expression = new JRDesignExpression();

				
				// Money (double) or Duration(long)
				if(field.isMoney()) {
					textField.setPattern(getFieldPattern(field));
					expression.setText("$V{" + getFieldName(field, true) + "Sum}");
				} else if (field.isWork()) {
					expression.setText("com.microproject.datatype.DurationFormat.formatWork($V{" + getFieldName(field, true) + "Sum})" );
				} else if(field.isDuration()) {
					expression.setText("com.microproject.datatype.DurationFormat.format($V{" + getFieldName(field, true) + "Sum})" );
				}

				textField.setExpression(expression);
				band.addElement(textField);
			}

			x += field.getColumnWidth();
		}

		return band;

	}
	
	private void addLastPageFooter(SpreadSheetFieldArray fields) throws JRException {
		JRDesignBand band = getAggregatableFooter(fields);
		band.addElement(getPageFooter());
		
		jasperDesign.setLastPageFooter(band);
	}
	private void addPageFooter() throws JRException {
		//page footer
		JRDesignBand band = new JRDesignBand();
		band.setHeight(30);
		JRDesignLine line = new JRDesignLine();
		line.setX(0);
		line.setY(0);
		line.setWidth(968);
		line.setHeight(0);
		band.addElement(line);
		
		band.addElement(getPageFooter());
		jasperDesign.setPageFooter(band);
	}
	
	public void generateDesign(SpreadSheetFieldArray fieldArray) throws JRException {
		hasAggregableField = false;
		generateBaseDesign();
		ArrayList columnsList = (ArrayList) reportDefinition.getColumnsList();

		ReportColumns columns;
		if(columnsList.size() == 1) {
			// simple flat report
			SpreadSheetFieldArray fields;
			if (fieldArray != null)
				fields =  fieldArray;
			else {
				columns = (ReportColumns)columnsList.get(0);
				fields = columns.getFieldArray();
			}	
			addFields(fields);
			
			if(hasAggregableField) {
				addAggregableFields(fields,null);
			}
			
			jasperDesign.setPageHeader(getFieldsHeader(fields, false));
			
			((JRDesignSection) jasperDesign.getDetailSection()).addBand(getDetail(fields, null));
			
			addPageFooter();
			
			// last page footer (if any)
			if(hasAggregableField) {
				addLastPageFooter(fields);
			}
			
//			int neededW = neededWidth(fields);
//			System.out.println("Needed width is " + neededW);
//			System.out.println("columns number is " + fields.size());
		} else if(columnsList.size() == 2) {
			// reports & subreports
			columns = (ReportColumns)columnsList.get(0);
			String groupByField = columns.getGroupbyField();
			SpreadSheetFieldArray mainFields = columns.getFieldArray();
			columns = (ReportColumns)columnsList.get(1);
			SpreadSheetFieldArray detailFields;
			if (fieldArray != null)
				detailFields = fieldArray;
			else
				detailFields = columns.getFieldArray();
			
			addFields(mainFields);
			addFields(detailFields);
			
			JRDesignGroup group = new JRDesignGroup();
			if(hasAggregableField) {
				addAggregableFields(detailFields,group);
			}
			
			
			group.setName(GroupName);
			group.setStartNewColumn(false);
			group.setStartNewPage(false);
			JRDesignExpression expression = new JRDesignExpression();
			
			Iterator iterator = mainFields.iterator();
			while(iterator.hasNext()) {
				Field f = (Field)iterator.next();

				if(groupByField.equals(f.getId())) 
				{
					expression.setText("$F{" + getFieldName(f, false) + "}");
					group.setExpression(expression);
					break;
				}
			}
			
			JRDesignBand band = getDetail(mainFields, group);
			band = addFieldsHeader(band, detailFields, true);
			((JRDesignSection) group.getGroupHeaderSection()).addBand(band);

			if(hasAggregableField) {
				((JRDesignSection) group.getGroupFooterSection()).addBand(getAggregatableFooter(detailFields));
			}
			jasperDesign.addGroup(group);

			jasperDesign.setPageHeader(getFieldsHeader(mainFields, false));
			((JRDesignSection) jasperDesign.getDetailSection()).addBand(getDetail(detailFields, null));
			addPageFooter();
			
		} else {
			throw new JRException("report definition must contain either one or two columns (see view.xml)");
		}
		
	}
	
	/**
	 * @return Returns the jasperDesign.
	 */
	public JasperDesign getJasperDesign() {
		return jasperDesign;
	}
}
