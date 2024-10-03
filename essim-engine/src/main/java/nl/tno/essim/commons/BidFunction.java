package nl.tno.essim.commons;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

@EqualsAndHashCode
@Slf4j
public class BidFunction {
    public final static BigDecimal DELTA_POWER = new BigDecimal("1e-6", MathContext.DECIMAL128);

    @EqualsAndHashCode
    @Getter
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
    }

    private final List<Point> points;
    @Getter
    @Setter
    private double marginalCost = 0.0;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final BigDecimal minDeltaPrice;
    private final BigDecimal mergePriceDelta;

    private BidFunction(BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minDeltaPrice, List<BidFunction.Point> list) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minDeltaPrice = minDeltaPrice;
        this.mergePriceDelta = this.minDeltaPrice.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
        this.points = new ArrayList<>(list);
        verifyAndUpdateBitCurve();
    }

    public BidFunction(BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minDeltaPrice, BigDecimal[]... pointsList) {
        this(minPrice, maxPrice, minDeltaPrice, Arrays.stream(pointsList).map(p -> new Point(p[0], p[1])).toList());
    }

    public BidFunction(double minPrice, double maxPrice, double minDeltaPrice, double[]... pointsList) {
        this(BigDecimal.valueOf(minPrice), BigDecimal.valueOf(maxPrice), BigDecimal.valueOf(minDeltaPrice), Arrays.stream(pointsList).map(p -> new Point(BigDecimal.valueOf(p[0]), BigDecimal.valueOf(p[1]))).toList());
    }

    private void verifyAndUpdateBitCurve() {
        Collections.sort(points);

        if (points.get(0).price.compareTo(minPrice) < 0) {
            // Rounding stuff
            if (points.get(0).price.subtract(minPrice).abs().compareTo(mergePriceDelta) < 0) {
                points.get(0).price = minPrice;
            } else {
                throw new IllegalArgumentException("Invalid bidcurve: price < " + minPrice + ": " + points);
            }
        }
        Point previous = null;
        for (var i = points.iterator(); i.hasNext(); ) {
            Point point = i.next();
            if (previous != null) {
                BigDecimal powerDelta = point.power.subtract(previous.power);
                if (powerDelta.compareTo(BigDecimal.ZERO) >= 0) {
                    throw new InvalidBidCurveException("BidCurve is not mathematically decreasing: {}" + this);
                }
            }
            previous = point;
        }
    }

    private static BigDecimal getPowerAtMaxPrice(BigDecimal price, Point lastPoint) {
        return lastPoint.power
                .subtract(price.subtract(lastPoint.price)
                        .divide(price, RoundingMode.HALF_UP)
                        .multiply(DELTA_POWER));
    }

    private static BigDecimal getPriceAtPower(BigDecimal target, Point begin, Point end) {
        BigDecimal fraction;
        if (begin.power == end.power) {
            fraction = BigDecimal.ONE;
        } else {
            fraction = target.subtract(begin.power)
                    .divide(end.power.subtract(begin.power), MathContext.DECIMAL128);
        }
        BigDecimal price = begin.price.add(fraction.multiply(end.price.subtract(begin.price)));
        return price;
    }

    public BigDecimal findDemandFromCurve(BigDecimal price) {
        Point begin = null;
        Point end = null;
        for (Point p : points) {
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
            return getPowerAtMaxPrice(price, end);
        }
        BigDecimal v = getPowerAtPrice(price, begin, end);
        log.trace("Allocation: {} at {} for {}", v, price, toJsonArray());
        return v;
    }

    public BigDecimal findPriceFromCurve(BigDecimal demand) {
        Point begin = null;
        Point end = null;
        for (Point p : points) {
            end = p;
            if (p.power.compareTo(demand) <= 0) {
                break;
            }
            begin = p;
        }
        if (begin == null) {
            if (end == null) {
                throw new InvalidBidCurveException("Illegal BidCurve data");
            }
            return end.price;
        }
        if (end == null) {
            return begin.price;
        }
        if (demand.compareTo(end.power) < 0) {
            return maxPrice;
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
            sb.append(sb.isEmpty() ? '[' : ',');
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
        return begin.power.add(fraction.multiply(end.power.subtract(begin.power)));
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

            if (p1.price.subtract(p2.price).abs().compareTo(c1.mergePriceDelta) < 0) {
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
