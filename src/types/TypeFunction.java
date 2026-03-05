package types;

public class TypeFunction extends Type
{
    /***********************************/
    /* Return type of the function     */
    /***********************************/
    public Type returnType;

    /***********************************/
    /* Formal parameter type list       */
    /***********************************/
    public TypeList params;

    /***********************************/
    /* Constructor                      */
    /***********************************/
    public TypeFunction(Type returnType, String name, TypeList params)
    {
        super.name = name;
        this.returnType = returnType;
        this.params = params;
    }

    @Override
    public String toString() {
        return "Function(" + name + ")";
    }

    /************************************/
    /** Count number of parameters      */
    /************************************/
    public int numParams()
    {
        int count = 0;
        for (TypeList it = params; it != null; it = it.tail)
            count++;
        return count;
    }

    /************************************************/
    /** Check if TWO parameter lists are identical **/
    /** Used in method override checking           **/
    /************************************************/
    public boolean sameSignature(TypeFunction other)
    {
        // Compare return types
        if (this.returnType != other.returnType)
            return false;

        // Compare parameter lists (same length, same types)
        TypeList p1 = this.params;
        TypeList p2 = other.params;

        while (p1 != null && p2 != null)
        {
            if (p1.head != p2.head)
                return false;

            p1 = p1.tail;
            p2 = p2.tail;
        }

        // If one list continues → lengths differ → not same signature
        return (p1 == null && p2 == null);
    }

    /******************************************************/
    /** Check if call argument types match formal params  **/
    /** This is used when validating function calls       **/
    /******************************************************/
    public boolean paramsMatch(TypeList callParams)
    {
        TypeList formal = this.params;
        TypeList actual = callParams;

        while (formal != null && actual != null)
        {
            Type formalType = formal.head;
            Type actualType = actual.head;

            // Assignment compatibility check
            if (!TypesHelper.isAssignable(formalType, actualType))
                return false;

            formal = formal.tail;
            actual = actual.tail;
        }

        // Both must end together
        return (formal == null && actual == null);
    }
}
