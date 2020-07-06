package nl.tno.essim.commons;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import lombok.Getter;

public class BidFunction {
	private static final double eps = 1e-3;
	
	@Getter
	private TreeMap<Double, Double> curve;

	public BidFunction() {
		curve = new TreeMap<Double, Double>();
	}
	
	public BidFunction addPoint(double price, double bid) {
		curve.put(price, bid);
		return this;
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
	
	public double findEquillibrium() {
		return findPriceFromCurve(curve, 0.0);
	}
	
	public double findDemandFromCurve(double price) {
		return findDemandFromCurve(curve, price);
	}
	
	public double findDemandFromCurve(TreeMap<Double, Double> curve, double price) {
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
	
	public double findPriceFromCurve(TreeMap<Double, Double> curve, double bidPoint) {
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

}
