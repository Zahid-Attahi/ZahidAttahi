package PlainEnglish;

import java.util.ArrayList;
import java.util.List;

// Root AST node representing an entire PlainEnglish program.
// Contains zero or more type definitions and method definitions.
public class ProgramNode {
    public List<TypeDefNode> typedef = new ArrayList<>();
    public List<MethodNode> method = new ArrayList<>();
}
