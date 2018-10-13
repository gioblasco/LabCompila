/*
 * Integrantes: 
 * Giovanna Blasco Martin - 620378
 * Mateus Silva Vasconcelos - 620580
 */
package comp;

import java.io.PrintWriter;
import java.util.ArrayList;
import ast.CianetoClass;
import ast.LiteralInt;
import ast.MetaobjectAnnotation;
import ast.Program;
import ast.Statement;
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
        if (lexer.getStringValue().equals("open")) {
            next();
        }
        if (lexer.token != Token.CLASS) {
            error("'class' expected");
        }
        next();
        if (lexer.token != Token.ID) {
            error("Identifier expected");
        }
        String className = lexer.getStringValue();
        next();
        if (lexer.token == Token.EXTENDS) {
            next();
            if (lexer.token != Token.ID) {
                error("Identifier expected");
            }
            String superclassName = lexer.getStringValue();

            next();
        }

        memberList();
        if (lexer.token != Token.END) {
            error("'end' expected");
        }
        next();

    }

    // MemberList ::= { [ Qualifier ] Member }
    private void memberList() {
        while (true) {
            qualifier();
            if (lexer.token == Token.VAR) {
                fieldDec();
            } else if (lexer.token == Token.FUNC) {
                methodDec();
            } else {
                break;
            }
        }
    }

    // Qualifier ::= “private” | “public” | “override” | “override” “public” | “final” | “final” “public” | “final” “override” | “final” “override” “public”
    private void qualifier() {
        if (lexer.token == Token.PRIVATE) {
            next();
        } else if (lexer.token == Token.PUBLIC) {
            next();
        } else if (lexer.token == Token.OVERRIDE) {
            next();
            if (lexer.token == Token.PUBLIC) {
                next();
            }
        } else if (lexer.token == Token.FINAL) {
            next();
            if (lexer.token == Token.PUBLIC) {
                next();
            } else if (lexer.token == Token.OVERRIDE) {
                next();
                if (lexer.token == Token.PUBLIC) {
                    next();
                }
            }
        }
    }

    // FieldDec ::= “var” Type IdList “;”
    private void fieldDec() {
        if (lexer.token != Token.VAR) {
            error("Missing 'var' keyword");
        }
        next();
        type();
        idList();

        if (lexer.token != Token.SEMICOLON) {
            error("Semicolon Expected");
        }
    }

    // Type ::= BasicType | Id
    private void type() {
        if (lexer.token == Token.INT || lexer.token == Token.BOOLEAN || lexer.token == Token.STRING) {
            next();
        } else if (lexer.token == Token.ID) {
            next();
        } else {
            this.error("A type was expected");
        }

    }

    // BasicType ::= “Int” | “Boolean” | “String”
    private void basicType() {

    }

    // IdList ::= Id { “,” Id }
    private void idList() {
        while (true) {
            if (lexer.token != Token.ID) {
                error("Identifier expected");
            }
            next();
            if (lexer.token == Token.COMMA) {
                next();
            } else {
                break;
            }
        }
    }

    // MethodDec ::= “func” IdColon FormalParamDec [ “->” Type ] “{” StatementList “}” | “func” Id [ “->” Type ] “{” StatementList “}”
    private void methodDec() {
        if (lexer.token != Token.FUNC) {
            error("'func' keyword expected!");
        }

        next();
        if (lexer.token == Token.ID) {
            // unary method
            next();

        } else if (lexer.token == Token.IDCOLON) {
            // keyword method. It has parameters
            next();
            formalParamDec();

        } else {
            error("An 'identifier' or 'identifier:' was expected after 'func' ");
        }
        if (lexer.token == Token.MINUS_GT) {
            // method declared a return type
            next();
            type();
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

    }

    // FormalParamDec ::= ParamDec [{ “,” ParamDec }]
    private void formalParamDec() {
        paramDec();
        while (lexer.token == Token.COMMA) {
            next();
            paramDec();
        }
    }

    // ParamDec ::= Type Id
    private void paramDec() {
        type();
        if (lexer.token != Token.ID) {
            error("Identifier Expected!");
        }
        next();
    }

    // StatementList ::= { Statement }
    private void statementList() {
        // only '}' is necessary in this test
        while (lexer.token != Token.RIGHTCURBRACKET) {
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
    private void expr() {
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

    }

    // BasicValue ::= IntValue | BooleanValue | StringValue
    private void basicValue() {

    }

    // BooleanValue ::= “true” | “false”
    private void booleanValue() {

    }

    // ObjectCreation ::= Id “.” “new”
    private void objectCreation() {
        if (lexer.token != Token.ID)
            error("Identifier expected in object creation");
        next();
        if (lexer.token != Token.DOT)
            error("Dot expected");
        next();
        if (lexer.token != Token.NEW)
            error("New expected in object creation");
        next();
 
    }

    /* PrimaryExpr ::= “super” “.” IdColon ExpressionList | “super” “.” Id | Id | Id “.” Id |
	Id “.” IdColon ExpressionList | “self” |
	“self” “.” Id |
	“self” ”.” IdColon ExpressionList |
	“self” ”.” Id “.” IdColon ExpressionList |
	“self” ”.” Id “.” Id |
	ReadExpr */
    private void primaryExpr() {
        if (lexer.token == Token.SUPER) {
            next();
            if (lexer.token != Token.DOT) {
                error("A '.' was expected after the 'super' keyword");
            }
            next();
            if (lexer.token != Token.IDCOLON && lexer.token != Token.ID) {
                error("An idcolon or an id were expected after the super call");
            } else if (lexer.token != Token.IDCOLON) {
                next();
                exprList();
            }
        } else if (lexer.token == Token.ID) {
            next();
            if (lexer.token == Token.DOT) {
                next();
                if (lexer.token != Token.IDCOLON && lexer.token != Token.ID) {
                    error("An idcolon or an id were expected after the id call");
                } else if (lexer.token != Token.IDCOLON) {
                    next();
                    exprList();
                }
            }
        } else if (lexer.token == Token.SELF) {

        } else {
            readExpr();
        }
    }

    // ExpressionList ::= Expression { “,” Expression } 
    private void exprList() {

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
        while (lexer.token != Token.RIGHTCURBRACKET && lexer.token != Token.ELSE) {
            statement();
        }
        check(Token.RIGHTCURBRACKET, "'}' was expected");
        if (lexer.token == Token.ELSE) {
            next();
            check(Token.LEFTCURBRACKET, "'{' expected after 'else'");
            next();
            while (lexer.token != Token.RIGHTCURBRACKET) {
                statement();
            }
            check(Token.RIGHTCURBRACKET, "'}' was expected");
        }
    }

    // WhileStat ::= “while” Expression “{” StatementList “}”
    private void whileStat() {
        next();
        expr();
        check(Token.LEFTCURBRACKET, "'{' expected after the 'while' expression");
        next();
        while (lexer.token != Token.RIGHTCURBRACKET) {
            statement();
        }
        check(Token.RIGHTCURBRACKET, "'}' was expected");
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
        if (lexer.getStringValue().equals("print:") && lexer.getStringValue().equals("println:")) {
            error("'print:' or 'println:' was expected after 'Out.'");
        }
        String printName = lexer.getStringValue();
        expr();
    }

    // RepeatStat ::= “repeat” StatementList “until” Expression
    private void repeatStat() {
        next();
        while (lexer.token != Token.UNTIL && lexer.token != Token.RIGHTCURBRACKET) {
            statement();
        }
        check(Token.UNTIL, "'until' was expected");
    }

    // Não existe originalmente, mas em Statement eh ::= “break” “;”
    private void breakStat() {
        next();
    }

    // LocalDec ::= “var” Type IdList [ “=” Expression ] “;”
    private void localDec() {
        next();
        type();
        check(Token.ID, "A variable name was expected");
        while (lexer.token == Token.ID) {
            next();
            if (lexer.token == Token.COMMA) {
                next();
            } else {
                break;
            }
        }
        if (lexer.token == Token.ASSIGN) {
            next();
            // check if there is just one variable
            expr();
        }

    }

    /**
     * change this method to 'private'. uncomment it implement the methods it
     * calls
     */
    // AssertStat ::= “assert” Expression “,” StringValue
    public Statement assertStat() {

        next();
        int lineNumber = lexer.getLineNumber();
        expr();
        if (lexer.token != Token.COMMA) {
            this.error("',' expected after the expression of the 'assert' statement");
        }
        next();
        if (lexer.token != Token.LITERALSTRING) {
            this.error("A literal string expected after the ',' of the 'assert' statement");
        }
        String message = lexer.getLiteralStringValue();
        next();
        if (lexer.token == Token.SEMICOLON) {
            next();
        }

        return null;
    }

    private LiteralInt literalInt() {

        LiteralInt e = null;

        // the number value is stored in lexer.getToken().value as an object of
        // Integer.
        // Method intValue returns that value as an value of type int.
        int value = lexer.getNumberValue();
        next();
        return new LiteralInt(value);
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
