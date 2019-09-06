package de.uniwue.compare.variance.types;


/**
 * Variance for separated tokens
 * e.g. separations 1
 * 
 * Variance between text
 * Test: this is an example	| Test : this is an exam ple
 * Would be VarianceSeparation of "Test:" and "Test :", as well as "example" and "exam ple"
 */
public abstract class VarianceSeparation extends Variance{
	
	public VarianceSeparation(String color, int priority, int seperations) {
		super("SEPARATION", VarianceType.SEPARATION, color, priority);
	}
	
}
