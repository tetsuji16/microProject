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
package com.microproject.grouping.core.transform;

import org.apache.commons.digester.Digester;

import com.microproject.configuration.Dictionary;
import com.microproject.configuration.NamedItem;
import com.microproject.strings.Messages;

/**
 * 
 */
public class ViewConfiguration implements NamedItem {
//	static Log log = LogFactory.getLog(ViewConfiguration.class);
	public static final String category="ViewConfigurationCategory";
	public String getCategory() {
		return category;
	}
	
	String name = null;
	String id = null;
	ViewTransformer transform;

	public ViewConfiguration() {}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setId(String id) {
		this.id = id;
		setName(Messages.getString(id));
	}	
	public String getId() {
		return id;
	}
	public ViewTransformer getTransform() {
        return transform;
    }
    public void setTransform(ViewTransformer transform) {
        this.transform = transform;
    }
    
    public static void addDigesterEvents(Digester digester){
		// main properties of bar
		digester.addObjectCreate("*/views/view", "com.microproject.grouping.core.transform.ViewConfiguration");
	    digester.addSetProperties("*/views/view");
		digester.addSetNext("*/views/view", "add", "com.microproject.configuration.NamedItem");

		// start section
		digester.addObjectCreate("*/views/view/transform", "com.microproject.grouping.core.transform.ViewTransformer");
	    digester.addSetProperties("*/views/view/transform");
	    digester.addSetNext("*/views/view/transform", "setTransform", "com.microproject.grouping.core.transform.ViewTransformer");
	    
	    
		digester.addObjectCreate("*/views/view/transform/filter", "com.microproject.grouping.core.transform.TransformId");
	    digester.addSetProperties("*/views/view/transform/filter");
	    digester.addSetNext("*/views/view/transform/filter", "setFilterId", "com.microproject.grouping.core.transform.TransformId");
		
	    
		digester.addObjectCreate("*/views/view/transform/sorter", "com.microproject.grouping.core.transform.TransformId");
	    digester.addSetProperties("*/views/view/transform/sorter");
	    digester.addSetNext("*/views/view/transform/sorter", "setSorterId", "com.microproject.grouping.core.transform.TransformId");
	    
	    
	    
		digester.addObjectCreate("*/views/view/transform/grouper", "com.microproject.grouping.core.transform.TransformId");
	    digester.addSetProperties("*/views/view/transform/grouper");
	    digester.addSetNext("*/views/view/transform/grouper", "setGrouperId", "com.microproject.grouping.core.transform.TransformId");

	    
		digester.addObjectCreate("*/views/view/transform/transformer", "com.microproject.grouping.core.transform.TransformId");
	    digester.addSetProperties("*/views/view/transform/transformer");
	    digester.addSetNext("*/views/view/transform/transformer", "setTransformerId", "com.microproject.grouping.core.transform.TransformId");

	    
	}
	
	
	public static ViewConfiguration getView(String viewName){ //Dictionary wants names and not ids
	    return (ViewConfiguration)Dictionary.get(category,viewName);
	}
	
	
	
}
