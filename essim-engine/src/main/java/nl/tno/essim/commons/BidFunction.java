/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.essim.commons;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

public class BidFunction {
	private static final double eps = 1e-22;
	private static final double pmin = 0.0;
	private static final double pmax = 1.0;
	private static final double factor = 1E6;

	@Getter
	@Setter
	private TreeMap<Double, Double> curve;
	@Getter
	@Setter
	private double marginalCost;

	public BidFunction() {
		curve = new TreeMap<Double, Double>();
		marginalCost = 0.0;
	}

	public BidFunction(TreeMap<Double, Double> curve) {
		this.curve = curve;
		marginalCost = 0.0;
	}

	public BidFunction addPoint(double price, double bid) {
		curve.put(price, bid);
		return this;
	}

	public boolean isEmpty() {
		return curve != null && curve.isEmpty();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BidFunction) {
			BidFunction other = (BidFunction) obj;
			return other.curve.equals(this.curve) && other.marginalCost == this.marginalCost;
		}
		return false;
	}

	@Override
	public String toString() {
		return "[curve=" + curve + "; mc=" + marginalCost + "]";
	}

	public BidFunction sumWith(BidFunction otherCurve) {
		TreeMap<Double, Double> sum = new TreeMap<Double, Double>();
		TreeSet<Double> allPrices = new TreeSet<Double>();
		allPrices.addAll(curve.keySet());
		allPrices.addAll(otherCurve.getCurve().keySet());
		for (double price : allPrices) {
			double aValue = findDemandFromCurve(curve, price);
			double bValue = findDemandFromCurve(otherCurve.getCurve(), price);
			sum.put(price, aValue + bValue);
		}

		return this;
	}

	public static BidFunction sumCurves(BidFunction firstCurve, BidFunction otherCurve) {
		TreeMap<Double, Double> summedCurve = new TreeMap<Double, Double>();
		TreeSet<Double> allPrices = new TreeSet<Double>();
		allPrices.addAll(firstCurve.getCurve().keySet());
		allPrices.addAll(otherCurve.getCurve().keySet());
		for (double price : allPrices) {
			double aValue = findDemandFromCurve(firstCurve.getCurve(), price);
			double bValue = findDemandFromCurve(otherCurve.getCurve(), price);
			summedCurve.put(price, aValue + bValue);
		}

		return new BidFunction(summedCurve);
	}

	public double findDemandFromCurve(double price) {
		return findDemandFromCurve(curve, price);
	}

	public BigDecimal findDemandFromCurveBD(BigDecimal price) {
		return findDemandFromCurveBD(curve, price);
	}

	public static double findDemandFromCurve(TreeMap<Double, Double> curve, double price) {
		// Choose here which precision is required
		return findDemandFromCurveFactor(curve, price);
	}

	/**
	 * Calculates the demand at the input price using interpolation utilizing regular double calculation.
	 * (the 64-bit (constrainted) precision of a Java double (max 15/16 digits)).
	 * @param curve
	 * @param price
	 * @return
	 */
	public static double findDemandFromCurveDouble(TreeMap<Double, Double> curve, double price) {
		if (curve.isEmpty()) {
			return 0.0;
		}

		if (curve.containsKey(price)) {
			return curve.get(price);
		}

		double p1 = Double.NaN;
		double p2 = Double.NaN;
		double v1 = Double.NaN;
		double v2 = Double.NaN;
		for (double p : curve.keySet()) {
			if (p > price) {
				p2 = p;
				v2 = curve.get(p2);
				break;
			}
			p1 = p;
			v1 = curve.get(p1);
		}

		if (Double.isNaN(p1)) {
			return v2;
		} else if (Double.isNaN(p2)) {
			return v1;
		} else {
			return v1 + (((v2 - v1) / (p2 - p1)) * (price - p1));
		}
	}
	
	/**
	 * Calculated the demand from a price in the BidFunction, but before calculating it
	 * divides the energy axis by a factor of 1E6 (J to MJ) to deal with the precision constraints of a
	 * 64-bit double value, when a large number is divided by a small number in this algorithm.<br/>  
	 * As ESSIM always works internally in J and usual values are MJ for households, this seems a smart fix for
	 * dealing with precision issues. Alternatives are using BigDecimals or don't deal with it...
	 * @param curve
	 * @param price
	 * @return
	 */
	public static double findDemandFromCurveFactor(TreeMap<Double, Double> curve, double price) {
		if (curve.isEmpty()) {
			return 0.0;
		}

		if (curve.containsKey(price)) {
			return curve.get(price);
		}

		double p1 = Double.NaN;
		double p2 = Double.NaN;
		double v1 = Double.NaN;
		double v2 = Double.NaN;
		for (double p : curve.keySet()) {
			if (p > price) {
				p2 = p;
				v2 = curve.get(p2) / factor;
				break;
			}
			p1 = p;
			v1 = curve.get(p1) / factor;
		}

		if (Double.isNaN(p1)) {
			return v2 * factor;
		} else if (Double.isNaN(p2)) {
			return v1;
		} else {
			return (v1 + (((v2 - v1) / (p2 - p1)) * (price - p1))) * factor;
		}
	}
	
	/**
	 * Calculates the demand from the input price value using interpolation of the points in the BidFunction and utilizing
	 * BigDecimals in this calculation to make sure that the algorithm does not throw away required precision,
	 * compared to a 64-bit double. It calculates with 32 decimals (double = max 15/16 decimals)
	 * @param curve
	 * @param bidPoint
	 * @return
	 */
	public static BigDecimal findDemandFromCurveBD(TreeMap<Double, Double> curve, BigDecimal price) {
		if (curve.isEmpty()) {
			return new BigDecimal(0.0);
		}
		
		if (curve.containsKey(price.doubleValue())) {
			return new BigDecimal(curve.get(price.doubleValue()));
		}
		
		double p1 = Double.NaN;
		double p2 = Double.NaN;
		double v1 = Double.NaN;
		double v2 = Double.NaN;
		for (double p : curve.keySet()) {
			if (p > price.doubleValue()) {
				p2 = p;
				v2 = curve.get(p2);
				break;
			}
			p1 = p;
			v1 = curve.get(p1);
		}
		
		if (Double.isNaN(p1)) {
			return new BigDecimal(v2);
		} else if (Double.isNaN(p2)) {
			return new BigDecimal(v1);
		} else {
			double result = v1 + (((v2 - v1) / (p2 - p1)) * (price.doubleValue() - p1));
			
			var bdV1 = new BigDecimal(v1);
			var bdV2 = new BigDecimal(v2);
			var bdP1 = new BigDecimal(p1);
			var bdP2 = new BigDecimal(p2);
			var v2MinV1 = bdV2.subtract(bdV1);
			var p2MinP1 = bdP2.subtract(bdP1);
			var priceMinP1 = price.subtract(bdP1);
			
			var bdResult = bdV1.add(v2MinV1.divide(p2MinP1, MathContext.DECIMAL32).multiply(priceMinP1));
			System.out.println("findDemandFromCurveBD Original    : " + result );
			System.out.println("findDemandFromCurveBD BD result   : " + bdResult);
			System.out.println("findDemandFromCurveBD BD as double: " + bdResult.doubleValue());
			
			return bdResult;
		}
	}

	

	public double findEquillibrium() {
		return findPriceFromCurve(curve, 0.0);
	}
	
	public double findPriceFromCurve(double bidPoint) {
		return findPriceFromCurve(curve, bidPoint);
	}

	public BigDecimal findPriceFromCurveBD(BigDecimal bidPoint) {
		return findPriceFromCurveBD(curve, bidPoint);
	}

	public static double findPriceFromCurve(TreeMap<Double, Double> curve, double bidPoint) {
		// Choose here which precision is required
		
		//return findPriceFromCurveBD(curve, BigDecimal.valueOf(bidPoint)).doubleValue();
		return findPriceFromCurveFactor(curve, bidPoint);
	}

	/**
	 * Calculates the price from the input demand using interpolation technique and utilizing regular double calculation.
	 * (the 64-bit (constrainted) precision of a Java double (max 15 digits)).
	 * @param curve
	 * @param price
	 * @return
	 */
	public static double findPriceFromCurveDouble(TreeMap<Double, Double> curve, double bidPoint) {
		for (Entry<Double, Double> entry : curve.entrySet()) {
			if (Math.abs(entry.getValue() - bidPoint) < eps) {
				return entry.getKey();
			}
		}

		double p1 = Double.NaN;
		double p2 = Double.NaN;
		double v1 = Double.NaN;
		double v2 = Double.NaN;
		for (double p : curve.keySet()) {
			double v = curve.get(p);
			if (v < bidPoint) {
				p2 = p;
				v2 = v;
				break;
			}
			p1 = p;
			v1 = v;
		}

		if (Double.isNaN(p1)) {
			return p2;
		} else if (Double.isNaN(p2)) {
			return p1;
		} else {
			return p1 + ((p2 - p1)  * ((bidPoint - v1) / (v2 - v1)));
		}
	}
	
	
	/**
	 * Calculated the price from a demand in the BidFunction, but before calculating it
	 * divides the energy axis by a factor of 1E6 (J to MJ) to deal with the precision constraints of a
	 * 64-bit double value, when a large number is divided by a small number in this algorithm.  
	 * @param curve
	 * @param bidPoint demand at that point
	 * @return
	 */
	public static double findPriceFromCurveFactor(TreeMap<Double, Double> curve, double bidPoint) {
		double demand = bidPoint / factor;
		
		for (Entry<Double, Double> entry : curve.entrySet()) {
			if (Math.abs(entry.getValue() - bidPoint) < eps) {
				return entry.getKey();
			}
		}

		double p1 = Double.NaN;
		double p2 = Double.NaN;
		double v1 = Double.NaN;
		double v2 = Double.NaN;
		for (double p : curve.keySet()) {
			double v = curve.get(p) / factor;
			if (v < demand) {
				p2 = p;
				v2 = v;
				break;
			}
			p1 = p;
			v1 = v;
		}

		if (Double.isNaN(p1)) {
			return p2;
		} else if (Double.isNaN(p2)) {
			return p1;
		} else {
			return p1 + ((p2 - p1)  * ((demand - v1) / (v2 - v1)));
		}
	}
	
	
	/**
	 * Calculates the price from the demand using interpolation of the points in the BidFunction and utilizing
	 * BigDecimals in this calculation to make sure that the algorithm does not throw away required precision,
	 * compared to a 64-bit double. It calculates with 32 decimals (double = max 15/16 decimals)
	 * @param curve
	 * @param bidPoint
	 * @return
	 */
	public static BigDecimal findPriceFromCurveBD(TreeMap<Double, Double> curve, BigDecimal bidPoint) {
		for (Entry<Double, Double> entry : curve.entrySet()) {
			if (Math.abs(entry.getValue() - bidPoint.doubleValue()) < eps) {
				return new BigDecimal(entry.getKey());
			}
		}
		
		double p1 = Double.NaN;
		double p2 = Double.NaN;
		double v1 = Double.NaN;
		double v2 = Double.NaN;
		for (double p : curve.keySet()) {
			double v = curve.get(p);
			if (v < bidPoint.doubleValue()) {
				p2 = p;
				v2 = v;
				break;
			}
			p1 = p;
			v1 = v;
		}
		
		if (Double.isNaN(p1)) {
			return new BigDecimal(p2);
		} else if (Double.isNaN(p2)) {
			return new BigDecimal(p1);
		} else {
			var normalResult = p1 + ((p2 - p1)  * ((bidPoint.doubleValue() - v1) / (v2 - v1)));
			
			// BigDecimal
			BigDecimal bdP1 = BigDecimal.valueOf(p1);
			BigDecimal bdP2 = BigDecimal.valueOf(p2);
			BigDecimal bdV1 = BigDecimal.valueOf(v1);
			BigDecimal bdV2 = BigDecimal.valueOf(v2);
			var p2Minp1 = bdP2.subtract(bdP1);
			var bpMinV1 = bidPoint.subtract(bdV1);
			var v2MinV1 = bdV2.subtract(bdV1);
			
			BigDecimal result = bdP1.add(p2Minp1.multiply(bpMinV1.divide(v2MinV1, MathContext.DECIMAL32)));
			
			System.out.println("findPriceFromCurveBD Normal      : " + normalResult);
			System.out.println("findPriceFromCurveBD BD res      : " + result);
			System.out.println("findPriceFromCurveBD BD as double: " + result.doubleValue());
			return result;
			
		}
	}

	public void normaliseCurve(double marginalCostSum) {
		if (marginalCostSum - marginalCost < eps) {
			return;
		}

		TreeMap<Double, Double> duplicateCurve = new TreeMap<Double, Double>();

		for (double p : curve.keySet()) {
			double e = curve.get(p);
			duplicateCurve.put(Math.min(Math.max(p / marginalCostSum, pmin), pmax), e);
			if (duplicateCurve.containsKey(pmax)) {
				break;
			}
		}

		curve = duplicateCurve;
	}

}
