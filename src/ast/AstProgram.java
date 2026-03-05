package ast;

import java.util.ArrayList;
import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstProgram extends AstNode
{
    public AstDecList decList;     // global declarations
    public AstStmtList stmtList;   // top-level statements (optional, depends on grammar)

    // Constructor
    public AstProgram(AstDecList decList, AstStmtList stmtList)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.decList = decList;
        this.stmtList = stmtList;
    }

    // Print AST node
    @Override
    public void printMe()
    {
        System.out.print("AST NODE PROGRAM\n");

        if (decList != null) decList.printMe();
        if (stmtList != null) stmtList.printMe();

        AstGraphviz.getInstance().logNode(serialNumber, "PROGRAM");

        if (decList != null) AstGraphviz.getInstance().logEdge(serialNumber, decList.serialNumber);
        if (stmtList != null) AstGraphviz.getInstance().logEdge(serialNumber, stmtList.serialNumber);
    }

    // Semantic analysis for the program
    @Override
    public Type semantMe() throws Exception
    {
        // Begin global scope
        SymbolTable.beginScope("global");

        /************************************************/
        /* Single-pass semantic analysis:               */
        /* Process declarations in order of appearance. */
        /* Functions only see globals declared BEFORE   */
        /* them in the source file.                     */
        /*                                              */
        /* Note: Global variable initializers are       */
        /* evaluated in order, so a global can only     */
        /* reference globals defined before it.         */
        /************************************************/
        
        // Semant all global declarations
        if (decList != null)
        {
            decList.semantMe();
        }

        // Semant top-level statements (if your grammar allows it)
        if (stmtList != null)
        {
            stmtList.semantMe();
        }

        // Note: we intentionally do NOT end the global scope here.
        // User-defined classes and functions must remain in the symbol
        // table so that the IR generation phase (which runs after semantMe)
        // can still query their types (e.g., call return types, class layouts).

        // Programs do not have a type
        return null;
    }

    /** Collect all TypeClass from class declarations (for vtable building). */
    public ArrayList<TypeClass> getClassTypes() {
        ArrayList<TypeClass> list = new ArrayList<>();
        for (AstDecList it = decList; it != null; it = it.tail) {
            if (it.head instanceof AstDecClass) {
                Type t = SymbolTable.find(((AstDecClass) it.head).name);
                if (t instanceof TypeClass)
                    list.add((TypeClass) t);
            }
        }
        return list;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    public Temp irMe()
    {
        /************************************************/
        /* Strategy: Global initialized variables must  */
        /* be evaluated BEFORE entering main.           */
        /*                                              */
        /* 0. Build vtables for dynamic dispatch        */
        /* 1. First pass: IR for global variable        */
        /*    declarations (allocate + initialize)      */
        /* 2. Second pass: IR for functions (including  */
        /*    main)                                     */
        /************************************************/

        types.VtableBuilder.build(getClassTypes());

        // First pass: only global variable declarations
        for (AstDecList it = decList; it != null; it = it.tail)
        {
            if (it.head instanceof AstDecVar)
            {
                it.head.irMe();
            }
        }
        Ir.getInstance().AddIrCommand(new IrCommandGlobalInitEnd());

        // Second pass: functions (and other declarations like classes/arrays)
        AstDecClass.currentClassType = null;  // so global functions (monthJuly, main) don't see a stale class from semantMe
        for (AstDecList it = decList; it != null; it = it.tail)
        {
            if (!(it.head instanceof AstDecVar))
            {
                it.head.irMe();
            }
        }

        return null;
    }
}