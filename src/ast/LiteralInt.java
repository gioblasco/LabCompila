/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

public class LiteralInt extends Expr {
    
    public LiteralInt( int value ) { 
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public Type getType() {
        return Type.intType;
    }
    
    private int value;
}
