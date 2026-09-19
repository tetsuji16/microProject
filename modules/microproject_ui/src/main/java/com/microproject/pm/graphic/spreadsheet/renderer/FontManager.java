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
package com.microproject.pm.graphic.spreadsheet.renderer;

import java.awt.Component;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.UIManager;

import com.microproject.graphic.configuration.CellFormat;
import com.microproject.util.FlatUiSupport;

/**
 * Manager of fonts for cells based on conditions
 */
public class FontManager {
	public static final Font SVG_DEFAULT_FONT=FlatUiSupport.uiFont();
	public static final Font DEFAULT_FONT=FlatUiSupport.uiFont();

	public static final Map<Font, Font> boldMapping=new HashMap<>(32);
	public static final Map<Font, Font> italicMapping=new HashMap<>(32);
	public static final Map<Font, Font> boldItalicMapping=new HashMap<>(32);
	//fonts are mapped to avoid using deriveFont each time. deriveFont causes useless memory consumption 
	
	public static void setComponentFont(CellFormat props, Component component) {
		Font font=component.getFont();
//		System.out.println("font="+font);
		if (offlineDefaultFont!=null) component.setFont(offlineDefaultFont);
		if (props.isBold()||props.isItalic()){
			Map map;
			int type;
			if (props.isBold()&&!props.isItalic()){
				map=boldMapping;
				type=Font.BOLD;
			}else if (!props.isBold()&&props.isItalic()){
				map=italicMapping;
				type=Font.ITALIC;
			}else{
				map=boldItalicMapping;
				type=Font.BOLD+Font.ITALIC;
			}
			Font f=(Font)map.get(font);
			if (f==null){
				f=font.deriveFont(type);
				map.put(font,f);
			}
			component.setFont(f);
		}
		
//		if (props.isBold()){
//			System.out.println("bold");
//			component.setFont(component.getFont().deriveFont(Font.BOLD));
//		}
//		if (props.isItalic()){
//			System.out.println("italic");
//			component.setFont(component.getFont().deriveFont(Font.ITALIC));
//		}
	}
	
	//UIManager.put("Label.font",DEFAULT_FONT) can impact other parts
	protected static Font offlineDefaultFont;
	public static void setOfflineDefaultFont(Font font){
		offlineDefaultFont=font == null ? FlatUiSupport.uiFont() : font;
	}
	public static Font getOfflineDefaultFont(){
		return offlineDefaultFont;
	}

	public static Font getDefaultFont() {
		return FlatUiSupport.uiFont();
	}
	
}
