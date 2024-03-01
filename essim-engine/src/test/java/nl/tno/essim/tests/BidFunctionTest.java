package nl.tno.essim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

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
		/*
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
		*/
		
	}
	/*
	@Test
	void testSignificance() {
		//curve={-100.0=0.0, 30.0=0.0, 30.000000000001=-5.4E7, 100.0=-5.4E7}; mc=30.0
		BidFunction bf1 = new BidFunction().addPoint(-100, 0).addPoint(30, 0)
				.addPoint(30.01,-5.4E7).addPoint(100.0,-5.4E7);
		System.out.println(bf1);
		
		BidFunction demand = new BidFunction().addPoint(-100.0,179730.0).addPoint(100.0,179730.0);
		
		BidFunction sumCurves = BidFunction.sumCurves(bf1, demand);
		System.out.println(sumCurves);
		var price = sumCurves.findPriceFromCurveBD(BigDecimal.ZERO);
		System.out.println("** BD price: " + price);
		System.out.println("** Allocation: " + bf1.findDemandFromCurveBD(price));
		
		var findPriceFromCurve = bf1.findPriceFromCurveBD(new BigDecimal(-179730.0));
		System.out.println("** Manual price from curve: " + findPriceFromCurve);
		System.out.println("** Allocations: (anwser = 179730.0)");
		String input = "30.00000000000000";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.000000000000001";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.000000000000002";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.000000000000003";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.000000000000004";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.000000000000005";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.000000000000006";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.000000000000007";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.000000000000008";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.000000000000009";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
		input = "30.00000000000001";
		System.out.println(input + " = " + bf1.findDemandFromCurveBD(new BigDecimal(input)));
	
		
		//179730.0
				
		
	}
	*/
}
