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
package org.projectlibre.core.configuration;

/**
 * @author Laurent Chretienneau
 *
 */
public class ConfigurationFile {
	protected String file;
	protected Class<?>[] classesToBeBound;
	protected Object root;
	protected boolean binded;
	public ConfigurationFile(String file, Class<?>[] classesToBeBound) {
		super();
		this.file = file;
		this.classesToBeBound = classesToBeBound;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public Class<?>[] getClassesToBeBound() {
		return classesToBeBound;
	}
	public void setClassesToBeBound(Class<?>[] classesToBeBound) {
		this.classesToBeBound = classesToBeBound;
	}
	public Object getRoot() {
		return root;
	}
	public void setRoot(Object root) {
		this.root = root;
	}
	public boolean isBinded() {
		return binded;
	}
	public void setBinded(boolean binded) {
		this.binded = binded;
	}
	
}
