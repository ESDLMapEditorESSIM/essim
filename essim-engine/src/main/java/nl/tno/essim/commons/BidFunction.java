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

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

public class BidFunction {
	private static final double eps = 1e-3;
	private static final double pmin = 0.0;
	private static final double pmax = 1.0;

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

	public double findEquillibrium() {
		return findPriceFromCurve(curve, 0.0);
	}

	public double findDemandFromCurve(double price) {
		return findDemandFromCurve(curve, price);
	}

	public static double findDemandFromCurve(TreeMap<Double, Double> curve, double price) {
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

	public double findPriceFromCurve(double bidPoint) {
		return findPriceFromCurve(curve, bidPoint);
	}

	public static double findPriceFromCurve(TreeMap<Double, Double> curve, double bidPoint) {
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
			return p1 + (((p2 - p1) / (v2 - v1)) * (bidPoint - v1));
		}
	}

	public void normaliseCurve(double marginalCostSum) {
		if (marginalCostSum - marginalCost < eps) {
			return;
		}
		TreeMap<Double, Double> duplicateCurve = new TreeMap<Double, Double>(curve);
		for (double p : curve.keySet()) {
			double e = curve.get(p);
			if (p != pmin && p != pmax) {
				duplicateCurve.remove(p);
				duplicateCurve.put(p / marginalCostSum, e);
			}
		}
		curve = duplicateCurve;
	}

}
