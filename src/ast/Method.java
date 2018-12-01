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
		int tam1 = 0, tam2 = 0;
		if(parameters != null || this.parameters != null) {
			if (parameters != null)
				tam1 = parameters.size();
			if (this.parameters != null)
				tam2 = this.parameters.size();
			if(tam1 != tam2)
				return  "Invalid number of parameters";
			
			for(int i = 0; i < tam1; i++) {
				if(parameters.get(i) != this.parameters.get(i).getType()) {
					if(this.parameters.get(i).getType() instanceof CianetoClass && (!(parameters.get(i) instanceof CianetoClass) && parameters.get(i) != Type.nullType)) {
						retorno = retorno.concat("\n\tExpected " +this.parameters.get(i).getType().getName()+ " or nil, but received " + parameters.get(i).getName());
					} else if (parameters.get(i) instanceof CianetoClass && this.parameters.get(i).getType() instanceof CianetoClass) {
						if(!((CianetoClass)parameters.get(i)).findParent(this.parameters.get(i).getType().getName()))
							retorno = retorno.concat("\n\tTrying to use a parameter of type " +parameters.get(i).getName()+ " that is not subclass of " + this.parameters.get(i).getType().getName());
					} else if (this.parameters.get(i).getType() == Type.stringType && (parameters.get(i) != Type.stringType && parameters.get(i) != Type.nullType)) {
						retorno = retorno.concat("\n\tExpected " +this.parameters.get(i).getType().getName()+ " or nil, but received " +parameters.get(i).getName());
					} else if (this.parameters.get(i).getType() == Type.undefinedType || parameters.get(i) == Type.undefinedType) {
						retorno = retorno.concat("\n\tTrying to use parameters of undefined type");
					} else
						retorno = retorno.concat("\n\tExpected "+this.parameters.get(i).getType().getName() + " but received " + parameters.get(i).getName() + " at the "+ (i+1) +"º parameter");
				}
			}
		}
		return retorno;
		
	}
	
}
