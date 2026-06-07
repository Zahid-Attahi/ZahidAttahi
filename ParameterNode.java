package PlainEnglish;

import java.util.Optional;

// AST node for a single method parameter
// Example: "number named value" or just "number"
public class ParameterNode {

    // The type of the parameter (e.g. "number", "string")
    public String paramType;

    // True if the parameter has a "named" alias
    public boolean named;

    // The alias name if "named" is present (e.g. "value" from "number named value")
    public Optional<String> nameOverride = Optional.empty();
}
