package de.uniwue.compare.variance.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Variance with missing character sequences e.g. 
 * characters [.!:;,]
 * 
 * Variance between text
 * Test: this is an example! 		| Test, this is an example 
 * Would be VarianceMissing of of Test: to Test, and example! to example
 */
public class VarianceMissing extends Variance{
	
	protected final List<String> missing;
	
	public VarianceMissing(String name, String color, int priority, List<String> missing) {
		super(name, VarianceType.MISSING, color, priority);
		this.missing = missing;
	}
	
	public List<String> getMissing() {
		return new ArrayList<>(missing);
	}
}
