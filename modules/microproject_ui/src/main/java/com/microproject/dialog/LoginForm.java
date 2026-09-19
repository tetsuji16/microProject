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
package com.microproject.dialog;

import java.io.Serializable;

public class LoginForm implements Serializable{
	static final long serialVersionUID = 893920204932L;
		String login;
		String password;
		boolean storeCredentials;
		boolean useMenus = false;
		transient boolean cancelled = false;
		public final String getLogin() {
			return login;
		}
		public final void setLogin(String login) {
			this.login = login;
		}
		public final String getPassword() {
			return password;
		}
		public final void setPassword(String password) {
			this.password = password;
		}
		public boolean isStoreCredentials() {
			return storeCredentials;
		}
		public void setStoreCredentials(boolean storeCredentials) {
			this.storeCredentials = storeCredentials;
		}
		public final boolean isCancelled() {
			return cancelled;
		}
		public final void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}
		public boolean isUseMenus() {
			return useMenus;
		}
		public void setUseMenus(boolean useMenus) {
			this.useMenus = useMenus;
		}
		
	}

