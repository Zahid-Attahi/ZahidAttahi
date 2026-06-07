// Zahidullah
package PlainEnglish;

import PlainEnglish.AST.compareOps;
import java.util.Optional;

public class BoolExpFactorNode {

    // Left-hand side of a comparison (present when thecompareOps is present)
    public Optional<ExpressionNode> lhs = Optional.empty();

    // The comparison operator
    public Optional<compareOps> thecompareOps = Optional.empty();

    // Right-hand side of a comparison (present when thecompareOps is present)
    public Optional<ExpressionNode> rhs = Optional.empty();

    // A bare variable reference (present when this is NOT a comparison)
    public Optional<VariableReferenceNode> variablereference = Optional.empty();
}
