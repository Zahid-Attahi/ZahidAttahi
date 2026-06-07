// Zahidullah
package PlainEnglish;

import java.util.Optional;


public class IfNode {

    // The boolean condition
    public BoolExpTermNode boolexpterm;

    // The "true" branch body
    public StatementBlockNode statementblock;

    // True when an "else" clause is present
    public boolean $else = false;

    // The "false" (else) branch body, when $else is true
    public Optional<StatementBlockNode> falseCase = Optional.empty();
}
