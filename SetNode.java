package PlainEnglish;

// AST node for a "Set variablereference to expression" statement
public class SetNode {

    // The variable being assigned to
    public VariableReferenceNode variablereference;

    // The value being assigned
    public ExpressionNode expression;
}
