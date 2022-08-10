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
package com.ibm.wala.memsat;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.memsat.concurrent.MemoryModel;
import com.ibm.wala.types.TypeReference;

import kodkod.engine.config.Options.IntEncoding;
import kodkod.engine.satlab.SATFactory;


/**
 * Stores translation and analysis options for Miniatur.
 * 
 * @specfield isContextSensitive:  boolean // use context sensitive analysis to build call graphs? (default true)
 * @specfield loopUnrollDepth:  int // default 3
 * @specfield openWorldScopeSize:  int // the cardinality of each set of instances in the open world (default 4)
 * @specfield numberOfIndexAtoms:  int // number of atoms used for array indexing (default 10)
 * @specfield recursionLimit:  int // default 1
 * @specfield primordialConcreteTypes:  Set<TypeReference> // default int and String
 * @specfield kodkodOptions:  kodkod.engine.config.Options // options for the Kodkod engine (for defaults, see {@linkplain #Options()})
 * @specfield memoryModel: MemoryModel // memory model used for analyzing concurrent programs (default is JMMAlt)
 * @specfield assertsAreAssumptions: boolean // default is false
 * @specified eclipseProjectName: String // the Eclipse project to analyze, if any.  default is null.
 * @author Emina Torlak
 */
public final class Options { 
	private boolean isContextSensitive,assertsAreAssumptions;
	private int loopUnrollDepth, openWorldScopeSize, numberOfIndexAtoms, recursionLimit;
	
	private TypeReference undefinedType;
	private MemoryModel memoryModel;
	
	private String eclipseProjectName;
	
	private final Set<TypeReference> primordialConcreteTypes;
	private final kodkod.engine.config.Options kodkodOptions;
	
	/**
	 * Returns an Options instance initialized with default values.
	 * @effects 
	 * this.isContextSensitive' = true and
	 * this.loopUnrollDepth' = 3 and
	 * this.openWorldScopeSize' = 4 and
	 * this.numberOfIndexAtoms' =  10 and
	 * this.recursionLimit' = 1 and
	 * this.primordialConcreteTypes' = {int, String} and
	 * this.kodkodOptions'.solver = SATFactory.MiniSAT and
	 * this.kodkodOptions'.bitwidth = 8 and 
	 * this.memoryModel' = RelaxedModelFactory and
	 * this.assertsAreAssumptions = false
	 */
	public Options() {
		this.isContextSensitive = true;
		this.loopUnrollDepth = 3;
		this.openWorldScopeSize = 4;
		this.numberOfIndexAtoms = 10;
		this.recursionLimit = 1;
		this.primordialConcreteTypes = new HashSet<TypeReference>();
		primordialConcreteTypes.add(TypeReference.JavaLangInteger);
	    primordialConcreteTypes.add(TypeReference.JavaLangString);
		this.undefinedType = null;
		this.kodkodOptions = new kodkod.engine.config.Options();
		kodkodOptions.setBitwidth(8);
		kodkodOptions.setIntEncoding(IntEncoding.TWOSCOMPLEMENT);
		kodkodOptions.setSolver(SATFactory.MiniSat);
		this.memoryModel = null;
		this.assertsAreAssumptions = false;
		this.eclipseProjectName = null;
	}
	/**
	 * Returns the value of isContextSensitive flag.
	 * @return this.isContextSensitive
	 */
	public boolean isContextSensitive() { return this.isContextSensitive; }
	
	/**
	 * Sets the value of isContextSensitive flag.
	 * @return this.isContextSensitive' = isContextSensitive
	 */
	public void setContextSensitivity(boolean isContextSensitive) {
		this.isContextSensitive = isContextSensitive;
	}
	
	/**
	 * @throws IllegalArgumentException - i !in [min..max]
	 */
	public void closedRangeCheck(int i, int min, int max) {
		if (i < min || i > max) 
			throw new IllegalArgumentException(i + " must be in [" + min + ".." + max + "]");
	}
	
	/**
	 * Returns theloop unrolling depth.
	 * @return this.loopUnrollDepth
	 */
	public int loopUnrollDepth() { return loopUnrollDepth; } 
	
	/**
	 * Sets the loop unrolling depth.
	 * @return this.loopUnrollDepth' = depth
	 * @throws IllegalArgumentException - depth <= 0
	 */
	public void setloopUnrollDepth(int depth) { 
		closedRangeCheck(depth, 1, Integer.MAX_VALUE);
		this.loopUnrollDepth = depth;
	}
	
	/**
	 * Return open world scope size.  This number is the lower bound the cardinality of a 
	 * set of instances in the open world, as determined by pointer analysis.
	 * For example, if a concrete class C has two sets of disjoint instances 
	 * that reach into the open world, then each of those sets is guaranteed to 
	 * be represented with at least this.openWorldScopeSize atoms.
	 * @return this.openWorldScopeSize
	 */
	public int openWorldScopeSize() { return openWorldScopeSize; } 
	
	/**
	 * Sets theopen world scope size.
	 * @return this.openWorldScopeSize' = size
	 * @throws IllegalArgumentException - size < 0
	 */
	public void setOpenWorldScopeSize(int size) { 
		closedRangeCheck(size, 0, Integer.MAX_VALUE);
		this.openWorldScopeSize = size;
	}
	
	/**
	 * Returns the number of index atoms.
	 * @return this.numberOfIndexAtoms
	 */
	public int numberOfIndexAtoms() { return numberOfIndexAtoms; } 
	
	/**
	 * Sets the number of index atoms.
	 * @requires 0 <= atoms <= this.kodkodOptions.integers().max()
	 * @return this.numberOfIndexAtoms' = atoms
	 * @throws IllegalArgumentException - atoms < 0
	 */
	public void setNumberOfIndexAtoms(int atoms) { 
		closedRangeCheck(atoms, 0, kodkodOptions.integers().max());
		this.numberOfIndexAtoms = atoms;
	}
	
	/**
	 * Returns the recursion limit.
	 * @return this.recursionLimit
	 */
	public int recursionLimit() { return recursionLimit; } 
	
	/**
	 * Sets the recursion limit.
	 * @return this.recursionLimit' = limit
	 * @throws IllegalArgumentException - limit <= 0
	 */
	public void setRecursionLimit(int limit) { 
		closedRangeCheck(limit, 1, Integer.MAX_VALUE);
		this.recursionLimit = limit;
	}
	
	/**
	 * Returns the (modifiable) kodkod options.  Note that 
	 * modifying the bitwidth field of the returned object
	 * will modify this.numberOfContexts, if necessary, so 
	 * that it's never larger than this.kodkodOptions.integers().max().
	 * @return this.kodkodOptions
	 */
	public kodkod.engine.config.Options kodkodOptions() { 
		return kodkodOptions;
	}
	
	/**
	 * Returns the primordial concrete types (modifiable).
	 * @return this.primordialConcreteTypes
	 */
	public Set<TypeReference> primordialConcreteTypes() {
		return primordialConcreteTypes;
	}
	
	/**
	 * Returns the undefined type.
	 * @return this.undefinedType
	 */
	public TypeReference undefinedType() {
		return undefinedType;
	}
	
	/**
	 * Sets the undefined type.
	 * @effects this.undefinedType' = undefined
	 */
	public void setUndefinedType(TypeReference undefined) {
		this.undefinedType = undefined;
	}
	
	/**
	 * Returns the memory model used for analysis of concurrent code.
	 * @return this.memoryModel
	 */
	public MemoryModel memoryModel() { 
		return memoryModel;
	}
	
	/**
	 * Sets the memory model used for analysis of concurrent code.
	 * @effects this.memoryModel' = memoryModel
	 * @throws NullPointerException - memoryModel = null
	 */
	public void setMemoryModel(MemoryModel memoryModel) {
		this.memoryModel = memoryModel;
	}
	
	/**
	 * Returns true if the assert statements in code should be handled as 
	 * assumptions instead of assertions.  The default is false.
	 * @return this.assertsAreAssumptions
	 */
	public boolean assertsAreAssumptions() { return assertsAreAssumptions; }
	
	/**
	 * Sets the flag for handling the translation of assert statements. 
	 * @effects this.assertsAreAssumptions' = assertsAreAssumptions
	 */
	public void setAssertsAreAssumptions(boolean assertsAreAssumptions) { 
		this.assertsAreAssumptions = assertsAreAssumptions;
	}
	
	
	public String getEclipseProjectName() {
		return eclipseProjectName;
	}
	
	public void setEclipseProjectName(String eclipseProjectName) {
		this.eclipseProjectName = eclipseProjectName;
	}
	
	/**
	 * Returns a string view of these options.
	 * @return a string view of these options.
	 */
	public String toString() {
		final StringBuilder s = new StringBuilder();

		s.append("Options:\n");
		s.append(" loopUnrollDepth: " + loopUnrollDepth + "\n");
		s.append(" openWorldScopeSize: " + openWorldScopeSize + "\n");
		s.append(" numberOfIndexAtoms: " + numberOfIndexAtoms + "\n");
		s.append(" recursionLimit: " + recursionLimit + "\n");
		s.append(" primordialConcreteTypes: " + primordialConcreteTypes + "\n");
		s.append(" undefinedType: " + undefinedType + "\n");
		s.append(" kodkodOptions: " + kodkodOptions + "\n");
		s.append(" memoryModel: " + memoryModel + "\n");
		s.append(" assertsAreAssumptions: " + assertsAreAssumptions + "\n");
		return s.toString();
	}
}
