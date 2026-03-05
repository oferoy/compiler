package types;

public class TypeVar extends Type {
    public String name;
    public Type type;

    public TypeVar(Type type, String name) {
        this.type = type;
        this.name = name;
    }
}