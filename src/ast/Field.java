/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

public class Field {

	public Field(Type type, String name) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	public Type getType() {
		return type;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;
	private Type type;
}
