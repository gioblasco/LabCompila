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
	   this.fieldList = new Hashtable<String, Field>();
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
	public Method getParentPublicMethod(String methodName) {
		if(this.parent == null)
			return null;
		return this.parent.getPublicMethod(methodName);
	}
	
	public Method getPublicMethod(String methodName) {
		Method tempMethod = this.publicMethodList.get(methodName);
		if(tempMethod != null)
			return tempMethod;
		return (this.parent == null) ? null : this.parent.getPublicMethod(methodName);
	}
	
	public boolean findParent(String className) {
		if(this.getName().equals(className))
			return true;
		return (this.parent == null) ? false : this.parent.findParent(className);
	}
	
	public Hashtable<String, Method> getPublicMethod() {
		return this.publicMethodList;
	}

	public Hashtable<String, Method> getPrivateMethod() {
		return this.privateMethodList;
	}
	
	public Hashtable<String, Field> getFieldList() {
		return fieldList;
	}

	private Hashtable<String, Field> fieldList;
	private Hashtable<String, Method> publicMethodList, privateMethodList;
	private CianetoClass parent;
	private boolean open;
}
