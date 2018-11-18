/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

import java.util.ArrayList;

public class Method {
	
	public Method(Type type, ArrayList<Field> parameters, String name) {
		this.type = type;
		this.parameters = parameters;
		this.name = name;
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
	
	// Returns "" if okay, else a multiline error message.
	public String checkSignature(ArrayList<Type> parameters) {
		String retorno = "";
		int tam = parameters.size();
		if(parameters == null && this.parameters != null || parameters != null && this.parameters == null || parameters.size() != this.parameters.size())
			return  "Invalid number of parameters";
		
		for(int i = 0; i < tam; i++) {
			if(parameters.get(i) != this.parameters.get(i).getType()) {
				retorno.concat("\n\tExpected "+this.parameters.get(i).getType().getName() + " but received " + parameters.get(i).getName() + "at the "+ (i+1) +"º parameter");
			}
		}
		return retorno;
		
	}
	
}
