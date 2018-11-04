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
