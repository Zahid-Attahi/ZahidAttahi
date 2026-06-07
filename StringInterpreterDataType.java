package PlainEnglish;

public class StringInterpreterDataType extends InterpreterDataType {
    public String value;

    public StringInterpreterDataType() {
        this("");
    }

    public StringInterpreterDataType(String value) {
        this.value = value;
    }

    @Override
    public InterpreterDataType copy() {
        return new StringInterpreterDataType(value);
    }

    @Override
    public void assign(InterpreterDataType incoming) {
        if (!(incoming instanceof StringInterpreterDataType string)) {
            throw new RuntimeException("Cannot assign non-string to string");
        }
        value = string.value;
    }

    @Override
    public String printableValue() {
        return value;
    }
}
