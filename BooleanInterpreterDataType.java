package PlainEnglish;

public class BooleanInterpreterDataType extends InterpreterDataType {
    public boolean value;

    public BooleanInterpreterDataType() {
        this(false);
    }

    public BooleanInterpreterDataType(boolean value) {
        this.value = value;
    }

    @Override
    public InterpreterDataType copy() {
        return new BooleanInterpreterDataType(value);
    }

    @Override
    public void assign(InterpreterDataType incoming) {
        if (!(incoming instanceof BooleanInterpreterDataType bool)) {
            throw new RuntimeException("Cannot assign non-boolean to boolean");
        }
        value = bool.value;
    }

    @Override
    public String printableValue() {
        return Boolean.toString(value);
    }
}
