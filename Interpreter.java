package PlainEnglish;

import PlainEnglish.AST.andORor;
import PlainEnglish.AST.asteriskORslashORpercent;
import PlainEnglish.AST.compareOps;
import PlainEnglish.AST.plusORhyphen;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {
    private static final float NUMBER_EPSILON = 0.00001f;
    private final ProgramNode program;

    private final Map<String, MethodNode> methods = new HashMap<>();
    private final Map<String, TypeDefNode> typeDefs = new HashMap<>();
    private final Deque<HashMap<String, InterpreterDataType>> scopes = new ArrayDeque<>();
    private final StringBuilder output = new StringBuilder();

    public Interpreter(ProgramNode program) {
        this.program = program;

        for (TypeDefNode typeDef : program.typedef) {
            typeDefs.put(typeDef.name.toLowerCase(), typeDef);
        }

        for (MethodNode method : program.method) {
            methods.put(methodKey(method.name, method.className.orElse(null)), method);
            if (!method.className.isPresent()) {
                methods.putIfAbsent(method.name.toLowerCase(), method);
            }
        }
    }

    public void run() {
        MethodNode runMethod = methods.get("run");
        if (runMethod == null) {
            throw new RuntimeException("No Run method found");
        }
        DoMethod(runMethod, List.of());
    }

    public String getOutput() {
        return output.toString();
    }

    public void DoMethod(MethodNode method, List<InterpreterDataType> parameters) {
        if (method.parameter.size() != parameters.size()) {
            throw new RuntimeException(

                "Method " + method.name + " expected " + method.parameter.size()
                    + " parameters but got " + parameters.size());
        }

        HashMap<String, InterpreterDataType> locals = new HashMap<>();
        for (int i = 0; i < method.parameter.size(); i++) {
            ParameterNode parameterNode = method.parameter.get(i);
            String parameterName = parameterNode.nameOverride.orElse(parameterNode.paramType);
            locals.put(parameterName, parameters.get(i).copy());
        }

        scopes.push(locals);
        try {
            for (StatementNode statement : method.statementblock.statement) {
                DoStatement(statement);
            }
        } finally {
            scopes.pop();
        }
    }

    public void DoStatement(StatementNode statement) {
        if (statement.make.isPresent()) {
            Make(statement.make.get());
            return;
        }
        if (statement.set.isPresent()) {
            Set(statement.set.get());
            return;
        }
        if (statement.$if.isPresent()) {
            DoIf(statement.$if.get());
            return;
        }
        if (statement.loop.isPresent()) {
            DoLoop(statement.loop.get());
            return;
        }
        if (statement.functioncall.isPresent()) {
            DoFunctionCall(statement.functioncall.get());
            return;
        }
        throw new RuntimeException("Unknown statement type");
    }

    public void DoFunctionCall(FunctionCallNode functionCall) {
        if (functionCall.name.equalsIgnoreCase("Print")) {
            doPrint(functionCall);
            return;
        }

        MethodNode method = lookupMethod(functionCall);
        if (method == null) {
            throw new RuntimeException("Unknown method: " + functionCall.name);
        }

        List<InterpreterDataType> evaluatedParameters = new ArrayList<>();
        for (ExpressionNode parameter : functionCall.parameter) {
            evaluatedParameters.add(DoExpression(parameter));
        }
        DoMethod(method, evaluatedParameters);
    }

    public void DoIf(IfNode ifNode) {
        if (DoBoolTerm(ifNode.boolexpterm)) {
            doBlock(ifNode.statementblock);
        } else if (ifNode.$else && ifNode.falseCase.isPresent()) {
            doBlock(ifNode.falseCase.get());
        }
    }

    public void DoLoop(LoopNode loopNode) {
        while (DoBoolTerm(loopNode.boolexpterm)) {
            doBlock(loopNode.statementblock);
        }
    }

    public boolean DoBoolTerm(BoolExpTermNode boolExpTerm) {
        if (boolExpTerm.not) {
            return !DoBoolTerm(boolExpTerm.notTerm.orElseThrow());
        }

        boolean value = DoBoolFactor(boolExpTerm.boolexpfactor.orElseThrow());
        for (int i = 0; i < boolExpTerm.theandORor.size(); i++) {

            andORor operator = boolExpTerm.theandORor.get(i);
            boolean rightValue = DoBoolTerm(boolExpTerm.boolexpterm.get(i));
            if (operator == andORor.and) {
                value = value && rightValue;

            } else {
                value = value || rightValue;
            }
        }
        return value;
    }

    public boolean DoBoolFactor(BoolExpFactorNode boolExpFactor) {
        if (boolExpFactor.thecompareOps.isPresent()) {

                  InterpreterDataType left = DoExpression(boolExpFactor.lhs.orElseThrow());
            InterpreterDataType right = DoExpression(boolExpFactor.rhs.orElseThrow());
            return compareValues(left, right, boolExpFactor.thecompareOps.get());
        }

        InterpreterDataType value = resolveReference(boolExpFactor.variablereference.orElseThrow());

        if (!(value instanceof BooleanInterpreterDataType bool)) {
            throw new RuntimeException("Expected a boolean variable in condition");
        }
        return bool.value;
    }

    public void Set(SetNode setNode) {
        InterpreterDataType target = resolveReference(setNode.variablereference);
        InterpreterDataType value = DoExpression(setNode.expression);
        target.assign(value);
    }

    public InterpreterDataType DoExpression(ExpressionNode expression) {
        InterpreterDataType value = DoTerm(expression.term.get(0));
        for (int i = 0; i < expression.theplusORhyphen.size(); i++) {
            plusORhyphen operator = expression.theplusORhyphen.get(i);
            InterpreterDataType rightValue = DoTerm(expression.term.get(i + 1));
            value = applyPlusMinus(value, rightValue, operator);
        }
        return value;
    }

    public InterpreterDataType DoTerm(TermNode term) {

        InterpreterDataType value = DoFactor(term.factor.get(0));
        for (int i = 0; i < term.theasteriskORslashORpercent.size(); i++) {

            asteriskORslashORpercent operator = term.theasteriskORslashORpercent.get(i);
            InterpreterDataType rightValue = DoFactor(term.factor.get(i + 1));
            value = applyTermOperator(value, rightValue, operator);
        }
        return value;
    }

    public InterpreterDataType DoFactor(FactorNode factor) {

        if (factor.number.isPresent()) {
            return new NumberInterpreterDataType(Float.parseFloat(factor.number.get()));
        }
        if (factor.stringliteral.isPresent()) {
            return new StringInterpreterDataType(factor.stringliteral.get());
        }
        if (factor.characterliteral.isPresent()) {

            String value = factor.characterliteral.get();
            if (value.isEmpty()) {
                return new CharacterInterpreterDataType();
            }
            return new CharacterInterpreterDataType(value.charAt(0));
        }
        if (factor.$true) {
            return new BooleanInterpreterDataType(true);
        }
        if (factor.$false) {
            return new BooleanInterpreterDataType(false);
        }
        if (factor.expression.isPresent()) {
            return DoExpression(factor.expression.get());
        }
        if (factor.variableReference.isPresent()) {

            return resolveReference(factor.variableReference.get()).copy();
        }
        if (factor.variablereference.isPresent()) {
            return resolveReference(factor.variablereference.get()).copy();
        }
        throw new RuntimeException("Unknown factor type");
    }

    public void Make(MakeNode makeNode) {

        HashMap<String, InterpreterDataType> currentScope = scopes.peek();
        if (currentScope == null) {
            throw new RuntimeException("No active scope for make statement");
        }
        currentScope.put(makeNode.name, createDefaultValue(makeNode.type));
    }

    public static void main(String[] args) throws Exception {

    if (args.length == 0) {
        throw new RuntimeException("Please pass one or more .eng files to run");
    }

    for (String fileName : args) {
        System.out.println("#########################");

        System.out.println("File: .\\" + Path.of(fileName).getFileName());

        String code = Files.readString(Path.of(fileName));
        Lexer lexer = new Lexer(code);
        ProgramNode program = new PlainEnglishParser(lexer.lex()).program().orElseThrow();
        Interpreter interpreter = new Interpreter(program);

        interpreter.run();
        System.out.print(interpreter.getOutput());
    }
}


    private void doBlock(StatementBlockNode block) {

        for (StatementNode statement : block.statement) {
            DoStatement(statement);
        }
    }

    private void doPrint(FunctionCallNode functionCall) {
        if (functionCall.parameter.isEmpty()) {
            output.append('\n');
            return;
        }

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < functionCall.parameter.size(); i++) {
            if (i > 0) {
                line.append(' ');
            }
            line.append(DoExpression(functionCall.parameter.get(i)).printableValue());
        }
        output.append(stripTrailingSpaces(line.toString())).append('\n');
    }

    private MethodNode lookupMethod(FunctionCallNode functionCall) {
        if (functionCall.obj.isPresent()) {
            InterpreterDataType object = lookupVariable(functionCall.obj.get());

            if (object instanceof ObjectInterpreterDataType objectValue) {
                MethodNode method = methods.get(methodKey(functionCall.name, objectValue.typeName));
                if (method != null) {
                    return method;
                }
            }
        }
        return methods.get(functionCall.name.toLowerCase());
    }

    private String methodKey(String methodName, String className) {

        if (className == null || className.isBlank()) {
            return methodName.toLowerCase();
        }
        return methodName.toLowerCase() + "#" + className.toLowerCase();
    }

    private InterpreterDataType resolveReference(VariableReferenceNode variableReference) {

        if (!variableReference.of) {
            return lookupVariable(variableReference.name);
        }

        Object objectName = variableReference.$object.orElseThrow();
        InterpreterDataType object = lookupVariable(objectName.toString());

        if (!(object instanceof ObjectInterpreterDataType objectValue)) {
            throw new RuntimeException(objectName + " is not an object");
        }

        InterpreterDataType field = objectValue.fields.get(variableReference.name);

        if (field == null) {
            throw new RuntimeException("Unknown field " + variableReference.name + " of " + objectName);
        }
        return field;
    }

    private InterpreterDataType lookupVariable(String name) {
        for (HashMap<String, InterpreterDataType> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        throw new RuntimeException("Unknown variable: " + name);
    }

    private InterpreterDataType createDefaultValue(String typeName) {
        String lowerType = typeName.toLowerCase();
        switch (lowerType) {
            case "number":
                return new NumberInterpreterDataType();
             case "string":
                return new StringInterpreterDataType();
            case "boolean":

                case "bool":
                return new BooleanInterpreterDataType();
            case "character":
            case "char":
                return new CharacterInterpreterDataType();
            default:
                TypeDefNode typeDef = typeDefs.get(lowerType);
                if (typeDef == null) {
                    throw new RuntimeException("Unknown type: " + typeName);
                }
                ObjectInterpreterDataType object = new ObjectInterpreterDataType(typeDef.name);
                for (FieldNode field : typeDef.field) {
                    object.fields.put(field.name, createDefaultValue(field.type));
                }
                return object;
        }
    }

    private InterpreterDataType applyPlusMinus(
            InterpreterDataType left,
            InterpreterDataType right,

            plusORhyphen operator) {
        if (operator == plusORhyphen.plus) {
            if (left instanceof NumberInterpreterDataType leftNumber
                    && right instanceof NumberInterpreterDataType rightNumber) {
                return new NumberInterpreterDataType(leftNumber.value + rightNumber.value);
            }
            if (left instanceof StringInterpreterDataType leftString) {
                return new StringInterpreterDataType(leftString.value + right.printableValue());
            }
            if (right instanceof StringInterpreterDataType rightString) {
                return new StringInterpreterDataType(left.printableValue() + rightString.value);
            }
            throw new RuntimeException("Invalid plus operation");
        }

        if (left instanceof NumberInterpreterDataType leftNumber
                && right instanceof NumberInterpreterDataType rightNumber) {
            return new NumberInterpreterDataType(leftNumber.value - rightNumber.value);
        }
        throw new RuntimeException("Invalid minus operation");
    }

    private InterpreterDataType applyTermOperator( 

            InterpreterDataType left,
            InterpreterDataType right,
            asteriskORslashORpercent operator) {
        if (!(left instanceof NumberInterpreterDataType leftNumber)
                || !(right instanceof NumberInterpreterDataType rightNumber)) {
            throw new RuntimeException("Term operators only work on numbers");
        }

        if (operator == asteriskORslashORpercent.asterisk) {
            return new NumberInterpreterDataType(leftNumber.value * rightNumber.value);
        }

        if (operator == asteriskORslashORpercent.slash) {
            if (rightNumber.value == 0.0f) {
                throw new RuntimeException("Division by 0");
            }
            return new NumberInterpreterDataType(leftNumber.value / rightNumber.value);
        }

        if (rightNumber.value == 0.0f) {
            throw new RuntimeException("Division by 0");
        }
        return new NumberInterpreterDataType(leftNumber.value % rightNumber.value);
    }

    private boolean compareValues(
            InterpreterDataType left,
            InterpreterDataType right,
            compareOps operator) {

        if (left instanceof NumberInterpreterDataType leftNumber
                && right instanceof NumberInterpreterDataType rightNumber) {
            return compareNumbers(leftNumber.value, rightNumber.value, operator);
        }
        if (left instanceof StringInterpreterDataType leftString
                && right instanceof StringInterpreterDataType rightString) {
            return compareOrder(leftString.value.compareTo(rightString.value), operator);
        }
        if (left instanceof CharacterInterpreterDataType leftCharacter
                && right instanceof CharacterInterpreterDataType rightCharacter) {
            return compareOrder(Character.compare(leftCharacter.value, rightCharacter.value), operator);
        }
        if (left instanceof BooleanInterpreterDataType leftBoolean
                && right instanceof BooleanInterpreterDataType rightBoolean) {
            return compareOrder(Boolean.compare(leftBoolean.value, rightBoolean.value), operator);
        }
        throw new RuntimeException("Cannot compare values of different types");
    }

    private boolean compareNumbers(float left, float right, compareOps operator) {
        float difference = Math.abs(left - right);
        return switch (operator) {
            case doubleequal -> difference <= NUMBER_EPSILON;
            case notequal -> difference > NUMBER_EPSILON;
            case lessthan -> left < right - NUMBER_EPSILON;
            case greaterthan -> left > right + NUMBER_EPSILON;
            case lessthanequal -> left < right || difference <= NUMBER_EPSILON;
            case greaterthanequal -> left > right || difference <= NUMBER_EPSILON;
        };
    }

    private boolean compareOrder(int order, compareOps operator) {
        return switch (operator) {
            
            case doubleequal -> order == 0;
            case notequal -> order != 0;
            case lessthan -> order < 0;
            case greaterthan -> order > 0;
            case lessthanequal -> order <= 0;
            case greaterthanequal -> order >= 0;
        };
    }

    private String stripTrailingSpaces(String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }
}
