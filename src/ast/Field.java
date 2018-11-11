/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

public class Field {

	public Field(String name, Type type) {
		this.name = name;
		this.type = type;
	}
	
	private String name;
	private Type type;
}
