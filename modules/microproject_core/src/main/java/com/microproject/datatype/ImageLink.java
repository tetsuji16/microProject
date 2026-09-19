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
package com.microproject.datatype;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * 
 */
public class ImageLink extends Hyperlink {
	private static final long serialVersionUID = -6628595994406734747L;
	String httpImage;
	String localImage;
	String id;
	public ImageLink(String label, String address,String httpImage, String localImage, String id, boolean link) {
		super(label, address);
		this.httpImage = httpImage;
		this.localImage = localImage;
		this.id = id;
		this.link = link;
	}
	
	public static ImageLink trafficLight(String label,double value, double green, double yellow) {
		String url; //http://www.projectlibre.com/web/img/
		String icon;
		if (value == 0.0D) {
			url = "/img/GrayCircle.gif";
			icon = "grayCircle";
			
		} else if (value >= green) {
			url = "/img/GreenCircle.gif";
			icon = "greenCircle";
		} else if (value >= yellow) {
			url = "/img/YellowCircle.gif";
			icon = "yellowCircle";
		} else {
			url = "/img/RedCircle.gif";
			icon = "redCircle";
		}
		return new ImageLink(label,icon,url,icon,null,false);
	}
	
	public final String getHttpImage() {
		return httpImage;
	}
	public final void setHttpImage(String httpImage) {
		this.httpImage = httpImage;
	}
	public final String getLocalImage() {
		return localImage;
	}
	public final void setLocalImage(String localImage) {
		this.localImage = localImage;
	}
	public final String getId() {
		return id;
	}
	public final void setId(String id) {
		this.id = id;
	}
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}


}
