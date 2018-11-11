/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */

package ast;

public class LiteralBoolean extends Expr {

    public LiteralBoolean( boolean value ) {
        this.value = value;
    }

    @Override
	public Type getType() {
        return Type.booleanType;
    }

    public static LiteralBoolean True  = new LiteralBoolean(true);
    public static LiteralBoolean False = new LiteralBoolean(false);

    private boolean value;
}
