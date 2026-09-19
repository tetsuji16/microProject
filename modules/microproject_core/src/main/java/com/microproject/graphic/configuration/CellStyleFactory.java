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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.field.InvalidFormulaException;
import com.microproject.scripting.GroovyClassCompiler;
import com.microproject.strings.Messages;

/**
 *
 */
public class CellStyleFactory {
	private static final Logger logger = Logger.getLogger(CellStyleFactory.class.getName());
	protected String formulaText;
	protected String formulaClass;
 	protected String id = null;
 	protected String name = null;
	protected String type = null; 
	protected String format = null; 
	
//	static Log log = LogFactory.getLog(CellStyleFactory.class);
	public CellStyle getCellStyle() throws InvalidFormulaException{
		if (formulaClass!=null){
			try {
				return Class.forName(formulaClass).asSubclass(CellStyle.class)
					.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException | ClassCastException e) {
				logger.log(Level.WARNING, "Cell style class not found " + formulaClass, e);
			}
			return null;
		}else if (formulaText!=null){
		    StringBuilder classText = new StringBuilder();
		    classText.append("package com.microproject.graphic.configuration;\n");
		    classText.append("import com.microproject.pm.graphic.model.cache.GraphicNode;\n");
			String definition = type + "\n" + format + "\n" + formulaText;
			String className = GroovyClassCompiler.scriptClassName("CellStyle", definition);
		    classText.append("public class ").append(className).append(" implements CellStyle{\n");
		    classText.append("\tpublic CellFormat getCellFormat(Object _nodeObject){\n\t\tGraphicNode ")
		    	.append(type).append("=(GraphicNode)_nodeObject;\n\t\tCellFormat ")
		    	.append(format).append("=new CellFormat();\n")
		    	.append(formulaText).
		    	append("\n\t\treturn ").append(format).append(";\n\t}\n");
		    classText.append("}\n");
			try {
				return GroovyClassCompiler.compileAndInstantiate(classText.toString(), CellStyle.class);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Failed to compile scripted cell style " + id, e);
				throw new InvalidFormulaException(e);
			}
		}else return null;
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
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }

	public String getFormulaClass() {
		return formulaClass;
	}

	public void setFormulaClass(String formulaClass) {
		this.formulaClass = formulaClass;
	}
}
