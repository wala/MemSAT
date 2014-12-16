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
package com.ibm.wala.memsat.translation;

import static com.ibm.wala.types.TypeReference.JavaLangObject;
import static com.ibm.wala.types.TypeReference.JavaLangString;
import static com.ibm.wala.types.TypeReference.JavaLangSystem;
import static com.ibm.wala.types.TypeReference.JavaLangThrowable;
import static kodkod.ast.Formula.FALSE;
import static kodkod.ast.Formula.TRUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntExpression;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ArrayLengthKey;
import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.frontEnd.FieldSSATable;
import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.representation.ArrayExpression;
import com.ibm.wala.memsat.representation.ExpressionFactory;
import com.ibm.wala.memsat.representation.FieldExpression;
import com.ibm.wala.memsat.representation.PhiExpression;
import com.ibm.wala.memsat.translation.Environment.Frame;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 * Provides access to specialized translators that translate methods according to 
 * their specification rather than actual code.
 * 
 * @author Emina Torlak
 */
final class SpecialTranslatorFactory {
	private final Map<MethodReference,MethodTranslator> transls;
	
	private SpecialTranslatorFactory(Set<MethodReference> empties) {
		transls = translators(empties);
	}
	
	/**
	 * Returns a factory of special translators created using the given {@linkplain Options} and {@linkplain WalaInformation}.
	 * @return factory of special translators created using the given {@linkplain Options} and {@linkplain WalaInformation}.
	 */
	@SuppressWarnings("unchecked")
	public static SpecialTranslatorFactory factory(Options options, WalaInformation info) { 
//		if (info.threads().getNumberOfNodes()>1) { 
//			return new SpecialTranslatorFactory(options.memoryModel().memoryInstructions());
//		} else {
			return new SpecialTranslatorFactory(Collections.EMPTY_SET);
//		}
	}
	
	/**
	 * Returns a translator for the given method reference, if it is a special method,
	 * or null otherwise.
	 * @return a translator for the given method reference, if it is a special method,
	 * or null otherwise.
	 */
	public MethodTranslator translatorFor(MethodReference m) { return transls.get(m); }
	
	/**
	 * Returns true if this factory has a translator for the given method.
	 * @return true if this factory has a translator for the given method.
	 */
	public boolean hasTranslatorFor(MethodReference m) { return transls.containsKey(m); }
	
	/**
	 * Returns a map of available specification-based method translators. 
	 * All methods in the given set are mapped to unconditionally executed,
	 * no-body translators.
	 * @requires all m: empties.elements | m.getReturnType = TypeReference.Void
	 * @return a map that maps method references to their corresponding
	 * specification-based translators translators 
	 **/
	private static Map<MethodReference,MethodTranslator> translators(Set<MethodReference> empties) { 
		final Map<MethodReference,MethodTranslator> translators = 
			new LinkedHashMap<MethodReference, MethodTranslator>();
		
		final MethodTranslator emptyTrue = emptyTranslator(TRUE);
		final MethodTranslator emptyFalse = emptyTranslator(FALSE);
		
		// empties
		for(MethodReference m : empties) { 
			if (!TypeReference.Void.equals(m.getReturnType())) { 
				throw new IllegalArgumentException("Special user methods cannot return any values: " + m);
			}
			translators.put(m, emptyTrue);
		}
		
		//	object 
		translators.put(method(JavaLangObject,"<init>","()V"), emptyTrue);
		translators.put(method(JavaLangObject,"equals","(Ljava/lang/Object;)Z"), javaLangObjectEquals());
		translators.put(method(JavaLangObject,"hashCode","()I"), javaLangObjectHashCode());
		
		// throwable
		translators.put(method(JavaLangThrowable,"printStackTrace","()V"), emptyTrue);
		translators.put(method(JavaLangThrowable,"fillInStackTrace","()Ljava/lang/Throwable;"), javaLangThrowableFillInStackTrace());
		
		// system
		translators.put(method(JavaLangSystem,"exit","(I)V"), emptyFalse);
		
		// string
		translators.put(method(JavaLangString,"equals","(Ljava/lang/Object;)Z"), javaLangStringEquals());
		translators.put(method(JavaLangString,"length","()I"), javaLangStringLength());
		translators.put(method(JavaLangString,"charAt","(I)C"), javaLangStringCharAt());
		translators.put(method(JavaLangString,"hashCode","()I"), javaLangStringHashCode());

		//nondet
		final TypeName nonDetName = TypeName.string2TypeName("Ldata/angelic/NonDetChoice");
		final TypeReference nonDet = TypeReference.findOrCreate(JavaSourceAnalysisScope.SOURCE, nonDetName);
		translators.put(method(nonDet,"chooseObject","()Ljava/lang/Object;"), nonDetChoiceTranslator());
		translators.put(method(nonDet,"chooseInt","()I"), nonDetChoiceTranslator());
		translators.put(method(nonDet,"chooseBoolean","()Z"), nonDetChoiceTranslator());
		return translators;
	}
	

	/** @return method reference corresponding to the given arguments */
	private static MethodReference method(TypeReference type, String name, String descriptor) { 
		return MethodReference.findOrCreate(type, 
				Atom.findOrCreateUnicodeAtom(name), 
				Descriptor.findOrCreateUTF8(descriptor));
	}
	
	
	private static MethodTranslator nonDetChoiceTranslator(){
		return new MethodTranslator() {
			public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
				System.err.println("translating nondet!");
				return result(TRUE, null, env);
			}
		};
	}
	
	/**
	 * @return a translator that simply propagates the initial heap through and 
	 * pops the top frame with the given guard (return and exception values 
	 * are set to null.)
	 */
	private static MethodTranslator emptyTranslator(final Formula exitGuard) { 
		return new MethodTranslator() {
			public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
				return result(exitGuard, null, env);
			}	
		};
	}
	
	/** @return a spec translator for java.lang.Object.equals */
	private static MethodTranslator javaLangObjectEquals() {
		return new MethodTranslator() {			
			public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
				return result(TRUE, env.refUse(1).eq(env.refUse(2)), env);
			}
		};
	}
	
	/** @return a spec translator for java.lang.Object.hashCode */
	private static MethodTranslator javaLangObjectHashCode() {
		return new MethodTranslator() {
			public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
				return result(TRUE, env.factory().systemHashCode().read(env.refUse(1)), env);
			}
		};
	}
	
	/** @return a spec translator for java.lang.Throwable.fillInStackTrace */
	private static MethodTranslator javaLangThrowableFillInStackTrace() {
		return new MethodTranslator() {
			public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
				return result(TRUE, env.refUse(1), env);
			}
		};
	}
	
	/** @return a spec translator for java.lang.String.hashCode */
	private static MethodTranslator javaLangStringHashCode() {
		return new MethodTranslator() {
			
			public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
				final Expression self = env.refUse(1);
								
				final ExpressionFactory factory = env.factory();
				final PhiExpression<IntExpression> phi = factory.valuePhi(IRType.INTEGER);
				
				for(Map.Entry<FieldExpression<Expression>,Set<ArrayExpression<IntExpression>>>
				   fieldPiece : stringContents(factory).entrySet()) { 
					
					FieldExpression<Expression> fieldExpr = fieldPiece.getKey();
					Formula fieldGuard = self.in(fieldExpr.instances());
					
					Expression selfArrayRef  = fieldExpr.read(self);
					
					for(ArrayExpression<IntExpression> arrayExpr : fieldPiece.getValue()) { 
						Formula arrayGuard = selfArrayRef.in(arrayExpr.instances());
						
						List<IntExpression> chars = new ArrayList<IntExpression>();
						for(int i = 0, max = arrayExpr.cardinality(); i < max; i++) { 
							chars.add(arrayExpr.value(selfArrayRef, i));
						}
						phi.add(fieldGuard.and(arrayGuard), IntExpression.plus(chars));
					}
				}
				
				return result(Formula.TRUE, phi.value(), env);
			}
		};
	}
	
	/** @return a spec translator for java.lang.String.charAt  */
	private static MethodTranslator javaLangStringCharAt() {
		return new MethodTranslator() {
			
			public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
				final Expression self = env.refUse(1);
				final IntExpression idx = env.intUse(2);
					
				final ExpressionFactory factory = env.factory();
				final PhiExpression<IntExpression> phi = factory.valuePhi(IRType.INTEGER);
					
				for(Map.Entry<FieldExpression<Expression>,Set<ArrayExpression<IntExpression>>>
				   fieldPiece : stringContents(factory).entrySet()) { 
					
					FieldExpression<Expression> fieldExpr = fieldPiece.getKey();
					Formula fieldGuard = self.in(fieldExpr.instances());
					
					Expression selfArrayRef  = fieldExpr.read(self);
					
					for(ArrayExpression<IntExpression> arrayExpr : fieldPiece.getValue()) { 
						Formula arrayGuard = selfArrayRef.in(arrayExpr.instances());
						
						phi.add(fieldGuard.and(arrayGuard), arrayExpr.read(selfArrayRef, idx));
					}
				}

				return result(Formula.TRUE, phi.value(), env);
			}
		};
	}
	
	/** @return a spec translator for java.lang.String.length */
	private static MethodTranslator javaLangStringLength() {
		return new MethodTranslator() {
			
			public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
				final Expression self = env.refUse(1);
				
				final ExpressionFactory factory = env.factory();
				final PhiExpression<IntExpression> phi = factory.valuePhi(IRType.INTEGER);

				for(Map.Entry<FieldExpression<Expression>,Set<FieldExpression<IntExpression>>>
				   lengthPiece : stringLengths(factory).entrySet()) { 
					
					FieldExpression<Expression> fieldExpr = lengthPiece.getKey();
					Formula fieldGuard = self.in(fieldExpr.instances());
					
					Expression selfArrayRef  = fieldExpr.read(self);
					
					for(FieldExpression<IntExpression> lengthExpr : lengthPiece.getValue()) { 
						Formula lengthGuard = selfArrayRef.in(lengthExpr.instances());
						
						phi.add(fieldGuard.and(lengthGuard), lengthExpr.read(selfArrayRef));
					}
				}
				
				return result(Formula.TRUE, phi.value(), env);
			}
		};
	}
	
	/** @return a spec translator for java.lang.String.equals */
	private static MethodTranslator javaLangStringEquals() {
		return new MethodTranslator() {			
			
			public MethodTranslation translate(Formula entryGuard, Environment env, MemoryInstructionHandler memoryHandler) {
				final Expression self = env.refUse(1);
				final Expression other = env.refUse(2);
				
				final List<Formula> eqConditions = new ArrayList<Formula>();
				eqConditions.add(self.eq(other));
				
				final ExpressionFactory factory = env.factory();
						
				for(Map.Entry<FieldExpression<Expression>,Set<ArrayExpression<IntExpression>>>
					fieldPiece : stringContents(factory).entrySet()) {
					
					FieldExpression<Expression> fieldExpr = fieldPiece.getKey();
					
					Formula fieldGuard = self.in(fieldExpr.instances()).
						and(other.in(fieldExpr.instances()));
					
					Expression selfArrayRef  = fieldExpr.read(self);
					Expression otherArrayRef = fieldExpr.read(other);
					
					for(ArrayExpression<IntExpression> arrayExpr : fieldPiece.getValue()) {
				
						Formula arrayGuard = selfArrayRef.in(arrayExpr.instances()).
							and(otherArrayRef.in(arrayExpr.instances()));
						
						List<Formula> eqArray = new ArrayList<Formula>(arrayExpr.cardinality()+1);
						eqArray.add(fieldGuard.and(arrayGuard));
						
						for(int i = 0, max = arrayExpr.cardinality(); i < max; i++) { 
							Formula sameIdx  = arrayExpr.index(selfArrayRef, i).
								eq(arrayExpr.index(otherArrayRef, i));
							Formula sameChar = arrayExpr.value(selfArrayRef, i).
								eq(arrayExpr.value(otherArrayRef, i));
							eqArray.add(sameIdx.and(sameChar));
						}
						
						eqConditions.add(Formula.and(eqArray));
					}
				}
				
				return result(Formula.TRUE, Formula.or(eqConditions), env);
			}
		};            
	}
	
	/**
	 * Returns a Result from the given values and env.top. 
	 * (The method is assumed to throw no exception, so exception is set to null)
	 * The topmost frame in the environment is updated before
	 * being placed into the final result by propagating initial
	 * heap values into unfilled exit values, if any.
	 * @return a a Result from the given values and env.top
	 */
	@SuppressWarnings("unchecked")
	private static MethodTranslation result(final Formula exitGuard, final Object returnValue, final Environment env) {
		
		final FieldSSATable fieldSSA = env.top().callInfo().fieldSSA();
		for(Iterator<PointerKey> fields = fieldSSA.getFields(); fields.hasNext();) {
			PointerKey field = fields.next();
			int entry = fieldSSA.getEntryValue(field);
			int exit = fieldSSA.getExitValue(field);
			if (env.heapUse(exit)==null)
				env.heapDef(exit, env.heapUse(entry));
		}
		
		return new MethodTranslation() {		
			final Frame frame = env.pop();
			
			public Set<Formula> assertions() { return Collections.EMPTY_SET; }
			public Set<TranslationWarning> warnings() { return Collections.EMPTY_SET; }
			public Set<Formula> assumptions() { return Collections.EMPTY_SET;}
			
			public <T> T returnValue() { return (T) returnValue; }
			public Expression exceptionValue() { return null; }
			public Formula normalExitGuard() { return exitGuard; }
			public Frame frame() { return frame; }
			
		};
	}
	
	/** @return the field object representing the value field of the String class. */
	private static IField stringValueField(WalaInformation info) {
		final IClass str = info.callGraph().getClassHierarchy().lookupClass(TypeReference.JavaLangString);
		return str.getField(Atom.findOrCreateUnicodeAtom("value"));
	}
	
	/** 
	 * @return a map from each FieldExpression F corresponding to 
	 * String.value field in the given factory.info.callGraph(),
	 * to the set of array expressions that represent the contents of the 
	 * array instances pointed to by F. 
	 **/
	private static Map<FieldExpression<Expression>,Set<ArrayExpression<IntExpression>>> stringContents(ExpressionFactory factory) { 
		final IField field = stringValueField(factory.info());
		final Map<InstanceKey, FieldExpression<Expression>> tmp = 
			new LinkedHashMap<InstanceKey, FieldExpression<Expression>>();
		
		final Map<FieldExpression<Expression>,Set<ArrayExpression<IntExpression>>> ret = 
			new LinkedHashMap<FieldExpression<Expression>, Set<ArrayExpression<IntExpression>>>();
		
		final WalaInformation info = factory.info();
		
		for(PointerKey key : info.relevantFields()) { 
			if (key instanceof InstanceFieldKey &&
				((InstanceFieldKey)key).getField().equals(field)) {
				FieldExpression<Expression> fieldExpr = factory.initValueOf(key);
				ret.put(fieldExpr, new LinkedHashSet<ArrayExpression<IntExpression>>());
				for(InstanceKey pointedTo : info.pointsTo(key)) { 
					tmp.put(pointedTo, fieldExpr);
				}
			}
		}
		
		for(PointerKey key : info.relevantFields()) { 
			if (key instanceof ArrayContentsKey && tmp.containsKey(((ArrayContentsKey)key).getInstanceKey())) {
				ArrayExpression<IntExpression> arrayExpr = factory.initValueOf(key);
				ret.get(tmp.get(((ArrayContentsKey)key).getInstanceKey())).add(arrayExpr);
			}
		}
		
		return ret;
	}
	
	/** 
	 * @return a map from each FieldExpression F corresponding to 
	 * String.value field in the given factory.info.callGraph(),
	 * to the set of field expressions that represent the length of the 
	 * array instances pointed to by F. 
	 **/
	private static Map<FieldExpression<Expression>,Set<FieldExpression<IntExpression>>> stringLengths(ExpressionFactory factory) { 
		final IField field = stringValueField(factory.info());
		final Map<InstanceKey, FieldExpression<Expression>> tmp = 
			new LinkedHashMap<InstanceKey, FieldExpression<Expression>>();
		
		final Map<FieldExpression<Expression>,Set<FieldExpression<IntExpression>>> ret = 
			new LinkedHashMap<FieldExpression<Expression>, Set<FieldExpression<IntExpression>>>();
		
		final WalaInformation info = factory.info();
		
		for(PointerKey key : info.relevantFields()) { 
			if (key instanceof InstanceFieldKey &&
				((InstanceFieldKey)key).getField().equals(field)) {
				FieldExpression<Expression> fieldExpr = factory.initValueOf(key);
				ret.put(fieldExpr, new LinkedHashSet<FieldExpression<IntExpression>>());
				for(InstanceKey pointedTo : info.pointsTo(key)) { 
					tmp.put(pointedTo, fieldExpr);
				}
			}
		}
		
		for(PointerKey key : info.relevantFields()) { 
			if (key instanceof ArrayLengthKey && tmp.containsKey(((ArrayContentsKey)key).getInstanceKey())) {
				FieldExpression<IntExpression> lengthExpr = factory.initValueOf(key);
				ret.get(((ArrayContentsKey)key).getInstanceKey()).add(lengthExpr);
			}
		}
		
		return ret;
	}
}
