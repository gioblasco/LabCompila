/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

import java.util.ArrayList;

public class CianetoClass extends Type {
	
	public CianetoClass(String name) {
	   super(name);
	   this.publicMethodList = new ArrayList<Method>();
	   this.privateMethodList = new ArrayList<Method>();
	   this.fieldList = new ArrayList<Field>();	   
	}
	
   public ArrayList<Field> getFieldList() {
		return fieldList;
	}
	public void setFieldList(ArrayList<Field> fieldList) {
		this.fieldList = fieldList;
	}
	public ArrayList<Method> getPublicMethodList() {
		return publicMethodList;
	}
	public void setPublicMethodList(ArrayList<Method> publicMethodList) {
		this.publicMethodList = publicMethodList;
	}
	public ArrayList<Method> getPrivateMethodList() {
		return privateMethodList;
	}
	public void setPrivateMethodList(ArrayList<Method> privateMethodList) {
		this.privateMethodList = privateMethodList;
	}

	private ArrayList<Field> fieldList;
	private ArrayList<Method> publicMethodList, privateMethodList;
   // m�todos p�blicos get e set para obter e iniciar as vari�veis acima,
   // entre outros m�todos
}
