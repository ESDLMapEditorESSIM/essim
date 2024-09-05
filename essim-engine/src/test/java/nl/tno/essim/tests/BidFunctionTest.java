package nl.tno.essim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons;

class BidFunctionTest {

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
    }

    static void compareBidFunctions(BidFunction expected, BidFunction actual, double powerAccuracy) {
        for (BidFunction.Point point : expected.getPoints()) {
            assertEquals(point.getPower(), actual.findDemandFromCurve(point.getPrice()), powerAccuracy);
        }
    }

    @Test
    void testBidFunction() {
        // demand
//		new BidFunction(Commons.P_MIN, Commons.P_MAX, Commons.MIN_DELTA_PRICE);
        // flat bid curve with negative price, no mc
        BidFunction df1 = new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
                new double[][]{{-10, 50E6}, {10, 50E6 - BidFunction.DELTA_POWER.doubleValue()}});
        // higher demand when price < 0, no mc
        BidFunction df2 = new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
                new double[][]{{-10, 70E6}, {10, 0}});
        // stop consuming if price to high, mc=5
        BidFunction df3 = new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
                new double[][]{{-10, 50E6}, {0, 50E6 - BidFunction.DELTA_POWER.doubleValue()}, {5, 0}});
        df3.setMarginalCost(5);

        // production, produce at mc=4
        BidFunction pf1 = new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
                new double[][]{{-10, 0}, {0, 0 - BidFunction.DELTA_POWER.doubleValue()}, {4, 0 - 2 * BidFunction.DELTA_POWER.doubleValue()}, {4 + Commons.eps, -50E6}});

        // buffer consume when price is low, produce when price is high, mc1=2, mc2=4
        BidFunction bf1 = new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE, new double[][]{{-10, 50E6}, {0, 50E6 - BidFunction.DELTA_POWER.doubleValue()},
                {2, 50E6 - 2 * BidFunction.DELTA_POWER.doubleValue()}, {2 + Commons.eps, 0}, {4, 0 - BidFunction.DELTA_POWER.doubleValue()}, {4 + Commons.eps, -50E6}});

        System.out.println("df1: " + df1);
        System.out.println("df2: " + df2);
        System.out.println("df3: " + df3);
        BidFunction sumCurves1 = BidFunction.sumCurves(df1, df2);
        System.out.println("SC1: " + sumCurves1);
        compareBidFunctions(
                new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
                        new double[][]{{-10.0, 120E6}, {10.0, 50.0E6}}),
                sumCurves1, BidFunction.DELTA_POWER.doubleValue());
        BidFunction sumCurves2 = BidFunction.sumCurves(sumCurves1, df3);
        System.out.println("SC2: " + sumCurves2);
        compareBidFunctions(
                new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
                        new double[][]{{-10, 170E6}, {0, 135E6}, {5, 67.5E6}}),
                sumCurves2, BidFunction.DELTA_POWER.doubleValue() * 2);

    }

    @Test
    void testSum() {
        BidFunction bf1 = new BidFunction(-110, 100, Commons.MIN_DELTA_PRICE, new double[][]{{-110, 10000000.00000100024044513702392578}, {100, 10000000}});
        BidFunction bf2 = new BidFunction(-110, 100, Commons.MIN_DELTA_PRICE, new double[][]{{-110, 0}, {2.570580000000000112565879469180008E-8, -9.999999999999999547481118258862587E-7}, {2.580580000000000202110611233208282E-8, -360000000}, {100, -360000000.000001013278961181640625}});
        BidFunction sumCurves = BidFunction.sumCurves(bf1, bf2);
        System.out.println("SC1: " + sumCurves);
        BidFunction expected = new BidFunction(-110, 100, Commons.MIN_DELTA_PRICE, new double[][]{
                {-110, 10000000.00000100024044513702392578},
                {2.570580000000000112565879469180008E-8, 9999999.999999476304973752335339268062884793527075307762055176668728},
                {2.580580000000000202110611233208282E-8, -349999999.999999523695026248141010957700066883264776840574400753524852},
                {100, -350000000.000001013278961181640625}
        });
        compareBidFunctions(expected, sumCurves, 1E-5);

        double equilibriumPrice = sumCurves.findPriceFromCurve(0);
        System.out.println("equilibriumPrice: " + equilibriumPrice);
        double allocation = sumCurves.findDemandFromCurve(equilibriumPrice);
        System.out.println("allocation: " + allocation);
        assertEquals(0, allocation, 1E-5);
    }
}
