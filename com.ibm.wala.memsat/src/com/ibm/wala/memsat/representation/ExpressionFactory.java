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
package com.ibm.wala.memsat.representation;

import static com.ibm.wala.memsat.frontEnd.IRType.BOOLEAN;
import static com.ibm.wala.memsat.frontEnd.IRType.INTEGER;
import static com.ibm.wala.memsat.frontEnd.IRType.OBJECT;
import static com.ibm.wala.memsat.frontEnd.IRType.REAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntExpression;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;

import com.ibm.wala.cast.java.ipa.callgraph.AstJavaSSAPropagationCallGraphBuilder.EnclosingObjectReferenceKey;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.modref.ArrayLengthKey;
import com.ibm.wala.memsat.Options;
import com.ibm.wala.memsat.frontEnd.IRType;
import com.ibm.wala.memsat.frontEnd.WalaCGNodeInformation;
import com.ibm.wala.memsat.frontEnd.WalaInformation;
import com.ibm.wala.memsat.util.Strings;
import com.ibm.wala.types.TypeReference;
/**
 * A factory for generating Kodkod Expressions that 
 * represent Wala values, fields, and arrays.  These
 * are unknowns in the sense that their relational values
 * are not known a priori and have to be solved for.  They include
 * field values and initial arguments to top level methods.
 * 
 * @specfield info: WalaInformation // info used for constructing representations of instance keys, etc.
 * @specfield options: Options // options used for determining bitwidth for int representation, etc.
 * @specfield constants: ConstantFactory
 * @specfield fields: info.relevantFields() ->one HeapExpression
 * @specfield systemHashCode: FieldExpression<IntExpression> // binary field that models the system hashcode values
 * @specfield arguments: CGNode -> seq[ Node + RealExpression ]
 * 
 * @invariant all f: info.relevantFields() & ArrayContentsKey | fields[f] in FieldExpression
 * @invariant all f: info.relevantFields() - ArrayContentsKey | fields[f] in ArrayExpression
 * @invariant constants.info = this.info && constants.options = this.options  
 * 
 * @author Emina Torlak
 */
public final class ExpressionFactory {
	private final WalaInformation info; 
	private final Options options;

	private final ConstantFactory constants;
	private final Map<PointerKey, HeapExpression<?>> fields;
	private final FieldExpression<IntExpression> hash;	
	private final Map<CGNode, Relation[]> arguments;
	
	/**
	 * Constructs an expression factory from the given info and options
	 * @requires info.options = options
	 * @effects this.info' = info and this.options' = options
	 */
	public ExpressionFactory(WalaInformation info, Options options) {
		this.info = info;
		this.options = options;		
		this.constants = new ConstantFactory(info,options);
		
		final Collection<? extends Expression> setExprs = constants.setExpressions();
		this.hash = FieldExpression.field("systemHashCode",
				setExprs.isEmpty() ? Expression.NONE : Expression.union(setExprs),
				constants.intInterpreter());
		this.fields = new LinkedHashMap<PointerKey, HeapExpression<?>>();
	
		final int card = options.numberOfIndexAtoms();
		for(Entry<PointerKey, String> entry : Strings.pointerNames(info.relevantFields()).entrySet()) {
			PointerKey key = entry.getKey();
			String name = entry.getValue();
			if (key instanceof ArrayContentsKey) {
				TypeReference declType = ((ArrayContentsKey)key).getInstanceKey().getConcreteType().getReference();
				IRType valType = IRType.convert(declType.getArrayElementType());
				Expression dom = constants.valueOf(((ArrayContentsKey)key).getInstanceKey());
				fields.put(key, ArrayExpression.array(name, card, dom, 
						constants.intInterpreter(),
						constants.interpreter(valType)));
			
			} else if (key instanceof InstanceFieldKey) {
				InstanceFieldKey instanceField = (InstanceFieldKey) key;
				IRType valType = IRType.convert(instanceField.getField().getFieldTypeReference());
					fields.put(key, FieldExpression.field(name,
							constants.valueOf(instanceField.getInstanceKey()),
							constants.interpreter(valType)));
			} else if (key instanceof StaticFieldKey) {
				StaticFieldKey staticField = (StaticFieldKey) key;
				IRType valType = IRType.convert(staticField.getField().getFieldTypeReference());
				fields.put(key, FieldExpression.field(name,null,constants.interpreter(valType)));
			}else if (key instanceof ArrayLengthKey) {
				fields.put(key, FieldExpression.field(name,
						constants.valueOf(((ArrayLengthKey)key).getInstanceKey()),
						constants.intInterpreter()));
			
			} else if (key instanceof EnclosingObjectReferenceKey) {
				fields.put(key, FieldExpression.field(name,
						constants.valueOf(((EnclosingObjectReferenceKey)key).getInstanceKey()),
						constants.objInterpreter()));
			
			} else {
				throw new AssertionError("unrecognized field pointer key: " + key);
			}
		}
		
		this.arguments = new LinkedHashMap<CGNode, Relation[]>();	
		for(CGNode node : info.threads()) { 
			WalaCGNodeInformation nodeInfo = info.cgNodeInformation(node);
			Relation[] args = new Relation[node.getMethod().getNumberOfParameters()];
			for(int j = 0; j < args.length; j++) { 
				Interpreter<?> argInterpreter  = constants.interpreter(nodeInfo.typeOf(j+1));
				args[j] = Relation.nary(node.getMethod().getName()+"_arg"+j, argInterpreter.defaultObj().arity());
			}
			arguments.put(node, args);
		}
		
//		System.out.println(toString());
	}
	
	/**
	 * Returns this.info.
	 * @return this.info
	 */
	public WalaInformation info() { return info; }

	/**
	 * Returns this.options.
	 * @return this.options
	 */
	public Options options()  { return options; }
	
	/**
	 * Returns the (open world) arugments for the given entry-point node.
	 * @requires some i: [0..this.info.threads()) | this.info.entry(i) = node
	 * @return open world arugments for the given entry-point node
	 */
	public Object[] arguments(CGNode node) { 
		final Relation[] rawArgs = arguments.get(node);
		final Object[] args = new Object[rawArgs.length];
		final WalaCGNodeInformation nodeInfo = info.cgNodeInformation(node);
		for(int i = 0; i < rawArgs.length; i++) { 
			Interpreter<?> argInterpreter  = constants.interpreter(nodeInfo.typeOf(i+1));
			args[i] = argInterpreter.fromObj(rawArgs[i]);
		}
		return args;
	}

	
	/**
	 * Returns an empty PhiExpression of the given type.
	 * @return { phi: PhiExpression | no phi.phis and 
	 *  [[ phi.phis[Formula] ]] in [[ type ]] } 
	 */
	@SuppressWarnings("unchecked")
	public <T> PhiExpression<T> valuePhi(IRType type) { 
		return PhiExpression.valuePhi((Interpreter<T>)constants.interpreter(type));
	}
	
	/**
	 * Returns a PhiExpression initialized with the given
	 * guard / heap expression pair.  All subsequent calls to 
	 * {@linkplain #add(Formula, Object)} on the returned
	 * phi expression must be passed a HeapExpression with 
	 * the same <tt>walaField</tt> as the given heap expression.
	 * @return a PhiExpression initialized with the given
	 * guard / heap value pair
	 */
	public <T> PhiExpression<HeapExpression<T>> heapPhi(Formula guard, HeapExpression<T> heapExpr) { 
		return PhiExpression.heapPhi(guard, heapExpr);
	}
	
	/**
	 * Returns the constant factory used by this expression factory
	 * to manage constant-valued Wala expressions (primitives and instances).
	 * @return this.constants
	 */
	public ConstantFactory constants() { return constants; }
	
	
	/**
	 * Returns a field or array expression that models the initial heap value 
	 * represented by the given key.  The returned object
	 * is an instance of ArrayExpression if the given key is
	 * an ArrayContentsKey, otherwise it is an instance of FieldExpression.
	 * The parameter type of the returned expression's contents is determined by the
	 * type of the key's points-to set (e.g. if key is an InstanceFieldKey
	 * for a boolean field, then the returned expression is an instance of
	 * FieldExpression<Formula>, etc.).
	 * @requires key in this.info.relevantFields()
	 * @return this.fields[key]
	 */
	@SuppressWarnings("unchecked")
	public final <H extends HeapExpression<?>> H initValueOf(PointerKey key) {
		return (H)fields.get(key);
	}
	
	/**
	 * Returns a field expression that models the system hashcode function.
	 * @return a field expression that models the system hashcode function.
	 */
	public final FieldExpression<IntExpression> systemHashCode() { return hash; }
		
	/*------------------ REP INVARIANTS ------------------ */
	/**
	 * Returns a formula that expresses the representation
	 * invariants over all Kodkod relations that make up 
	 * the expressions generated by this factory.
	 * @return a formula that expresses the representation
	 * invariants over all Kodkod relations that make up 
	 * the expressions generated in this factory
	 */
	public final Formula invariants() { 
		
		final List<Formula> formulas = new ArrayList<Formula>();
		final Map<ArrayContentsKey,FieldExpression<IntExpression>> lengths = lengths();
//		System.out.println("lengths: " + lengths);

		for(Map.Entry<PointerKey, HeapExpression<?>> entry : fields.entrySet()) { 
			final HeapExpression<?> heapExpr = entry.getValue();
			
			if (heapExpr.isArray()) { 
				final InstanceKey dom = ((ArrayContentsKey)entry.getKey()).getInstanceKey();
				// only add rep invariants for non-empty arrays in the open world
				if (info.openWorldType(dom)) {
					final ArrayExpression<?> array = (ArrayExpression<?>) heapExpr;
					if (array.cardinality()>0) {  
						final FieldExpression<IntExpression> length = lengths.get(entry.getKey());
						formulas.add(invariants(constants.openValuesOf(dom), array, length));
					}
				}
			} else { 
				final FieldExpression<?> field = (FieldExpression<?>)heapExpr;
				formulas.add(invariants(field));
			}
		}
		
		// argument constraints -- constraint cardinality of refs and bools to one
		for(Map.Entry<CGNode, Relation[]> entry : arguments.entrySet()) { 
			WalaCGNodeInformation nodeInfo = info.cgNodeInformation(entry.getKey());
			Relation[] args = entry.getValue();
			for(int i = 0; i < args.length; i++) { 
				if (constants.interpreter(nodeInfo.typeOf(i+1)).singletonEncoding()) { 
					formulas.add(args[i].one());
				}
			}
		}
		
		return Formula.and(formulas);
 
	}
	
	/**
	 * Returns representation invariants for the given open-world instances of the given array expression and 
	 * its corresponding length field
	 * @requires some [[openWorldValues]] and array.cardinality() > 0
	 * @requires [[openWorldValues]] in [[array.instances()]]
	 * @requires array.getInstanceKey() = length.getInstanceKey();
	 * @return representation invariants for the given open-world instances of the given array expression and 
	 * its corresponding length field
	 */
	private Formula invariants(Expression openWorldValues, ArrayExpression<?> array, FieldExpression<IntExpression> length) { 
		final int card = array.cardinality();
			
		final List<Formula> aInvs = new ArrayList<Formula>(2*card+1);
		final Variable a = Variable.unary("a"+System.identityHashCode(array));

		// index constraints
		for(int i = 0, max = card-1; i < max; i++) { 
			final IntExpression iprev = array.index(a, i);
			final IntExpression inext = array.index(a, i+1);
			final Expression vprev = a.join(array.value(i));
			final Expression vnext = a.join(array.value(i+1));
			aInvs.add(iprev.lt(inext).or(iprev.eq(inext).and(vprev.eq(vnext))));
		}
		
		final IntExpression aLength = length.read(a);
		final IntExpression aLast = array.index(a, card-1);
		
		aInvs.add((aLast.lt(aLength)).or
				  (aLast.eq(aLength).and(aLength.eq(IntConstant.constant(0)))));
		
		// value constraints (function if values are references or booleans)
		if (array.valueInterpreter().singletonEncoding()) { 
			for(int i = 0; i < card; i++) { 
				aInvs.add(a.join(array.value(i)).one());
			}
		}
		
		return Formula.and(aInvs).forAll(a.oneOf(openWorldValues));
	}
	
	/**
	 * @return representation invariants for the given field
	 */
	private Formula invariants(FieldExpression<?> field) { 
		if (field.valueInterpreter().singletonEncoding()) { 
			if (field.isStatic()) { 
				return field.field().one();
			} else {
				Variable v = Variable.unary("v"+System.identityHashCode(field));
				return v.join(field.field()).one().forAll(v.oneOf(field.instances().difference(constants.nil())));
			}
		}
		return Formula.TRUE;
	}
	
	/** @return map from each array instance key in this.fields to its corresponding length field */
	@SuppressWarnings("unchecked")
	private Map<ArrayContentsKey, FieldExpression<IntExpression>> lengths() { 
		final Map<ArrayContentsKey, FieldExpression<IntExpression>> ret = new LinkedHashMap<ArrayContentsKey, FieldExpression<IntExpression>>();
		final Map<InstanceKey,ArrayContentsKey> m = new LinkedHashMap<InstanceKey, ArrayContentsKey>();
		for(PointerKey key : fields.keySet()) { 
			if (key instanceof ArrayContentsKey) {
				m.put(((ArrayContentsKey)key).getInstanceKey(), (ArrayContentsKey)key);
			}
		}
		for(PointerKey key : fields.keySet()) { 
			if (key instanceof ArrayLengthKey) {
				ret.put(m.get(((ArrayLengthKey)key).getInstanceKey()), (FieldExpression<IntExpression>)fields.get(key));
			}
		}
		return ret;
	}
	/**
	 * Returns an ordered set of atom objects needed to represent all primitives
	 * generated by this factory.  In particular, the first two atoms represent
	 * the values "true" and "false", 
	 * the next <tt>this.options.bitsForIntegers()</tt> atoms are Integer objects 
	 * representing the powers two (in the increasing
	 * order of absolute values) needed to represent all <tt>options.bitsForIntegers()</tt>-bit integers
	 * in two's complement; the next atom represents the null value, and the remaining
	 * atoms uniquely represent the instances of this.info.relevantClasses().
	 * All atoms except Integers are Kodkod relations. 
	 * @return an ordered set of atom objects needed to represent all primitives
	 * generated by this factory
	 */
	public Set<?> atoms() { 
		return constants.atoms();
	}
	
	/*------------------ BOUNDS ------------------ */
	/**
	 * Updates the given Bounds instance with 
	 * upper/lower bounds for the relations that make up
	 * the expressions generated by this factory and this.constants.  The bounds
	 * are constructed using the bitwidth, set cardinality, etc.,
	 * values given by this.options and this.info.
	 * @requires this.atoms() in bounds.universe.atoms[int]
	 * @effects updates the given Bounds instance with 
	 * upper/lower bounds for the relations that make up
	 * the expressions generated by this factory and this.constants
	 */
	public void  boundAll(Bounds bounds) {
		final TupleFactory f = bounds.universe().factory();
		
		constants.boundAll(bounds);
		
		final EnumMap<IRType,TupleSet> primitives = new EnumMap<IRType, TupleSet>(IRType.class);
		primitives.put(BOOLEAN, constants.constantAtoms(f, BOOLEAN));
		primitives.put(INTEGER, constants.constantAtoms(f, INTEGER));
		primitives.put(REAL, constants.constantAtoms(f, REAL));
		primitives.put(OBJECT, constants.constantAtoms(f, OBJECT));
		
		final EnumMap<IRType,TupleSet> defaults = new EnumMap<IRType, TupleSet>(IRType.class);
		defaults.put(BOOLEAN, constants.defaultAtoms(f, BOOLEAN));
		defaults.put(INTEGER, constants.defaultAtoms(f, INTEGER));
		defaults.put(REAL, constants.defaultAtoms(f, REAL));
		defaults.put(OBJECT, constants.defaultAtoms(f, OBJECT));
		
		// bound arguments
		for(Map.Entry<CGNode, Relation[]> entry : arguments.entrySet()) { 
			WalaCGNodeInformation nodeInfo = info.cgNodeInformation(entry.getKey());
			Relation[] args = entry.getValue();
			for(int i = 0; i < args.length; i++) { 
				bounds.bound(args[i], range(f, nodeInfo, i+1, primitives));
			}
		}
		
		// bound system hashcode
		bounds.bound((Relation)hash.field(), constants.instanceAtoms(f, info.relevantClasses()).product(primitives.get(INTEGER)));

		// bound arrays and fields
		final TupleSet nats = naturals(primitives.get(INTEGER));
		
		for(Map.Entry<PointerKey, ?> entry : fields.entrySet()) { 
			final PointerKey key = entry.getKey();
			if (key instanceof ArrayContentsKey) {
				final InstanceKey domKey = ((ArrayContentsKey) key).getInstanceKey();
				final ArrayExpression<?> array = (ArrayExpression<?>)entry.getValue();
				final IRType valType = array.valueInterpreter().type();
				final TupleSet closedDom = constants.closedInstanceAtoms(f, domKey);
				final TupleSet openDom = constants.openInstanceAtoms(f, domKey);
				final TupleSet uidx = union(closedDom, openDom).product(nats);
				final TupleSet lval = closedDom.product(defaults.get(valType));
				final TupleSet uval = union(lval, openDom.product(range(f,key,valType,primitives)));
				for(int i = 0, max = array.cardinality(); i < max; i++) { 
					bounds.bound((Relation)array.index(i), uidx);
					bounds.bound((Relation)array.value(i), lval, uval);
				}
			} else {
				final FieldExpression<?> field = (FieldExpression<?>)entry.getValue();
				
				if (field.isStatic()) { // static fields are set to default values
					bounds.bound((Relation)field.field(), defaults.get(field.valueInterpreter().type()));
				} else {
					final InstanceKey domKey = ((InstanceFieldPointerKey) key).getInstanceKey();
					if (key instanceof EnclosingObjectReferenceKey) {
					    final TupleSet dom = constants.instanceAtoms(f, domKey);
					    final TupleSet uval = constants.instanceAtoms(f, info.pointsTo(key));
						bounds.bound((Relation)field.field(), dom.product(uval));
					} else if (key instanceof ArrayLengthKey) { 
						// don't need to bound closed-world instances since they map to 0 (the empty set)
						final TupleSet openDom = constants.openInstanceAtoms(f, domKey);
						bounds.bound((Relation)field.field(), openDom.product(nats));
					} else {
						final IRType valType = field.valueInterpreter().type();
						final TupleSet closedDom = constants.closedInstanceAtoms(f, domKey);
					    final TupleSet lval = closedDom.product(defaults.get(valType));
					    
					    final TupleSet openDom = constants.openInstanceAtoms(f, domKey);
					    final TupleSet uval = union(lval, openDom.product(range(f, key, valType, primitives)));
					   
						bounds.bound((Relation)field.field(), lval, uval);
					}
				}
			}
		}
		
//		System.out.println("BOUNDS: "  + bounds);
	}
	
	/**
	 * Returns the non-negative subset of the given set of integers.
	 * @requires ints = constants.constantAtoms(INTEGER)
	 * @return non-negative subset of the given set of integers.
	 */
	private TupleSet naturals(TupleSet ints) { 
		final TupleSet nats = ints.clone();
		nats.remove(ints.universe().factory().tuple(-(1<<(options.kodkodOptions().bitwidth()-1))));
		return nats;
	}
	
	/**
	 * @requires this.info.pointsTo(key) in this.info.relevantClasses()
	 * @return upper bound on the points-to set of the given key (includes null)
	 **/
	private TupleSet range(TupleFactory factory, PointerKey key, IRType type, EnumMap<IRType, TupleSet> primitives) { 
		if (type==OBJECT) {
			final TupleSet instances = constants.instanceAtoms(factory, info.pointsTo(key));
			instances.addAll(primitives.get(OBJECT));
			return instances;
		} else {
			return primitives.get(type);
		}
	}
	
	/**
	 * @return upper bound on the points-to set of the given value number (includes null).
	 */
	private TupleSet range(TupleFactory factory, WalaCGNodeInformation nodeInfo, int valueNumber, EnumMap<IRType, TupleSet> primitives) {
		return range(factory, nodeInfo.pointerKeyFor(valueNumber), nodeInfo.typeOf(valueNumber), primitives);
	}

	/**
	 * Returns a new TupleSet that is the union of the specified tuplesets.
	 * @requires sets.length > 0 and all t: sets[int] | t.arity = sets[0].arity
	 * @return a new TupleSet that is the union of the specified tuplesets.
	 */
	private TupleSet union(TupleSet...sets) {
		final TupleSet ret = sets[0].clone();
		for(int i = 1; i < sets.length; i++) { ret.addAll(sets[i]); }
		return ret;
	}

	
	/**
	 * Returns a string representation of this expression factory.
	 * @return a string representation of this expression factory
	 */
	public String toString() {
		final StringBuilder buf = new StringBuilder();
		buf.append("constants:\n");
		buf.append(constants);
		buf.append("\n");
		
		buf.append("fields:\n");
		for(Map.Entry<PointerKey,HeapExpression<?>> e : fields.entrySet()) { 
			buf.append(" ");
			buf.append(e.getKey());
			buf.append(" :: ");
			HeapExpression<?> val = e.getValue();
			if (val.isArray()) { 
				ArrayExpression<?> array = (ArrayExpression<?>) val;
				int card = array.cardinality();
				buf.append("{");
				for(int i = 0; i < card; i++) { 
					buf.append(" (");
					buf.append(array.index(i));
					buf.append(", ");
					buf.append(array.value(i));
					buf.append(")");
				}
				buf.append(" }");
			} else {
				buf.append(((FieldExpression<?>)val).field());
			}
			buf.append("\n");
		}
		
		buf.append("arguments:\n");
		for(Map.Entry<CGNode,Relation[]> e : arguments.entrySet()) { 
			buf.append(" ");
			buf.append(e.getKey().getMethod().getSignature());
			buf.append(" :: {");
			for(Relation r : e.getValue()) { 
				buf.append(" ");
				buf.append(r);
			}
			buf.append(" }\n");
		}
		
		return buf.toString();
	}
}
