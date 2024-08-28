package nl.tno.essim.commons;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@EqualsAndHashCode
@Slf4j
public class BidFunction {
    public final static double DELTA_POWER_THRESHOLD = 10;
    private final static BigDecimal DELTA_POWER = new BigDecimal("1e-6", MathContext.DECIMAL128);

    @EqualsAndHashCode
    public static class Point implements Comparable<Point> {
        private BigDecimal price;
        private BigDecimal power;

        public Point(BigDecimal price, BigDecimal power) {
            this.price = price;
            this.power = power;
        }

        public Point(Double price, Double power) {
            this.price = new BigDecimal(price, MathContext.DECIMAL128);
            this.power = new BigDecimal(power, MathContext.DECIMAL128);
        }

        @Override
        public int compareTo(Point o) {
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

    private List<Point> points;
    @Getter
    @Setter
    private double marginalCost = 0.0;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final BigDecimal minDeltaPrice;

    private BidFunction(BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minDeltaPrice, List<BidFunction.Point> list) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minDeltaPrice = minDeltaPrice;
        this.points = new ArrayList<BidFunction.Point>(list);
        verifyAndUpdateBitCurve();
    }

    public BidFunction(double minPrice, double maxPrice, double minDeltaPrice, double[]... pointsList) {
        this.minPrice = BigDecimal.valueOf(minPrice);
        this.maxPrice = BigDecimal.valueOf(maxPrice);
        this.minDeltaPrice = BigDecimal.valueOf(minDeltaPrice);
        points = new ArrayList<BidFunction.Point>();
        for (double[] point : pointsList) {
            points.add(new Point(point[0], point[1]));
        }
        verifyAndUpdateBitCurve();
    }

    private void verifyAndUpdateBitCurve() {
        Collections.sort(points);

        if (points.get(0).price.compareTo(minPrice) < 0) {
            // Rounding stuff
            if (points.get(0).price.subtract(minPrice).abs().compareTo(minDeltaPrice.divide(BigDecimal.TWO, MathContext.DECIMAL128)) < 0) {
                points.get(0).price = minPrice;
            } else {
                throw new IllegalArgumentException("Invalid bidcurve: price < " + minPrice + ": " + points);
            }
        }
        Point previous = null;
        for (var i = points.listIterator(points.size()); i.hasPrevious(); ) {
            Point bidCurvePoint = i.previous();
            if (previous != null) {

                if (previous.price.subtract(bidCurvePoint.price).abs().compareTo(minDeltaPrice) <= 0) {
                    if (previous.power.subtract(bidCurvePoint.power).abs().doubleValue() <= DELTA_POWER_THRESHOLD) {
                        // We don't need double points
                        i.remove();
                        continue;
                    }
                    bidCurvePoint.price = previous.price.subtract(minDeltaPrice);
                }
                BigDecimal powerDelta = previous.power.subtract(bidCurvePoint.power);
                // Correct minor errors
                BigDecimal minDeltaPower = new BigDecimal(DELTA_POWER_THRESHOLD, MathContext.DECIMAL128);
                if (powerDelta.compareTo(BigDecimal.ZERO) >= 0 && powerDelta.compareTo(minDeltaPower) <= 0) {
                    bidCurvePoint.power = previous.power.add(DELTA_POWER);
                } else if (powerDelta.compareTo(minDeltaPower) > 0) {
                    // Don't allow big errors
                    throw new InvalidBidCurveException("BidCurve is not mathematically decreasing: " + this);
                }
            }
            previous = bidCurvePoint;
        }
    }

    private static double getPowerAtMaxPrice(double price, Point lastPoint) {
        return lastPoint.power.doubleValue() - ((price - lastPoint.price.doubleValue()) / price) * DELTA_POWER_THRESHOLD;
    }

    private static double getPriceAtPower(double target, Point begin, Point end) {
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

    public double findDemandFromCurve(double price) {
        BigDecimal target = new BigDecimal(price, MathContext.DECIMAL128);
        Point begin = null;
        Point end = null;
        for (Point p : points) {
            end = p;
            if (p.price.compareTo(target) > 0) {
                break;
            }
            begin = p;

        }
        if (begin == null) {
            throw new IllegalArgumentException("Price must be positive or zero!");
        }
        if (target.compareTo(end.price) > 0) {
            return getPowerAtMaxPrice(price, end);
        }
        double v = getPowerAtPrice(target, begin, end).doubleValue();
        log.debug("Allocation: {} at {} for {}", v, price, toJsonArray());
        return v;
    }

    public double findPriceFromCurve(double demand) {
        BigDecimal power = new BigDecimal(demand, MathContext.DECIMAL128);
        Point begin = null;
        Point end = null;
        for (Point p : points) {
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
            return maxPrice.doubleValue();
        }
        return getPriceAtPower(demand, begin, end);
    }

    @Override
    public String toString() {
        String listString = "[";
        for (Point p : points) {
            listString += "{" + p.price + ", " + p.power + "}, ";
        }
        listString = listString.substring(0, listString.length() - 2);
        return "BidCurve " + listString + "], mc=" + getMarginalCost();
    }

    public String toJsonArray() {
        StringBuilder sb = new StringBuilder();
        for (BidFunction.Point point : getPoints()) {
            sb.append(sb.length() == 0 ? '[' : ',');
            sb.append('[');
            sb.append(point.getPrice());
            sb.append(',');
            sb.append(point.getPower());
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }

    public List<BidFunction.Point> getPoints() {
        return Collections.unmodifiableList(points);
    }

    private static BigDecimal getPowerAtPrice(@NotNull BigDecimal price, @NotNull Point begin, @NotNull Point end) {
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
        if (c2.points.isEmpty()) {
            return new BidFunction(c1.minPrice, c1.maxPrice, c1.minDeltaPrice, c1.points);
        }
        if (c1.points.isEmpty()) {
            return new BidFunction(c2.minPrice, c2.maxPrice, c2.minDeltaPrice, c2.points);
        }
        // TODO compare min/max/delta
        ArrayList<Point> combined = new ArrayList<Point>();
        Iterator<Point> i1 = c1.points.iterator();
        Iterator<Point> i2 = c2.points.iterator();
        Point p1 = i1.next();
        Point p2 = i2.next();
        Point previous1 = null;
        Point previous2 = null;
        for (; ; ) {

            if (p1.price.subtract(p2.price).abs().compareTo(c1.minDeltaPrice) < 0) {
                BigDecimal price = p1.price.add(p2.price).divide(BigDecimal.TWO, MathContext.DECIMAL128);
                combined.add(new Point(price, p1.power.add(p2.power)));
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
                BigDecimal powerAtP1 = BidFunction.getPowerAtPrice(p1.price, previous2, p2);
                combined.add(new Point(p1.price, p1.power.add(powerAtP1)));
                previous1 = p1;
                if (i1.hasNext()) {
                    p1 = i1.next();
                } else {
                    break;
                }
            } else { // p1.price > p2.price
                // calculate power on curve 1 at price p2.price
                BigDecimal powerAtP2 = BidFunction.getPowerAtPrice(p2.price, previous1, p1);
                combined.add(new Point(p2.price, p2.power.add(powerAtP2)));
                previous2 = p2;
                if (i2.hasNext()) {
                    p2 = i2.next();
                } else {
                    break;
                }
            }
        }
        return new BidFunction(c1.minPrice, c1.maxPrice, c1.minDeltaPrice, combined);
    }

}
