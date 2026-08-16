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
package com.microproject.pm.graphic.spreadsheet.renderer;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.field.Field;
import com.microproject.util.FlatUiSupport;

public class IndicatorsRenderer extends DefaultTableCellRenderer implements OfflineRenderer{
	private static final long serialVersionUID = 190987129201L;
	protected static JLabel cellHeader;
	protected IndicatorsComponent indicatorsComponent;

	private static final class IndicatorPanel extends JPanel {
		private static final long serialVersionUID = 1L;
	}
	
	public IndicatorsRenderer() {
		super();
	}
	
	
	
	public Component getTableCellRendererComponent (JTable table, Object value,boolean isSelected, boolean hasFocus, int row, int column){
		if(indicatorsComponent==null){
			if (TaskIndicatorsComponent.acceptTask(value)) indicatorsComponent=new TaskIndicatorsComponent();
			else if (ResourceIndicatorsComponent.acceptResource(value)) indicatorsComponent=new ResourceIndicatorsComponent();
		}
		if (indicatorsComponent!=null&&indicatorsComponent.acceptValue(value)) {
			JComponent label;
			label = new IndicatorPanel();
			//label=(JComponent)super.getTableCellRendererComponent(table,"",isSelected,hasFocus,row,column);
			//indicatorsComponent.setLook(label,isSelected,hasFocus);
			if (table!=null) {
				label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
			}
			label.setBorder(CellUtility.withRowGridOverlay(table, resolveCellBorder(table, isSelected, hasFocus)));
			label.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
			StringBuilder text = new StringBuilder();
			
			// I would like to also show a gif next to the text as MS does.  unfortunately, this is not doable
			// with the html tag, since there is no way to refrence the image from the jar (darn)
			// This could be accomplished via a custom tooltip UI.  See http://www.javareference.com/jrexamples/viewexample.jsp?id=83
			
			indicatorsComponent.setIndicators(value, label, text, isSelected, hasFocus);
			
			if (text.length() == 0){
				if (table==null){
					this.setText("");
					setBorder(resolveCellBorder(table, isSelected, hasFocus));
					return this;
				}else {
					JComponent empty = (JComponent)super.getTableCellRendererComponent(table,"",isSelected,hasFocus,row,column);//empty;
					empty.setBorder(resolveCellBorder(table, isSelected, hasFocus));
					return empty;
				}
			}
			text.insert(0,"<html>");
			text.append("</html>");
			label.setToolTipText(text.toString());
			return label;
		} else {
			if (table==null){
				this.setText("");
				setBorder(resolveCellBorder(table, isSelected, hasFocus));
				return this;
			}else {
				JComponent empty = (JComponent)super.getTableCellRendererComponent(table,"",isSelected,hasFocus,row,column);//empty;
				empty.setBorder(resolveCellBorder(table, isSelected, hasFocus));
				return empty;
			}
		}
	}

	static Border resolveCellBorder(JTable table, boolean isSelected, boolean hasFocus) {
		Border baseBorder;
		if (hasFocus) {
			baseBorder = FlatUiSupport.spreadsheetActiveCellBorder();
		} else if (isSelected) {
			baseBorder = FlatUiSupport.tableCellBorder();
		} else {
			baseBorder = FlatUiSupport.tableCellBorder();
		}
		return baseBorder;
	}
	
	public static JLabel getCellHeader() {
		if (cellHeader == null) {
			cellHeader = new JLabel(IconManager.getIcon("infomation.icon"));
			cellHeader.setHorizontalAlignment(JLabel.CENTER);
		}
		return cellHeader;
			
	}
	
	public Component getComponent(Object value, GraphicNode node,Field field,SpreadSheetParams params){
		Component component=getTableCellRendererComponent(null, value, false, false, -1, -1);
		return component;
	}

}

