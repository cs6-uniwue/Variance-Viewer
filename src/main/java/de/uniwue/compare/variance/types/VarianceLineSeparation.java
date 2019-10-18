package de.uniwue.compare.variance.types;


/**
 * Variance for new line differences
 * 
 * Variance between text
 * Test: this is an| Test: this is an example 
 * example	 	   |
 * Would be VarianceNewLine of an¶ and an
 */
public class VarianceLineSeparation extends Variance{
	
	public VarianceLineSeparation(String color, int priority) {
		super("LINESEPARATION", VarianceType.LINESEPARATION, color, priority);
	}
	
}
