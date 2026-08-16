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
package com.microproject.server.data.linker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.Predicate;

import com.microproject.server.data.AssignmentData;
import com.microproject.server.data.SerializeOptions;
import com.microproject.server.data.SerializedDataObject;
import com.microproject.server.data.TaskData;
import com.microproject.server.data.TypeSystemConverter;
import com.microproject.server.data.TypeSystemConverterFactory;
import com.microproject.grouping.core.hierarchy.NodeHierarchy;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

/**
 *
 */
public abstract class TaskLinker extends Linker {
	public void initIterator(){
		iterator=((Project)getParent()).getTaskOutlineIterator();
	}
	public Object executeNext(){
        Task task=(Task)iterator.next();
        //if (globalIdsOnly) CommonDataObject.makeGlobal(task);
     	return task;
	}

	public NodeHierarchy getHierarchy(){return ((Project)getParent()).getTaskOutline().getHierarchy();}

	protected Collection flatAssignments;

	public Collection getFlatAssignments() {
		return flatAssignments;
	}
	public void setFlatAssignments(Collection flatAssignments) {
		this.flatAssignments = flatAssignments;
	}
//	protected ArrayList<Long> unchanged;
//
//	public ArrayList<Long> getUnchanged() {
//		return unchanged;
//	}
//	public void setUnchanged(ArrayList<Long> unchanged) {
//		this.unchanged = unchanged;
//	}





	//extra field union needed for rollup fields
	protected class PreparedAttributes{
		protected SerializedDataObject data;
		protected Object obj;
		protected Collection extrafields; //extra fields
		protected List fieldArray;
		protected NodeModel model;
		public PreparedAttributes(SerializedDataObject data, Object obj, Collection extrafields, List fieldArray, NodeModel model) {
			super();
			this.data = data;
			this.obj = obj;
			this.extrafields = extrafields;
			this.fieldArray = fieldArray;
			this.model = model;
		}
		public SerializedDataObject getData() {
			return data;
		}
		public void setData(SerializedDataObject data) {
			this.data = data;
		}
		public Collection getExtrafields() {
			return extrafields;
		}
		public void setExtrafields(Collection extrafields) {
			this.extrafields = extrafields;
		}
		public List getFieldArray() {
			return fieldArray;
		}
		public void setFieldArray(List fieldArray) {
			this.fieldArray = fieldArray;
		}
		public NodeModel getModel() {
			return model;
		}
		public void setModel(NodeModel model) {
			this.model = model;
		}
		public Object getObj() {
			return obj;
		}
		public void setObj(Object obj) {
			this.obj = obj;
		}
	}
//	protected List<PreparedAttributes> preparedAttributes; //claur
//
//	public void addPreparedAttributes(SerializedDataObject data, Object obj, NodeModel model,SerializeOptions options) {
//		if (preparedAttributes==null) preparedAttributes=new ArrayList<PreparedAttributes>();
//		TypeSystemConverter converter=TypeSystemConverterFactory.getInstance().getConverter();
//		Predicate fieldFilter=options==null?null:options.getFieldFilter();
//   		if (data instanceof TaskData) preparedAttributes.add(new PreparedAttributes(data,obj,converter.getDirtyExtraFields(obj,fieldFilter),converter.getExposedTaskFields(fieldFilter),model));
//		else if (data instanceof AssignmentData) preparedAttributes.add(new PreparedAttributes(data,obj,converter.getDirtyExtraFields(obj,fieldFilter),converter.getExposedAssignmentFields(fieldFilter),model));
//
//	}
	
	public void computeAttributes(){
//        if (Environment.isNoPodServer()){
//           	TypeSystemConverter converter=TypeSystemConverterFactory.getInstance().getConverter();
//           	ArrayList<Field> unionExtraTaskFields=new ArrayList<Field>();
//           	ArrayList<Field> unionExtraAssignmentFields=new ArrayList<Field>();
//           	/*DEF164438: 	 Error exporting task plan to .xml
//           	  this stops the bombout which occurs.  may require revisiting if we find
//           	  that this code path is needed for msp export --TAF090707*/
//           	if (preparedAttributes ==  null) return;
//        	for (PreparedAttributes attrs:preparedAttributes){
//        		if (attrs.getExtrafields()==null) continue;
//        		if (attrs.getData() instanceof TaskData){
//        			unionExtraTaskFields.addAll(attrs.getExtrafields());
//        		}
//        		else if (attrs.getData() instanceof AssignmentData){
//        			unionExtraAssignmentFields.addAll(attrs.getExtrafields());
//        		}
//        	}
//        	for (PreparedAttributes attrs:preparedAttributes){
//        		SerializedDataObject data=attrs.getData();
//        		if (data instanceof TaskData){
//            		Map<String,Object> exposedAttributes=converter.convertFieldsAndCustomAttributes(attrs.getObj(), unionExtraTaskFields, attrs.getFieldArray(), attrs.getModel(),false);
//        			((TaskData)data).setAttributes(exposedAttributes);
//        		}
//        		else if (data instanceof AssignmentData){
//            		Map<String,Object> exposedAttributes=converter.convertFieldsAndCustomAttributes(attrs.getObj(), unionExtraAssignmentFields, attrs.getFieldArray(), attrs.getModel(),false);
//        			((AssignmentData)data).setAttributes(exposedAttributes);
//        		}
//        	}
//        }

	}

	public void addTransformedObjects() throws Exception{
		super.addTransformedObjects();
		computeAttributes();
	}
//	public List<PreparedAttributes> getPreparedAttributes() { //claur
//		return preparedAttributes;
//	}
//	public void setPreparedAttributes(List<PreparedAttributes> preparedAttributes) {
//		this.preparedAttributes = preparedAttributes;
//	}

	protected SerializeOptions options;
	public SerializeOptions getOptions() {
		return options;
	}
	public void setOptions(SerializeOptions options) {
		this.options = options;
	}


}
