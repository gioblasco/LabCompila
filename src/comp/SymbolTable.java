/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package comp;

import java.util.*;
import ast.Type;

public class SymbolTable {

    public SymbolTable() {
        localTable = new Hashtable<String, Type>();
        globalTable = new Hashtable<String, Type>();
    }

    public void putInGlobal(String key, Type value) {
            globalTable.put(key, value);
    }

    public void putInLocal(String key, Type value) {
        localTable.put(key, value);
    }

    public Type getInLocal(String key) {
        return localTable.get(key);
    }
    
    public Type getInGlobal(String key){
        return globalTable.get(key);
    }

    public void removeLocalIdent() {
        // remove all local identifiers from the table
        localTable.clear();
    }
    
    private Hashtable<String, Type> localTable;
    private Hashtable<String, Type> globalTable;
}