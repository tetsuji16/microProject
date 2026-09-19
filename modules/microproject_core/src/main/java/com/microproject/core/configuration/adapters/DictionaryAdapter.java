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
package com.microproject.core.configuration.adapters;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.microproject.core.dictionary.HasStringId;

 
public class DictionaryAdapter<T> extends XmlAdapter<DictionaryAdapterList<T>, Map<String, T>> {
// class containing the dictionary:
//	@XmlAccessorType(XmlAccessType.PROPERTY)
//	public class GraphShapes {
//		protected String name;
//		protected Map<String,GraphShape> shape;
//		
//		@XmlAttribute(name="name")
//		public String getName() {
//			return name;
//		}
//
//		public void setName(String name) {
//			this.name = name;
//		}
//
//		@XmlElement(name="dictionary")
//		@XmlJavaTypeAdapter(DictionaryAdapter.class)
//		public Map<String,GraphShape> getShape() {
//			return shape;
//		}
//
//		public void setShape(Map<String,GraphShape> shape) {
//			this.shape = shape;
//		}
//	}
//
// class in dictionary
//	
//	@XmlRootElement(name="shape")
//	@XmlAccessorType(XmlAccessType.NONE)
//	public class GraphShape implements HasName{
//		protected String name;
//
//		@XmlAttribute(name="name") @XmlID
//		public String getName() {
//			return name;
//		}
//
//		public void setName(String name) {
//			this.name = name;
//		}
//	}
//
//
//	<shapes name="vertex">
//	<dictionary>
//		<shape name="test">
//			... elements
//		</shape>
//		<shape name="square">
//			... elements
//		</shape>
//	</dictionary>
//</shapes>

	
 
    @Override
    public DictionaryAdapterList<T> marshal(Map<String, T> map) throws Exception {
        DictionaryAdapterList<T> list = new DictionaryAdapterList<T>(); 
        for (Map.Entry<String, T> entry : map.entrySet()) {
            list.getEntry().add(entry.getValue());
        }
        return list;
    }
    
   @Override
    public Map<String, T> unmarshal(DictionaryAdapterList<T> list) throws Exception {
        HashMap<String, T> map = new HashMap<String, T>(list.getEntry().size());
    	for (T element : list.getEntry()) {
            map.put(((HasStringId)element).getId(), element); //T must implement HasName
        }
        return map;
    }
 
}
