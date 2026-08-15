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

package com.projectlibre1.exchange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.Predicate;

import com.projectlibre1.exchange.ResourceMappingForm.MergeField;
import com.projectlibre1.server.data.EnterpriseResourceData;
import com.projectlibre1.pm.resource.EnterpriseResource;
import com.projectlibre1.session.Session;
import com.projectlibre1.session.SessionFactory;
import com.projectlibre1.strings.Messages;

public abstract class ServerFileImporter extends FileImporter{
	private static final Logger logger = Logger.getLogger(ServerFileImporter.class.getName());
	
	
	


	protected void prepareResources(List<?> srcResources,Predicate resourceFilter,boolean resourceDescriptorsOnly) throws Exception{
		
		ResourceMappingForm form=getResourceMapping();
		if (form==null) return;

		
		//server resources
		ArrayList<Object> projectlibreResources = new ArrayList<>();
		EnterpriseResourceData unassigned=new EnterpriseResourceData();
		unassigned.setUniqueId(EnterpriseResource.UNASSIGNED_ID);
		unassigned.setName(Messages.getString("Text.Unassigned")); //$NON-NLS-1$
		form.setUnassignedResource(unassigned);
		projectlibreResources.add(unassigned);
		try{
			Session session=SessionFactory.getInstance().getSession(false);
			projectlibreResources.addAll((Collection)SessionFactory.call(session,resourceDescriptorsOnly?"retrieveResourceDescriptors":"retrieveResourceHierarchy",null,null));
			if (projectlibreResources!=null&&projectlibreResources.size()>0) form.setUnassignedResource(projectlibreResources.get(0));
		}catch (Exception e){
			logger.log(Level.WARNING, "Falling back to local resource mapping because server resources could not be loaded", e);
			form.setLocal(true);
			return;
		}
		form.setResources(projectlibreResources);
		
		//imported resources
		List<Object> resourcesToMap=new ArrayList<>();
		if (srcResources!=null)
			for (Object resource:srcResources){
				if (resourceFilter==null||resourceFilter.evaluate(resource))
					resourcesToMap.add(resource);
			}
		form.setImportedResources(resourcesToMap);
		
		MergeField mergeField=new ResourceMappingForm.MergeField("name","name","name"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		form.addMergeField(mergeField);
//		if (!form.isJunit()) //claur
//			form.setMergeField(mergeField);
		mergeField=new ResourceMappingForm.MergeField("emailAddress","emailAddress","email"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		form.addMergeField(mergeField);
		mergeField=new ResourceMappingForm.MergeField("uniqueId","externalId","id"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		form.addMergeField(mergeField);
	}

	
}
