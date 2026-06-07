// Zahidullah
package PlainEnglish;

import java.util.Optional;

public class StatementNode {

    // A "Set x to expr" statement
    public Optional<SetNode> set = Optional.empty();

    // A "Make type named name" statement
    public Optional<MakeNode> make = Optional.empty();

    // An "If condition ..." statement
    public Optional<IfNode> $if = Optional.empty();

    // A "Loop condition ..." statement
    public Optional<LoopNode> loop = Optional.empty();

    // A function call statement
    public Optional<FunctionCallNode> functioncall = Optional.empty();
}
