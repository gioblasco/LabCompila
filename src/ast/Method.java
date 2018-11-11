/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

import java.util.ArrayList;

public class Method {
	public Method(String name, Field ret, ArrayList<Field> parameters) {
		this.name = name;
		this.ret = ret;
		this.parameters = parameters;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	private String name;
	private Field ret;
	private ArrayList<Field> parameters;
}
