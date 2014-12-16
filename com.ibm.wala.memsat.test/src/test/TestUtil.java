/******************************************************************************
 * Copyright (c) 2009 - 2015 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/**
 * 
 */
package test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 * A collection of utility classes for testing Miniatur.
 * 
 * @author etorlak
 */
public final class TestUtil {
	private TestUtil() {}
	
	/**
	 * Returns a set of references to all methods in the given class that are public,
	 * static, take no arguments, have no return value, and whose name starts with 
	 * the given prefix.
	 * @return a set of references to all methods in the given class that are public,
	 * static, take no arguments, have no return value, and whose name starts with 
	 * the given prefix.
	 */
	public static Set<MethodReference> threadMethods(String prefix, Class<?> klass) { 
		final Set<MethodReference> methods = new LinkedHashSet<MethodReference>();
		for(Method method: klass.getDeclaredMethods()){
			 final int mod = method.getModifiers();
			 if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && 
				 method.getReturnType().equals(void.class) && method.getParameterTypes().length==0 &&
				 method.getName().startsWith(prefix))  {
				 methods.add(method(method));
			 }
		}
		return methods;
	}
	
	/**
	 * Returns a set of references to all methods in the given class that are public,
	 * static, take no arguments, have no return value, and whose name starts with 
	 * the prefix "thread."
	 * @return a set of references to all methods in the given class that are public,
	 * static, take no arguments, have no return value, and whose name starts with 
	 * the prefix "thread."
	 */
	public static Set<MethodReference> threadMethods(Class<?> klass) { 
		return threadMethods("thread", klass);
	}
	
	/** 
	 * Returns the first method returned by the given iterator of the given class that has the given name.
	 * @return method from the given class with the given name 
	 * @throws NoSuchMethodException */
	public static MethodReference method(Class<?> klass, String methodName) throws NoSuchMethodException { 
		for(Method method: klass.getMethods()){
			if (method.getName().equals(methodName)){
				return method(method);
			}
		}
		throw new NoSuchMethodException(methodName);
	}
	

	/**
	 * Returns a method reference for the given method 
	 * @return method reference for the given Method */
	public static MethodReference method(Method jmethod) { 
		final TypeReference declaringClass = typeReference(jmethod.getDeclaringClass());

		final StringBuilder descriptor = new StringBuilder("(");
		for(Class<?> paramType : jmethod.getParameterTypes()) { 
			descriptor.append(bytecodeDescriptor(paramType));
		}
		descriptor.append(")");
		descriptor.append(bytecodeDescriptor(jmethod.getReturnType()));
		
		return MethodReference.findOrCreate(
				declaringClass,
				Atom.findOrCreateUnicodeAtom(jmethod.getName()), 
				Descriptor.findOrCreateUTF8(descriptor.toString()));
	}
	
	/**
	 * Returns the type reference for the given class. 
	 * @return type reference for the given class */
	public static TypeReference typeReference(Class<?> jclass) { 
		return TypeReference.findOrCreate(
				JavaSourceAnalysisScope.SOURCE, 
				"L" + jclass.getName().replace('.','/'));
		
	}
	
	/**
	 * Returns the  byte code descriptor for the given class.
	 * @return byte code descriptor for the given class */
	public static String bytecodeDescriptor(Class<?> jclass) { 
		final String name;
		if (jclass.isArray()) { 
			name = jclass.getName();
		} else if (jclass.isPrimitive()) { 
			if (jclass.equals(long.class)) { 
				name = "J";
			} else if (jclass.equals(boolean.class)) { 
				name = "Z";
			} else {
				name = jclass.getName().substring(0, 1).toUpperCase();
			}
		} else {
			name = "L"+jclass.getName()+";";
		}
		return name.replace('.', '/');
	}
}
