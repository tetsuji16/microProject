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

import java.io.BufferedInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import com.microproject.grouping.core.model.DefaultNodeModel;
import com.microproject.job.Job;
import com.microproject.job.JobRunnable;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.pm.task.Project;
import com.microproject.server.data.DataUtil;
import com.microproject.server.data.DocumentData;
import com.microproject.session.LoadOptions;
import com.microproject.session.LocalSession;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.SafeObjectInput;

/**
 * Loads/Saves a project from/to a pod file
 */
public class LocalFileImporter extends FileImporter {
	private static final Logger logger = Logger.getLogger(LocalFileImporter.class.getName());
	public static final String VERSION="1.0.0"; //$NON-NLS-1$
	private static final String PROJECT_LIBRE_FILE_SEPARATOR="@@@@@@@@@@ProjectLibreSeparator_MSXML@@@@@@@@@@";
	private static final String OLD_FILE="com.projity.server.data.ProjectData";
	private static final String XML_FILE_START="<?xml";
	/**
	 *
	 */
	public LocalFileImporter() {
		super();
	}
	
	

	@Override
	public void importFile() throws Exception{
		File f=new File(getFileName());
		FileInputStream fin=new FileInputStream(f);
		Exception ex=null;
		
		if (/*findString(fin, OLD_FILE)*/false) {
			logger.info("Old file: ignoring binary content");
			project=null;
		}else {
	        try {
				DataUtil serializer=new DataUtil();
				logger.info("Loading " + getFileName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$

				long t1=System.currentTimeMillis();
				var in=SafeObjectInput.create(fin);
				Object obj=in.readObject();
				if (obj instanceof String) obj=in.readObject(); //check version in the future
				DocumentData projectData=(DocumentData)obj;
				projectData.setMaster(true);
				projectData.setLocal(true);
				long t2=System.currentTimeMillis();
				logger.info("Loading...Done in " + (t2 - t1) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$


				logger.info("Deserializing..."); //$NON-NLS-1$
				t1=System.currentTimeMillis();
//	        project=serializer.deserializeProject(projectData,false,true,resourceMap);
				setProject(serializer.deserializeLocalDocument(projectData));
				t2=System.currentTimeMillis();
				logger.info("Deserializing...Done in " + (t2 - t1) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (Exception e) {
				ex=e;
				project=null;
				logger.log(Level.WARNING, "Failed to load serialized POD payload", e);
			}finally{
				try {
					fin.close();
				} catch (Exception e) {
					logger.log(Level.WARNING, "Error during file import", e);
				}
			}
			
		}
        
        if (project==null){
        	//recreate project
        	
        	BufferedInputStream in=null;
			try {
				//using xml
				logger.info("Trying to recover with XML...");
				fin=new FileInputStream(f);
				byte[] keyBuf=PROJECT_LIBRE_FILE_SEPARATOR.getBytes();
				byte[] startXmlKeyBuf=XML_FILE_START.getBytes();
				int bufSize=100;
				if (bufSize<keyBuf.length) bufSize=keyBuf.length;
				byte[] buf= new byte[bufSize];
				in=new BufferedInputStream(fin); //use default 8192 bytes size
				
				int keyPos=0;
				int n;
//				int pos=0;
				boolean found=false;
				boolean xmlStartFound=false;
				boolean first=true;
				in.mark(bufSize);
				while ( (n=in.read( buf, 0, bufSize )) != -1 ){
					// testing if it's xml without PROJECT_LIBRE_FILE_SEPARATOR
					if (first && n>startXmlKeyBuf.length) {
						 for (int i=0; i<startXmlKeyBuf.length; i++ ){
							 if (startXmlKeyBuf[i]!=buf[i]) {
								 first=false;
								 break;								 
							 }
						 }
						 if (first) {
							 xmlStartFound=true;
							 break;
						 }
					}
						
				    for (int i=0; i<n; i++ ){
				    	if (keyBuf[keyPos]==buf[i]){
				    		if (keyPos==keyBuf.length-1){
				    			//found keyword
				    			found=true;
				    			in.reset();
				    			in.read(buf,0,i+1);
				    			break;
				    		}else{
				    			keyPos++;
				    		}
				    	}else keyPos=0;
				    }
				    if (found) break;
					in.mark(bufSize);
//				    pos+=n;
				}
				
				if (xmlStartFound) {
					if (in!=null){
						try {
							in.close();
						} catch (Exception e1) {
							logger.log(Level.WARNING, "Error during file import", e1);
						}
					}
					fin=new FileInputStream(f);
					in=new BufferedInputStream(fin);
					
				}
				if (found || xmlStartFound) {
					//xml found
					logger.info("XML found");
					final LoadOptions opt=new LoadOptions();
					opt.setFileName(fileName);
					opt.setLocal(true);
					opt.setSync(false);
					opt.setImporter(LocalSession.MICROSOFT_PROJECT_IMPORTER);
					opt.setFileInputStream(in);
					
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							projectFactory.openProject(opt);
							
						}
					});
//					project=projectFactory.openProject(opt);

					
//					FileImporter importer=LocalSession.getImporter("com.microproject.exchange.MicrosoftImporter");
//					
//					ResourcePool resourcePool=null;
//					DataFactoryUndoController undoController=new DataFactoryUndoController();
//					resourcePool = ResourcePoolFactory.getInstance().createResourcePool("",undoController);
//					resourcePool.setLocal(true);
//					project = Project.createProject(resourcePool,undoController);						
//					((DefaultNodeModel)project.getTaskOutline()).setDataFactory(project);		
//					importer.setProject(project);
//					
//					importer.loadProject(in);
					logger.info("Recovered with XML");
				}else{
					//unable to recover from xml 
		    		if ( ex!=null &&
		    				ex instanceof ClassNotFoundException &&
		    				ex.getMessage().equals("com.projity.server.data.ProjectData")) {
		    			SwingUtilities.invokeLater(new Runnable(){
		    				public void run(){
				    			Alert.error(Messages.getString("Message.ImportOldFormatError"));
		    				}
		    			});
		    		}else {
		    			SwingUtilities.invokeLater(new Runnable(){
		    				public void run(){
				    			Alert.error(Messages.getString("Message.ImportError"));
		    				}
		    			});
		    			
		    		}
					
					
					if (ex!=null) throw ex;
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "Error during file import", e);
				if (in!=null){
					try {
						in.close();
					} catch (Exception e1) {
						logger.log(Level.WARNING, "Error during file import", e1);
					}
				}
			}
        }
	}

	
	private static boolean findString(InputStream fin, String stringToSearch) {
		BufferedInputStream in=null;
		try {
			byte[] keyBuf=stringToSearch.getBytes();
			int bufSize=100;
			if (bufSize<keyBuf.length) bufSize=keyBuf.length;
			byte[] buf= new byte[bufSize];
			in=new BufferedInputStream(fin); //use default 8192 bytes size
			
			int keyPos=0;
			int n;
			in.mark(bufSize);
			while ( (n=in.read( buf, 0, bufSize )) != -1 ){
			    for (int i=0; i<n; i++ ){
			    	if (keyBuf[keyPos]==buf[i]){
			    		if (keyPos==keyBuf.length-1){
			    			//found keyword
			    			return true;
			    		}else{
			    			keyPos++;
			    		}
			    	}else keyPos=0;
			    }
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error during file import", e);
		} finally {
			if (in!=null){
				try {
					in.close();
				} catch (Exception e1) {
					logger.log(Level.WARNING, "Error during file import", e1);
				}
			}
		}
		return false;
    }



	@Override
	public void exportFile() throws Exception{
		String extension="";
		String name=fileName;
		String tmpFileName=fileName;
		int i=fileName.lastIndexOf('.');
		if (i>0){
			extension=fileName.substring(i);
			name=fileName.substring(0, i);
		}
		
		File file=new File(fileName);
		File tmpFile=file;
		for (int count=0;tmpFile.exists();count++){
			tmpFileName=name+"_tmp"+count+extension;
			tmpFile=new File(tmpFileName);
		}
		
		

		boolean error=false;
		
		try {
			FileOutputStream fout=new FileOutputStream(tmpFile);
			try {
				DataUtil serializer=new DataUtil();
				logger.info("Serialization..."); //$NON-NLS-1$
				long t1=System.currentTimeMillis();
				DocumentData projectData=serializer.serializeDocument(getProject());
				projectData.setMaster(true);
				projectData.setLocal(true);
				long t2=System.currentTimeMillis();
				logger.info("Serialization...Done in " + (t2 - t1) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
				logger.info("Saving " + file + "..."); //$NON-NLS-1$ //$NON-NLS-2$
				t1=System.currentTimeMillis();
				ObjectOutputStream out=new ObjectOutputStream(fout);
				out.writeObject(VERSION);
				out.writeObject(projectData);
				out.flush();
				//out.close();
				t2=System.currentTimeMillis();
				logger.info("Saving...Done in " + (t2 - t1) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (Exception e) {
				error=true;
				logger.log(Level.WARNING, "Error during file import", e);
			}
			try{
				BufferedOutputStream bout=new BufferedOutputStream(fout);
				bout.write(PROJECT_LIBRE_FILE_SEPARATOR.getBytes());
				bout.flush();
				FileImporter importer=LocalSession.getImporter("com.microproject.exchange.MicrosoftImporter");
				String previousFileName = importer.getFileName();
				try {
					// POD stores a serialized ProjectLibre payload followed by embedded MSPDI XML.
					importer.setFileName(name + ".xml");
					importer.saveProject(project, bout);
				} finally {
					importer.setFileName(previousFileName);
				}
				bout.flush();
				
			}catch (Exception e) {
				error=true;
				logger.log(Level.WARNING, "Error during file import", e);
			}
			fout.close();
		} catch (Exception e) {
			error=true;
			logger.log(Level.WARNING, "Error during file import", e);
		}

		//Don't replace original file if an error occurred
		if (error){
			if (file.equals(tmpFile))
				Alert.error(Messages.getString("Message.saveError"));
			else Alert.error(Messages.getString("Message.saveErrorTmpFile")+tmpFileName);
		}else if (!file.equals(tmpFile)){
			file.delete();
			tmpFile.renameTo(file);
		}

	}



	public Job getImportFileJob(){
		return getImportFileJob(this);
	}

    public static Job getImportFileJob(final FileImporter importer){
    	final Job job=new Job(importer.getJobQueue(),"importFile",Messages.getString("LocalFileImporter.Importing"),true); //$NON-NLS-1$ //$NON-NLS-2$
        job.addRunnable(new JobRunnable("Import",1.0f){ //$NON-NLS-1$
    		public Object run() throws Exception{
    			importer.importFile();
    			setProgress(1.0f);
                return null;
    		}
        });
        return job;
    }

    public Job getExportFileJob(){
    	return getExportFileJob(this);
    }
    public static Job getExportFileJob(final FileImporter importer){
    	final Job job=new Job(importer.getJobQueue(),"exportFile",Messages.getString("LocalFileImporter.Exporting"),true); //$NON-NLS-1$ //$NON-NLS-2$
        job.addRunnable(new JobRunnable("Export",1.0f){ //$NON-NLS-1$
    		public Object run() throws Exception{
    			importer.exportFile();
     			setProgress(1.0f);
                return null;
    		}
        });
        return job;
    }
    
    //disabled
    @Override
	public boolean saveProject(Project project,OutputStream out) throws Exception{
		return false;
	}
    
    @Override
	public Project loadProject(InputStream in)  throws Exception{
    	//disabled
    	return null;
	}

}
