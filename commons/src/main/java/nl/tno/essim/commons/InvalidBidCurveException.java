package nl.tno.essim.commons;

public class InvalidBidCurveException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public InvalidBidCurveException(String errorDesc) {
		super(errorDesc);
	}

}
