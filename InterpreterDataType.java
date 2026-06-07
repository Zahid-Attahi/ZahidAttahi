package PlainEnglish;

public abstract class InterpreterDataType {
    public abstract InterpreterDataType copy();

    public abstract void assign(InterpreterDataType incoming);

    public abstract String printableValue();
}
