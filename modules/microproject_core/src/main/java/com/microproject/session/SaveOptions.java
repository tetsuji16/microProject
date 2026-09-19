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
package com.microproject.session;

import java.util.function.Consumer;


import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.pm.task.Project;

public class SaveOptions implements Cloneable{
	protected boolean local;
	protected String fileName;
	protected String fileType;
	protected String importer;
	protected boolean saveAs;
	protected Consumer<Object> postSaving;
	protected Consumer<Object> preSaving;
	protected boolean sync;
	protected boolean collaborationEnabled;
	protected String collaborationUserKey;
	protected String sidecarFileName;
	protected boolean reloadFromCollaborationSync;
	/**
	 * Writes a recovery copy without changing the document identity or persisted
	 * dirty state. Recovery snapshots deliberately use the normal exporter so
	 * they exercise the same complete project serialization as an explicit save.
	 */
	protected boolean recoverySnapshot;
	
	public SaveOptions() {
	}
	public boolean isLocal() {
		return local;
	}
	public void setLocal(boolean local) {
		this.local = local;
	}
	public boolean isSaveAs() {
		return saveAs;
	}
	public void setSaveAs(boolean saveAs) {
		this.saveAs = saveAs;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public Consumer<Object> getPostSaving() {
		return postSaving;
	}
	public void setPostSaving(Consumer<Object> postSaving) {
		this.postSaving = postSaving;
	}
	public String getImporter() {
		return importer;
	}
	public void setImporter(String importer) {
		this.importer = importer;
	}
	public boolean isSync() {
		return sync;
	}
	public void setSync(boolean sync) {
		this.sync = sync;
	}
	@Override
	public Object clone(){
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public Consumer<Object> getPreSaving() {
		return preSaving;
	}
	public void setPreSaving(Consumer<Object> preSaving) {
		this.preSaving = preSaving;
	}
	public boolean isCollaborationEnabled() {
		return collaborationEnabled;
	}
	public void setCollaborationEnabled(boolean collaborationEnabled) {
		this.collaborationEnabled = collaborationEnabled;
	}
	public String getCollaborationUserKey() {
		return collaborationUserKey;
	}
	public void setCollaborationUserKey(String collaborationUserKey) {
		this.collaborationUserKey = collaborationUserKey;
	}
	public String getSidecarFileName() {
		return sidecarFileName;
	}
	public void setSidecarFileName(String sidecarFileName) {
		this.sidecarFileName = sidecarFileName;
	}
	public boolean isReloadFromCollaborationSync() {
		return reloadFromCollaborationSync;
	}
	public void setReloadFromCollaborationSync(boolean reloadFromCollaborationSync) {
		this.reloadFromCollaborationSync = reloadFromCollaborationSync;
	}
	public boolean isRecoverySnapshot() {
		return recoverySnapshot;
	}
	public void setRecoverySnapshot(boolean recoverySnapshot) {
		this.recoverySnapshot = recoverySnapshot;
	}
	
	
}
