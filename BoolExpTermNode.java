// Zahidullah
package PlainEnglish;

import PlainEnglish.AST.andORor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BoolExpTermNode {

    // True when this node represents "not <term>"
    public boolean not = false;

    // The BoolExpFactor on the left side (present when not==false)
    public Optional<BoolExpFactorNode> boolexpfactor = Optional.empty();

    // The "and"/"or" operators connecting this term to subsequent terms
    public List<andORor> theandORor = new ArrayList<>();

    // The subsequent BoolExpTerms connected by and/or
    public List<BoolExpTermNode> boolexpterm = new ArrayList<>();

    // When not==true, the term being negated
    public Optional<BoolExpTermNode> notTerm = Optional.empty();
}
