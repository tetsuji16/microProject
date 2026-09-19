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
package com.microproject.grouping.core.transform.sorting;

import com.microproject.field.InvalidFormulaException;
import com.microproject.grouping.core.transform.CommonTransform;
import com.microproject.grouping.core.transform.CommonTransformFactory;
import com.microproject.scripting.GroovyClassCompiler;

/**
 *
 */
public class NodeSorterFactory extends CommonTransformFactory{
//	static Log log = LogFactory.getLog(NodeSorterFactory.class);
	protected String type1 = null; 
	protected String type2 = null; 
	protected String groupNameFormula=null;
	
	public CommonTransform getTransform() throws InvalidFormulaException{
	    CommonTransform t=getTransformFromDefinition();
	    if (t!=null) return t;
	    if (formulaText==null&&subTransforms==null) return null;
		StringBuilder classText = new StringBuilder();
	    classText.append("package com.microproject.grouping.core.transform.sorting;\n");
	    classText.append("import com.microproject.grouping.core.Node;\n");
	    classText.append("import com.microproject.datatype.*;\n");
		String definition = type1 + "\n" + type2 + "\n" + formulaText + "\n" + groupNameFormula;
		String className = GroovyClassCompiler.scriptClassName("SorterFormula", definition);
	    classText.append("public class ").append(className).append(" extends NodeSorter{\n");
	    
		if (formulaText!=null){
	    	//compare
		    classText.append("\tpublic int compare(Object _nodeObject1,Object _nodeObject2){\n\t\tObject ")
		    	.append(type1).append("=(_nodeObject1 instanceof Node)?((Node)_nodeObject1).getImpl():_nodeObject1;\n\t\tObject ")
		    	.append(type2).append("=(_nodeObject2 instanceof Node)?((Node)_nodeObject2).getImpl():_nodeObject2;\n")
		    	.append("\t\t").append(formulaText).append("\n\t}\n");
		    
		    //groupName
		    if (groupNameFormula!=null){
			    classText.append("\tpublic String getGroupName(Object _nodeObject1){\n\t\tObject ")
			    .append(type1).append("=(_nodeObject1 instanceof Node)?((Node)_nodeObject1).getImpl():_nodeObject1;\n\t\t")
			    .append(groupNameFormula).append("\n\t}\n");
		    }
		}
	    classText.append("}\n");
	    
		try {
			t = GroovyClassCompiler.compileAndInstantiate(classText.toString(), NodeSorter.class);
			setProperties(t);
			return t;
		} catch (Exception e) {
			throw new InvalidFormulaException(e);
		}
	}
	
    public String getType1() {
        return type1;
    }
    public void setType1(String type1) {
        this.type1 = type1;
    }
    public String getType2() {
        return type2;
    }
    public void setType2(String type2) {
        this.type2 = type2;
    }
    
    public String getGroupNameFormula() {
        return groupNameFormula;
    }
    public void setGroupNameFormula(String groupNameFormula) {
        this.groupNameFormula = groupNameFormula;
    }
    
	protected void setProperties(CommonTransform t) throws InvalidFormulaException{
		super.setProperties(t);
		//((NodeSorter)t).setFieldsText(getFieldsText());
	}
}
