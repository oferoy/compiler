package symboltable;

import java.io.PrintWriter;
import types.*;

public class SymbolTable {

    // Hash table size
    private int hashArraySize = 13;

    // The actual hash table
    private SymbolTableEntry[] table = new SymbolTableEntry[hashArraySize];

    // Pointer to top of stack
    private SymbolTableEntry top;
    private int topIndex = 0;

    // Singleton instance
    private static SymbolTable instance = null;

    // For debug graph dumps
    public static int n = 0;
    private static final boolean DEBUG = false;
    private static final String SCOPE_BOUNDARY = "SCOPE-BOUNDARY";

    // Private constructor
    private SymbolTable() {}

    // Get instance
    public static SymbolTable getInstance() {
        if (instance == null) {
            instance = new SymbolTable();

            // Primitive types
            instance.enter("int",    TypeInt.getInstance());
            instance.enter("string", TypeString.getInstance());
            instance.enter("void",   TypeVoid.getInstance());

            // Built-in library functions:
            // void PrintInt(int i);
            instance.enter(
                "PrintInt",
                new TypeFunction(
                    TypeVoid.getInstance(),
                    "PrintInt",
                    new TypeList(TypeInt.getInstance(), null)
                )
            );

            // void PrintString(string s);
            instance.enter(
                "PrintString",
                new TypeFunction(
                    TypeVoid.getInstance(),
                    "PrintString",
                    new TypeList(TypeString.getInstance(), null)
                )
            );

            instance.enter("nil", TypeNil.getInstance());
        }
        return instance;
    }

    // Hash function (robust and general)
    private int hash(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        return Math.abs(s.hashCode()) % hashArraySize;
    }

    // Wrapper to call instance
    public static void enter(String name, Type t) {
        getInstance()._enter(name, t);
    }

    // Insert into table (no duplicate check)
    private void _enter(String name, Type t) {
        int hv = hash(name);
        SymbolTableEntry next = table[hv];

        SymbolTableEntry e = new SymbolTableEntry(name, t, hv, next, top, topIndex++);
        top = e;
        table[hv] = e;

        if (DEBUG) printMe();
    }

    // Static find (search all scopes)
    public static Type find(String name) {
        return getInstance()._find(name);
    }

    // Search from inner to outer scope via bucket chain
    private Type _find(String name) {
        int hv = hash(name);
        for (SymbolTableEntry e = table[hv]; e != null; e = e.next) {
            if (name.equals(e.name)) {
                return e.type;
            }
        }
        return null;
    }

    // findInCurrentScope: only search entries since last SCOPE-BOUNDARY
    public static Type findInCurrentScope(String name) {
        SymbolTableEntry e = getInstance().top;
        while (e != null && !e.name.equals(SCOPE_BOUNDARY)) {
            if (e.name.equals(name)) return e.type;
            e = e.prevtop;
        }
        return null;
    }

    // beginScope()
    public static void beginScope() {
        getInstance()._beginScope();
    }

    // beginScope(String ignored) for compatibility
    public static void beginScope(String ignored) {
        beginScope();
    }

    private void _beginScope() {
        enter(SCOPE_BOUNDARY, new TypeForScopeBoundaries("NONE"));
        if (DEBUG) printMe();
    }

    // endScope()
    public static void endScope() {
        getInstance()._endScope();
    }

    // Pop until boundary
    private void _endScope() {
        while (top != null && !top.name.equals(SCOPE_BOUNDARY)) {
            table[top.index] = top.next;
            topIndex--;
            top = top.prevtop;
        }
        // Pop boundary itself
        if (top != null) {
            table[top.index] = top.next;
            topIndex--;
            top = top.prevtop;
        }
        if (DEBUG) printMe();
    }

    // isSubclass(a, b) returns true if a <= b in inheritance
    public static boolean isSubclass(TypeClass child, TypeClass parent) {
        if (child == null || parent == null) return false;
        TypeClass it = child;
        while (it != null) {
            if (it.name.equals(parent.name)) return true;
            it = it.father;
        }
        return false;
    }

    // Dump symbol table to Graphviz
    public void printMe() {
        int i, j;
        String dirname = "./output/";
        String filename = String.format("SYMBOL_TABLE_%d_IN_GRAPHVIZ_DOT_FORMAT.txt", n++);

        try {
            PrintWriter fileWriter = new PrintWriter(dirname + filename);

            fileWriter.print("digraph structs {\n");
            fileWriter.print("rankdir = LR\n");
            fileWriter.print("node [shape=record];\n");

            fileWriter.print("hashTable [label=\"");
            for (i = 0; i < hashArraySize - 1; i++) fileWriter.format("<f%d>\n%d\n|", i, i);
            fileWriter.format("<f%d>\n%d\n\"];\n", hashArraySize - 1, hashArraySize - 1);

            for (i = 0; i < hashArraySize; i++) {
                if (table[i] != null)
                    fileWriter.format("hashTable:f%d -> node_%d_0:f0;\n", i, i);

                j = 0;
                for (SymbolTableEntry it = table[i]; it != null; it = it.next) {
                    fileWriter.format("node_%d_%d ", i, j);
                    fileWriter.format(
                        "[label=\"<f0>%s|<f1>%s|<f2>prevtop=%d|<f3>next\"];\n",
                        it.name,
                        (it.type == null ? "null" : it.type.name),
                        it.prevtopIndex
                    );

                    if (it.next != null) {
                        fileWriter.format("node_%d_%d -> node_%d_%d [style=invis,weight=10];\n",
                                i, j, i, j + 1);
                        fileWriter.format("node_%d_%d:f3 -> node_%d_%d:f0;\n",
                                i, j, i, j + 1);
                    }
                    j++;
                }
            }

            fileWriter.print("}\n");
            fileWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Safe insert with same-scope conflict check
    public static boolean insert(String name, Type t) {
        // check for conflict in current scope
        if (findInCurrentScope(name) != null) {
            return false;
        }

        // perform normal insert
        enter(name, t);
        return true;
    }
}
