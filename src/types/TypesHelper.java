package types;

import symboltable.SymbolTable;

public class TypesHelper
{
    /***************************************/
    /** Check if src can be assigned to dst */
    /** Implements ALL assignment rules     */
    /***************************************/
    public static boolean isAssignable(Type dst, Type src)
    {
        // EXACT match
        if (dst == src) return true;

        // ========== NIL assignment rules ==========
        if (src instanceof TypeNil)
        {
            // nil allowed only for class or array
            return (dst instanceof TypeClass) || (dst instanceof TypeArray);
        }

        // NIL cannot receive anything
        if (dst instanceof TypeNil)
            return false;

        // ========== VOID disallowed ==========
        if (dst instanceof TypeVoid || src instanceof TypeVoid)
            return false;

        // ========== Primitive rules ==========
        if (dst instanceof TypeInt && src instanceof TypeInt)
            return true;

        if (dst instanceof TypeString && src instanceof TypeString)
            return true;

        // No implicit conversion allowed between primitives

        // ========== Class assignment rules ==========
        if (dst instanceof TypeClass && src instanceof TypeClass)
        {
            // src <= dst (inheritance)
            return SymbolTable.isSubclass((TypeClass) src, (TypeClass) dst);
        }

        // ========== Array assignment rules ==========
        if (dst instanceof TypeArray && src instanceof TypeArray)
        {
            TypeArray A = (TypeArray) dst;
            TypeArray B = (TypeArray) src;

            // If src is an *anonymous* array (new int[...] etc),
            // allow assignment when element types match.
            if (src.name != null && src.name.startsWith("anon_array_of_")) {
                return A.elementType == B.elementType;
            }

            // Otherwise (typedef-to-typedef), they must be the SAME typedef.
            return A.sameArrayType(B);
        }

        return false;
    }

    /***********************************************/
    /** Check if equality a = b is legal           */
    /** Tables 7, 8, 9 in assignment               */
    /***********************************************/
    public static boolean canCompare(Type a, Type b)
    {
        // EXACT type match
        if (a == b) return true;

        // nil == class
        if (a instanceof TypeClass && b instanceof TypeNil) return true;
        if (b instanceof TypeClass && a instanceof TypeNil) return true;

        // nil == array
        if (a instanceof TypeArray && b instanceof TypeNil) return true;
        if (b instanceof TypeArray && a instanceof TypeNil) return true;

        // class == class (inheritance)
        if (a instanceof TypeClass && b instanceof TypeClass)
        {
            return SymbolTable.isSubclass((TypeClass)a, (TypeClass)b) ||
                   SymbolTable.isSubclass((TypeClass)b, (TypeClass)a);
        }

        // array == array only if typedef matches
        if (a instanceof TypeArray && b instanceof TypeArray)
        {
            return ((TypeArray)a).sameArrayType((TypeArray)b);
        }

        // primitive equality
        if (a instanceof TypeInt && b instanceof TypeInt) return true;
        if (a instanceof TypeString && b instanceof TypeString) return true;

        return false; // everything else illegal
    }

    /****************************************************/
    /** Get result type of binary operation             */
    /** op: 0:+ 1:- 2:* 3:/ 4:< 5:> 6:=                 */
    /****************************************************/
    public static Type binopResult(int op, Type left, Type right, Integer rightConst)
    {
        // ========= EQUALS =========
        if (op == 6)
        {
            if (!canCompare(left, right))
                return null; // indicates semantic error
            return TypeInt.getInstance();
        }

        // ========= PLUS =========
        if (op == 0)
        {
            // int + int → int
            if (left instanceof TypeInt && right instanceof TypeInt)
                return TypeInt.getInstance();

            // string + string → string
            if (left instanceof TypeString && right instanceof TypeString)
                return TypeString.getInstance();

            return null;
        }

        // ========= ARITHMETIC ops =========
        if (op == 1 || op == 2 || op == 3) // -, *, /
        {
            if (!(left instanceof TypeInt && right instanceof TypeInt))
                return null;

            // division by zero check if rightConst provided
            if (op == 3 && rightConst != null && rightConst == 0)
                return null;

            return TypeInt.getInstance();
        }

        // ========= RELATIONAL ops (<, >) =========
        if (op == 4 || op == 5)
        {
            if (left instanceof TypeInt && right instanceof TypeInt)
                return TypeInt.getInstance();
            return null;
        }

        return null;
    }

    /***********************************************/
    /** Check member access t.f                   */
    /***********************************************/
    public static Type classField(Type t, String fieldName)
    {
        if (!(t instanceof TypeClass))
            return null;

        TypeClass cls = (TypeClass)t;
        return cls.findField(fieldName);
    }

    /***********************************************/
    /** Check method access t.f()                 */
    /***********************************************/
    public static TypeFunction classMethod(Type t, String methodName)
    {
        if (!(t instanceof TypeClass))
            return null;

        TypeClass cls = (TypeClass)t;
        return cls.findMethod(methodName);
    }

    /***********************************************/
    /** Check array access arr[i]                 */
    /***********************************************/
    public static Type arrayAccess(Type arr, Type index, Integer constIdx)
    {
        if (!(arr instanceof TypeArray))
            return null;

        if (!(index instanceof TypeInt))
            return null;

        if (constIdx != null && constIdx < 0)
            return null;

        return ((TypeArray)arr).elementType;
    }

    /***********************************************/
    /** Field visibility inside a class (plain ID)*/
    /** Enforces: “only after it has been defined”*/
    /***********************************************/
    public static Type findVisibleField(TypeClass cls, String fieldName, int usageLine) {
        for (TypeClass c = cls; c != null; c = c.father) {
            for (TypeClassMember m = c.dataMembers; m != null; m = m.next) {
                if (m.kind == TypeClassMember.FIELD && m.name.equals(fieldName)) {

                    if (c == cls) {
                        // For fields of THIS class, enforce “only after definition”:
                        // if field declared later than the use line → not visible yet.
                        if (m.declLine != 0 && usageLine != 0 && m.declLine > usageLine) {
                            return null;
                        }
                    }

                    // Either ancestor field, or declared early enough in this class
                    return m.type;
                }
            }
        }
        return null;
    }
}
