package types;

public class TypeClassMember {

    public static final int FIELD  = 0;
    public static final int METHOD = 1;

    public int kind;      // FIELD or METHOD
    public Type type;     // field type OR function type
    public String name;   // member name
    public TypeClassMember next;

    // line where this member was declared (for “only after def” rule)
    public int declLine;

    /** Optional init expression for fields (e.g. int age := 10). Stored as Object to avoid ast dependency. */
    public Object initValue;

    public TypeClassMember(Type type, String name, int kind, TypeClassMember next) {
        this.type     = type;
        this.name     = name;
        this.kind     = kind;
        this.next     = next;
        this.declLine = 0; // set later by creator (AstDecClass)
    }
}
