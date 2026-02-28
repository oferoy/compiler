package types;

public class TypeArray extends Type
{
    // Unique ID for this array typedef
    private static int counter = 0;
    public final int arrayId;

    // The element type (T in T[])
    public Type elementType;

    public TypeArray(String name, Type elementType)
    {
        super.name = name;         // Use inherited name field
        this.elementType = elementType;
        this.arrayId = counter++;  // Every typedef creates a NEW array type
    }

    @Override
    public boolean isArray()
    {
        return true;
    }

    public String getName()
    {
        return super.name;
    }

    // Two array types are equal ONLY if they come from the same typedef
    public boolean sameArrayType(TypeArray other)
    {
        return this.arrayId == other.arrayId;
    }
}
