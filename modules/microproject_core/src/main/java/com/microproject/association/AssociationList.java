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
package com.microproject.association;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.field.FieldParseException;


/**
 * Container for managing lists of associated elements, such as Dependency or Assignment
 */
public class AssociationList implements List<Association> {

    private static final Logger logger = Logger.getLogger(AssociationList.class.getName());

    protected LinkedList<Association> list;
    
    
    public AssociationList() {
    	list = new LinkedList<>();
    }
    
    public AssociationList(AssociationList from) {
    	this();
    	list.addAll(from.list);
    }
    public boolean add(Association association) {
    	Association found = AssociationList.findAssociation(list,association.getLeft(),association.getRight(),null);
    	if (found != null) // if already in list
    		return false;
        return list.add(association);
    }
    
    private static Object getObject(Association association, boolean leftObject) {
    	return leftObject ? association.getLeft() : association.getRight();
    }

    public Association find(boolean leftObject, Object object) {
    	Association association;
        for (Iterator<Association> i = list.iterator(); i.hasNext();) {
			association = i.next();
            if (getObject(association,leftObject) == object)
                return association;
        }
        return null;
    }

    public Association findLeft(Object left) {
    	return find(true,left);
    }

    public Association findRight(Object right) {
    	return find(false,right);
    }
    
    public static Association findAssociation(LinkedList<Association> findInList, Object left, Object right, Association exclude) {
        for ( Iterator<Association> i = findInList.iterator(); i.hasNext();) {
            Association association = i.next();
        	if (association == exclude)
        		continue;
            if (association.getLeft() == left && association.getRight() == right)
                return association;
        }
        return null;
    }
    
    public static List<Object> extractDistinct(List<? extends Association> list, boolean leftObject) {
		LinkedHashSet<Object> distinct = new LinkedHashSet<>();
		for (Association association : list) {
			Object object = getObject(association,leftObject);
			distinct.add(object);
        }
		return new ArrayList<>(distinct);
    }
    

    
    
    protected void testValid(boolean allowDuplicate) throws InvalidAssociationException {
        for ( Iterator<Association> i = list.iterator(); i.hasNext();) {
        	Association association = i.next();
        	association.testValid(allowDuplicate); //throws if exception
        }	
    }    
    
    public void replaceAll(Object object, boolean leftObject) {
        for ( Iterator<Association> i = list.iterator(); i.hasNext();) {
        	Association association = i.next();
        	association.replace(object,leftObject);
        }
    }
	public AssociationList setAssociations(String associations, AssociationFormat associationFormat) throws FieldParseException {
		AssociationListFormat format = AssociationListFormat.getInstance(associationFormat);
		AssociationList result = (AssociationList)format.parseObject(associations,new ParsePosition(0));
		if (result == null) {
			logger.warning(associationFormat.getParameters().getError());
			throw new FieldParseException(associationFormat.getParameters().getError());
		}
		LinkedList<Association> oldList = list; // (LinkedList) list.clone(); // make a copy of original list since we'll be modifying real list
		LinkedList<Association> newList = result.list; 
		
		// validate each element in new list
		try {
			result.testValid(true);
		} catch (InvalidAssociationException e) {
//			newList = oldList;
			
        	logger.log(Level.WARNING, e.getMessage(), e);
			throw new FieldParseException(e.getMessage());				
        }	

		Association association;
        Iterator<Association> i;
		
		// check for duplicates
        for (i = newList.iterator(); i.hasNext();) {
			association = i.next();
        	// if duplicate
        	if (AssociationList.findAssociation(newList,association.getLeft(),association.getRight(),association) != null) {
//        		newList = oldList;
				throw new FieldParseException("Duplicate association between "
					+ association.getLeft() + " and " + association.getRight());
        	}
        }		
		
		// At this point, the newList is valid, so now merge
        
		
		// Go through old list figuring out which elements were removed and updating those that are modified.
        Association oldAssociation;
        Association newAssociation;
        LinkedList<Association> removed = new LinkedList<>();
        LinkedList<Association> modified = new LinkedList<>();
        for (i = oldList.iterator(); i.hasNext();) {
        	oldAssociation = i.next();
        	if (oldAssociation.isDefault()) // don't treat default association.  It will be removed by later code if needed
        		continue;
        	newAssociation = AssociationList.findAssociation(newList,oldAssociation.getLeft(),oldAssociation.getRight(),null);
        	if (newAssociation == null) { 
        		removed.add(oldAssociation);
        	} else {
        		if (associationFormat.getParameters().isAllowDetailsEntry()) // some fields don't allow you to enter details. In which case, ignore values
        			modified.add(oldAssociation); // for later use?
        			oldAssociation.copyPrincipalFieldsFrom(newAssociation);
			}
        }
        
        // Remove ones that were eliminated
        for (i = removed.iterator(); i.hasNext();) {
			i.next().doRemoveService(this); // will remove from real list
        }
        
        

        
        // Get a list of added elements
        LinkedList<Association> added = new LinkedList<>();
        for (i = newList.iterator(); i.hasNext();) {
			association = i.next();
        	if (association.isDefault()) // don't treat default association.  It will be added by later code if needed
        		continue;
        	
        	// see if new one (not in modified list)
        	if (AssociationList.findAssociation(modified,association.getLeft(),association.getRight(),null) == null) {
        		added.add(association);
        	}
        }		
        
        // Add new ones
        for (i = added.iterator(); i.hasNext();) {
			i.next().doAddService(this); // will add to the real list
        }
        
        // Signal update of modified ones
        for (i = modified.iterator(); i.hasNext();) {
			i.next().doUpdateService(this); // will send update message
        }        
		AssociationComparator comparator = new AssociationComparator(associationFormat.getParameters().getIdField());
		sortRelatedLists(comparator);
		return result;
	}

	private void sortRelatedLists(AssociationComparator comparator) {
		IdentityHashMap<AssociationList, Boolean> relatedLists = new IdentityHashMap<>();
		relatedLists.put(this, Boolean.TRUE);
		for (Association association : list) {
			for (AssociationList related : association.getAssociationLists())
				relatedLists.put(related, Boolean.TRUE);
		}
		for (AssociationList related : relatedLists.keySet())
			related.sort(comparator);
	}
    
    
	/**
	 * @return
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * @param arg0
	 * @return
	 */
	public boolean remove(Object arg0) {
		return list.remove(arg0);
	}

	/**
	 * @param arg0
	 */
	public void addFirst(Association association) {
		list.addFirst(association);
	}

	/**
	 * @return
	 */
	public Iterator<Association> iterator() {
		return list.iterator();
	}

	/**
	 * @return Returns the list.
	 */
	public LinkedList<Association> getList() {
		return list;
	}
	/**
	 * @param arg0
	 * @param arg1
	 */
	public void add(int index, Association association) {
		list.add(index, association);
	}
	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public boolean addAll(int index, Collection<? extends Association> associations) {
		return list.addAll(index, associations);
	}
	/**
	 * @param arg0
	 * @return
	 */
	public boolean addAll(Collection<? extends Association> associations) {
		return list.addAll(associations);
	}
	/**
	 * @param arg0
	 */
	public void addLast(Association association) {
		list.addLast(association);
	}
	/**
	 * 
	 */
	public void clear() {
		list.clear();
	}
	/**
	 * @param arg0
	 * @return
	 */
	public boolean contains(Object arg0) {
		return list.contains(arg0);
	}
	/**
	 * @param arg0
	 * @return
	 */
	public boolean containsAll(Collection<?> arg0) {
		return list.containsAll(arg0);
	}
	public boolean equals(Object arg0) {
		return list.equals(arg0);
	}
	/**
	 * @param arg0
	 * @return
	 */
	public Association get(int arg0) {
		return list.get(arg0);
	}
	/**
	 * @return
	 */
	public Association getFirst() {
		return list.getFirst();
	}
	/**
	 * @return
	 */
	public Association getLast() {
		return list.getLast();
	}
	public int hashCode() {
		return list.hashCode();
	}
	/**
	 * @param arg0
	 * @return
	 */
	public int indexOf(Object arg0) {
		return list.indexOf(arg0);
	}
	/**
	 * @param arg0
	 * @return
	 */
	public int lastIndexOf(Object arg0) {
		return list.lastIndexOf(arg0);
	}
	/**
	 * @return
	 */
	public ListIterator<Association> listIterator() {
		return list.listIterator();
	}
	/**
	 * @param arg0
	 * @return
	 */
	public ListIterator<Association> listIterator(int arg0) {
		return list.listIterator(arg0);
	}
	/**
	 * @param arg0
	 * @return
	 */
	public Association remove(int arg0) {
		return list.remove(arg0);
	}
	/**
	 * @param arg0
	 * @return
	 */
	public boolean removeAll(Collection<?> arg0) {
		return list.removeAll(arg0);
	}
	/**
	 * @return
	 */
	public Association removeFirst() {
		return list.removeFirst();
	}
	/**
	 * @return
	 */
	public Association removeLast() {
		return list.removeLast();
	}
	/**
	 * @param arg0
	 * @return
	 */
	public boolean retainAll(Collection<?> arg0) {
		return list.retainAll(arg0);
	}
	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public Association set(int index, Association association) {
		return list.set(index, association);
	}
	/**
	 * @return
	 */
	public int size() {
		return list.size();
	}
	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public List<Association> subList(int arg0, int arg1) {
		return list.subList(arg0, arg1);
	}
	/**
	 * @return
	 */
	public Object[] toArray() {
		return list.toArray();
	}
	/**
	 * @param arg0
	 * @return
	 */
	public <T> T[] toArray(T[] target) {
		return list.toArray(target);
	}
	public String toString() {
		return list.toString();
	}
	
	public void dump(boolean leftObject) {
	   	Association association;
        for ( Iterator<Association> i = list.iterator(); i.hasNext();) {
        	association = i.next();
            logger.fine(String.valueOf(getObject(association,leftObject)));
        }
	}
}
