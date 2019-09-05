package de.uniwue.compare;

/**
 * Variance with predefined types and user defined names 
 */
public class Variance {

	private final String name;
	private final VarianceType type;
	
	public Variance(String name, VarianceType type) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	public VarianceType getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null || getClass() != obj.getClass())
			return false;

		Variance other = (Variance) obj;
		if ((name == null && other.name != null) || !name.equals(other.name))
			return false;

		if (type != other.type)
			return false;

		return true;
	}
	
}
