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
package com.microproject.util;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

public class FontUtil {
	private static final Logger logger = Logger.getLogger(FontUtil.class.getName());
	public static void setUIFont (String font){
		
		for (Enumeration e=UIManager.getDefaults().keys();e.hasMoreElements();) {
			Object key = e.nextElement();
			Object value = UIManager.get (key);
			if (value instanceof FontUIResource)
				UIManager.put (key, new FontUIResource(Font.decode(font)));
		}
	}    
	public static Font getUIFont(){
		Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get (key);
			if (value instanceof FontUIResource)
				return (FontUIResource)value;
		}
		return null;
	}
	
	public static String getValidFont(String[] fonts){
		String[] f=GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (int j=0;j<fonts.length;j++){
			for (int i=0;i<f.length;i++){
			if (f[i].equals(fonts[j])) return fonts[j];
			}
		}
		return null;
	}
	
	
	public static void listFonts(){
		Font current=getUIFont();
		logger.info("Current font: " + current);
		logger.info("Available fonts:");
		String[] fonts=GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (int i=0;i<fonts.length;i++){
			logger.info("\t" + fonts[i]);
		}
	}
	
	public static Font getFont(String fontName,int type){
		Font font=Font.decode(Environment.getFont(type));
		if (fontName==null) return font;
		Font newFont=font.decode(fontName);
		if (newFont.getName().equals("_Default_"))
			return font.deriveFont(newFont.getStyle(),newFont.getSize()+font.getSize()-12);
		else return newFont; 
	}

}
