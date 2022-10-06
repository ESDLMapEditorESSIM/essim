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

	@Test
	void testBidFunction() {
		//demand
		
		// flat bid curve with negative price, no mc
		BidFunction df1 = new BidFunction().addPoint(-10, 50).addPoint(10, 50);
		// higher demand when price < 0, no mc
		BidFunction df2 = new BidFunction().addPoint(-10, 70).addPoint(10, 0);
		// stop consuming if price to high, mc=5
		BidFunction df3 = new BidFunction().addPoint(-10, 50).addPoint(0, 50).addPoint(5, 0);
		df3.setMarginalCost(5);
		
		
		//production, produce at mc=4
		BidFunction pf1 = new BidFunction().addPoint(-10, 0).addPoint(0, 0).addPoint(4, 0).addPoint(4+Commons.eps, 50);

		// buffer consume when price is low, produce when price is high, mc1=2, mc2=4
		BidFunction bf1 = new BidFunction().addPoint(-10, 50).addPoint(0, 50)
				.addPoint(2, 50).addPoint(2+Commons.eps, 0)
				.addPoint(4, 0).addPoint(4+Commons.eps, -50);
		
		System.out.println(df1);
		System.out.println(df2);
		BidFunction sumCurves = BidFunction.sumCurves(df1, df2);
		System.out.println(sumCurves);
		assertEquals(new BidFunction().addPoint(-10.0,120.0).addPoint(10.0,50.0), sumCurves);
		
		
	}

}
