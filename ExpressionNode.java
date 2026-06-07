
package PlainEnglish;

import PlainEnglish.AST.asteriskORslashORpercent;
import PlainEnglish.AST.plusORhyphen;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


// AST node for an expression
// Expression = Term { (+|-) Term }
public class ExpressionNode {

    // List of terms in this expression
    public List<TermNode> term = new ArrayList<>();

    // The operator between each pair of terms (size = term.size() - 1)
    // e.g. for "a + b - c": [plus, hyphen]
    public List<plusORhyphen> theplusORhyphen = new ArrayList<>();
}


// Term = Factor { (*|/|%) Factor }
class TermNode {

    // List of factors in this term
    public List<FactorNode> factor = new ArrayList<>();

    // The operator between each pair of factors (size = factor.size() - 1)
    // e.g. for "a * b / c": [asterisk, slash]
    public List<asteriskORslashORpercent> theasteriskORslashORpercent = new ArrayList<>();
}

// Factor = NUMBER | CHARACTERLITERAL | STRINGLITERAL | TRUE | FALSE
//        | ( Expression ) | VariableReference
class FactorNode {

    // A number literal (e.g. "311", "7.4")
    public Optional<String> number = Optional.empty();

    // A character literal (e.g. 'a')
    public Optional<String> characterliteral = Optional.empty();

    // A string literal (e.g. "Hello World")
    public Optional<String> stringliteral = Optional.empty();

    // True if this factor is the literal "true"
    public boolean $true;

    // True if this factor is the literal "false"
    public boolean $false;

    // A parenthesized sub-expression, if present

    public Optional<ExpressionNode> expression = Optional.empty();

    // A variable reference (e.g. "numVar" or "length of square"), if present
    public Optional<VariableReferenceNode> variableReference = Optional.empty();
    
    // Alias for test compatibility (lowercase version)
    public Optional<VariableReferenceNode> variablereference = variableReference;
}
