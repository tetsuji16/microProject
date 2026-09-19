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
package com.microproject.pm.graphic.spreadsheet.common;

import java.util.Date;
import java.util.Vector;

import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.netbeans.swing.outline.Outline;

import com.microproject.pm.graphic.spreadsheet.editor.DateEditor;
import com.microproject.pm.graphic.spreadsheet.editor.SimpleEditor;
import com.microproject.pm.graphic.spreadsheet.editor.SpreadSheetCellEditorAdapter;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.renderer.DateRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.OfflineCapableBooleanRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.SimpleRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.SpreadSheetCellRendererAdapter;
import com.microproject.datatype.Duration;
import com.microproject.datatype.Money;
import com.microproject.datatype.Work;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.util.FlatUiSupport;

/**
 *
 */
public class CommonTable extends Outline {

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		if (getModel() instanceof SpreadSheetModel model) {
			com.microproject.field.Field field = model.getFieldInViewColumn(column);
			if (field != null && (field.isDuration() || "Field.duration".equals(field.getId()))) {
				return getDefaultEditor(Duration.class);
			}
		}
		return super.getCellEditor(row, column);
	}

    /**
     * 
     */
    public CommonTable() {
        super();
        FlatUiSupport.applySpreadsheetTableStyle(this);
        setRootVisible(false);
    }

    /**
     * @param numRows
     * @param numColumns
     */
    public CommonTable(int numRows, int numColumns) {
        super();
        setModel(new javax.swing.table.DefaultTableModel(numRows, numColumns));
        FlatUiSupport.applySpreadsheetTableStyle(this);
        setRootVisible(false);
    }

    /**
     * @param dm
     */
    public CommonTable(TableModel dm) {
        super();
        setModel(dm);
        FlatUiSupport.applySpreadsheetTableStyle(this);
        setRootVisible(false);
    }

    /**
     * @param rowData
     * @param columnNames
     */
    public CommonTable(Object[][] rowData, Object[] columnNames) {
        super();
        setModel(new javax.swing.table.DefaultTableModel(rowData, columnNames));
        FlatUiSupport.applySpreadsheetTableStyle(this);
        setRootVisible(false);
    }

    /**
     * @param rowData
     * @param columnNames
     */
    public CommonTable(Vector<? extends Vector<?>> rowData, Vector<?> columnNames) {
        super();
        setModel(createDefaultTableModel(rowData, columnNames));
        FlatUiSupport.applySpreadsheetTableStyle(this);
        setRootVisible(false);
    }

    /**
     * @param dm
     * @param cm
     */
    public CommonTable(TableModel dm, TableColumnModel cm) {
        super();
        setModel(dm);
        setColumnModel(cm);
        FlatUiSupport.applySpreadsheetTableStyle(this);
        setRootVisible(false);
    }

    /**
     * @param dm
     * @param cm
     * @param sm
     */
    public CommonTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super();
        setModel(dm);
        setColumnModel(cm);
        setSelectionModel(sm);
        FlatUiSupport.applySpreadsheetTableStyle(this);
        setRootVisible(false);
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new CommonTableHeader(getColumnModel());
    }

    @Override
    public void updateUI() {
        super.updateUI();
        FlatUiSupport.applySpreadsheetTableStyle(this);
    }

    private static javax.swing.table.DefaultTableModel createDefaultTableModel(Vector<? extends Vector<?>> rowData, Vector<?> columnNames) {
        @SuppressWarnings("unchecked")
        Vector<Vector<?>> typedRows = (Vector<Vector<?>>) rowData;
        return new javax.swing.table.DefaultTableModel(typedRows, columnNames);
    }
    public boolean editorsRegistered;
	protected void registerEditors(){
		registerEditors(false);
	}
	protected void registerEditors(boolean compact){
		if (editorsRegistered) return;
		GraphicConfiguration config=GraphicConfiguration.getInstance();
		
		//Modify here to register a custom editor
		//all the types used have to be registered here
		setAdaptedRenderer(String.class,new SimpleRenderer());
		setAdaptedEditor(String.class,new SimpleEditor(String.class));
		
		setAdaptedRenderer(Integer.class,new SimpleRenderer());
		setAdaptedEditor(Integer.class,new SimpleEditor(Integer.class));
		
		setAdaptedRenderer(Double.class,new SimpleRenderer());
		setAdaptedEditor(Double.class,new SimpleEditor(Double.class));
		
       setAdaptedEditor(Date.class, new DateEditor());
//       setAdaptedRenderer(Date.class, new DateRendererDecorator( new SimpleRenderer(), format)); // format will be used
       setAdaptedRenderer(Date.class,new DateRenderer());
       
       setAdaptedRenderer(Boolean.class,new OfflineCapableBooleanRenderer());
		//setAdaptedRenderer(Boolean.class,null);
		setAdaptedEditor(Boolean.class,null);
		
		setAdaptedRenderer(Work.class,new SimpleRenderer(compact));
		setAdaptedEditor(Work.class,new SimpleEditor(Work.class));

		setAdaptedRenderer(Duration.class,new SimpleRenderer());
		setAdaptedEditor(Duration.class,new SimpleEditor(Duration.class));
//		setDefaultEditor(Duration.class,new DefaultCellEditor(new JTextField()));
		setAdaptedRenderer(Money.class,new SimpleRenderer(compact));
		setAdaptedEditor(Money.class,new SimpleEditor(Money.class));
		
		editorsRegistered=true;
	}
	protected void setAdaptedRenderer(Class columnClass,TableCellRenderer renderer) {
		setDefaultRenderer(columnClass,new SpreadSheetCellRendererAdapter(
				(renderer==null)?getDefaultRenderer(columnClass):renderer));
	}
	protected void setAdaptedEditor(Class columnClass,TableCellEditor editor) {
		setDefaultEditor(columnClass, new SpreadSheetCellEditorAdapter(
				(editor==null)?getDefaultEditor(columnClass):editor));
	}
}
