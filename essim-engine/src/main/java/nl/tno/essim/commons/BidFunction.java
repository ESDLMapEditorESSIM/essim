package nl.tno.essim.commons;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
public class BidFunction {
	private final static double MIN_PRICE = 0.0;
	private final static double MAX_PRICE = 1.0;
	public final static double MIN_DELTA_POWER = 10;
	public final static double MIN_DELTA_PRICE = 1e-6;

	@EqualsAndHashCode
	public static class BidCurvePoint implements Comparable<BidCurvePoint> {
		private BigDecimal price;
		private BigDecimal power;

		public BidCurvePoint(BigDecimal price, BigDecimal power) {
			this.price = price;
			this.power = power;
		}

		public BidCurvePoint(Double price, Double power) {
			this.price = new BigDecimal(price, MathContext.DECIMAL128);
			this.power = new BigDecimal(power, MathContext.DECIMAL128);
		}

		@Override
		public int compareTo(BidCurvePoint o) {
			return price.equals(o.price) ? o.power.compareTo(power) : price.compareTo(o.price);
		}

		@Override
		public String toString() {
			return "{" + price + ", " + power + "}";
		}

		public double getPrice() {
			return price.doubleValue();
		}

		public double getPower() {
			return power.doubleValue();
		}
	}

	private List<BidCurvePoint> list = new ArrayList<BidFunction.BidCurvePoint>();
	@Getter
	@Setter
	private double marginalCost = 0.0;

	public BidFunction(List<BidCurvePoint> list) {
		this.list = new ArrayList<BidFunction.BidCurvePoint>(list);
		verifyAndUpdateBitCurve();
	}

	public BidFunction() {
	}

	public BidFunction(TreeMap<Double, Double> points) {
		for (var point : points.entrySet()) {
			BidCurvePoint bidCurvePoint = new BidCurvePoint(point.getKey(), point.getValue());
			this.list.add(bidCurvePoint);
		}
		verifyAndUpdateBitCurve();
	}

	public BidFunction addPoint(double price, double bid) {
		BidCurvePoint bidCurvePoint = new BidCurvePoint(price, bid);
		this.list.add(bidCurvePoint);
		verifyAndUpdateBitCurve();
		return this;
	}

	private void verifyAndUpdateBitCurve() {
		Collections.sort(list);

		if (list.get(0).price.doubleValue() < MIN_PRICE) {
			// Rounding stuff
			if (list.get(0).price.abs().doubleValue() < MIN_DELTA_PRICE / 2) {
				list.get(0).price = new BigDecimal(0, MathContext.DECIMAL128);
			} else {
				throw new IllegalArgumentException("Invalid bidcurve: price < " + MIN_PRICE + ": " + list);
			}
		}
		// Now make sure we have a strictly decreasing function
		BidCurvePoint previous = null;
		for (var i = list.listIterator(list.size()); i.hasPrevious();) {
			BidCurvePoint bidCurvePoint = i.previous();
			if (previous != null) {

				if (previous.price.subtract(bidCurvePoint.price).abs().doubleValue() <= MIN_DELTA_PRICE) {
					if (previous.power.subtract(bidCurvePoint.power).abs().doubleValue() <= MIN_DELTA_POWER) {
						// We don't need double points
						i.remove();
						continue;
					}
					bidCurvePoint.price = previous.price
							.subtract(new BigDecimal(MIN_DELTA_PRICE, MathContext.DECIMAL128));
				}
				BigDecimal powerDelta = previous.power.subtract(bidCurvePoint.power);
				// Correct minor errors
				BigDecimal minDeltaPower = new BigDecimal(MIN_DELTA_POWER, MathContext.DECIMAL128);
				if (powerDelta.compareTo(BigDecimal.ZERO) >= 0 && powerDelta.compareTo(minDeltaPower) <= 0) {
					bidCurvePoint.power = previous.power.add(minDeltaPower);
				} else if (powerDelta.compareTo(minDeltaPower) >= 0) {
					// Don't allow big errors
					throw new InvalidBidCurveException("BidCurve is not strictly decreasing: " + this);
				}
			}
			previous = bidCurvePoint;
		}
	}

	public BidFunction combine(BidFunction other) {
		if (other.list.isEmpty()) {
			return new BidFunction(list);
		}
		if (list.isEmpty()) {
			return new BidFunction(other.list);
		}
		ArrayList<BidCurvePoint> combined = new ArrayList<BidCurvePoint>();
		Iterator<BidCurvePoint> i1 = list.iterator();
		Iterator<BidCurvePoint> i2 = other.list.iterator();
		BidCurvePoint p1 = i1.next();
		BidCurvePoint p2 = i2.next();
		BidCurvePoint previous1 = null;
		BidCurvePoint previous2 = null;
		for (;;) {

			if (p1.price.subtract(p2.price).abs().doubleValue() < MIN_DELTA_PRICE) {
				BigDecimal price = p1.price.add(p2.price).divide(BigDecimal.valueOf(2));
				combined.add(new BidCurvePoint(price, p1.power.add(p2.power)));
				if (i1.hasNext() && i2.hasNext()) {
					previous1 = p1;
					previous2 = p2;
					p1 = i1.next();
					p2 = i2.next();
				} else {
					break;
				}
			} else if (p1.price.compareTo(p2.price) < 0) {
				// calculate power on curve 2 at price p1.price
				BigDecimal powerAtP1 = getPowerAtPrice(p1.price, previous2, p2);
				combined.add(new BidCurvePoint(p1.price, p1.power.add(powerAtP1)));
				previous1 = p1;
				if (i1.hasNext()) {
					p1 = i1.next();
				} else {
					break;
				}
			} else { // p1.price > p2.price
						// calculate power on curve 1 at price p2.price
				BigDecimal powerAtP2 = getPowerAtPrice(p2.price, previous1, p1);
				combined.add(new BidCurvePoint(p2.price, p2.power.add(powerAtP2)));
				previous2 = p2;
				if (i2.hasNext()) {
					p2 = i2.next();
				} else {
					break;
				}
			}
		}
		return new BidFunction(combined);
	}

	private static double getPowerAtMaxPrice(double price, BidCurvePoint lastPoint) {
		return lastPoint.power.doubleValue() - ((price - lastPoint.price.doubleValue()) / price) * MIN_DELTA_POWER;
	}

	private static double getPriceAtPower(double target, BidCurvePoint begin, BidCurvePoint end) {
		BigDecimal fraction;
		if (begin.power == end.power) {
			fraction = BigDecimal.ONE;
		} else {
			fraction = new BigDecimal(target, MathContext.DECIMAL128).subtract(begin.power)
					.divide(end.power.subtract(begin.power), MathContext.DECIMAL128);
		}
		BigDecimal price = begin.price.add(fraction.multiply(end.price.subtract(begin.price)));
		return price.doubleValue();
	}

	public double findDemandFromCurve(double pric) {
		BigDecimal price = new BigDecimal(pric, MathContext.DECIMAL128);
		BidCurvePoint begin = null;
		BidCurvePoint end = null;
		for (BidCurvePoint p : list) {
			end = p;
			if (p.price.compareTo(price) > 0) {
				break;
			}
			begin = p;

		}
		if (begin == null) {
			throw new IllegalArgumentException("Price must be positive or zero!");
		}
		if (price.compareTo(end.price) > 0) {
			return getPowerAtMaxPrice(pric, end);
		}
		return getPowerAtPrice(price, begin, end).doubleValue();
	}

	public double findPriceFromCurve(double demand) {
		BigDecimal power = new BigDecimal(demand, MathContext.DECIMAL128);
		BidCurvePoint begin = null;
		BidCurvePoint end = null;
		for (BidCurvePoint p : list) {
			end = p;
			if (p.power.compareTo(power) < 0) {
				break;
			}
			begin = p;
		}
		if (begin == null) {
			if (end == null) {
				throw new InvalidBidCurveException("Illegal BidCurve data");
			}
			return end.price.doubleValue();
		}
		if (end == null) {
			return begin.price.doubleValue();
		}
		if (power.compareTo(end.power) < 0) {
			return MAX_PRICE;
		}
		return getPriceAtPower(demand, begin, end);
	}

	@Override
	public String toString() {
		String listString = "[";
		for (BidCurvePoint p : list) {
			listString += "{" + p.price + ", " + p.power + "}, ";
		}
		listString = listString.substring(0, listString.length() - 2);
		return "BidCurve " + listString + "], mc=" + getMarginalCost();
	}

	public List<BidCurvePoint> getPoints() {
		return Collections.unmodifiableList(list);
	}

	private static BigDecimal getPowerAtPrice(BigDecimal price, BidCurvePoint begin, BidCurvePoint end) {
		BigDecimal fraction;
		if (begin.price.equals(end.price)) {
			fraction = BigDecimal.ONE;
		} else {
			fraction = (price.subtract(begin.price)).divide(end.price.subtract(begin.price), MathContext.DECIMAL128);
		}
		BigDecimal power = begin.power.add(fraction.multiply(end.power.subtract(begin.power)));
		return power;
	}

	public static BidFunction sumCurves(BidFunction c1, BidFunction c2) {
		return c1.combine(c2);
	}

}
