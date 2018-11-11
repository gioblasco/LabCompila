/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

public class LiteralString extends Expr {
    
    public LiteralString( String literalString ) { 
        this.literalString = literalString;
    }
    
    public Type getType() {
        return Type.stringType;
    }
    
    private String literalString;
}
