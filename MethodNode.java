package PlainEnglish;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// AST node for a method definition
// Example: "To Run" or "To Create a string with number named value"
public class MethodNode {

    // The name of the method (e.g. "Run", "Create")
    public String name;

    // True if the method has "with" parameters
    public boolean with;

    // The class name if the method has "a ClassName" (e.g. "string" from "To Create a string")
    public Optional<String> className = Optional.empty();

    // The list of parameters after "with"
    public List<ParameterNode> parameter = new ArrayList<>();

    // The body of the method
    public StatementBlockNode statementblock;
}
