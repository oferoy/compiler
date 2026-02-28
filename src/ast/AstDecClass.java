package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstDecClass extends AstDec {

    public String name;
    public String fatherName;          // may be null if no extends

    // Separate lists for fields and methods
    public AstTypeNameList fields;     // int ID; Person bestFriend; ...
    public AstDecList      methods;    // int getAge() { ... }, void birthday(){...}, ...

    // Used by AstExpVarSimple / AstDecFunc to know when we are inside a class
    public static TypeClass currentClassType = null;

    /******************/
    /* CONSTRUCTOR(S) */
    /******************/

    // CLASS ID { <fields> <methods> }
    public AstDecClass(String name,
                       AstTypeNameList fields,
                       AstDecList methods)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.name         = name;
        this.fatherName   = null;
        this.fields       = fields;
        this.methods      = methods;
    }

    // CLASS ID EXTENDS ID { <fields> <methods> }
    public AstDecClass(String name,
                       String fatherName,
                       AstTypeNameList fields,
                       AstDecList methods)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.name         = name;
        this.fatherName   = fatherName;
        this.fields       = fields;
        this.methods      = methods;
    }

    /***************/
    /* PRINT ME    */
    /***************/
    @Override
    public void printMe()
    {
        System.out.format("CLASS(%s", name);
        if (fatherName != null) {
            System.out.format(" EXTENDS %s", fatherName);
        }
        System.out.println(")");

        if (fields != null)  fields.printMe();
        if (methods != null) methods.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("CLASS\n%s", name)
        );

        if (fields != null)
            AstGraphviz.getInstance().logEdge(serialNumber, fields.serialNumber);
        if (methods != null)
            AstGraphviz.getInstance().logEdge(serialNumber, methods.serialNumber);
    }

    /*****************/
    /* SEMANT ME     */
    /*****************/
    @Override
    public Type semantMe()
    {
        /******** 1. Ensure class name is not redeclared ********/
        if (SymbolTable.find(name) != null) {
            AstNode.error(lineNumber, "class " + name + " already declared");
        }

        /******** 2. Resolve father class (if any) ********/
        TypeClass father = null;

        if (fatherName != null) {
            Type t = SymbolTable.find(fatherName);

            if (!(t instanceof TypeClass)) {
                AstNode.error(lineNumber,
                    "class " + name + " extends non-class " + fatherName);
            }

            father = (TypeClass)t;
        }

        /******** 3. Pre-insert empty class (forward refs) ********/
        TypeClass cls = new TypeClass(father, name, null);
        SymbolTable.enter(name, cls);

        /******** 4. Build field members into the class type ********/
        TypeClassMember memberList = null;
        TypeClassMember tail       = null;

        for (AstTypeNameList it = fields; it != null; it = it.tail) {
            AstTypeName field = it.head;

            Type fieldType = SymbolTable.find(field.type);
            if (fieldType == null ||
                fieldType instanceof TypeVoid ||
                fieldType instanceof TypeNil)
            {
                // use the field’s own line number
                AstNode.error(field.lineNumber,
                    "illegal field type " + field.type + " in class " + name);
            }

            // Check for duplicate fields in THIS class
            if (memberExists(memberList, field.name)) {
                AstNode.error(field.lineNumber,
                    "duplicate field " + field.name + " in class " + name);
            }

            // Check conflict with inherited fields
            if (father != null && father.findField(field.name) != null) {
                AstNode.error(field.lineNumber,
                    "field " + field.name + " already exists in parent class");
            }

            TypeClassMember mem =
                new TypeClassMember(fieldType, field.name, TypeClassMember.FIELD, null);

            // remember declaration line on the member
            mem.declLine = field.lineNumber;
            mem.initValue = field.initValue;

            // append to linked list
            if (memberList == null) {
                memberList = mem;
                tail       = mem;
            } else {
                tail.next  = mem;
                tail       = mem;
            }
        }

        // Attach fields to the class type
        cls.dataMembers = memberList;

        /******** 5. Semant methods with currentClassType set ********/
        TypeClass prevClass = AstDecClass.currentClassType;
        AstDecClass.currentClassType = cls;

        // Open a "class scope" so methods of the same class
        // can't duplicate names, but different classes can.
        SymbolTable.beginScope();

        if (methods != null) {
            methods.semantMe();  // each AstDecFunc will use currentClassType
        }

        SymbolTable.endScope();

        AstDecClass.currentClassType = prevClass;

        return null;
    }

    private boolean memberExists(TypeClassMember list, String name)
    {
        for (TypeClassMember m = list; m != null; m = m.next) {
            if (m.name.equals(name)) return true;
        }
        return false;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe() {
        // Set currentClassType so method bodies see the correct class (for "this", field lookup). Restore after.
        TypeClass cls = (TypeClass) SymbolTable.find(name);
        TypeClass prevClass = AstDecClass.currentClassType;
        AstDecClass.currentClassType = cls;
        for (AstDecList it = methods; it != null; it = it.tail)
            if (it.head != null)
                it.head.irMe();
        AstDecClass.currentClassType = prevClass;
        return null;
    }
}
