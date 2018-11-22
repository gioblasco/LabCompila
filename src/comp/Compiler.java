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
        // TODO: Every program must have a class named Program with a parameterless method called run.
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
        	if(symbolTable.getInGlobal(className) != null)
        		error("A class with the name " +className+ " is already declared.");
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
       
    	if(symbolTable.getInGlobal(className) == null)
    		symbolTable.putInGlobal(className, classe);
    	
    	symbolTable.setCurrentClass(classe);

        memberList();
        if (lexer.token != Token.END) {
            error("'end' expected in class declaration");
        }
        
        next();

    }

    // MemberList ::= { [ Qualifier ] Member }
    private void memberList() { //checar se o nome do membro nao eh igual ao da classe
        CianetoClass classe = symbolTable.getCurrentClass();
        String qualifier;
    	while (true) {
            qualifier = qualifier();
            if(!classe.isOpen() && qualifier.contains("final")) {
            	error("A final class cannot have final members. It's redudant.");
            }
            if (lexer.token == Token.VAR) {
                fieldDec(qualifier);
            } else if (lexer.token == Token.FUNC) {
                methodDec(qualifier);
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
    private ArrayList<Field> fieldDec(String qualifier) {
        ArrayList<Field> field = new ArrayList<Field>();
        Field addField, tempField = null;
        CianetoClass classe = symbolTable.getCurrentClass();
        
        if(qualifier.contains("override") || qualifier.contains("public")) {
        	error("A field must be declared private");
        }
        
        // já está sendo verificado se chamou o var, fique tranquilo...
    	next();
        Type type = type();
        ArrayList<String> idList = idList();

        for(String name: idList) {
        	addField = new Field(type, name);
            field.add(addField);
            if(name.equals(classe.getName())) {
    			error("Field name is equal class name " + classe.getName());
    		}
            tempField = classe.getFieldList().get(name);
        	if(tempField != null)
        		error("Field with the name " +name+ " has already been declared.");
        	else
        		classe.getFieldList().put(name, addField);
        }
  
        if (lexer.token != Token.SEMICOLON) {
            error("Semicolon Expected");
        }
        next();
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
    private Method methodDec(String qualifier) {
        Method method = null, tempMethod = null;
        Field tempField = null;
        String methodName = new String("");
        ArrayList<Field> parameters = null;
        CianetoClass classe = symbolTable.getCurrentClass();
        Type t = Type.undefinedType;
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
                    symbolTable.putInLocal(f.getName(), f.getType());
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
        
        method = new Method(t, parameters, methodName);
        
        if(methodName.equals(classe.getName()))
        	error("Method name is equal to the class name");
        if(qualifier.contains("override")) {
        	if(qualifier.contains("private")) {
        		error("Cannot override a private method!");
            } else { // public with override
                // verificar se ja nao existe na classe atual
                tempMethod = classe.getPrivateMethod().get(method.getName());
                if(tempMethod == null)
                    tempMethod = classe.getPublicMethod().get(method.getName());
                if(tempMethod == null)
                	tempField = classe.getFieldList().get(method.getName());
                if(tempMethod != null|| tempField != null)
                	error("Method " +methodName+ " has the same name as another member in the scope.");
                else {
                    tempMethod = (classe.getParent() != null ) ? classe.getParent().getPublicMethod(method.getName()) : null;
                    if(tempMethod == null) {
                        error("Trying to override a non existent method " + methodName);
                    } else {
                        classe.getPublicMethod().put(method.getName(), method);
                    }
                }
            }
        } else { // Sem @override
            tempMethod = classe.getMethod(method.getName());
            tempField = classe.getFieldList().get(method.getName());
            if(tempMethod != null || tempField != null)
                error("Method " +methodName+ " has the same name as another member in the scope or in the parent (not using override)");
            else{
                if(qualifier.contains("private"))
                    classe.getPrivateMethod().put(method.getName(),method);
                else
                    classe.getPublicMethod().put(method.getName(),method);
            }
        } 
        
        if (lexer.token != Token.LEFTCURBRACKET) {
            error("'{' expected");
        }
        next();
       
        symbolTable.setCurrentMethod(method);
        
        statementList();
        if (lexer.token != Token.RIGHTCURBRACKET) {
            error("'}' expected");
        }

        next();
        
        symbolTable.removeLocalIdent();
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
                checkSemiColon = false;
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
                    assignExpr();
                }
        }
        if (checkSemiColon) {
            check(Token.SEMICOLON, "';' expected");
            next();
        }
    }

    // AssignExpr ::= Expression [ “=” Expression ]
    private void assignExpr() {
    	Type tipoAssign1 = Type.undefinedType, tipoAssign2 = Type.undefinedType;

        tipoAssign1 = expr();
        if (lexer.token == Token.ASSIGN) {
            next();
            tipoAssign2 = expr(); // verifica se Expr1 tem mesmo tipo ou é conversível para Expr2
            if(tipoAssign1 instanceof CianetoClass && (!(tipoAssign2 instanceof CianetoClass) && tipoAssign2 != Type.nullType)) {
        		error("Only allowed to assign a class with another instance of the same class or nil value");
        	} else if (tipoAssign1 instanceof CianetoClass && tipoAssign2 instanceof CianetoClass) {
        		if(!((CianetoClass)tipoAssign2).findParent(tipoAssign1.getName()))
        			error("Only allowed to assign a class with its subclasses");
        	} else if(tipoAssign1 == Type.stringType && (tipoAssign2 != Type.stringType && tipoAssign2 != Type.nullType)) {
        		error("Only allowed to assign a String with another String or nil value");
        	} else if(tipoAssign1 == Type.undefinedType || tipoAssign2 == Type.undefinedType) {
        		error("Trying to assign undefined types");
        	}  else if((tipoAssign1 == Type.intType || tipoAssign1 == Type.booleanType) && tipoAssign1 != tipoAssign2) {
        		error("Trying to assign incompatible types");
        	}
        }
    }

    // Expression ::= SimpleExpression [ Relation SimpleExpression ]
    private Type expr() {
    	Type tipoExpr1 = Type.undefinedType, tipoExpr2 = Type.undefinedType;
    	boolean erro = false;
    	
        tipoExpr1 = simpleExpr();
        // Relation ::= “==” | “<” | “>” | “<=” | “>=” | “! =”
        if(lexer.token == Token.LT || lexer.token == Token.GT || lexer.token == Token.LE || lexer.token == Token.GE) {
        	next();
        	tipoExpr2 = simpleExpr();
        	if(tipoExpr1 != Type.intType || tipoExpr2 != Type.intType) {
        		error("< > <= >= only applies to Int values");
        		erro = true;
        	} else {
        		tipoExpr1 = Type.booleanType;
        	}
        } else if(lexer.token == Token.EQ || lexer.token == Token.NEQ) {
        	next();
        	tipoExpr2 = simpleExpr();
        	if(tipoExpr1 instanceof CianetoClass && (!(tipoExpr2 instanceof CianetoClass) || tipoExpr2 != Type.nullType)) {
        		error("Only allowed to compare a class with another instance of the same class or nil value");
        		erro = true;
        	} else if (tipoExpr1 instanceof CianetoClass && tipoExpr2 instanceof CianetoClass) {
        		if(!((CianetoClass)tipoExpr2).findParent(tipoExpr1.getName())) {
        			error("Only allowed to compare a class with its subclasses");
        			erro = true;
        		}
        	} else if(tipoExpr1 == Type.stringType && (tipoExpr2 != Type.stringType || tipoExpr2 != Type.nullType)) {
        		error("Only allowed to compare a String with another String or nil value");
        		erro = true;
        	} else if(tipoExpr1 == Type.undefinedType && tipoExpr2 == Type.undefinedType) {
        		error("Trying to compare undefined types");
        		erro = true;
        	} else if (tipoExpr1 != tipoExpr2) {
        		error("Trying to compare incompatible types");
        		erro = true;
        	} else {
        		tipoExpr1 = Type.booleanType;
        	}
        }

        if(erro)
        	return Type.undefinedType;
        else
        	return tipoExpr1;
    }

    // SimpleExpression ::= SumSubExpression { “++” SumSubExpression }
    private Type simpleExpr() {
    	Type tipoSimple1 = Type.undefinedType, tipoSimple2 = Type.undefinedType;
    	boolean erro = false;
    	
        tipoSimple1 = sumSubExpr(); 
        while (lexer.token == Token.CONCAT) {
            next();
            tipoSimple2 = sumSubExpr(); // verifica se sumSubExpr é Int ou String
            if(tipoSimple1 != Type.intType && tipoSimple1 != Type.stringType || tipoSimple2 != Type.intType && tipoSimple2 != Type.stringType) {
            	error("Concatenation only applies to Int or String values");
            	erro = true;
            }
            tipoSimple1 = tipoSimple2;
        }
        
        if(erro)
        	return Type.undefinedType;
        else
        	return tipoSimple1;
    }

    // SumSubExpression ::= Term { LowOperator Term }
    private Type sumSubExpr() {
    	Type tipoSumSub1 = Type.undefinedType, tipoSumSub2 = Type.undefinedType;
    	boolean oroperator;
    	boolean erro = false;
    	
    	tipoSumSub1 = term();
        while (true) { // lowOperator
        	oroperator = false;
        	if(lexer.token != Token.PLUS && lexer.token != Token.MINUS && lexer.token != Token.OR)
        		break;
        	else if(lexer.token == Token.OR)
        		oroperator = true;
            next();
            tipoSumSub2 = term();
            if(!oroperator) {
            	if(tipoSumSub1 != Type.intType || tipoSumSub2 != Type.intType) { // verifica se Term1 e Term2 são Int (quando PLUS ou MINUS)
            		error("+ and - only apply to Int values");
            		erro = true;
            	}
            } else {
            	if(tipoSumSub1 != Type.booleanType || tipoSumSub2 != Type.booleanType) { // verifica se são Boolean (quando OR)
            		error("AND only apply to boolean values");
            		erro = true;
            	}
            }
            tipoSumSub1 = tipoSumSub2;
        }
        
        if(erro)
        	return Type.undefinedType;
        else
        	return tipoSumSub1;
    }

    // Term ::= SignalFactor { HighOperator SignalFactor }
    private Type term() {
    	Type tipoTerm1 = Type.undefinedType, tipoTerm2 = Type.undefinedType;
    	boolean andoperator;
    	boolean erro = false;
    	
        tipoTerm1 = signalFactor();
        while (true) { // highOperator
        	andoperator = false;
        	if(lexer.token != Token.MULT && lexer.token != Token.DIV && lexer.token != Token.AND)
        		break;
        	else if(lexer.token == Token.AND)
        		andoperator = true;
            next();
            tipoTerm2 = signalFactor();
            if(!andoperator) {
            	if(tipoTerm1 != Type.intType || tipoTerm2 != Type.intType) { // verifica se Term1 e Term2 são Int (quando MULT ou DIV)
            		error("* and / only apply to Int values");
            		erro = true;
            	}
            } else {
            	if(tipoTerm1 != Type.booleanType || tipoTerm2 != Type.booleanType) { // verifica se são Boolean (quando AND)
            		error("AND only apply to boolean values");
            		erro = true;
            	}
            }
            tipoTerm1 = tipoTerm2;
        }
        
        if(erro)
        	return Type.undefinedType;
        else
        	return tipoTerm1;
    }

    // SignalFactor ::= [ Signal ] Factor
    private Type signalFactor() {
    	boolean sinal = false;
        if (lexer.token == Token.PLUS || lexer.token == Token.MINUS) { // positivo ou negativo
            next();
            sinal = true;
        }
        Type tipoFactor = factor(); // se tiver signal, verifica se o factor é Int
        if(sinal)
        	if(tipoFactor != Type.intType)
        		error("Only int values accept prefixed plus or minus signals");
        
        return tipoFactor;
    }
    
    // Factor ::= BasicValue | “(” Expression “)” | “!” Factor | “nil” | ObjectCreation | PrimaryExpr
    private Type factor() {
    	Type tipoFactor = Type.undefinedType;
    	if(!startExpr(lexer.token))
    		error("Expected: BasicValue or (Expression) or !Factor or 'nil' or ObjectCreation or PrimaryExpr");
    	else if(lexer.token == Token.LITERALINT) {
    		tipoFactor = Type.intType;
    		next();
    	} else if(lexer.token == Token.LITERALSTRING) {
    		tipoFactor = Type.stringType;
    		next();
    	}
    	else if(lexer.token == Token.TRUE || lexer.token == Token.FALSE) {
    		tipoFactor = Type.booleanType;
    		next();
    	} else if(lexer.token == Token.LEFTPAR) {
    		next();
    		tipoFactor = expr();
    		if(lexer.token != Token.RIGHTPAR)
    			error("')' expected!");
    		else {
    			next();
    		}
    	} else if (lexer.token == Token.NOT) { // isso aqui nao parece certo, pode gerar "! id ! id ! id ! id"
    		next();
    		tipoFactor = factor();
    	} else if (lexer.token == Token.ID){
    		tipoFactor = primaryExpr();
    	} else {
    		tipoFactor = primaryExpr();
    	}
    	
    	return tipoFactor;
    }

    /* PrimaryExpr ::= “super” “.” IdColon ExpressionList | “super” “.” Id | Id | Id “.” Id | 
	Id “.” IdColon ExpressionList | Id "." new | “self” |
	“self” “.” Id |
	“self” ”.” IdColon ExpressionList |
	“self” ”.” Id “.” IdColon ExpressionList |
	“self” ”.” Id “.” Id | 	ReadExpr
	 */
    // objectCreation foi adicionado aqui por questões de ambiguidade
    private Type primaryExpr() { 
    	CianetoClass classe = symbolTable.getCurrentClass();
    	CianetoClass parent = classe.getParent();
    	Method currentMethod = null;
    	Field currentField = null;
    	Type tipoPrimary = Type.undefinedType;
    	
        if (lexer.token == Token.SUPER) {
            next();
            if (lexer.token != Token.DOT) {
                error("A '.' was expected after the 'super' keyword");
            }
            next();
            if (lexer.token != Token.IDCOLON && lexer.token != Token.ID) {
                error("An identifier: or identifier were expected after the super call");
            } else {
            	if(parent == null)
            		error("Class " + classe.getName() + " doesn't extend another class");
            	if (lexer.token == Token.IDCOLON) {
            		if(parent != null) {
            			currentMethod = parent.getMethod(lexer.getStringValue());
            			if(currentMethod == null)
            				error("Superclass " +parent.getName()+" has no method named " + lexer.getStringValue());
            		}
            		next();
            		ArrayList<Type> retorno = exprList();
            		if(currentMethod != null) {
            			String result = currentMethod.checkSignature(retorno);
            			if (!result.equals(""))
            				error("Wrong usage of the method " + currentMethod.getName() + ": " + result);
            			else
            				tipoPrimary = currentMethod.getType();
            		}
            	} else if(lexer.token == Token.ID) {
            		if(parent != null) {
            			currentMethod = parent.getPublicMethod(lexer.getStringValue());
            			if(currentMethod == null)
            				error("Superclass "+parent.getName()+" has no method named " + lexer.getStringValue());
            			else {
            				String result = currentMethod.checkSignature(null);
            				if (!result.equals(""))
            					error("Wrong usage of the method " + currentMethod.getName() + ": " + result);
            				else
            					tipoPrimary = currentMethod.getType();
            			}
            		}
            		next();
            	}
            }
        } else if (lexer.token == Token.ID) { // Se não for super, mas sim um id
        	String identifier = lexer.getStringValue();
            next();
            if (lexer.token == Token.DOT) {
                next();
                if (lexer.token != Token.IDCOLON && lexer.token != Token.ID && lexer.token != Token.NEW) {
                    error("An identifier: or identifier or a 'new' were expected after the id call");
                } else {
                	if(lexer.token == Token.ID) {
                		Type t  =  symbolTable.getInLocal(identifier);
                		if(t == null || t == Type.undefinedType)
                			error("Trying to use a object that does not exist");
                		else {
                			classe = (CianetoClass) t;
                			currentMethod = classe.getMethod(lexer.getStringValue());
                			currentField = classe.getFieldList().get(lexer.getStringValue());
                			
                			if(currentMethod == null && currentField == null)
                				error("Class " + classe.getName() + " has no method or field named " + lexer.getStringValue());
                			else if(currentMethod != null) {
                				String result = currentMethod.checkSignature(null);
                				if (!result.equals(""))
                					error("Wrong usage of the method " + currentMethod.getName() + ": " + result);
                				else
                					tipoPrimary = currentMethod.getType();
                			} else {
                				tipoPrimary = currentField.getType();
                			}
                			next();
                		}
                	} else if (lexer.token == Token.IDCOLON) {
                		Type t  =  symbolTable.getInLocal(identifier);
                		if(t == null || t == Type.undefinedType)
                			error("Trying to use a object that does not exist");
                		else {
                			classe = (CianetoClass) t;
                			currentMethod = classe.getMethod(lexer.getStringValue());
                			if(currentMethod == null)
                				error("Class "+classe.getName()+" has no method named " + lexer.getStringValue());
                		}
                		next();
                		ArrayList<Type> retorno = exprList();
                		if(currentMethod != null) {
                			String result = currentMethod.checkSignature(retorno);
                			if (!result.equals(""))
                				error("Wrong usage of the method " + result);
                			else
                				tipoPrimary = currentMethod.getType();
                		}
                	} else if (lexer.token == Token.NEW) {
                		if( (classe = (CianetoClass)symbolTable.getInGlobal(identifier)) == null )
                			error("Trying to create a new instance of an undefined class " + identifier);
                		else
                			tipoPrimary = Type.nullType;
                		next();
                	}                     	
                }
            } else {
            	Type t  =  symbolTable.getInLocal(identifier);
        		if(t == null || t == Type.undefinedType)
        			error("Trying to use a object that does not exist " + identifier);
        		else
        			tipoPrimary = t;
            }
        } else if (lexer.token == Token.SELF) {
        	next();
        	if (lexer.token == Token.DOT) {
        		next();
        		if(lexer.token != Token.ID && lexer.token != Token.IDCOLON) {
        			error("An identifier: or identifier were expected after the self call");
        		} else if(lexer.token == Token.IDCOLON) { // chama metodo de self
        			if(classe != null) {
            			currentMethod = classe.getMethod(lexer.getStringValue());
            			if(currentMethod == null)
            				error("Class " +classe.getName()+ " has no method named " + lexer.getStringValue());
            		}
            		next();
            		ArrayList<Type> retorno = exprList();
            		if(currentMethod != null) {
            			String result = currentMethod.checkSignature(retorno);
            			if (!result.equals(""))
            				error("Wrong usage of the method " + result);
            			else
            				tipoPrimary = currentMethod.getType();
            		}
        		} else if(lexer.token == Token.ID) { // chama metodo ou campo de self ou metodo de campo (classe) de self 
        			if(classe != null) {
            			currentMethod = classe.getMethod(lexer.getStringValue());
            			currentField = classe.getFieldList().get(lexer.getStringValue());
            			if(currentMethod == null && currentField == null)
            				error("Class " + classe.getName() + " has no method or field named " + lexer.getStringValue());
            			else if(currentMethod != null) {
            				String result = currentMethod.checkSignature(null);
            				if (!result.equals(""))
            					error("Wrong usage of the method " + currentMethod.getName() + ": " + result);
            			}
        			}
        			next();
        			if(lexer.token == Token.DOT) {
        				next();
        				if(lexer.token != Token.IDCOLON && lexer.token != Token.ID) {
        					error("An identifier: or identifier were expected after the self.Id call");
        				} else {
        					String memberName = lexer.getStringValue();
        					if (lexer.token == Token.IDCOLON) { 
        						next();
        						ArrayList<Type> retorno = exprList();
        						if(currentField != null && (currentField.getType() instanceof CianetoClass)) {
        							classe = (CianetoClass) symbolTable.getInGlobal(currentField.getName());
        							if(classe == null)
        								error("Trying to call a method from a field, which is not a class");
        							else {
        								currentMethod = classe.getMethod(memberName);
        								if(currentMethod == null)
        									error("Calling method " +memberName+ " that doesn't exist in class " + classe.getName());
        								else {
        									String result = currentMethod.checkSignature(retorno);
                	            			if (!result.equals(""))
                	            				error("Wrong usage of the method " + memberName + ": " + result);
                	            			else
                	            				tipoPrimary = currentMethod.getType();
        								}
        							}
        						} else {
        							error("Trying to call a method from a property that is not a class");
        						}
        					} else if(lexer.token == Token.ID) {
        						if(currentField != null && (currentField.getType() instanceof CianetoClass)) {
        							classe = (CianetoClass) symbolTable.getInGlobal(currentField.getType().getName());
        							if(classe == null)
        								error("Trying to call a method from a field that is not a class");
        							else {
        								currentMethod = classe.getMethod(memberName);
        								if(currentMethod == null)
        									error("Calling member " +memberName+ " that doesn't exist in class " + classe.getName());
        								else if(currentMethod != null){
        									String result = currentMethod.checkSignature(null);
                	            			if (!result.equals(""))
                	            				error("Wrong usage of the method " + memberName + ": " + result);
                	            			else
                	            				tipoPrimary = currentMethod.getType();
        								}
        							}	
        						} else {
        							error("Trying to call a method from a property that is not a class");
        						}
        						next();
        					}
        				}
        			} else {
        				if(currentMethod != null)
        					tipoPrimary = currentMethod.getType();
        				else if(currentField != null)
        					tipoPrimary = currentField.getType();
        			}
        		}
        	} else {
        		if(classe != null)
        			tipoPrimary = classe;
        	}
        } else {
            tipoPrimary = readExpr();
        }
        return tipoPrimary;
    }

    // ExpressionList ::= Expression { “,” Expression } 
    private ArrayList<Type> exprList() {
    	ArrayList<Type> at = new ArrayList<Type>();
    	at.add(expr());
    	while (lexer.token == Token.COMMA) {
    		next();
    		at.add(expr());
    	}
    	return at;
    }

    // ReadExpr ::= “In” “.” [ “readInt” | “readString” ]
    private Type readExpr() {
    	Type tipoRead = Type.undefinedType;
        next();
        check(Token.DOT, "a '.' was expected after 'In'");
        next();
        if (!lexer.getStringValue().equals("readInt") && !lexer.getStringValue().equals("readString")) {
            error("'readInt' or 'readString' was expected after 'In.'");
        } else if(lexer.getStringValue().equals("readInt")) {
        	tipoRead = Type.intType;
        } else {
        	tipoRead = Type.stringType;
        }    
        next();
        
        return tipoRead;
    }

    // IfStat ::= “if” Expression “{” Statement “}” [ “else” “{” Statement “}” ]
    private void ifStat() {
        next();
        Type tipo = expr();
        if(tipo != Type.booleanType)
        	error("If condition must be boolean");
        check(Token.LEFTCURBRACKET, "'{' expected after the 'if' expression");
        next();
        statementList();         
        check(Token.RIGHTCURBRACKET, "'}' was expected");
        next();
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
        Type tipo = expr();
        if(tipo != Type.booleanType)
        	error("While condition must be boolean");
        check(Token.LEFTCURBRACKET, "'{' expected after the 'while' expression");
        next();
        statementList();
        check(Token.RIGHTCURBRACKET, "'}' was expected");
        next();
    }

    // ReturnStat ::= “return” Expression
    private void returnStat() { 
    	Method currentMethod = symbolTable.getCurrentMethod();
        next();
        Type tipo = expr();
        if(currentMethod != null)
        	if(currentMethod.getType() != tipo)
        		error("Type of return expression isn't the same of the method declaration");
    
    }

    // WriteStat ::= “Out” “.” [ “print:” | “println:” ] Expression
    private void writeStat() {
        next();
        check(Token.DOT, "a '.' was expected after 'Out'");
        next();
        if (!lexer.getStringValue().equals("print:") && !lexer.getStringValue().equals("println:"))
            error("'print:' or 'println:' was expected after 'Out.'");
    	next();
    	Type tipo = expr();
    	if(tipo != Type.stringType && tipo != Type.intType)
    		error("Print or println parameter must be string or int");
    }

    // RepeatStat ::= “repeat” StatementList “until” Expression
    private void repeatStat() {
        next();
        statementList();
        check(Token.UNTIL, "'until' was expected");
        next();
        Type tipo = expr();
        if(tipo != Type.booleanType)
        	error("Repeat until condition must be boolean");
    }

    // Não existe originalmente, mas em Statement eh ::= “break” “;”
    private void breakStat() {
        next();
    }

    // LocalDec ::= “var” Type IdList [ “=” Expression ] “;”
    private void localDec() {
        next();
        Type t = type();
        Type e;
        ArrayList<String> identifiers = idList(); // verificar se nao eh repetido
        for(String id : identifiers) {
        	if(symbolTable.getInLocal(id) != null)
        		error("There's another variable with the same name ("+id+")");
        	else
        		symbolTable.putInLocal(id, t);
        }
        
        if (lexer.token == Token.ASSIGN) {
        	next();
        	e = expr();
        	if( identifiers.size() == 1) {
        		// check if there is just one variable    
        		if (t != e) {
        			error("Trying to assign an expression of type " + e.getName() + "to an variable of type "+ t.getName());
        		}
    		} else {
    			error("You can't assign a value when declaring multiple variables!");
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
        Type tipo = expr();
        if(tipo != Type.booleanType)
        	error("Assert condition must have boolean type");
        if (lexer.token != Token.COMMA) {
            error("',' expected after the expression of the 'assert' statement");
        }
        next();
        if (lexer.token != Token.LITERALSTRING) {
            error("A literal string expected after the ',' of the 'assert' statement");
        }
        next();
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
