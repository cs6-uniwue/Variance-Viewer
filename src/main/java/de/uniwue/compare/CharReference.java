package de.uniwue.compare;

import de.uniwue.compare.token.Token;

/**
 * A token reference to compare objects that reference a token.
 *
 * e.g. splitting a token into its characters, comparing the characters
 *  and applying changes found onto the tokens.
 *
 * Token1:"Test"
 * Token2:"Test2"
 * 
 * T->Token1 == T->Token2
 * e->Token1 == e->Token2
 * s->Token1 == s->Token2
 * t->Token1 == t->Token2
 * 				2->Token2
 * 
 *
 * @param <T> Token reference to compare
 */
public class CharReference<T> {

	
	private final T reference;
	private final Token token;
	private final ConnectedContent connection;

	public CharReference(T reference, Token token, ConnectedContent connection) {
		if(reference == null)
			throw new NullPointerException("The pointer of a token reference can not be null.");
		this.reference = reference;
		this.token = token;
		this.connection = connection;
	}
	
	public T getReference() {
		return reference;
	}

	public Token getToken() {
		return token;
	}

	public ConnectedContent getConnection() {
		return connection;
	}
	
	@Override
	public String toString() {
		return String.format("['%s' -> %s ]", this.reference, token);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if(obj instanceof CharReference<?>) {
			CharReference<?> other = (CharReference<?>) obj;
			return this.reference.equals(other.getReference());
		} else {
			return false;
		}
	}

}
