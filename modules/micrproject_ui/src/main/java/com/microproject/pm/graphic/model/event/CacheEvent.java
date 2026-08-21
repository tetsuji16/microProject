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
package com.microproject.pm.graphic.model.event;

import java.util.function.Consumer;
import java.util.List;
import java.util.ListIterator;

import com.microproject.pm.graphic.event.GraphicEvent;


/**
 *
 */
public class CacheEvent extends GraphicEvent {
    public static final int NODES_CHANGED = 0;
    public static final int NODES_INSERTED = 1;
    public static final int NODES_REMOVED = 2;
    
    protected int type;
    protected List nodes;
    protected List intervals;
   
    
    

    /**
     * @param source
     * @param type
     * @param nodes
     */
    public CacheEvent(Object source, int type, List nodes, List intervals) {
        super(source);
        this.type = type;
        this.nodes = nodes;
        this.intervals = intervals;
    }
    
    public List getNodes() {
        return nodes;
    }
    public void setNodes(List nodes) {
        this.nodes = nodes;
    }
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    
    
    public void forIntervals(Consumer<Object> f){
        if (type==NODES_REMOVED){
			for (ListIterator i=intervals.listIterator(intervals.size());i.hasPrevious();)
				f.accept(i.previous());
        }else{
			for (ListIterator i=intervals.listIterator();i.hasNext();)
				f.accept(i.next());            
        }
    }
    
    public String getStringType(){
        switch (type) {
        case NODES_CHANGED:
            return "CHANGED";
        case NODES_INSERTED:
            return "INSERTED";
        case NODES_REMOVED:
            return "REMOVED";
        default:
            return "UNKNOWN";
        }
    }
    
    public String toString(){
        return getStringType()+": "+nodes+","+intervals;
    }
    
    
}
