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
package com.microproject.exchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Used to merge resources found in an imported file
 */
	public abstract class ResourceMappingForm {
		private static final Logger logger = Logger.getLogger(ResourceMappingForm.class.getName());
		protected List<Object> importedResources;
		protected ArrayList<Object> resources;
		protected List<Object> selectedResources;
		protected boolean local=false,master=false;
		protected int accessControlType;
		protected JFrame owner;
		
		public static final MergeField NO_MERGE=new MergeField(null,null,"");
		protected ArrayList<MergeField> mergeFields=new ArrayList<>();
		protected MergeField mergeField;
		protected Object unassignedResource;
		
		public ResourceMappingForm(){
			selectedResources=new ArrayList<>();
			mergeFields.add(NO_MERGE);
			mergeField=NO_MERGE;
			
		}
		
		public List<Object> getSelectedResources() {
			return selectedResources;
		}

		public void setSelectedResources(List<Object> selectedResources) {
			this.selectedResources = selectedResources;
		}

		public List<Object> getImportedResources() {
			return importedResources;
		}

		public void setImportedResources(List<Object> importedResources) {
			this.importedResources = importedResources;
		}

		public ArrayList<Object> getResources() {
			return resources;
		}

		public void setResources(ArrayList<Object> resources) {
			this.resources = resources;
		}

		public boolean isLocal() {
			return local;
		}

		public void setLocal(boolean local) {
			this.local = local;
		}

		public boolean isMaster() {
			return master;
		}

		public void setMaster(boolean master) {
			this.master = master;
		}

		public JFrame getOwner() {
			return owner;
		}

		public void setOwner(JFrame owner) {
			this.owner = owner;
		}
		
		
		
		public int getAccessControlType() {
			return accessControlType;
		}

		public void setAccessControlType(int accessControlType) {
			this.accessControlType = accessControlType;
		}

		public abstract boolean execute();
		
		
		
		
		public Object getUnassignedResource() {
			return unassignedResource;
		}

		public void setUnassignedResource(Object projectlibreUnassignedResource) {
			this.unassignedResource = projectlibreUnassignedResource;
		}

		public MergeField getMergeField() {
			return mergeField;
		}

		public void setMergeField(MergeField mergeField) {
			this.mergeField = mergeField;
			Map<Object, Object> mergeFieldMap=new HashMap<>();
			Set<Object> notMergedValues=new HashSet<>();
			if (mergeField!=NO_MERGE) {
			for (Object resource : resources){
				try {
					Object value=PropertyUtils.getProperty(resource,mergeField.getProjectLibreName());
					if (notMergedValues.contains(value)) continue;
					if (mergeFieldMap.containsKey(value)){ //not duplicates
						mergeFieldMap.remove(value);
						notMergedValues.add(value);
					}else mergeFieldMap.put(value,resource);
				} catch (Exception e) {
					logger.log(Level.WARNING, "Failed to read resource property for merge mapping", e);
				}
			}
			}
			
			selectedResources.clear();
			for (Object resource : importedResources){
				if (mergeField==NO_MERGE) selectedResources.add(unassignedResource);
				else{
					try {
						Object value=PropertyUtils.getProperty(resource,mergeField.getImportName());
						if (value==null||!mergeFieldMap.containsKey(value)) selectedResources.add(unassignedResource);
						else selectedResources.add(mergeFieldMap.get(value));
					} catch (Exception e) {selectedResources.add(unassignedResource);}
				}
				
			}
		}

		public void addMergeField(MergeField mergeField){
			mergeFields.add(mergeField);
		}
		
		public ArrayList<MergeField> getMergeFields(){
			return mergeFields;
		}
		
		public void selectMergeField(MergeField mergeField){
		}
		
		
		public static class MergeField{
			protected String importName,projectlibreName,displayName;

			public MergeField(String importName, String projectlibreName, String displayName) {
				super();
				this.importName = importName;
				this.projectlibreName = projectlibreName;
				this.displayName = displayName;
			}

			public String getImportName() {
				return importName;
			}

			public void setImportName(String importName) {
				this.importName = importName;
			}

			public String getProjectLibreName() {
				return projectlibreName;
			}

			public void setProjectLibreName(String projectlibreName) {
				this.projectlibreName = projectlibreName;
			}
			
			public String getDisplayName() {
				return displayName;
			}

			public void setDisplayName(String displayName) {
				this.displayName = displayName;
			}

			public String toString(){
				return displayName;
			}
			
		}
		
}
