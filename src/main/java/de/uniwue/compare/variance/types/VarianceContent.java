package de.uniwue.compare.variance.types;


/**
 * Variance for all changes that do not fall into any other variance type
 */
public class VarianceContent extends Variance{
	
	public VarianceContent(String color, int priority) {
		super("CONTENT", VarianceType.CONTENT, color, priority);
	}
	
}
