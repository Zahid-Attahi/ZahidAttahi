package PlainEnglish;

//
  //AST node representing a single field declaration inside a type definition.
 //Example: "number length"  →  type="number", name="length"
 
public class FieldNode {
    // The type of the field (e.g. "number", "string")
    public String type;

    // The name of the field (e.g. "length", "firstname").
    public String name;
}