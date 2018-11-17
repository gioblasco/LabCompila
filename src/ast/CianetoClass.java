/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

import java.util.Hashtable;

public class CianetoClass extends Type {
	
	public CianetoClass(String name, boolean open, CianetoClass parent) {
	   super(name);
	   this.open = open;
	   this.parent = parent;
	   this.publicMethodList = new Hashtable<String, Method>();
	   this.privateMethodList = new Hashtable<String, Method>();
	   this.publicFieldList = new Hashtable<String, Field>();
	   this.privateFieldList = new Hashtable<String, Field>();
	}
	
	public boolean isOpen() {
		return open;
	}
	
	public CianetoClass getParent() {
		return parent;
	}

	public void setParent(CianetoClass parent) {
		this.parent = parent;
	}
	
	public Method getMethod(String methodName) {
		Method tempMethod;
		tempMethod = this.privateMethodList.get(methodName);
		if(tempMethod == null)
			tempMethod = this.publicMethodList.get(methodName);
		if(tempMethod != null)
			return tempMethod;
		return (this.parent == null) ? null : this.parent.getPublicMethod(methodName);
	}

	public Method getPublicMethod(String methodName) {
		Method tempMethod = this.publicMethodList.get(methodName);
		if(tempMethod != null)
			return tempMethod;
		return (this.parent == null) ? null : this.parent.getPublicMethod(methodName);
	}

	public Field getAttribute(){
		// TODO: não deixar isso pra depois
		return null;
	}
	
	public Hashtable<String, Method> getPublicMethod() {
		return this.publicMethodList;
	}

	public Hashtable<String, Method> getPrivateMethod() {
		return this.privateMethodList;
	}

	public Hashtable<String, Field> getPublicField() {
		return publicFieldList;
	}
	
	public Hashtable<String, Field> getPrivateField() {
		return privateFieldList;
	}

	private Hashtable<String, Field> publicFieldList, privateFieldList;
	private Hashtable<String, Method> publicMethodList, privateMethodList;
	private CianetoClass parent;
	private boolean open;
}
