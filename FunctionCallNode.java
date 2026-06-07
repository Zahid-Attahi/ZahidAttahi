// Zahidullah
package PlainEnglish;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FunctionCallNode {

    // The function/method name
    public String name;

    // The object this method is called on (optional second identifier)
    public Optional<String> obj = Optional.empty();

    // The list of argument expressions (after "with")
    public List<ExpressionNode> parameter = new ArrayList<>();
}
