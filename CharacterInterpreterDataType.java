package PlainEnglish;

public class CharacterInterpreterDataType extends InterpreterDataType {
    public char value;

    public CharacterInterpreterDataType() {
        this('\0');
    }

    public CharacterInterpreterDataType(char value) {
        this.value = value;
    }

    @Override
    public InterpreterDataType copy() {
        return new CharacterInterpreterDataType(value);
    }

    @Override
    public void assign(InterpreterDataType incoming) {
        if (!(incoming instanceof CharacterInterpreterDataType character)) {
            throw new RuntimeException("Cannot assign non-character to character");
        }
        value = character.value;
    }

    @Override
    public String printableValue() {
        return Character.toString(value);
    }
}
