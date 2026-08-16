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
package org.projectlibre.strings;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Laurent Chretienneau
 *
 */
public class Strings {
	protected static final String[] BUNDLE_NAMES={"org.projectlibre.strings.client"};
	protected static ResourceBundle[] bundles;
	
	
	public static String getString(String key) {
		if (bundles==null){
			bundles=new ResourceBundle[BUNDLE_NAMES.length];
			int i=0;
			for (String bundleName : BUNDLE_NAMES){
				bundles[i++]=ResourceBundle.getBundle(bundleName,Locale.getDefault(),Strings.class.getClassLoader());
			}
		}
		String s=null;
		for (ResourceBundle bundle : bundles){
			s=bundle.getString(key);
			if (s!=null)
				break;
		}
		return s;
	}
	

}
