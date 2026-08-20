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
package com.microproject.core.fields;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.microproject.core.configuration.Configuration;
import com.microproject.core.dictionary.DictionaryCategory;
import com.microproject.core.dictionary.HasStringId;

import com.microproject.pm.task.Task;

/**
 * @author Laurent Chretienneau
 *
 */
public class FieldUtil {
	protected static Logger log = Logger.getLogger("FieldUtil");
	public static void convertFields(HasFields hasFields, Class<?> inClass, Object inObject, String[] fieldNames, boolean from){
		for (int i=0;i<fieldNames.length;){
			convertFieldSeries(hasFields, inClass, inObject, fieldNames[i++], -1, -1, fieldNames[i++], -1, -1, -1, fieldNames[i++], from);
		}
	}
	
	protected static void convertFieldSeries(HasFields hasFields, Class<?> inClass, Object inObject, String fieldName1, int startIndex1, int endIndex1, String fieldName2, int startIndex2, int endIndex2, int index, String converterName, boolean from){
//		if (fieldName2.startsWith("customCost")){
//			log.info("convertFieldSeries("+hasFields+", "+fieldName1+", "+startIndex1+", "+endIndex1+", "+fieldName2+", "+startIndex2+", "+endIndex2+", "+index+")");
//		}
		String[] elements1=fieldName1.split(":", -1);
		if (elements1.length==3){
			fieldName1=elements1[0];
			startIndex1=Integer.parseInt(elements1[1]);
			endIndex1=Integer.parseInt(elements1[2]);
			int len=endIndex1-startIndex1+1;
			if (index==-1){
				for (int i=0;i<len;i++)
					convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, i, converterName, from);
			}else convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, index, converterName, from);
			return;
		}
		String[] elements2=fieldName2.split(":", -1);
		if (elements2.length==3){
			fieldName2=elements2[0];
			startIndex2=Integer.parseInt(elements2[1]);
			endIndex2=Integer.parseInt(elements2[2]);
			int len=endIndex2-startIndex2+1;
			if (index==-1){
				for (int i=0;i<len;i++)
					convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, i, converterName, from);
			}else convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, index, converterName, from);
			return;
		}

		elements1=fieldName1.split(",", -1);
		if (elements1.length==3){
			fieldName1=elements1[0];
			startIndex1=Integer.parseInt(elements1[1]);
			endIndex1=Integer.parseInt(elements1[2]);
			int len=endIndex1-startIndex1+1;
			if (index==-1){
				for (int i=0;i<len;i++)
					convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, i, converterName, from);
			}else convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, index, converterName, from);
			return;
		}
		elements2=fieldName2.split(",", -1);
		if (elements2.length==3){
			fieldName2=elements2[0];
			startIndex2=Integer.parseInt(elements2[1]);
			endIndex2=Integer.parseInt(elements2[2]);
			int len=endIndex2-startIndex2+1;
			if (index==-1){
				for (int i=0;i<len;i++)
					convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, i, converterName, from);
			}else convertFieldSeries(hasFields, inClass, inObject, fieldName1, startIndex1, endIndex1, fieldName2, startIndex2, endIndex2, index, converterName, from);
			return;
		}
		
		convertField(hasFields, inClass, inObject, fieldName1, index==-1? -1: index+startIndex1, fieldName2, index==-1? -1: index+startIndex2, converterName, from);
	}
	
	protected static void convertField(HasFields hasFields, Class<?> inClass, Object inObject, String fieldName1, int index1, String fieldName2, int index2, String converterName, boolean from){ 
		if (index1!=-1)
		 fieldName1+=index1;
		
		//index1 is ignored
		try {
			if (from) {
//				if (index2!=-1)
//					fieldName2+=index1;

				//get value
				Object value;
				if (index2==-1){
					Method m=inClass.getMethod(toGetterMethodName(fieldName2), (Class<?>[])null);
					value=m.invoke(inObject, (Object[])null);
				}else{
					Method m=inClass.getMethod(toGetterMethodName(fieldName2), new Class<?>[]{int.class});
					value=m.invoke(inObject, new Object[]{index2});
				}
				
				if (value==null) return; //skip null values, it will be considered as not set
				
				//convert
				if (converterName!=null){
					if (value==null) return;
					FieldTypeConverter converter = (FieldTypeConverter)Class.forName(converterName).getDeclaredConstructor().newInstance();
					value=converter.convert(value, from);
				}
				
				//set
				hasFields.setPropertyValue(fieldName1, value);
			} else {
				//get value
				Object value=hasFields.getPropertyValue(fieldName1);
				
				if (value==null || 
						((value instanceof Boolean) && ((Boolean)value)==false) )
					return; //skip null values, it will be considered as not set

				//convert
				if (converterName!=null){
					if (value==null) return;
					FieldTypeConverter converter = (FieldTypeConverter)Class.forName(converterName).getDeclaredConstructor().newInstance();
					value=converter.convert(value, from);
				}
				if (value==null) return;
				
				//set
				String methodToFind=toSetterMethodName(fieldName2);
				Method method=findCompatibleSetter(inClass, methodToFind, index2 != -1, value.getClass());
				if (method==null){
					log.info("Method not found: "+hasFields.getClass()+" "+methodToFind);
					return;
				}
				if (index2==-1)
					value=method.invoke(inObject, new Object[]{value});
				else {
					value=method.invoke(inObject, new Object[]{index2, value});
				}
				

				
			}
		} catch (SecurityException e) {
			log.log(java.util.logging.Level.WARNING, "Error", e);
		} catch (IllegalArgumentException e) {
			log.log(java.util.logging.Level.WARNING, "Error", e);
		} catch (ReflectiveOperationException e) {
			log.log(java.util.logging.Level.WARNING, "Error", e);
		}
	}

	private static Method findCompatibleSetter(Class<?> type, String methodName, boolean indexed, Class<?> valueType) {
		Method compatible = null;
		for (Method method : type.getMethods()) {
			if (!method.getName().equals(methodName))
				continue;
			Class<?>[] parameterTypes = method.getParameterTypes();
			int valueParameter = indexed ? 1 : 0;
			if (parameterTypes.length != valueParameter + 1 || indexed && parameterTypes[0] != int.class)
				continue;
			Class<?> parameterType = boxedType(parameterTypes[valueParameter]);
			if (!parameterType.isAssignableFrom(valueType))
				continue;
			if (parameterType == valueType)
				return method;
			if (compatible == null || boxedType(compatible.getParameterTypes()[valueParameter]).isAssignableFrom(parameterType))
				compatible = method;
		}
		return compatible;
	}

	private static Class<?> boxedType(Class<?> type) {
		if (!type.isPrimitive())
			return type;
		if (type == boolean.class) return Boolean.class;
		if (type == byte.class) return Byte.class;
		if (type == short.class) return Short.class;
		if (type == int.class) return Integer.class;
		if (type == long.class) return Long.class;
		if (type == float.class) return Float.class;
		if (type == double.class) return Double.class;
		if (type == char.class) return Character.class;
		return type;
	}
	
	protected static String toGetterMethodName(String s){
		return "get" + s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	protected static String toSetterMethodName(String s){
		return "set" + s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	protected static Field[] getDeclaredFields(Class<?> classe){
		Field[] fields=classe.getDeclaredFields();
		return fields;
	}
		
	
	public static com.microproject.core.fields.Field getField(String fieldId, String[] categories){
		com.microproject.core.fields.Field field=null;
		for (String category : categories){
			field=(com.microproject.core.fields.Field)Configuration.getInstance().getDictionary().get(
					new DictionaryCategory(com.microproject.core.fields.Field.class, category),
					fieldId);
			if (field!=null)
				return field;
		}
		return field;
	}
	
	public static Map<String,com.microproject.core.fields.Field> getFields(String[] categories){
		Map<String,com.microproject.core.fields.Field> map=new HashMap<String, com.microproject.core.fields.Field>();
		for (String category : categories){
			Map<String,HasStringId> m=Configuration.getInstance().getDictionary().get(
					new DictionaryCategory(com.microproject.core.fields.Field.class, category));
			if (m!=null)
				for (String key : m.keySet()){
					com.microproject.core.fields.Field existingField=map.get(key);
					if (existingField==null)
						map.put(key, (com.microproject.core.fields.Field)m.get(key));
				}
		}
		return map;
	}

	
	public static String[] getCategories(Class<?> cl){
		Set<String> categorySet=new HashSet<String>();
		List<String> categories=new LinkedList<String>();
		addCategories(cl, categories, categorySet);
		return categories.toArray(new String[categories.size()]);
		
	}
	
	private static void addCategories(Class<?> cl, List<String> categories, Set<String> categorySet){
		categorySet.add(cl.getName());
		categories.add(cl.getName());
		Class<?> superClass=cl.getSuperclass();
		Class<?>[] interfaces=cl.getInterfaces();
		if (superClass!=null &&
				!categorySet.contains(superClass.getName())){
			addCategories(superClass,categories,categorySet);

		}
		for (Class<?> i : interfaces){
			if (!categorySet.contains(i.getName())){
				addCategories(i,categories,categorySet);
			}
		}
	}
		
	

}
