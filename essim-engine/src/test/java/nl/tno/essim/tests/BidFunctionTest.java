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
				new double[][] { { -10, 50E6 }, { 10, 50E6 } });
		// higher demand when price < 0, no mc
		BidFunction df2 = new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
				new double[][] { { -10, 70E6 }, { 10, 0 } });
		// stop consuming if price to high, mc=5
		BidFunction df3 = new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
				new double[][] { { -10, 50E6 }, { 0, 50E6 }, { 5, 0 } });
		df3.setMarginalCost(5);

		// production, produce at mc=4
		BidFunction pf1 = new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
				new double[][] { { -10, 0 }, { 0, 0 }, { 4, 0 }, { 4 + Commons.eps, -50E6 } });

		// buffer consume when price is low, produce when price is high, mc1=2, mc2=4
		BidFunction bf1 = new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE, new double[][] { { -10, 50E6 }, { 0, 50E6 },
				{ 2, 50E6 }, { 2 + Commons.eps, 0 }, { 4, 0 }, { 4 + Commons.eps, -50E6 } });

		System.out.println("df1: " + df1);
		System.out.println("df2: " + df2);
		System.out.println("df3: " + df3);
		BidFunction sumCurves1 = BidFunction.sumCurves(df1, df2);
		System.out.println("SC1: " + sumCurves1);
		compareBidFunctions(
				new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
						new double[][] { { -10.0, 120E6 }, { 10.0, 50.0E6 } }),
				sumCurves1, BidFunction.DELTA_POWER_THRESHOLD);
		BidFunction sumCurves2 = BidFunction.sumCurves(sumCurves1, df3);
		System.out.println("SC2: " + sumCurves2);
		compareBidFunctions(
				new BidFunction(-10, 10, Commons.MIN_DELTA_PRICE,
						new double[][] { { -10, 170E6 }, { 0, 135E6 }, { 5, 67.5E6 } }),
				sumCurves2, BidFunction.DELTA_POWER_THRESHOLD * 2);

	}

}
