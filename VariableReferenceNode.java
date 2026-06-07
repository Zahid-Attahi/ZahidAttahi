package PlainEnglish;

import java.util.Optional;

public class VariableReferenceNode {
    public String name;
    public boolean of;
    public Optional<Object> $object = Optional.empty();
    
    public VariableReferenceNode() {
        this.name = "";
        this.of = false;
        this.$object = Optional.empty();
    }
    
    public VariableReferenceNode(String name, boolean of, Optional<Object> object) {
        this.name = name;
        this.of = of;
        this.$object = object;
    }
}
