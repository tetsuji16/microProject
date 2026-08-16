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

import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.digester.Digester;

import com.microproject.configuration.Configuration;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.NamedItem;
import com.microproject.field.Field;
import com.microproject.pm.assignment.TimeDistributedHelper;
import com.microproject.strings.Messages;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/**
 *
 */
public class SpreadSheetFieldArray extends ArrayList<Field> implements NamedItem, Cloneable, WorkspaceSetting {
	private static final long serialVersionUID = 6310711336308730391L;
	transient Map<String, String> map = new LinkedHashMap<>();
	transient boolean userCreated = false;
	ArrayList<Integer> widths = null;//new ArrayList<Integer>();
	@Override
	public SpreadSheetFieldArray clone() {
		SpreadSheetFieldArray copy = (SpreadSheetFieldArray) super.clone();
		copy.map = new LinkedHashMap<>(map);
		copy.widths = widths == null ? null : new ArrayList<>(widths);
		return copy;
	}

	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();
		map = new LinkedHashMap<>();
	}

	public SpreadSheetFieldArray() {

	}

	public SpreadSheetFieldArray makeUserDefinedCopy() {
		SpreadSheetFieldArray newOne = (SpreadSheetFieldArray) clone();
		newOne.setId(null); // it's user defined
		newOne.setName(Dictionary.generateUniqueName(this));
		newOne.userCreated = true;
		return newOne;
	}
	public SpreadSheetFieldArray makeEditableVersion() {
		SpreadSheetFieldArray f = this;
		if (!f.isUserDefined()) {
			f = f.makeUserDefinedCopy();
			Dictionary.add(f);
		}
		return f;
	}
	public SpreadSheetFieldArray insertField(int position,Field field) {
		SpreadSheetFieldArray f = makeEditableVersion();
		f.add(position,field);
		//f.widths.add(field.getColumnWidth());
		return f;
	}

	public SpreadSheetFieldArray removeField(int position) {
		SpreadSheetFieldArray f = makeEditableVersion();
		f.remove(position);
		//f.widths.remove(position);
		return f;
	}
	public SpreadSheetFieldArray move(int oldPosition, int newPosition) {
		SpreadSheetFieldArray f = makeEditableVersion();
		Field field = f.remove(oldPosition);
		//Integer w = f.widths.remove(oldPosition);
		SpreadSheetFieldArray result = f.insertField(newPosition,field);
		//result.widths.set(newPosition,w);
		return result;


	}

//	public void setWidth(int column, int width) {
//		widths.set(column,width);
//	}
	/**
	 * Equality is based on name, not on contents
	 */
	public boolean equals(Object arg0) {
		if (! (arg0 instanceof SpreadSheetFieldArray))
			return false;
		return Objects.equals(name, ((SpreadSheetFieldArray)arg0).getName());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}
	private String name = null;
	private String category;
	private String cellStyleId;
	private String actionListId;
	private String id = null;

    public String getCellStyleId() {
        return cellStyleId;
    }
    public void setCellStyleId(String cellStyleId) {
        this.cellStyleId = cellStyleId;
    }
    public CellStyle getCellStyle(){
        CellStyles cellStyles=CellStyles.getInstance();
        if (cellStyles == null) {
            return new CellStyle() {
                public CellFormat getCellFormat(Object node) {
                    return null;
                }
            };
        }
        if (cellStyleId==null||cellStyleId.length()==0)
            return safeDefaultStyle(cellStyles);
        CellStyle style=cellStyles.getStyle(cellStyleId);
        if (style==null) style=safeDefaultStyle(cellStyles);
        return style;
    }

    private CellStyle safeDefaultStyle(CellStyles cellStyles) {
        CellStyle defaultStyle = cellStyles.getDefaultStyle();
        if (defaultStyle != null) {
            return defaultStyle;
        }
        return new CellStyle() {
            public CellFormat getCellFormat(Object node) {
                return null;
            }
        };
    }

    public String getActionListId() {
        return actionListId;
    }
    public void setActionListId(String actionListId) {
        this.actionListId = actionListId;
    }
    public ActionList getActionList(){
        ActionLists actionLists=ActionLists.getInstance();
        if (actionLists == null) {
            return new ActionList() {
                public String getList(Object nodeModel) {
                    return null;
                }
            };
        }
        if (actionListId==null||actionListId.length()==0)
            return safeDefaultActionList(actionLists);
        ActionList actionList=actionLists.getActionList(actionListId);
        if (actionList==null) actionList=safeDefaultActionList(actionLists);
        return actionList;
    }

    private ActionList safeDefaultActionList(ActionLists actionLists) {
        ActionList defaultActionList = actionLists.getDefaultActionList();
        if (defaultActionList != null) {
            return defaultActionList;
        }
        return new ActionList() {
            public String getList(Object nodeModel) {
                return null;
            }
        };
    }

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	public void setId(String messageId) {
		this.id = messageId;
		if (name == null)
			setName(Messages.getString(messageId));
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return getName();
	}
	/**
	 * @return Returns the category.
	 */
	public String getCategory() {
		return category;
	}
	/**
	 * @param category The category to set.
	 */
	public void setCategory(String category) {
		this.category = category;
	}


	public boolean isUserDefined() {
		return id == null;
	}


	public void addField(String fieldId) {
		Field field = Configuration.getFieldFromId(fieldId);
		if (field != null) {
			if (mapFieldTo != null) {
				map.put(fieldId,mapFieldTo);
				mapFieldTo = null;
			}

			add(field);
			//widths.add(field.getColumnWidth());
		} else {
//			System.out.println("field is null in SpreadSheetFieldArray addField : ");
		}
	}
	public void removeField(String fieldId) {
		if (fieldId==null) return;
		map.remove(fieldId);
		for (int i = 0; i < size(); i++) {
			Field field=get(i);
			if (fieldId.equals(field.getId())) {
				remove(i);
				//widths.remove(i);
			}

		}
	}

	public String mapFieldTo;
	//root node needs to be Dictionary
	public static void addDigesterEvents(Digester digester){
		digester.addObjectCreate("*/spreadsheet", "com.microproject.graphic.configuration.SpreadSheetFieldArray");
	    digester.addSetProperties("*/spreadsheet");
		digester.addSetNext("*/spreadsheet", "add", "com.microproject.configuration.NamedItem");
	    digester.addSetProperties("*/spreadsheet/columns/column");
		digester.addCallMethod("*/spreadsheet/columns/column", "addField", 	0);

	}
	public static final SpreadSheetFieldArray getFromId(String category, String id) {
		SpreadSheetFieldArray result = (SpreadSheetFieldArray) Dictionary.get(category, Messages.getString(id));
		if (result == null)
			result = (SpreadSheetFieldArray) Dictionary.get(category, id);
		return result;
	}

	public final String getMapFieldTo() {
		return mapFieldTo;
	}

	public final void setMapFieldTo(String mapFieldTo) {
		this.mapFieldTo = mapFieldTo;
	}

	public final String getMappedValue(String key) {
		return map.get(key);
	}

	public static Object[] toIdArray(Object[] fieldArray) {
		Object[] result = new Object[fieldArray.length];
		for (int i = 0; i < fieldArray.length; i++)
			result[i] = TimeDistributedHelper.getIdForObject(fieldArray[i]);
		return result;
	}

	public static Object[] fromIdArray(Object[] fieldArray) {
		Object[] result = new Object[fieldArray.length];
		for (int i = 0; i < fieldArray.length; i++)
			result[i] = TimeDistributedHelper.getObjectFromId((String) fieldArray[i]);
		return result;
	}

	public static Collection<String> toIdArray(Collection<?> fieldArray) {
		ArrayList<String> result = new ArrayList<>(fieldArray.size());
		for (Object field : fieldArray) {
			result.add(TimeDistributedHelper.getIdForObject(field));
		}
		return result;
	}
	public static Collection<Field> fromIdArray(Collection<?> fieldArray) {
		ArrayList<Field> result = new ArrayList<>(fieldArray.size());
		for (Object id : fieldArray) {
			Object field = TimeDistributedHelper.getObjectFromId((String) id);
			if (field instanceof Field)
				result.add((Field) field);
		}
		return result;
	}

	public boolean isUserCreated() {
		return userCreated;
	}

	public void setUserCreated(boolean userCreated) {
		this.userCreated = userCreated;
	}

	public int getWidth(int column) {
		return (widths!=null&&column>=0&&column<widths.size())?widths.get(column):-1;
	}




	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.fields.addAll(toIdArray(this));
		if (widths!=null) ws.widths.addAll(widths);
		return ws;
	}

	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		addAll(fromIdArray(ws.fields));
		if (ws.version>0.0f&&ws.widths!=null&&ws.widths.size()>0){
			widths=new ArrayList<Integer>(ws.widths.size());
			widths.addAll(ws.widths);
		}
	}
	public static class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = -4517935309304612237L;
		ArrayList<Integer> widths = new ArrayList<Integer>();
		ArrayList<String> fields = new ArrayList<>();
		float version=1.0f;
	}

	public ArrayList<Integer> getWidths() {
		return widths;
	}

	public void setWidths(ArrayList<Integer> widths) {
		this.widths = widths;
	}

	public static SpreadSheetFieldArray restore(WorkspaceSetting spreadsheetWorkspace,String name,int context){
		SpreadSheetFieldArray fieldArray = new SpreadSheetFieldArray();
		fieldArray.setCategory(SpreadSheetCategories.taskSpreadsheetCategory);
		fieldArray.restoreWorkspace(spreadsheetWorkspace, context);
		fieldArray.setName(name);
		Dictionary.add(fieldArray);
		return fieldArray;
	}

}
