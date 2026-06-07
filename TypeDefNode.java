package PlainEnglish;

import java.util.ArrayList;
import java.util.List;

/**
 * AST node representing a type definition.
 * Example: "A square is" followed by indented fields.
 */
public class TypeDefNode {
    /** The name of the type (e.g. "square", "Student"). */
    public String name;

    /** The fields declared inside this type definition. */
    public List<FieldNode> field = new ArrayList<>();
}