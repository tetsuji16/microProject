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

import java.awt.Color;
import java.awt.Font;

import com.microproject.graphic.configuration.shape.Colors;

/**
 *
 */
public class CellFormat {
	protected Font fontObject;
	protected Color backgroundObject;
	protected Color foregroundObject;
	
	protected String font;
	protected boolean bold;
	protected boolean italic;
	protected String background;
	protected String foreground;
	protected boolean compositeIcon;
	
	public CellFormat() {
	}

	public String getBackground() {
		return background;
	}
	public void setBackground(String background) {
		this.background = background;
		backgroundObject=(background==null)?null:Colors.findColor(background);
	}
	public boolean isCompositeIcon() {
		return compositeIcon;
	}
	public void setCompositeIcon(boolean compositeIcon) {
		this.compositeIcon = compositeIcon;
	}
	public String getFont() {
		return font;
	}
	public void setFont(String font) {
		this.font = font;
		fontObject=(font==null)?null:Font.decode(font);
	}
	public String getForeground() {
		return foreground;
	}
	public void setForeground(String foreground) {
		this.foreground = foreground;
		foregroundObject=(foreground==null)?null:Colors.findColor(foreground);
	}
	
	
	public Color getBackgroundObject() {
		return backgroundObject;
	}
	public Font getFontObject() {
		return fontObject;
	}
	public Color getForegroundObject() {
		return foregroundObject;
	}
	
	
	public boolean isBold() {
		return bold;
	}
	public void setBold(boolean bold) {
		this.bold = bold;
	}
	public boolean isItalic() {
		return italic;
	}
	public void setItalic(boolean italic) {
		this.italic = italic;
	}
	
	public void reset(){
		fontObject=null;
		backgroundObject=null;
		foregroundObject=null;
		
		font=null;
		bold=false;
		italic=false;
		background=null;
		foreground=null;
		compositeIcon=false;
	}
}
