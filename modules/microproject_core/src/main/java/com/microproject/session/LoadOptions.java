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

import java.io.InputStream;


import com.microproject.exchange.ResourceMappingForm;

public class LoadOptions {
	protected boolean local;
	protected long id;
	protected boolean subproject;
	protected boolean sync;
	protected String fileName;
	protected InputStream fileInputStream;
	protected String importer;
	protected boolean openAs;
	protected ResourceMappingForm resourceMapping;
	protected Consumer<Object> endSwingClosure;
	protected boolean collaborationEnabled;
	protected String collaborationUserKey;
	protected String sidecarFileName;
	protected boolean reloadFromCollaborationSync;
	public boolean isLocal() {
		return local;
	}
	public void setLocal(boolean local) {
		this.local = local;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public boolean isSubproject() {
		return subproject;
	}
	public void setSubproject(boolean subproject) {
		this.subproject = subproject;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public boolean isSync() {
		return sync;
	}
	public void setSync(boolean sync) {
		this.sync = sync;
	}
	public String getImporter() {
		return importer;
	}
	public void setImporter(String importer) {
		this.importer = importer;
	}
	public ResourceMappingForm getResourceMapping() {
		return resourceMapping;
	}
	public void setResourceMapping(ResourceMappingForm resourceMapping) {
		this.resourceMapping = resourceMapping;
	}
	public boolean isOpenAs() {
		return openAs;
	}
	public void setOpenAs(boolean openAs) {
		this.openAs = openAs;
	}
	public Consumer<Object> getEndSwingClosure() {
		return endSwingClosure;
	}
	public void setEndSwingClosure(Consumer<Object> endSwingClosure) {
		this.endSwingClosure = endSwingClosure;
	}
	public InputStream getFileInputStream() {
		return fileInputStream;
	}
	public void setFileInputStream(InputStream fileInputStream) {
		this.fileInputStream = fileInputStream;
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

}
