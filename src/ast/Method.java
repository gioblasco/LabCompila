/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

import java.util.ArrayList;

public class Method {
	
	public Method(Type type, ArrayList<Field> parameters) {
		this.type = type;
		this.parameters = parameters;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public ArrayList<Field> getParameters() {
		return parameters;
	}
	public void setParameters(ArrayList<Field> parameters) {
		this.parameters = parameters;
	}

	private String name;
	private Type type;
	private ArrayList<Field> parameters;
}
