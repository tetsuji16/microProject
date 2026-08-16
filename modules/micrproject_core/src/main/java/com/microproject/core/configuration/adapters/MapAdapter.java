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

 
public class MapAdapter<K, V> extends XmlAdapter<MapAdapterList<K, V>, Map<K, V>> {
 
    @Override
    public MapAdapterList<K,V> marshal(Map<K, V> map) throws Exception {
        MapAdapterList<K, V> list = new MapAdapterList<K, V>(); 
        for (Map.Entry<K, V> entry : map.entrySet()) {
            list.getEntry().add(new MapAdapterEntry<K, V>(entry.getKey(),entry.getValue()));
        }
        return list;
    }
    
   @Override
    public Map<K, V> unmarshal(MapAdapterList<K, V> list) throws Exception {
        HashMap<K, V> map = new HashMap<K, V>(); 
        for (MapAdapterEntry<K, V> element : list.getEntry()) {
            map.put(element.getKey(), element.getValue());
        }
        return map;
    }
 
}
