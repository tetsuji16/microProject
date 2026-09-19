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
package com.microproject.graphic.configuration;

import com.microproject.field.InvalidFormulaException;
import com.microproject.scripting.GroovyClassCompiler;
import com.microproject.strings.Messages;

/**
 *
 */
public class ActionListFactory {
	protected String formulaText;
 	protected String id = null;
 	protected String name = null;
	protected String type = null; 
	
//	static Log log = LogFactory.getLog(ActionListFactory.class);
	public ActionList getActionList() throws InvalidFormulaException{
	    StringBuilder classText = new StringBuilder();
	    classText.append("package com.microproject.graphic.configuration;\n");
	    classText.append("import com.microproject.grouping.core.model.NodeModel;\n");
	    classText.append("import com.microproject.util.Environment;\n");
		String className = GroovyClassCompiler.scriptClassName("ActionList", type + "\n" + formulaText);
	    classText.append("public class ").append(className).append(" implements ActionList{\n");
	    classText.append("\tpublic String getList(Object _nodeModel){\n\t\tNodeModel ")
		    	.append(type).append("=(NodeModel)_nodeModel;\n\t\t")
	    	.append(formulaText)
	    	.append("\n\t}\n");
	    classText.append("}\n");
		try {
			return GroovyClassCompiler.compileAndInstantiate(classText.toString(), ActionList.class);
		} catch (Exception e) {
			throw new InvalidFormulaException(e);
		}
	}
	
    public String getFormulaText() {
        return formulaText;
    }
    public void setFormulaText(String formulaText) {
        this.formulaText = formulaText;
    }
	
     public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
		setName(Messages.getString(id));
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}
