/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */
package comp;

import java.io.PrintWriter;

import java.util.ArrayList;
import ast.*;
import lexer.Lexer;
import lexer.Token;

public class Compiler {

    // compile must receive an input with an character less than
    // p_input.lenght
    public Program compile(char[] input, PrintWriter outError) {

        ArrayList<CompilationError> compilationErrorList = new ArrayList<>();
        signalError = new ErrorSignaller(outError, compilationErrorList);
        symbolTable = new SymbolTable();
        lexer = new Lexer(input, signalError);
        signalError.setLexer(lexer);

        Program program = null;
        next();
        program = program(compilationErrorList);
        return program;
    }

    private Program program(ArrayList<CompilationError> compilationErrorList) {
        // Program ::= CianetoClass { CianetoClass }
        ArrayList<MetaobjectAnnotation> metaobjectCallList = new ArrayList<>();
        ArrayList<CianetoClass> CianetoClassList = new ArrayList<>();
        Program program = new Program(CianetoClassList, metaobjectCallList, compilationErrorList);
        boolean thereWasAnError = false;
        while (lexer.token == Token.CLASS
                || (lexer.token == Token.ID && lexer.getStringValue().equals("open"))
                || lexer.token == Token.ANNOT) {
            try {
                while (lexer.token == Token.ANNOT) {
                    metaobjectAnnotation(metaobjectCallList);
                }
                classDec();
            } catch (CompilerError e) {
                // if there was an exception, there is a compilation error
                thereWasAnError = true;
                while (lexer.token != Token.CLASS && lexer.token != Token.EOF) {
                    try {
                        next();
                    } catch (RuntimeException ee) {
                        e.printStackTrace();
                        return program;
                    }
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                thereWasAnError = true;
            }

        }
        if (!thereWasAnError && lexer.token != Token.EOF) {
            try {
                error("End of file expected");
            } catch (CompilerError e) {
            }
        }
        return program;
    }

    /**
     * parses a metaobject annotation as <code>{@literal @}cep(...)</code> in
     * <br>
     * <code>
     *
     * @cep(5, "'class' expected") <br>
     * class Program <br>
     * func run { } <br>
     * end <br>
     * </code>
     *
     *
     */
    // Annot ::= “@” Id [ “(” { AnnotParam } “)” ]
    @SuppressWarnings("incomplete-switch")
    private void metaobjectAnnotation(ArrayList<MetaobjectAnnotation> metaobjectAnnotationList) {
        String name = lexer.getMetaobjectName();
        int lineNumber = lexer.getLineNumber();
        next();
        ArrayList<Object> metaobjectParamList = new ArrayList<>();
        boolean getNextToken = false;
        if (lexer.token == Token.LEFTPAR) {
            // metaobject call with parameters
            next();
            while (lexer.token == Token.LITERALINT || lexer.token == Token.LITERALSTRING
                    || lexer.token == Token.ID) {
                switch (lexer.token) {
                    case LITERALINT:
                        metaobjectParamList.add(lexer.getNumberValue());
                        break;
                    case LITERALSTRING:
                        metaobjectParamList.add(lexer.getLiteralStringValue());
                        break;
                    case ID:
                        metaobjectParamList.add(lexer.getStringValue());
                }
                next();
                if (lexer.token == Token.COMMA) {
                    next();
                } else {
                    break;
                }
            }
            if (lexer.token != Token.RIGHTPAR) {
                error("')' expected after metaobject call with parameters");
            } else {
                getNextToken = true;
            }
        }
        if (name.equals("nce")) {
            if (metaobjectParamList.size() != 0) {
                error("Metaobject 'nce' does not take parameters");
            }
        } else if (name.equals("cep")) {
            if (metaobjectParamList.size() != 3 && metaobjectParamList.size() != 4) {
                error("Metaobject 'cep' take three or four parameters");
            }
            if (!(metaobjectParamList.get(0) instanceof Integer)) {
                error("The first parameter of metaobject 'cep' should be an integer number");
            } else {
                int ln = (Integer) metaobjectParamList.get(0);
                metaobjectParamList.set(0, ln + lineNumber);
            }
            if (!(metaobjectParamList.get(1) instanceof String) || !(metaobjectParamList.get(2) instanceof String)) {
                error("The second and third parameters of metaobject 'cep' should be literal strings");
            }
            if (metaobjectParamList.size() >= 4 && !(metaobjectParamList.get(3) instanceof String)) {
                error("The fourth parameter of metaobject 'cep' should be a literal string");
            }

        }
        metaobjectAnnotationList.add(new MetaobjectAnnotation(name, metaobjectParamList));
        if (getNextToken) {
            next();
        }
    }

    // ClassDec ::= [ “open” ] “class” Id [ “extends” Id ] MemberList “end”
    private void classDec() {
    	boolean open = false;
    	Type parent = null;
    	String className = new String();
        if (lexer.getStringValue().equals("open")) {
        	open = true;
            next();
        }
        if (lexer.token != Token.CLASS) {
            error("'class' expected");
        }
        next();
        if (lexer.token != Token.ID) {
            error("Identifier expected in class declaration");
        } else {
        	className = lexer.getStringValue();
        }
        next();
        if (lexer.token == Token.EXTENDS) {
            next();
            if (lexer.token != Token.ID) {
                error("Identifier expected in extension of class declaration");
            } else {
            	String superclassName = lexer.getStringValue();
            	if((parent = symbolTable.getInGlobal(superclassName)) == null)
            		error("Trying to extend an unexistant class");
            	// checa se o parent não eh final
            	else {
            		if(!((CianetoClass)parent).isOpen())
            			error("Trying to extend a final class");            			            			
            	}
            }
            next();
        }
        
        CianetoClass classe = new CianetoClass(className, open, (CianetoClass)parent);

        memberList(classe);
        if (lexer.token != Token.END) {
            error("'end' expected in class declaration");
        }
        
        // colocando classe na symbol table
        symbolTable.putInGlobal(className, classe);
        
        next();

    }

    // MemberList ::= { [ Qualifier ] Member }
    private void memberList(CianetoClass classe) { //checar se o nome do membro nao eh igual ao da classe
        Method method, tempMethod;
        ArrayList<Field> field;
        Field tempField;
        String qualifier;
    	while (true) {
            qualifier = qualifier();
            if(!classe.isOpen() && qualifier.contains("final")) {
            	error("A final class cannot have final members. It's redudant.");
            }
            if (lexer.token == Token.VAR) {
                // TODO: verificar se precisa tratar override, protect e etc..
                field = fieldDec();
                if(qualifier.contains("override")) {
                	if(qualifier.contains("private")) {
                		error("Cannot override a private method!");
                    } else { // public with override
                    	for(Field f : field){
                         // verificar se ja nao existe na classe atual
                            tempField = classe.getPrivateField().get(f.getName());
                            if(tempField == null)
                                tempField = classe.getPublicField().get(f.getName());
                            if(tempField != null)
                                error("Method already declared");
                            else {
                                tempField = (classe.getParent() != null ) ? classe.getParent().getPublicField(f.getName()) : null;
                                if(tempField == null) {
                                    error("Trying to override a non existent method");
                                } else {
                                    classe.getPublicField().put(f.getName(), f);
                                }
                            }
                        }
                    }
                } else { // Sem @override
                	for(Field f : field){
	                    tempField = classe.getField(f.getName());
	                    if(tempField != null)
	                        error( "Method already declared");
	                    else{
	                        if(qualifier.contains("private"))
	                            classe.getPrivateField().put(f.getName(),f);
	                        else
	                            classe.getPublicField().put(f.getName(), f);
	                    }
                	}
                } 
            } else if (lexer.token == Token.FUNC) {
                method = methodDec();
                if(qualifier.contains("override")) {
                	if(qualifier.contains("private")) {
                		error("Cannot override a private method!");
                    } else { // public with override
                        // verificar se ja nao existe na classe atual
                        tempMethod = classe.getPrivateMethod().get(method.getName());
                        if(tempMethod == null)
                            tempMethod = classe.getPublicMethod().get(method.getName());
                        if(tempMethod != null)
                            error("Method already declared");
                        else {
                            tempMethod = (classe.getParent() != null ) ? classe.getParent().getPublicMethod(method.getName()) : null;
                            if(tempMethod == null) {
                                error("Trying to override a non existent method");
                            } else {
                                classe.getPublicMethod().put(method.getName(), method);
                            }
                        }
                    }
                } else { // Sem @override
                    tempMethod = classe.getMethod(method.getName());
                    if(tempMethod != null)
                        error( "Method already declared");
                    else{
                        if(qualifier.contains("private"))
                            classe.getPrivateMethod().put(method.getName(),method);
                        else
                            classe.getPublicMethod().put(method.getName(),method);
                    }
                } 
            } else {
                break;
            }
        }
    }

    // Qualifier ::= “private” | “public” | “override” | “override” “public” | “final” | “final” “public” | “final” “override” | “final” “override” “public”
    private String qualifier() {
    	String qualifier = new String();
        if (lexer.token == Token.PRIVATE) {
        	qualifier = lexer.getStringValue();
            next();
        } else if (lexer.token == Token.PUBLIC) {
        	qualifier = lexer.getStringValue();
            next();
        } else if (lexer.token == Token.OVERRIDE) {
        	qualifier = lexer.getStringValue();
            next();
            if (lexer.token == Token.PUBLIC) {
            	qualifier.concat(" " + lexer.getStringValue());
                next();
            }
        } else if (lexer.token == Token.FINAL) {
        	qualifier = lexer.getStringValue();
            next();
            if (lexer.token == Token.PUBLIC) {
            	qualifier.concat(" " + lexer.getStringValue());
                next();
            } else if (lexer.token == Token.OVERRIDE) {
            	qualifier.concat(" " + lexer.getStringValue());
                next();
                if (lexer.token == Token.PUBLIC) {
                	qualifier.concat(" " + lexer.getStringValue());
                    next();
                }
            }
        }
        return qualifier;
    }

    // FieldDec ::= “var” Type IdList “;”
    private ArrayList<Field> fieldDec() {
        ArrayList<Field> field = new ArrayList<Field>();
        // já está sendo verificado se chamou o var, fique tranquilo...
    	next();
        Type type = type();
        ArrayList<String> idList = idList();

        for(String name: idList) {
            field.add(new Field(type, name));
        }

        if (lexer.token != Token.SEMICOLON) {
            error("Semicolon Expected");
        }
        return field;
    }

    // Type ::= BasicType | Id
    private Type type() {
    	Type type = null;
        if (lexer.token == Token.INT || lexer.token == Token.BOOLEAN || lexer.token == Token.STRING) {
            switch(lexer.token) {
                case INT: 
                    type = Type.intType;
                    break;
                case BOOLEAN:
                    type = Type.booleanType;
                    break;
                case STRING:
                    type = Type.stringType;
                    break;
                default:
                    break;
            }
        
            next();
        } else if(lexer.token == Token.ID) {
            if( (type = symbolTable.getInGlobal(lexer.getStringValue())) == null)
                error("Class not Found!");
            next();
        } else {
            error("A type was expected");
        }
        return type;
    }

 

    // IdList ::= Id { “,” Id }
    private ArrayList<String> idList() {
        ArrayList<String> temp = new ArrayList<String>();
        while (true) {
            if (lexer.token != Token.ID) {
                error("Identifier expected");
            } else
                temp.add(lexer.getStringValue());
            next();
            if (lexer.token == Token.COMMA) {
                next();
            } else {
                break;
            }
        }
        return temp;
    }

    // MethodDec ::= “func” IdColon FormalParamDec [ “->” Type ] “{” StatementList “}” | “func” Id [ “->” Type ] “{” StatementList “}”
    private Method methodDec() {
        Method method = null;
        String methodName = new String("");
        ArrayList<Field> parameters = null;
        Type t = Type.undefinedType;
        // Já esta verificando pela keyword "func" acima meu chapa, fique tranquilo...
        next();
        if (lexer.token == Token.ID) {
            methodName = lexer.getStringValue();
            next();

        } else if (lexer.token == Token.IDCOLON) {
            // keyword method. It has parameters
            methodName = lexer.getStringValue();
            next();
            parameters = formalParamDec();
            for (Field f: parameters){
                if( symbolTable.getInLocal(f.getName()) == null ) {
                    symbolTable.putInLocal( f.getName(),f.getType() );
                } else {
                    error("Duplicated parameter name");
                }
            }
        } else {
            error("An 'identifier' or 'identifier:' was expected after 'func' ");
        }

        if (lexer.token == Token.MINUS_GT) {
            next();
            t = type();
        }
        if (lexer.token != Token.LEFTCURBRACKET) {
            error("'{' expected");
        }
        next();
        statementList();
        if (lexer.token != Token.RIGHTCURBRACKET) {
            error("'}' expected");
        }

        next();
        method = new Method(t, parameters, methodName);
        return method;
    }

    // FormalParamDec ::= ParamDec [{ “,” ParamDec }]
    private ArrayList<Field> formalParamDec() {
        ArrayList<Field> temp = new ArrayList<Field>();
        temp.add(paramDec());
        while (lexer.token == Token.COMMA) {
            next();
            temp.add(paramDec());
        }
        return temp;
    }

    // ParamDec ::= Type Id
    private Field paramDec() {
        Field f = null;
        Type t = type();
        if (lexer.token != Token.ID) {
            error("Identifier Expected!");
        } else {
            f = new Field(t, lexer.getStringValue());
        }
        next();
        return f;
    }

    // StatementList ::= { Statement }
    private void statementList() {
        // only '}' is necessary in this test
        while (lexer.token != Token.RIGHTCURBRACKET && lexer.token != Token.UNTIL) {
            statement();
        }
    }

    // Statement ::= AssignExpr “;” | IfStat | WhileStat | ReturnStat “;” | 
    //              WriteStat “;” | “break” “;” | “;” | RepeatStat “;” | LocalDec “;” | AssertStat “;”
    private void statement() {
        boolean checkSemiColon = true;
        switch (lexer.token) {
            case IF:
                ifStat();
                checkSemiColon = false;
                break;
            case WHILE:
                whileStat();
                checkSemiColon = false;
                break;
            case RETURN:
                returnStat();
                break;
            case BREAK:
                breakStat();
                break;
            case SEMICOLON:
                next();
                break;
            case REPEAT:
                repeatStat();
                break;
            case VAR:
                localDec();
                break;
            case ASSERT:
                assertStat();
                break;
            default:
                if (lexer.token == Token.ID && lexer.getStringValue().equals("Out")) {
                    writeStat();
                } else if (lexer.token == Token.ID && lexer.getStringValue().equals("In")) {
                    readExpr();
                } else {
                    expr();
                }
        }
        if (checkSemiColon) {
            check(Token.SEMICOLON, "';' expected");
        }
    }

    // AssignExpr ::= Expression [ “=” Expression ]
    private void assignExpr() {
        expr();
        if (lexer.token == Token.ASSIGN) {
            next();
            expr();
        }
    }

    // Expression ::= SimpleExpression [ Relation SimpleExpression ]
    private Expr expr() {
        // TODO: Fazer isso aqui:
        simpleExpr();
        // Relation ::= “==” | “<” | “>” | “<=” | “>=” | “! =”
        if (lexer.token == Token.EQ || lexer.token == Token.LT || lexer.token == Token.GT || lexer.token == Token.LE || lexer.token == Token.GE || lexer.token == Token.NOT) {
            if (lexer.token == Token.NOT) {
                next();
                if (lexer.token != Token.ASSIGN) {
                    error("Expecting '=' in expression");
                } else {
                    next();
                }
            }
            simpleExpr();
        }
        return null;
    }

    // SimpleExpression ::= SumSubExpression { “++” SumSubExpression }
    private void simpleExpr() {
        sumSubExpr();
        while (lexer.token == Token.CONCAT) {
            next();
            sumSubExpr();
        }
    }

    // SumSubExpression ::= Term { LowOperator Term }
    private void sumSubExpr() {
        term();
        while (lexer.token == Token.PLUS || lexer.token == Token.MINUS || lexer.token == Token.OR) {
            next();
            term();
        }
    }

    // Term ::= SignalFactor { HighOperator SignalFactor }
    private void term() {
        signalFactor();
        while (lexer.token == Token.MULT || lexer.token == Token.DIV || lexer.token == Token.AND) {
            next();
            signalFactor();
        }
    }

    // HighOperator ::= “∗” | “/” | “&&”
    private void highOperator() {

    }

    // LowOperator ::= “+” | “−” | “||”	
    private void lowOperator() {

    }

    // SignalFactor ::= [ Signal ] Factor
    private void signalFactor() {
        if (lexer.token == Token.PLUS || lexer.token == Token.MINUS) {
            next();
        }
        factor();
    }

    // Signal ::= “+” | “−”
    private void signal() {

    }
    
    // Factor ::= BasicValue | “(” Expression “)” | “!” Factor | “nil” | ObjectCreation | PrimaryExpr
    private void factor() {
    	if(!startExpr(lexer.token)) {
    		error("Expected: BasicValue or (Expression) or !Factor or 'nil' or ObjectCreation or PrimaryExpr");
    	} if(lexer.token != Token.LITERALINT && lexer.token != Token.STRING && lexer.token != Token.TRUE && lexer.token != Token.FALSE) {
    		error("Basic value (int, string or boolean) expected!");
    	} else if(lexer.token == Token.LEFTPAR) {
    		next();
    		expr();
    		if(lexer.token != Token.RIGHTPAR)
    			error("')' expected!");
    	} else if (lexer.token == Token.NOT) {
    		next();
    		factor();
    	} else if (lexer.token == Token.ID){
    		primaryExpr();
    	} else {
    		primaryExpr();
    	}
    }

    // BasicValue ::= IntValue | BooleanValue | StringValue
    private void basicValue() {

    }

    // BooleanValue ::= “true” | “false”
    private void booleanValue() {

    }

    // ObjectCreation ::= Id “.” “new”
    private void objectCreation() {

    }

    /* PrimaryExpr ::= “super” “.” IdColon ExpressionList | “super” “.” Id | Id | Id “.” Id | 
	Id “.” IdColon ExpressionList | Id "." new | “self” |
	“self” “.” Id |
	“self” ”.” IdColon ExpressionList |
	“self” ”.” Id “.” IdColon ExpressionList |
	“self” ”.” Id “.” Id | 	ReadExpr
	 */
    // objectCreation foi adicionado aqui por questões de ambiguidade
    private boolean primaryExpr() {
    	boolean correct = true;
        if (lexer.token == Token.SUPER) {
            next();
            if (lexer.token != Token.DOT) {
                error("A '.' was expected after the 'super' keyword");
                correct = false;
            }
            next();
            if (lexer.token != Token.IDCOLON && lexer.token != Token.ID) {
                error("An identifier: or identifier were expected after the super call");
                correct = false;
            } else {
                next();
                if (lexer.token == Token.IDCOLON)
                	exprList();
            }
        } else if (lexer.token == Token.ID) {
            next();
            if (lexer.token == Token.DOT) {
                next();
                if (lexer.token != Token.IDCOLON && lexer.token != Token.ID && lexer.token != Token.NEW) {
                    error("An identifier: or identifier or a 'new' were expected after the id call");
                    correct = false;
                } else {
                    next();
                    if (lexer.token == Token.IDCOLON)
                    	exprList();
                }
            }
        } else if (lexer.token == Token.SELF) {
        	next();
        	if (lexer.token == Token.DOT) {
        		next();
        		if(lexer.token != Token.ID && lexer.token != Token.IDCOLON) {
        			error("An identifier: or identifier were expected after the self call");
                    correct = false;
        		} else if(lexer.token == Token.IDCOLON) {
        			next();
        			exprList();
        		} else if(lexer.token == Token.ID) {
        			next();
        			if(lexer.token == Token.DOT) {
        				next();
        				if(lexer.token != Token.IDCOLON && lexer.token != Token.ID) {
        					error("An identifier: or identifier were expected after the self.Id call");
        					correct = false;
        				} else {
        					next();
        					if (lexer.token == Token.IDCOLON)
        						exprList();
        				}
        			}
        		}
        	}
        } else {
            readExpr();
        }
        return correct;
    }

    // ExpressionList ::= Expression { “,” Expression } 
    private void exprList() {
    	expr();
    	while (lexer.token == Token.COMMA) {
    		next();
    		expr();
    	}
    }

    // ReadExpr ::= “In” “.” [ “readInt” | “readString” ]
    private void readExpr() {
        next();
        check(Token.DOT, "a '.' was expected after 'In'");
        next();
        if (lexer.getStringValue().equals("readInt") && lexer.getStringValue().equals("readString")) {
            error("'readInt' or 'readString' was expected after 'In.'");
        }
    }

    // Relation ::= “==” | “<” | “>” | “<=” | “>=” | “! =”
    private void relation() {

    }

    // IfStat ::= “if” Expression “{” Statement “}” [ “else” “{” Statement “}” ]
    private void ifStat() {
        next();
        expr();
        check(Token.LEFTCURBRACKET, "'{' expected after the 'if' expression");
        next();
        statementList(); 
        /* DUVIDA/Dúvida: Statement ou um StatementList ?
        while (lexer.token != Token.RIGHTCURBRACKET && lexer.token != Token.ELSE) {
            
        } */
        
        check(Token.RIGHTCURBRACKET, "'}' was expected");
        if (lexer.token == Token.ELSE) {
            next();
            check(Token.LEFTCURBRACKET, "'{' expected after 'else'");
            next();
            statementList();
            check(Token.RIGHTCURBRACKET, "'}' was expected");
            next();
        }
    }

    // WhileStat ::= “while” Expression “{” StatementList “}”
    private void whileStat() {
        next();
        expr();
        check(Token.LEFTCURBRACKET, "'{' expected after the 'while' expression");
        next();
        statementList();
        check(Token.RIGHTCURBRACKET, "'}' was expected");
        next();
    }

    // ReturnStat ::= “return” Expression
    private void returnStat() {
        next();
        expr();
    }

    // WriteStat ::= “Out” “.” [ “print:” | “println:” ] Expression
    private void writeStat() {
        next();
        check(Token.DOT, "a '.' was expected after 'Out'");
        next();
        if (!lexer.getStringValue().equals("print:") && !lexer.getStringValue().equals("println:")) {
            error("'print:' or 'println:' was expected after 'Out.'");
        }
        next();
        String printName = lexer.getStringValue();
        expr();
    }

    // RepeatStat ::= “repeat” StatementList “until” Expression
    private void repeatStat() {
        next();
        statementList();
        check(Token.UNTIL, "'until' was expected");
        next();
        expr();
    }

    // Não existe originalmente, mas em Statement eh ::= “break” “;”
    private void breakStat() {
        next();
    }

    // LocalDec ::= “var” Type IdList [ “=” Expression ] “;”
    private void localDec() {
        // TODO: Paramos aqui...
        ArrayList<String> identifiers = null;
        next();
        Type t = type();
        Expr e;
        identifiers = idList();
        
        if (lexer.token == Token.ASSIGN) {
        	next();
        	if( identifiers.size() == 1) {
        		// check if there is just one variable    
        		e = expr();
    		} else {
    			error("Ignore the next error! You can't assign a value when declaring multiple variables!");
    		}
        	
		}

    }

    /**
     * change this method to 'private'. uncomment it implement the methods it
     * calls
     */
    // AssertStat ::= “assert” Expression “,” StringValue
    public void assertStat() {

        next();
        // int lineNumber = lexer.getLineNumber();
        expr();
        if (lexer.token != Token.COMMA) {
            error("',' expected after the expression of the 'assert' statement");
        }
        next();
        if (lexer.token != Token.LITERALSTRING) {
            error("A literal string expected after the ',' of the 'assert' statement");
        }
        String message = lexer.getLiteralStringValue();
        next();
    }

    private int literalInt() {

        LiteralInt e = null;

        // the number value is stored in lexer.getToken().value as an object of
        // Integer.
        // Method intValue returns that value as an value of type int.
        int value = lexer.getNumberValue();
        next();
        return value;
    }

    private static boolean startExpr(Token token) {

        return token == Token.FALSE || token == Token.TRUE
                || token == Token.NOT || token == Token.SELF
                || token == Token.LITERALINT || token == Token.SUPER
                || token == Token.LEFTPAR || token == Token.NIL
                || token == Token.ID || token == Token.LITERALSTRING;

    }

    private SymbolTable symbolTable;
    private Lexer lexer;
    private ErrorSignaller signalError;

    private void error(String msg) {
        this.signalError.showError(msg);
    }

    private void next() {
        lexer.nextToken();
    }

    private void check(Token shouldBe, String msg) {
        if (lexer.token != shouldBe) {
            error(msg);
        }
    }

}
