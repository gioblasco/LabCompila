/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

import java.util.ArrayList;

public class CianetoClass extends Type {
	
	public CianetoClass(String name, boolean open) {
	   super(name);
	   this.open = open;
	   this.publicMethodList = new ArrayList<Method>();
	   this.privateMethodList = new ArrayList<Method>();
	   this.fieldList = new ArrayList<Field>();	   
	}
	
	public CianetoClass(String name, boolean open, CianetoClass parent) {
	   super(name);
	   this.open = open;
	   this.parent = parent;
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
	
	public boolean isOpen() {
		return open;
	}
	
	public boolean hasMethod(String methodName) {
		for(Method m : privateMethodList) {
			if (m.getName().equals(methodName))
				return true;
		}
		for(Method m : publicMethodList) {
			if (m.getName().equals(methodName))
				return true;
		}
		return (this.parent == null) ? false : this.parent.hasPublicMethod(methodName);
	}

	public boolean hasPublicMethod(String methodName) {
		for(Method m : publicMethodList) {
			if (m.getName().equals(methodName))
				return true;
		}
		return (this.parent == null) ? false : this.parent.hasPublicMethod(methodName);
	}

	private ArrayList<Field> fieldList;
	private ArrayList<Method> publicMethodList, privateMethodList;
	private CianetoClass parent;
	private boolean open; 
	
}
