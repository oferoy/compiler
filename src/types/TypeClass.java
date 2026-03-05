package types;

import java.util.ArrayList;
import java.util.List;

public class TypeClass extends Type {

    // Parent class (for inheritance)
    public TypeClass father;

    // Linked list of fields + methods
    public TypeClassMember dataMembers;

    public TypeClass(TypeClass father, String name, TypeClassMember dataMembers) {
        this.father = father;
        this.name = name;
        this.dataMembers = dataMembers;
    }

    /** Add a field to this class */
    public void addField(String fieldName, Type fieldType) {
        this.dataMembers =
            new TypeClassMember(fieldType, fieldName, TypeClassMember.FIELD, this.dataMembers);
    }

    /** Add a method to this class */
    public void addMethod(String methodName, TypeFunction methodType) {
        this.dataMembers =
            new TypeClassMember(methodType, methodName, TypeClassMember.METHOD, this.dataMembers);
    }

    /** Search for a field in this class or its ancestors */
    public Type findField(String fieldName) {
        for (TypeClass c = this; c != null; c = c.father) {
            for (TypeClassMember m = c.dataMembers; m != null; m = m.next) {
                if (m.name.equals(fieldName) && m.kind == TypeClassMember.FIELD) {
                    return m.type;
                }
            }
        }
        return null;
    }

    /** Search for a method in this class or its ancestors */
    public TypeFunction findMethod(String methodName) {
        for (TypeClass c = this; c != null; c = c.father) {
            for (TypeClassMember m = c.dataMembers; m != null; m = m.next) {
                if (m.name.equals(methodName) && m.kind == TypeClassMember.METHOD) {
                    return (TypeFunction)m.type;
                }
            }
        }
        return null;
    }

    /** Return the class that defines methodName (first in hierarchy from this); null if not found. */
    public TypeClass getMethodDefiningClass(String methodName) {
        for (TypeClass c = this; c != null; c = c.father) {
            for (TypeClassMember m = c.dataMembers; m != null; m = m.next) {
                if (m.name.equals(methodName) && m.kind == TypeClassMember.METHOD) {
                    return c;
                }
            }
        }
        return null;
    }

    /** Vtable pointer is at offset 0; fields start at 4. */
    public static final int VTABLE_PTR_SIZE = 4;

    /** Byte offset of field in object layout (vtable at 0, then base class fields, then this class). */
    public int getFieldOffset(String fieldName) {
        List<TypeClass> chain = new ArrayList<>();
        for (TypeClass c = this; c != null; c = c.father)
            chain.add(0, c);
        int offset = VTABLE_PTR_SIZE;
        for (TypeClass c : chain) {
            for (TypeClassMember m = c.dataMembers; m != null; m = m.next) {
                if (m.kind == TypeClassMember.FIELD) {
                    if (m.name.equals(fieldName))
                        return offset;
                    offset += 4;
                }
            }
        }
        return -1;
    }

    /** Total byte size of object (vtable ptr + all fields, 4 bytes each). At least 8. */
    public int getDataSize() {
        int size = VTABLE_PTR_SIZE;
        for (TypeClass c = this; c != null; c = c.father) {
            for (TypeClassMember m = c.dataMembers; m != null; m = m.next) {
                if (m.kind == TypeClassMember.FIELD)
                    size += 4;
            }
        }
        return size >= 8 ? size : 8;
    }
}
