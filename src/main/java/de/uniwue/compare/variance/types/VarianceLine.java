package de.uniwue.compare.variance.types;


/**
 * Variance if a complete line is missing
 * (Special form of VarianceContent for a whole line
 * 
 * Variance between text
 * Test: 				| This is an example
 * This is an example	|
 * Would be VarianceLine of Line "Test:"
 */
public abstract class VarianceLine extends Variance {
	
	public VarianceLine(String color, int priority, int seperations) {
		super("LINE", VarianceType.LINE, color, priority);
	}
	
}
