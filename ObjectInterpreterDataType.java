package PlainEnglish;

import java.util.HashMap;
import java.util.Map;

public class ObjectInterpreterDataType extends InterpreterDataType {
    public final String typeName;
    public final HashMap<String, InterpreterDataType> fields;

    public ObjectInterpreterDataType(String typeName) {
        this.typeName = typeName;
        this.fields = new HashMap<>();
    }

    @Override
    public InterpreterDataType copy() {
        ObjectInterpreterDataType copy = new ObjectInterpreterDataType(typeName);
        for (Map.Entry<String, InterpreterDataType> entry : fields.entrySet()) {
            copy.fields.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    @Override
    public void assign(InterpreterDataType incoming) {
        if (!(incoming instanceof ObjectInterpreterDataType object)
                || !typeName.equalsIgnoreCase(object.typeName)) {
            throw new RuntimeException("Cannot assign incompatible object type to " + typeName);
        }

        fields.clear();
        for (Map.Entry<String, InterpreterDataType> entry : object.fields.entrySet()) {
            fields.put(entry.getKey(), entry.getValue().copy());
        }
    }

    @Override
    public String printableValue() {
        return typeName;
    }
}
