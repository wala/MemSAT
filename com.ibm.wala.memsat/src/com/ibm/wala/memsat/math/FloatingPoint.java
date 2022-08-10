package com.ibm.wala.memsat.math;

import com.ibm.wala.memsat.representation.RealExpression;
import com.ibm.wala.util.collections.Pair;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntExpression;

public class FloatingPoint {
	
	private static final int signMask = 0x80000000;
	
	private static final int exponentBits = 8;
	static final int exponentMask = 0x7F800000;
	static final int exponentBias = 127;
	
	static final int mantissaBits = 23;
	private static final int mantissaMask = 0x007FFFFF;
	
	private static final IntExpression implicitOne = IntConstant.constant(1).shl(IntConstant.constant(mantissaBits));

	static final IntConstant minusOne = IntConstant.constant(-1);

	 public static final IntConstant zero = IntConstant.constant(0);

	 public static final IntConstant one = IntConstant.constant(1);

	 public static final int bitWidth = 32;

	 
	 public static IntExpression maxSetBit(IntExpression intValue) {
	    IntExpression bit = IntConstant.constant(-1);
	    for(int i = 0; i < bitWidth-1; i++) {
	      bit = IntConstant.constant(0).eq(IntConstant.constant(1<<i).and(intValue)).not()
	        .thenElse(IntConstant.constant(i), bit);
	    }
	    return intValue.lt(IntConstant.constant(0)).thenElse(IntConstant.constant(bitWidth-1), bit);
	  }

	 public static IntExpression floatToInt(RealExpression floatValue) {
	   IntExpression bits = floatValue.intBits();
	   
	   IntExpression exp = exponent(bits);
	   exp = exp.minus(IntConstant.constant(127));
	   
	   IntExpression mantissa = mantissa(bits);
	   mantissa = mantissa.or(IntConstant.constant(1).shl(IntConstant.constant(mantissaBits)));
	   
	   IntExpression v = exp.gt(IntConstant.constant(0))
	     .thenElse(mantissa.shl(exp), 
	         mantissa.shr(zero.minus(exp)));
	   
	   v = sign(bits).gt(zero).thenElse(v.negate(), v);
	   
	   return v;
	 }
	 
	public static IntExpression intToFloat(IntExpression intValue) {
		IntExpression abs = intValue.abs();
		IntExpression maxSetBit = maxSetBit(abs);
		Formula shiftRight = maxSetBit.gt(IntConstant.constant(mantissaBits));
		
		IntExpression re = maxSetBit.minus(IntConstant.constant(mantissaBits));
		IntExpression adjustRight = adjustRight(abs, re);
		
		IntExpression le = IntConstant.constant(mantissaBits).minus(maxSetBit);
		IntExpression adjustLeft = abs.shl(le);

		IntExpression mantissa = 
			shiftRight.thenElse(adjustRight, adjustLeft).xor(implicitOne);
			
		IntExpression exponent = maxSetBit.plus(IntConstant.constant(exponentBias));
					
		IntExpression sign = intValue.lt(IntConstant.constant(0))
				.thenElse(IntConstant.constant(1<<(exponentBits+mantissaBits)), zero);

		return intValue.eq(zero)
			.thenElse(zero,
				sign.or(exponent.shl(IntConstant.constant(mantissaBits))).or(mantissa));
	}

	private static IntExpression adjustRight(IntExpression value, IntExpression shift) {
		IntExpression adjustRight = value.shr(shift);

		Formula LSB = adjustRight.and(one).eq(one);
		Formula guard = value.and(one.shl(shift.minus(one))).eq(zero).not();
		
		IntExpression xxx = value.shr(shift.minus(IntConstant.constant(1)));
		Formula sticky = value.minus(xxx.shl(shift.minus(one))).eq(zero).not();

		adjustRight = guard.and(LSB.or(sticky)).thenElse(adjustRight.plus(one), adjustRight);
		return adjustRight;
	}

	static IntExpression mantissa(IntExpression floatBits) {
		return floatBits.and(IntConstant.constant(mantissaMask)).or(implicitOne);		
	}
		
	static IntExpression exponent(IntExpression l) {
		return (l.and(IntConstant.constant(exponentMask)).shr(IntConstant.constant(mantissaBits))).minus(IntConstant.constant(exponentBias));
	}

	static IntExpression sign(IntExpression l) {
		return l.and(IntConstant.constant(signMask)).shr(IntConstant.constant(mantissaBits+exponentBits));
	}

	public static Formula isNaN(IntExpression l) {
	  return
	      (l.and(IntConstant.constant(exponentMask)).eq(IntConstant.constant(exponentMask)))
	        .and(l.and(IntConstant.constant(mantissaMask)).eq(IntConstant.constant(0)).not());
	}

	public static Formula isInfinite(IntExpression l) {
    return
        (l.and(IntConstant.constant(exponentMask)).eq(IntConstant.constant(exponentMask)))
        .and(l.and(IntConstant.constant(mantissaMask)).eq(IntConstant.constant(0)));
	}

	private static IntExpression floatAddInternal(IntExpression geq, IntExpression leq) {
		IntExpression le = exponent(geq);
		IntExpression re = exponent(leq);
		IntExpression shift = le.minus(re);
		
		IntExpression lv = mantissa(geq);
		IntExpression rv = adjustRight(mantissa(leq), shift);
		
		Formula add = sign(geq).eq(sign(leq));
		IntExpression sum = 
				add.thenElse(
						lv.plus(rv), 
						lv.gt(rv).thenElse(
							lv.minus(rv),
							rv.minus(lv)));

		IntExpression sign = add.thenElse(
			sign(geq), 
			lv.gt(rv).thenElse(sign(geq), sign(leq)));
		
		IntExpression start = maxSetBit(lv);
		IntExpression end = maxSetBit(sum);
		IntExpression shiftAnswer = end.minus(start);
		sum = shiftAnswer.gte(zero).thenElse(
			adjustRight(sum, shiftAnswer),
			lv.gt(rv).thenElse(
				lv.shl(shiftAnswer.abs()).minus(adjustRight(mantissa(leq), shift.plus(shiftAnswer))),
				adjustRight(mantissa(leq), shift.plus(shiftAnswer)).minus(lv.shl(shiftAnswer.abs()))));
						
		IntExpression exp = le.plus(shiftAnswer);
		
		sum = sum.and(IntConstant.constant(mantissaMask));
		
		return sign.or(exp.plus(IntConstant.constant(exponentBias)).shl(IntConstant.constant(mantissaBits))).or(sum);
	}
	
	public static IntExpression floatAdd(IntExpression l, IntExpression r) {
		return l.gte(r).thenElse(floatAddInternal(l, r), floatAddInternal(r, l));
	}

	public static IntExpression floatMinus(IntExpression l, IntExpression r) {
		return floatAdd(l, r.xor(IntConstant.constant(signMask)));	
	}

	public static IntExpression floatMinus(IntExpression u) {
		return floatMinus(IntConstant.constant(Float.floatToIntBits(0f)), u);
	}

	 private static Formula lsb(IntExpression l) {
	    return l.and(one).eq(zero).not();
	  }

	 private static Formula unsignedOverflow(IntExpression l, IntExpression r) {
	    Formula carryFromLSB = lsb(l).and(lsb(r));
	    IntExpression shiftedSum = l.shr(one).plus(r.shr(one));
	    shiftedSum = carryFromLSB.thenElse(shiftedSum.plus(one), shiftedSum);
	    return shiftedSum.and(one.shl(IntConstant.constant(31))).eq(zero).not();
	  }

	 public static Pair<IntExpression,IntExpression> fullUnsignedIntegerMultiply(IntExpression ml, IntExpression mr) {
	    IntExpression low = zero;
	    IntExpression high = zero;
	    
	    for(int i = 0; i < bitWidth; i++) {
	      // add with carry
	      high = 
	        mr.and(IntConstant.constant(1<<i)).eq(zero)
	          .thenElse(high, 
	            unsignedOverflow(low, ml.shl(IntConstant.constant(i)))
	              .thenElse(high.plus(one), high));
	      low = mr.and(IntConstant.constant(1<<i)).eq(zero).thenElse(low, low.plus(ml.shl(IntConstant.constant(i))));
	      if (i != 0) {
	        high = mr.and(IntConstant.constant(1<<i)).eq(zero).thenElse(high, high.plus(ml.shr(IntConstant.constant(bitWidth-i))));     
	      }
	    }
	    
	    return Pair.make(low, high);
	  }

	public static IntExpression floatMultiply(IntExpression l, IntExpression r) {
		IntExpression le = exponent(l);
		IntExpression re = exponent(r);
		IntExpression e = le.plus(re);
		
		IntExpression lv = mantissa(l);
		IntExpression rv = mantissa(r);
		Pair<IntExpression,IntExpression> p = fullUnsignedIntegerMultiply(lv, rv);
		IntExpression shift = IntConstant.constant(23).minus(maxSetBit(p.snd));
		IntExpression m = adjustRight(p.fst, IntConstant.constant(32).minus(shift)).or(p.snd.shl(shift));
		e = e.plus(IntConstant.constant(9).minus(shift));
		
		// hide implicit one
		m = m.and(IntConstant.constant(mantissaMask));
		
		IntExpression s = sign(l).eq(sign(r)).thenElse(zero, one);
		
		return l.eq(zero).or(r.eq(zero)).thenElse(zero, s.shl(IntConstant.constant(exponentBits+mantissaBits))
			.or(e.plus(IntConstant.constant(exponentBias)).shl(IntConstant.constant(mantissaBits)))
			.or(m));
	}
	
	public static IntExpression floatAbs(IntExpression e) {
		return e.and(IntConstant.constant(signMask).not());
	}
	
	/*
	private static double log2(double x) {
		return Math.log(x)/Math.log(2);
	}
	
	private static int newtonSteps(int P) {
		return (int) Math.ceil(log2((P + 1)/log2(17)));
	}
	*/
	
	public static IntExpression floatDivide(IntExpression l, IntExpression r) {
		IntExpression sign = sign(l).xor(sign(r));
		
		l = floatAbs(l);
		IntExpression le = exponent(l).minus(exponent(r).plus(one)).plus(IntConstant.constant(exponentBias));
		l = l.and(IntConstant.constant(exponentMask).not()).or(le.shl(IntConstant.constant(mantissaBits)));
		
		r = floatAbs(r);
		r = r.and(IntConstant.constant(exponentMask).not()).or(minusOne.plus(IntConstant.constant(exponentBias)).shl(IntConstant.constant(mantissaBits)));
		
		IntExpression xi = 
			floatMinus(
				IntConstant.constant(Float.floatToIntBits((float)48/(float)17)), 
				floatMultiply(
					IntConstant.constant(Float.floatToIntBits((float)32/(float)17)), 
					r));
		
		IntExpression two = IntConstant.constant(Float.floatToIntBits(2f));
		
		// find out why this does not work...
		//int steps = newtonSteps(bitWidth);
		for(int i = 0; i < 7; i++) {
			xi = floatMultiply(xi, floatMinus(two, floatMultiply(r, xi)));
		}
		
		IntExpression answer = floatMultiply(xi, l);
		return answer.and(IntConstant.constant(signMask).not()).or(sign.shl(IntConstant.constant(exponentBits+mantissaBits)));
	}
	
	private static IntExpression threeWayTest(IntExpression l, IntExpression r, IntExpression equalExpr) {
		return l.gt(r).thenElse(
			one, 
			l.lt(r).thenElse(
				minusOne, 
				equalExpr));
	}
	
	public static IntExpression floatCompare(IntExpression l, IntExpression r) {
		IntExpression ls = sign(l);
		IntExpression rs = sign(r);
		
		IntExpression le = exponent(l);
		IntExpression re = exponent(r);
		
		IntExpression lm = mantissa(l);
		IntExpression rm = mantissa(r);
		
		return
			threeWayTest(rs, ls,
				ls.eq(IntConstant.constant(0)).thenElse(
					threeWayTest(le, re,
						threeWayTest(lm, rm, zero)),
					threeWayTest(re, le,
						threeWayTest(rm, lm, zero))));
	}
}
