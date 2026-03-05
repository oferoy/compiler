package types;

public class TypeNil extends Type {

    private static TypeNil instance = null;

    private TypeNil() {
        this.name = "nil";
    }

    public static TypeNil getInstance() {
        if (instance == null) {
            instance = new TypeNil();
        }
        return instance;
    }
}
