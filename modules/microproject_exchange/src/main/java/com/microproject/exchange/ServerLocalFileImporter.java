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

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.server.data.AssignmentData;
import com.microproject.server.data.DistributionData;
import com.microproject.server.data.EnterpriseResourceData;
import com.microproject.server.data.ProjectData;
import com.microproject.server.data.ResourceData;
import com.microproject.server.data.Serializer;
import com.microproject.server.data.TaskData;
import com.microproject.job.Job;
import com.microproject.job.JobCanceledException;
import com.microproject.job.JobRunnable;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.task.Project;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;
import com.microproject.util.SafeObjectInput;
import com.microproject.server.data.DataObject;

/**
 * Loads/Saves a project from/to a pod file
 */
public class ServerLocalFileImporter extends ServerFileImporter {
	private static final String ABORT = "Job aborted"; //$NON-NLS-1$
	private static final Logger logger = Logger.getLogger(ServerLocalFileImporter.class.getName());
	public ServerLocalFileImporter() {
		super();
	}

	protected ProjectData projectData;

	
	
	@Override
	public void importFile() throws Exception {
		throw new UnsupportedOperationException("Use getImportFileJob() for local file imports");
	}



	@Override
	public void exportFile() throws Exception {
		throw new UnsupportedOperationException("Local file export is not implemented");
	}



	public Job getImportFileJob(){
    	final Serializer serializer=new Serializer();
    	final FileImporter importer=this;
    	final Job job=new Job(importer.getJobQueue(),"importFile",Messages.getString("LocalFileImporter.Importing"),true); //$NON-NLS-1$ //$NON-NLS-2$
        job.addRunnable(new JobRunnable("Import",1.0f){ //$NON-NLS-1$
    		public Object run() throws Exception{
    	        logger.info("Loading " + importer.getFileName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$

     	        long t1=System.currentTimeMillis();
				try (var in = SafeObjectInput.create(new FileInputStream(importer.getFileName()))) {
     	        Object obj=in.readObject();
     	        if (obj instanceof String) obj=in.readObject();
    	        projectData=(ProjectData)obj;
    	        projectData.setMaster(false);
    	        projectData.setLocal(false);
    	        long t2=System.currentTimeMillis();
    	        logger.log(Level.INFO, "Loading...Done in {0} ms", t2 - t1); //$NON-NLS-1$

    	        Collection<? extends DataObject> rs=projectData.getResources();
    	        List<EnterpriseResourceData> ers=new ArrayList<EnterpriseResourceData>(rs.size());
    	        for (DataObject value: rs){
    	        	ers.add(((ResourceData)value).getEnterpriseResource());
    	        }

			((ServerFileImporter)importer).prepareResources(ers,null,false);
				renumberProject();

     	        setProgress(1.0f);
                return null;
     	        }
     		}
         });

    	job.addSwingRunnable(new JobRunnable("Import resources",1.0f){ //$NON-NLS-1$
			public Object run() throws Exception{
				ResourceMappingForm form=getResourceMapping();
				if (form!=null&&form.isLocal())
					if (!job.okCancel(Messages.getString("Message.ServerUnreacheableReadOnlyProject"),true)){ //$NON-NLS-1$
						setProgress(1.0f);
						throw new Exception(ABORT);
					}

				if(!importResources()){
					setProgress(1.0f);
					throw new JobCanceledException(ABORT);
				}
				setProgress(1f);
				return null;
	    	}
    	});

		job.addRunnable(new JobRunnable("Import",1.0f){ //$NON-NLS-1$
    		public Object run() throws Exception{
    	        logger.info("Deserializing..."); //$NON-NLS-1$
    	        long t1=System.currentTimeMillis();
    	        ResourceMappingForm form=getResourceMapping();
    	        Project project=serializer.deserializeProject(projectData,false,null,null,null,false);
    	        if (project!=null&&!Environment.getStandAlone()) project.setAllDirty();
    	        importer.setProject(project);
    	        long t2=System.currentTimeMillis();
    	        logger.log(Level.INFO, "Deserializing...Done in {0} ms", t2 - t1); //$NON-NLS-1$

    	        project.setGroupDirty(!Environment.getStandAlone());
			project.setTemporaryLocal(true);

    			project.setAccessControlPolicy(form.getAccessControlType());
    			project.resetRoles(form.getAccessControlType()==0);
    			if (form.isLocal()) project.setLocal(true);

    	        setProgress(1.0f);
                return null;
    		}
        });



        return job;
    }

    Map<Long, com.microproject.pm.resource.Resource> _localResourceMap;

	@SuppressWarnings("unchecked")
	protected boolean importResources() throws Exception{
		ResourceMappingForm form=getResourceMapping();

		if (form!=null&&!form.isLocal()){
			if (!form.execute()) return false;
			if (form.isLocal()){
				return true;
			}

			List<ResourceData> resources=new ArrayList<ResourceData>(form.getResources().size());
			Map<Long,ResourceData> resourceMap=new HashMap<Long, ResourceData>(form.getResources().size());

			for (EnterpriseResourceData enterpriseResource:(List<EnterpriseResourceData>)(List<?>)form.getResources()){
				if (enterpriseResource.getUniqueId()!=EnterpriseResource.UNASSIGNED_ID){
					ResourceData resource=new ResourceData();
					resource.setEnterpriseResource(enterpriseResource);
					resourceMap.put(enterpriseResource.getUniqueId(), resource);
					resources.add(resource);
				}
			}
			projectData.setResources(resources);


			Map<Long,ResourceData> idMap=new HashMap<Long, ResourceData>(form.getImportedResources().size());
			for (int i = 0; i < form.getImportedResources().size(); i++) {
				EnterpriseResourceData enterpriseSrc = (EnterpriseResourceData) form.getImportedResources().get(i);
				EnterpriseResourceData enterpriseDest = (EnterpriseResourceData) form.getSelectedResources().get(i);
				if (enterpriseDest.getUniqueId()!=EnterpriseResource.UNASSIGNED_ID){
					ResourceData resource=new ResourceData();
					resource.setEnterpriseResource(enterpriseDest);
					idMap.put(enterpriseSrc.getUniqueId(),resourceMap.get(enterpriseDest.getUniqueId()));
				}
			}


			Collection<? extends DataObject> tasks=projectData.getTasks();
			for (DataObject value: tasks){
				TaskData task=(TaskData)value;
				if (task.getAssignments()!=null)
				for (AssignmentData assignment:(List<AssignmentData>)(List<?>)task.getAssignments()){
					ResourceData resourceData=idMap.get(assignment.getUniqueId());
					if (resourceData==null){
						assignment.setResource(null);
						assignment.setResourceId(-1L);
					}else{
						assignment.setResource(resourceData.getEnterpriseResource());
						assignment.setResourceId(resourceData.getEnterpriseResource().getUniqueId());
					}
				}
			}
			Collection<DistributionData> dists=projectData.getDistributions();
			if (dists!=null)
			for (java.util.Iterator<DistributionData> i=dists.iterator(); i.hasNext();){
				DistributionData dist=i.next();
				ResourceData resourceData=idMap.get(dist.getResourceId());
				if (resourceData==null){
					i.remove();
				}else{
					dist.setResourceId(resourceData.getEnterpriseResource().getUniqueId());
				}
			}
		}

		return true;
	}



	protected void retrieveResourcesForMerge(Project existingProject) throws Exception{
		ResourceMappingForm form=getResourceMapping();
		if (form==null) return;


		ArrayList<Object> projectlibre1Resources = new ArrayList<>(existingProject.getResourcePool().getResourceList().size() + 1);
        _localResourceMap = new HashMap<Long, com.microproject.pm.resource.Resource>(existingProject.getResourcePool().getResourceList().size());

		EnterpriseResourceData unassigned=new EnterpriseResourceData();
		unassigned.setUniqueId(EnterpriseResource.UNASSIGNED_ID);
		unassigned.setName(Messages.getString("Text.Unassigned")); //$NON-NLS-1$
		form.setUnassignedResource(unassigned);
		projectlibre1Resources.add(unassigned);

		Serializer serializer=new Serializer();

		ProjectData projectData=(ProjectData)serializer.serialize(existingProject,ProjectData.FACTORY,null);
        if (existingProject.isForceNonIncremental()) projectData.setVersion(0);
        projectData.setMaster(existingProject.isMaster());

		Map<Long, ResourceData> resourceMap=serializer.saveResources(existingProject,projectData);
		List<com.microproject.pm.resource.Resource> existingResources=existingProject.getResourcePool().getResourceList();
        for (com.microproject.pm.resource.Resource resource:existingResources){
        	if (resource==null) continue;
        	ResourceData r=(ResourceData)resourceMap.get(resource.getUniqueId());
        	if (r!=null){
        		EnterpriseResourceData er=r.getEnterpriseResource();
        		er.setName(resource.getName());
        		projectlibre1Resources.add(er);
        		_localResourceMap.put((Long)r.getUniqueId(), resource);
        	}
        }

		form.setResources(projectlibre1Resources);
	}


	@SuppressWarnings("unchecked")
	protected void renumberProject() throws Exception{
		Session session=SessionFactory.getInstance().getSession(false);
		Map<Long,Long> idMap=new HashMap<Long, Long>(projectData.getTasks().size());
		Collection<? extends DataObject> tasks=projectData.getTasks();
		for (DataObject value: tasks){
			TaskData task=(TaskData)value;
			long oldUniqueId=task.getUniqueId();
			long uniqueId=session.getId();
			task.setUniqueId(uniqueId);
			idMap.put(oldUniqueId, uniqueId);
			if (task.getAssignments()!=null)
			for (AssignmentData assignment:(List<AssignmentData>)(List<?>)task.getAssignments()){
				assignment.setTaskId(uniqueId);
			}
		}

		Collection<DistributionData> dists=projectData.getDistributions();
		if (dists!=null)
		for (java.util.Iterator<DistributionData> i=dists.iterator(); i.hasNext();){
			DistributionData dist=i.next();
			dist.setTaskId(idMap.get(dist.getTaskId()));
		}

		projectData.setUniqueId(session.getId());
	}



    public Job getExportFileJob(){
    	return LocalFileImporter.getExportFileJob(this);
    }
    
    @Override
	public boolean saveProject(Project project,OutputStream out) throws Exception{
		return false;
	}
    @Override
	public Project loadProject(InputStream in)  throws Exception{
    	return null;
	}


}
