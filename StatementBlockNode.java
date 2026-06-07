package PlainEnglish;

import java.util.ArrayList;
import java.util.List;

// AST node for a block of statements (the indented body of a method)
public class StatementBlockNode {

    // The list of statements in this block
    public List<StatementNode> statement = new ArrayList<>();
}
