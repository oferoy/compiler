package types;

public class TypeString extends Type {

    private static TypeString instance = null;

    private TypeString() {
        this.name = "string";
    }

    public static TypeString getInstance() {
        if (instance == null) {
            instance = new TypeString();
        }
        return instance;
    }
}
