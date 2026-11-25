/*******************************************************************************
 * MIT License
 *
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
package com.microproject.ui.layout;
/**
 * Every column and row specification written in the source code of the UI module and in
 * {@code configuration/view.xml} (layout grids), collected at master {@code 80ce3279c}.
 * Specifications built at run time from variables are represented by their literal parts.
 */
final class RepositoryFormSpecs {
	private RepositoryFormSpecs() {
	}

	static final String[] COLUMNS = {
		"100dlu,3dlu,100dlu",
		"140dlu:grow",
		"19dlu,3dlu,p:grow",
		"1dlu, 5dlu,p,22dlu,3dlu,p,1dlu",
		"1dlu, p, 1dlu",
		"1dlu,p:grow,1dlu",
		"200dlu:grow",
		"20dlu,3dlu,p, 3dlu,75dlu,3dlu,30dlu ",
		"20px",
		"21dlu,3dlu,p:grow",
		"250dlu:grow",
		"250px,300px,250px",
		"25dlu,3dlu,p:grow",
		"27dlu,3dlu,p:grow",
		"300dlu:grow",
		"310dlu:grow",
		"31dlu,3dlu,p:grow",
		"33dlu,3dlu,p:grow",
		"350dlu:grow",
		"37dlu,3dlu,p:grow",
		"39dlu,3dlu,p:grow",
		"400dlu:grow",
		"430dlu:grow",
		"50dlu,3dlu,50dlu,3dlu,50dlu,3dlu,50dlu",
		"50dlu,3dlu,50dlu,3dlu,50dlu,3dlu,50dlu,3dlu,50dlu",
		"50dlu,3dlu,50dlu,3dlu,p:grow",
		"700px",
		"8dlu, p, 3dlu,30dlu,3dlu,p,3dlu,p,p,p,8dlu",
		"default",
		"default, 3dlu, 120dlu:grow",
		"default, 3dlu, 220dlu, 3dlu, default:grow",
		"default, 3dlu, 220dlu:grow",
		"default, 3dlu, default",
		"default, 3dlu, default, 3dlu, default",
		"default, 3dlu, default, 3dlu, default, 3dlu, fill:80dlu:grow",
		"default, 3dlu, fill:80dlu:grow",
		"default, 3dlu, fill:80dlu:grow, 3dlu, default",
		"fill:320dlu:grow, 3dlu, p",
		"max(50dlu;pref), 3dlu, 90dlu, 10dlu, p, 3dlu,max(90dlu;pref),60dlu",
		"max(50dlu;pref), 3dlu, max(90dlu;pref), 10dlu, p, 3dlu,max(90dlu;pref),30dlu",
		"max(70dlu;pref), 3dlu, 180dlu:grow",
		"p",
		"p, 1dlu, default ,3dlu, default",
		"p, 3dlu, 120dlu:grow",
		"p, 3dlu, 160dlu",
		"p, 3dlu, 160dlu, 3dlu, p, 3dlu, p:grow",
		"p, 3dlu, 300dlu",
		"p, 3dlu, 50dlu, 20dlu, p, 3dlu, 50dlu:grow",
		"p, 3dlu, fill:160dlu:grow",
		"p, 3dlu, max(160dlu;pref):grow",
		"p, 3dlu, p",
		"p, 3dlu, p, 3dlu",
		"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p",
		"p,3dlu,100dlu:grow",
		"p,3dlu,130dlu,12dlu,p,3dlu,80dlu",
		"p,3dlu,300dlu",
		"p,3dlu,p",
		"p,3dlu,p,p:grow",
		"p:grow",
		"p:grow, 3dlu,100dlu:grow,5dlu, default, 5dlu",
		"p:grow,0dlu,16dlu",
		"p:grow,0dlu,right:p",
		"right:pref, 8dlu, 280dlu:grow"
	};
	static final String[] ROWS = {
		"20dlu,20px, 3dlu, 20px",
		"50dlu,p,3dlu,p, 3dlu, p, 3dlu, p",
		"600px",
		"fill:250dlu:grow",
		"fill:275dlu:grow",
		"fill:pref:grow",
		"max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),fill:50dlu:grow",
		"max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref)",
		"p",
		"p, 3dlu",
		"p, 3dlu, fill:220dlu:grow",
		"p, 3dlu, p",
		"p, 3dlu, p, 3dlu, p",
		"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, 100dlu, 20dlu, p",
		"p, 3dlu,fill:200dlu:grow",
		"p, 3dlu,p, 3dlu, p, 3dlu,p,3dlu",
		"p, 3dlu,p,10dlu,p,3dlu,p,3dlu",
		"p, 3dlu,p,3dlu,p,3dlu,p,p,10dlu,p,3dlu,p,3dlu,p,3dlu",
		"p, 6dlu, p, 6dlu, p, 6dlu, p, 1dlu, p, 1dlu, p, 1dlu, p, 1dlu, p, 6dlu, p, 6dlu, p, 6dlu, p, 10dlu, p, 6dlu, p, 6dlu, p",
		"p,1dlu,p,1dlu,p,10dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p",
		"p,1dlu,p,3dlu, p,1dlu,p,2dlu",
		"p,2dlu",
		"p,3dlu, p",
		"p,3dlu,p",
		"p,3dlu,p,3dlu,p",
		"p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p",
		"p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p",
		"p,p",
		"p,p,p",
		"p,p,p,p",
		"p,p,p,p,fill:150dlu:grow",
		"p,p,p,p,fill:p:grow,p,p,p",
		"p,p,p,p,p, 10dlu, p,p,p, 10dlu, p,p,p, 10dlu, p,p,p,p,p",
		"pref,0dlu,p,3dlu,p,3dlu,p,3dlu,p,0dlu,p,0dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p",
		"pref,pref,fill:260dlu:grow"
	};
}
