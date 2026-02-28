package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstDecVar extends AstDec
{
    /****************/
    /* DATA MEMBERS */
    /****************/
    public String type;
    public String name;
    public AstExp initialValue;
    
    // Unique IR name for this variable (handles shadowing)
    public String irName;
    
    /******************/
    /* CONSTRUCTOR(S) */
    /******************/
    public AstDecVar(String type, String name, AstExp initialValue)
    {
        /******************************/
        /* SET A UNIQUE SERIAL NUMBER */
        /******************************/
        serialNumber = AstNodeSerialNumber.getFresh();

        this.type = type;
        this.name = name;
        this.initialValue = initialValue;
        this.irName = null; // Will be set during semantMe
    }

    /************************************************************/
    /* The printing message for a variable declaration AST node */
    /************************************************************/
    public void printMe()
    {
        /****************************************/
        /* AST NODE Type = AST VAR DECLARATION */
        /***************************************/
        if (initialValue != null) System.out.format("VAR-DEC(%s):%s := initialValue\n",name,type);
        if (initialValue == null) System.out.format("VAR-DEC(%s):%s                \n",name,type);

        /**************************************/
        /* RECURSIVELY PRINT initialValue ... */
        /**************************************/
        if (initialValue != null) initialValue.printMe();

        /**********************************/
        /* PRINT to AST GRAPHVIZ DOT file */
        /**********************************/
        AstGraphviz.getInstance().logNode(
                serialNumber,
            String.format("VAR\nDEC(%s)\n:%s",name,type));

        /****************************************/
        /* PRINT Edges to AST GRAPHVIZ DOT file */
        /****************************************/
        if (initialValue != null) AstGraphviz.getInstance().logEdge(serialNumber,initialValue.serialNumber);
            
    }

    @Override
    public Type semantMe()
    {
        /*****************************************/
        /* 1. Find declared type in symbol table */
        /*****************************************/
        Type t = SymbolTable.find(type);

        if (t == null) {
            // type does not exist at all
            AstNode.error(lineNumber, "type " + type + " not defined");
        }

        /***********************************************/
        /* 2. Illegal types for variable declarations  */
        /***********************************************/
        if (t instanceof TypeVoid || t instanceof TypeNil) {
            AstNode.error(lineNumber,
                    "cannot declare variable " + name + " of illegal type " + type);
        }

        /***********************************************/
        /* 3. Check name does NOT exist IN CURRENT SCOPE */
        /***********************************************/
        if (SymbolTable.findInCurrentScope(name) != null) {
            // This covers: re-declaring parameter as local, and local-local duplicates
            AstNode.error(lineNumber,
                    "variable " + name + " already declared in this scope");
        }

        /***********************************************/
        /* 4. Evaluate initial value (if exists)       */
        /***********************************************/
        if (initialValue != null)
        {
            Type initType = initialValue.semantMe();

            // Check assignability
            if (!TypesHelper.isAssignable(t, initType)) {
                String initName = (initType == null ? "null" : initType.name);
                AstNode.error(lineNumber,
                        "cannot assign " + initName + " to variable " + name + " of type " + type);
            }
        }

        /***********************************************/
        /* 5. Enter variable into table AFTER checks    */
        /***********************************************/
        SymbolTable.enter(name, t);
        // If we're inside a function, remember this is a local
        AstDecFunc.registerLocal(this.name);
        
        // Note: IR name registration moved to irMe() for proper scope handling

        return null;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe()
    {
        /**************************************/
        /* [0] Register unique IR name for    */
        /*     this variable NOW during IR    */
        /*     generation (for proper scope)  */
        /**************************************/
        this.irName = VarNameMapper.getInstance().registerVariable(name, serialNumber);
        
        /**************************************/
        /* [1] Emit IR command to allocate    */
        /*     memory for the variable        */
        /*     Use unique IR name!            */
        /**************************************/
        Ir.getInstance().AddIrCommand(new IrCommandAllocate(irName));

        /**************************************/
        /* [2] If there's an initial value,   */
        /*     generate IR and store it       */
        /**************************************/
        if (initialValue != null)
        {
            // Generate IR for the initial value expression
            Temp initTemp = initialValue.irMe();
            
            // Store the initial value to the variable (use unique IR name)
            Ir.getInstance().AddIrCommand(new IrCommandStore(irName, initTemp));
        }

        /**************************************/
        /* [3] Return null (declarations don't*/
        /*     return values)                 */
        /**************************************/
        return null;
    }
}
