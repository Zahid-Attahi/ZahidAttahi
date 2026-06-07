package PlainEnglish;

public class NumberInterpreterDataType extends InterpreterDataType {
    public float value;

    public NumberInterpreterDataType() {
        this(0.0f);
    }

    public NumberInterpreterDataType(float value) {
        this.value = value;
    }

    @Override
    public InterpreterDataType copy() {
        return new NumberInterpreterDataType(value);
    }

    @Override
    public void assign(InterpreterDataType incoming) {
        if (!(incoming instanceof NumberInterpreterDataType number)) {
            throw new RuntimeException("Cannot assign non-number to number");
        }
        value = number.value;
    }

    @Override
    public String printableValue() {
        return Float.toString(value);
    }
}
