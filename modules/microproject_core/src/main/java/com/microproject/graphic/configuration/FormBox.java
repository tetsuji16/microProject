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
package com.microproject.graphic.configuration;

import com.microproject.configuration.FieldDictionary;
import com.microproject.field.Field;



public class FormBox {
	String id=null;
	String textId=null;
	String fieldId=null;
	int column=-1;
	int row=-1;
	int columnSpan=1;
	int rowSpan=1;
	String alignment=null;
	String font=null;
	int minZoom=Integer.MIN_VALUE;
	Field field=null;
	
	public FormBox() {}
	
	
	
	
	
	public String getAlignment() {
		return alignment;
	}
	public void setAlignment(String alignment) {
		this.alignment = alignment;
	}
	public int getColumn() {
		return column;
	}
	public void setColumn(int column) {
		this.column = column;
	}
	public int getColumnSpan() {
		return columnSpan;
	}
	public void setColumnSpan(int columnSpan) {
		this.columnSpan = columnSpan;
	}
	public String getFieldId() {
		return fieldId;
	}
	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
		getField();
	}
	public Field getField(){
		if (field==null||field.getId()!=fieldId){
			if (fieldId==null) field=null;
			field=FieldDictionary.getInstance().getFieldFromId(fieldId);
		}
		return field;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getRow() {
		return row;
	}
	public void setRow(int row) {
		this.row = row;
	}
	public int getRowSpan() {
		return rowSpan;
	}
	public void setRowSpan(int rowSpan) {
		this.rowSpan = rowSpan;
	}
	public String getTextId() {
		return textId;
	}
	public void setTextId(String textId) {
		this.textId = textId;
	}
	
	public String getFont() {
		return font;
	}
	public void setFont(String font) {
		this.font = font;
	}
	
	public int getMinZoom() {
		return minZoom;
	}
	public void setMinZoom(int minZoom) {
		this.minZoom = minZoom;
	}





	public String toString(){
		StringBuilder buf = new StringBuilder();
		buf.append("id=").append(id);
		buf.append(" fieldId=").append(fieldId);
		buf.append("textId=").append(textId);
		buf.append(" row=").append(row);
		buf.append(" column=").append(column);
		buf.append(" rowSpan=").append(rowSpan);
		buf.append(" columnSpan=").append(columnSpan);
		buf.append(" alignment=").append(alignment);
		buf.append(" font=").append(font);
		return buf.toString();
	}
	
	
	
}
