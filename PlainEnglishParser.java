// Zahidullah


package PlainEnglish;

import PlainEnglish.AST.andORor;
import PlainEnglish.AST.asteriskORslashORpercent;
import PlainEnglish.AST.compareOps;
import PlainEnglish.AST.plusORhyphen;
import java.util.List;
import java.util.Optional;

// Recursive-descent parser that turns a token list into an AST
public class PlainEnglishParser {

    private final TokenManager tokenManager;

    public PlainEnglishParser(List<Token> tokens) {
        this.tokenManager = new TokenManager(tokens);
    }

    
    private void requireNewLine() throws SyntaxErrorException {
        if (tokenManager.done()) return;
        if (tokenManager.nextIs(Token.TokenTypes.DEDENT)) return;
        if (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()) {
            throw new SyntaxErrorException(
                "Expected NEWLINE",
                tokenManager.getCurrentLine(),
                tokenManager.getCurrentColumnNumber()
            );
        }
        while (tokenManager.nextIs(Token.TokenTypes.NEWLINE)) {
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }
    }

    
    public Optional<ProgramNode> program() throws SyntaxErrorException {
        ProgramNode program = new ProgramNode();

        while (!tokenManager.done()) {
            if (tokenManager.nextIs(Token.TokenTypes.NEWLINE)) {
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
                continue;
            }
            if (tokenManager.nextIs(Token.TokenTypes.A)
                    || tokenManager.nextIs(Token.TokenTypes.AN)) {
                typeDef().ifPresent(td -> program.typedef.add(td));
                continue;
            }
            if (tokenManager.nextIs(Token.TokenTypes.TO)) {
                method().ifPresent(m -> program.method.add(m));
                continue;
            }
            throw new SyntaxErrorException(
                "Unexpected token at top level: "
                    + tokenManager.peek(0).map(Object::toString).orElse("EOF"),
                tokenManager.getCurrentLine(),
                tokenManager.getCurrentColumnNumber()
            );
        }
        return Optional.of(program);
    }

    
    private Optional<TypeDefNode> typeDef() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.A).isEmpty()
                && tokenManager.matchAndRemove(Token.TokenTypes.AN).isEmpty()) {
            return Optional.empty();
        }

        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (nameToken.isEmpty()) {
            throw new SyntaxErrorException("Expected type name after 'a'/'an'",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        if (tokenManager.matchAndRemove(Token.TokenTypes.IS).isEmpty()) {
            throw new SyntaxErrorException(
                "Expected 'is' after type name '" + nameToken.get().Value.orElse("") + "'",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        requireNewLine();

        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Expected indented block after typedef header",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        TypeDefNode node = new TypeDefNode();
        node.name = nameToken.get().Value.orElse("");
        boolean gotOne = false;

        while (!tokenManager.nextIs(Token.TokenTypes.DEDENT) && !tokenManager.done()) {
            if (tokenManager.nextIs(Token.TokenTypes.NEWLINE)) {
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
                continue;
            }
            Optional<FieldNode> f = field();
            if (f.isPresent()) {
                node.field.add(f.get());
                gotOne = true;
            } else {
                throw new SyntaxErrorException("Expected field declaration inside type body",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }

        if (!gotOne) {
            throw new SyntaxErrorException("Type '" + node.name + "' has no fields",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            throw new SyntaxErrorException("Expected DEDENT to close type body",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        while (tokenManager.nextIs(Token.TokenTypes.NEWLINE)) {
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }
        return Optional.of(node);
    }

    
    // field  =  IDENTIFIER IDENTIFIER NEWLINE+
    
    private Optional<FieldNode> field() throws SyntaxErrorException {
        Optional<Token> typeToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (typeToken.isEmpty()) return Optional.empty();

        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (nameToken.isEmpty()) {
            throw new SyntaxErrorException(
                "Expected field name after type '" + typeToken.get().Value.orElse("") + "'",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        requireNewLine();

        FieldNode fn = new FieldNode();
        fn.type = typeToken.get().Value.orElse("");
        fn.name = nameToken.get().Value.orElse("");
        return Optional.of(fn);
    }

    
    private Optional<MethodNode> method() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.TO).isEmpty()) return Optional.empty();

        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (nameToken.isEmpty()) {
            throw new SyntaxErrorException("Expected method name after 'to'",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        MethodNode mn = new MethodNode();
        mn.name = nameToken.get().Value.orElse("");

        // Optional: "a ClassName"
        if (tokenManager.nextIs(Token.TokenTypes.A)) {
            tokenManager.matchAndRemove(Token.TokenTypes.A);
            Optional<Token> ct = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
            if (ct.isEmpty()) {
                throw new SyntaxErrorException("Expected class name after 'a'",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            mn.className = Optional.of(ct.get().Value.orElse(""));
        }

        // Optional: "with" parameters
        if (tokenManager.nextIs(Token.TokenTypes.WITH)) {
            tokenManager.matchAndRemove(Token.TokenTypes.WITH);
            mn.with = true;

            Optional<ParameterNode> p = parameter();
            if (p.isEmpty()) {
                throw new SyntaxErrorException("Expected parameter after 'with'",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            mn.parameter.add(p.get());

            while (tokenManager.nextIs(Token.TokenTypes.COMMA)) {
                tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                Optional<ParameterNode> np = parameter();
                if (np.isEmpty()) {
                    throw new SyntaxErrorException("Expected parameter after ','",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                mn.parameter.add(np.get());
            }
        }

        requireNewLine();
        mn.statementblock = statementBlock();
        return Optional.of(mn);
    }

    
    // parameter  =  IDENTIFIER ("named" IDENTIFIER)?
    // 
    private Optional<ParameterNode> parameter() throws SyntaxErrorException {
        Optional<Token> typeToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (typeToken.isEmpty()) return Optional.empty();

        ParameterNode pn = new ParameterNode();
        pn.paramType = typeToken.get().Value.orElse("");

        if (tokenManager.nextIs(Token.TokenTypes.NAMED)) {
            tokenManager.matchAndRemove(Token.TokenTypes.NAMED);
            pn.named = true;
            Optional<Token> alias = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
            if (alias.isEmpty()) {
                throw new SyntaxErrorException("Expected name after 'named'",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            pn.nameOverride = Optional.of(alias.get().Value.orElse(""));
        }
        return Optional.of(pn);
    }

    
    // statementBlock  =  INDENT Statement+ DEDENT
    
    private StatementBlockNode statementBlock() throws SyntaxErrorException {
        StatementBlockNode block = new StatementBlockNode();

        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Expected INDENT to open statement block",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        while (!tokenManager.nextIs(Token.TokenTypes.DEDENT) && !tokenManager.done()) {
            if (tokenManager.nextIs(Token.TokenTypes.NEWLINE)) {
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
                continue;
            }
            Optional<StatementNode> stmt = statement();
            if (stmt.isPresent()) {
                block.statement.add(stmt.get());
            } else {
                throw new SyntaxErrorException("Expected statement inside block",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }

        if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            throw new SyntaxErrorException("Expected DEDENT to close statement block",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        while (tokenManager.nextIs(Token.TokenTypes.NEWLINE)) {
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }
        return block;
    }

    
    // statement  =  If | Loop | Set | Make | FunctionCall
    
    private Optional<StatementNode> statement() throws SyntaxErrorException {
        StatementNode stmt = new StatementNode();

        if (tokenManager.nextIs(Token.TokenTypes.IF)) {
            stmt.$if = parseIf();
            return Optional.of(stmt);
        }
        if (tokenManager.nextIs(Token.TokenTypes.LOOP)) {
            stmt.loop = parseLoop();
            return Optional.of(stmt);
        }
        if (tokenManager.nextIs(Token.TokenTypes.SET)) {
            stmt.set = set();
            return Optional.of(stmt);
        }
        if (tokenManager.nextIs(Token.TokenTypes.MAKE)) {
            stmt.make = make();
            return Optional.of(stmt);
        }
        if (tokenManager.nextIs(Token.TokenTypes.IDENTIFIER)) {
            stmt.functioncall = functionCall();
            return Optional.of(stmt);
        }
        return Optional.empty();
    }

    
    // If  =  "if" BoolExpTerm NEWLINE+ StatementBlock ("else" NEWLINE StatementBlock)?
    
    private Optional<IfNode> parseIf() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.IF).isEmpty()) return Optional.empty();

        IfNode ifNode = new IfNode();
        ifNode.boolexpterm = boolExpTerm();
        requireNewLine();
        ifNode.statementblock = statementBlock();

        // Optional else clause
        if (tokenManager.nextIs(Token.TokenTypes.ELSE)) {
            tokenManager.matchAndRemove(Token.TokenTypes.ELSE);
            ifNode.$else = true;
            requireNewLine();
            ifNode.falseCase = Optional.of(statementBlock());
        }
        return Optional.of(ifNode);
    }

    
    // Loop  =  "loop" BoolExpTerm NEWLINE+ StatementBlock
    
    private Optional<LoopNode> parseLoop() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isEmpty()) return Optional.empty();

        LoopNode loopNode = new LoopNode();
        loopNode.boolexpterm = boolExpTerm();
        requireNewLine();
        loopNode.statementblock = statementBlock();
        return Optional.of(loopNode);
    }

    
    // Set  =  "set" VariableReference "to" Expression NEWLINE+
    
    private Optional<SetNode> set() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.SET).isEmpty()) return Optional.empty();

        VariableReferenceNode varRef = variableReference();

        if (tokenManager.matchAndRemove(Token.TokenTypes.TO).isEmpty()) {
            throw new SyntaxErrorException("Expected 'to' after variable in set statement",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        ExpressionNode expr = expression();
        if (!tokenManager.done()) requireNewLine();

        SetNode sn = new SetNode();
        sn.variablereference = varRef;
        sn.expression = expr;
        return Optional.of(sn);
    }

    
    // Make  =  "make" IDENTIFIER "named" IDENTIFIER NEWLINE+
    
    private Optional<MakeNode> make() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.MAKE).isEmpty()) return Optional.empty();

        Optional<Token> typeToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (typeToken.isEmpty()) {
            throw new SyntaxErrorException("Expected type name after 'make'",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        if (tokenManager.matchAndRemove(Token.TokenTypes.NAMED).isEmpty()) {
            throw new SyntaxErrorException("Expected 'named' after type in make statement",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (nameToken.isEmpty()) {
            throw new SyntaxErrorException("Expected variable name after 'named'",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        requireNewLine();

        MakeNode mn = new MakeNode();
        mn.type = typeToken.get().Value.orElse("");
        mn.name = nameToken.get().Value.orElse("");
        return Optional.of(mn);
    }

    
    private Optional<FunctionCallNode> functionCall() throws SyntaxErrorException {
        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (nameToken.isEmpty()) return Optional.empty();

        FunctionCallNode fc = new FunctionCallNode();
        fc.name = nameToken.get().Value.orElse("");

        
        
        
        if (tokenManager.nextIs(Token.TokenTypes.IDENTIFIER)) {
            Optional<Token.TokenTypes> afterNext = tokenManager.peek(1);
            boolean nextIsObj = afterNext.isEmpty()
                || afterNext.get() == Token.TokenTypes.WITH
                || afterNext.get() == Token.TokenTypes.NEWLINE
                || afterNext.get() == Token.TokenTypes.COMMA;
            if (nextIsObj) {
                Optional<Token> objToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
                fc.obj = objToken.map(t -> t.Value.orElse(""));
            }
        }

        // Optional "with" arguments
        if (tokenManager.nextIs(Token.TokenTypes.WITH)) {
            tokenManager.matchAndRemove(Token.TokenTypes.WITH);
            fc.parameter.add(expression());
            while (tokenManager.nextIs(Token.TokenTypes.COMMA)) {
                tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                fc.parameter.add(expression());
            }
        }

        if (!tokenManager.done()) requireNewLine();
        return Optional.of(fc);
    }

    
    private VariableReferenceNode variableReference() throws SyntaxErrorException {
        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (nameToken.isEmpty()) {
            throw new SyntaxErrorException("Expected variable name",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        VariableReferenceNode vr = new VariableReferenceNode();
        vr.name = nameToken.get().Value.orElse("");

        if (tokenManager.nextIs(Token.TokenTypes.OF)) {
            tokenManager.matchAndRemove(Token.TokenTypes.OF);
            Optional<Token> objToken = tokenManager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
            if (objToken.isEmpty()) {
                throw new SyntaxErrorException("Expected object name after 'of'",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            vr.of = true;
            vr.$object = Optional.of(objToken.get().Value.orElse(""));
        }
        return vr;
    }

    
    // BoolExpTerm  =  BoolExpFactor (("and"|"or") BoolExpTerm)*
    
    private BoolExpTermNode boolExpTerm() throws SyntaxErrorException {
        BoolExpTermNode node = new BoolExpTermNode();

        // "not" case
        if (tokenManager.nextIs(Token.TokenTypes.NOT)) {
            tokenManager.matchAndRemove(Token.TokenTypes.NOT);
            node.not = true;
            node.notTerm = Optional.of(boolExpTerm());
            return node;
        }

        // Normal case: BoolExpFactor (("and"|"or") BoolExpTerm)
        node.boolexpfactor = Optional.of(boolExpFactor());

        while (tokenManager.nextIs(Token.TokenTypes.AND)
                || tokenManager.nextIs(Token.TokenTypes.OR)) {
            if (tokenManager.nextIs(Token.TokenTypes.AND)) {
                tokenManager.matchAndRemove(Token.TokenTypes.AND);
                node.theandORor.add(andORor.and);
            } else {
                tokenManager.matchAndRemove(Token.TokenTypes.OR);
                node.theandORor.add(andORor.or);
            }
            node.boolexpterm.add(boolExpTerm());
        }

        return node;
    }

    
    private BoolExpFactorNode boolExpFactor() throws SyntaxErrorException {
        BoolExpFactorNode node = new BoolExpFactorNode();

        ExpressionNode lhsExpr = expression();

        // Check for a comparison operator
        compareOps op = null;
        if (tokenManager.nextIs(Token.TokenTypes.DOUBLEEQUAL)) {
            tokenManager.matchAndRemove(Token.TokenTypes.DOUBLEEQUAL);
            op = compareOps.doubleequal;
        } else if (tokenManager.nextIs(Token.TokenTypes.NOTEQUAL)) {
            tokenManager.matchAndRemove(Token.TokenTypes.NOTEQUAL);
            op = compareOps.notequal;
        } else if (tokenManager.nextIs(Token.TokenTypes.LESSTHANEQUAL)) {
            tokenManager.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL);
            op = compareOps.lessthanequal;
        } else if (tokenManager.nextIs(Token.TokenTypes.GREATERTHANEQUAL)) {
            tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL);
            op = compareOps.greaterthanequal;
        } else if (tokenManager.nextIs(Token.TokenTypes.GREATERTHAN)) {
            tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHAN);
            op = compareOps.greaterthan;
        } else if (tokenManager.nextIs(Token.TokenTypes.LESSTHAN)) {
            tokenManager.matchAndRemove(Token.TokenTypes.LESSTHAN);
            op = compareOps.lessthan;
        }

        if (op != null) {
            // It's a comparison: lhs op rhs
            node.lhs = Optional.of(lhsExpr);
            node.thecompareOps = Optional.of(op);
            node.rhs = Optional.of(expression());
        } else {
            // no operator found, so lhsExpr must be a bare variable reference — extract it
            FactorNode f = lhsExpr.term.get(0).factor.get(0);
            if (f.variableReference.isPresent()) {
                node.variablereference = Optional.of(f.variableReference.get());
            } else if (f.variablereference.isPresent()) {
                node.variablereference = f.variablereference;
            } else {
                // Treat the whole expression as lhs with no op (shouldn't happen per EBNF)
                node.lhs = Optional.of(lhsExpr);
            }
        }

        return node;
    }

    
    // Expression  =  Term (("+"|"-") Term)*
    
    private ExpressionNode expression() throws SyntaxErrorException {
        ExpressionNode expr = new ExpressionNode();
        expr.term.add(term());

        while (tokenManager.nextIs(Token.TokenTypes.PLUS)
                || tokenManager.nextIs(Token.TokenTypes.HYPHEN)) {
            if (tokenManager.nextIs(Token.TokenTypes.PLUS)) {
                tokenManager.matchAndRemove(Token.TokenTypes.PLUS);
                expr.theplusORhyphen.add(plusORhyphen.plus);
            } else {
                tokenManager.matchAndRemove(Token.TokenTypes.HYPHEN);
                expr.theplusORhyphen.add(plusORhyphen.hyphen);
            }
            expr.term.add(term());
        }
        return expr;
    }

   
    // Term  =  Factor (("*"|"/"|"%") Factor)*
   
    private TermNode term() throws SyntaxErrorException {
        TermNode termNode = new TermNode();
        termNode.factor.add(factor());

        while (tokenManager.nextIs(Token.TokenTypes.ASTERISK)
                || tokenManager.nextIs(Token.TokenTypes.SLASH)
                || tokenManager.nextIs(Token.TokenTypes.PERCENT)) {
            if (tokenManager.nextIs(Token.TokenTypes.ASTERISK)) {
                tokenManager.matchAndRemove(Token.TokenTypes.ASTERISK);
                termNode.theasteriskORslashORpercent.add(asteriskORslashORpercent.asterisk);
            } else if (tokenManager.nextIs(Token.TokenTypes.SLASH)) {
                tokenManager.matchAndRemove(Token.TokenTypes.SLASH);
                termNode.theasteriskORslashORpercent.add(asteriskORslashORpercent.slash);
            } else {
                tokenManager.matchAndRemove(Token.TokenTypes.PERCENT);
                termNode.theasteriskORslashORpercent.add(asteriskORslashORpercent.percent);
            }
            termNode.factor.add(factor());
        }
        return termNode;
    }

    
    // Factor  =  NUMBER | CHARACTERLITERAL | STRINGLITERAL | TRUE | FALSE
    //          | "(" Expression ")" | VariableReference
    
    private FactorNode factor() throws SyntaxErrorException {
        FactorNode fn = new FactorNode();

        if (tokenManager.nextIs(Token.TokenTypes.NUMBER)) {
            fn.number = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER).get().Value;
            return fn;
        }
        if (tokenManager.nextIs(Token.TokenTypes.CHARACTERLITERAL)) {
            fn.characterliteral = tokenManager.matchAndRemove(Token.TokenTypes.CHARACTERLITERAL).get().Value;
            return fn;
        }
        if (tokenManager.nextIs(Token.TokenTypes.STRINGLITERAL)) {
            fn.stringliteral = tokenManager.matchAndRemove(Token.TokenTypes.STRINGLITERAL).get().Value;
            return fn;
        }
        if (tokenManager.nextIs(Token.TokenTypes.TRUE)) {
            tokenManager.matchAndRemove(Token.TokenTypes.TRUE);
            fn.$true = true;
            return fn;
        }
        if (tokenManager.nextIs(Token.TokenTypes.FALSE)) {
            tokenManager.matchAndRemove(Token.TokenTypes.FALSE);
            fn.$false = true;
            return fn;
        }
        if (tokenManager.nextIs(Token.TokenTypes.OPENPAREN)) {
            tokenManager.matchAndRemove(Token.TokenTypes.OPENPAREN);
            ExpressionNode inner = expression();
            if (!tokenManager.nextIs(Token.TokenTypes.CLOSEPAREN)) {
                throw new SyntaxErrorException("Expected ')' to close parenthesized expression",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            tokenManager.matchAndRemove(Token.TokenTypes.CLOSEPAREN);
            fn.expression = Optional.of(inner);
            return fn;
        }
        if (tokenManager.nextIs(Token.TokenTypes.IDENTIFIER)) {
            VariableReferenceNode vr = variableReference();
            fn.variableReference = Optional.of(vr);
            fn.variablereference = Optional.of(vr);
            return fn;
        }

        throw new SyntaxErrorException(
            "Expected a value (number, true, false, string, char, variable, or '(') in expression",
            tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()
        );
    }
}
