package de.uniwue.compare.variance.types;


/**
 * Variance for typographical differences (in TEI documents)
 * (checks for the rend/rendering attribute)
 * 
 * <p rend="xxl">Test:</p><p>This is an example</p> | <p>Test:</p><p>This is an example</p>
 * Would be VarianceTypography of <p rend="xxl">Test:</p> and <p>Test:</p> with change "xxl"
 */
public class VarianceTypography extends Variance{
	
	public VarianceTypography(String color, int priority) {
		super("TYPOGRAPHY", VarianceType.TYPOGRAPHY, color, priority);
	}
	
}
