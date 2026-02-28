package ast;

import types.Type;

public abstract class AstNode {

    public int serialNumber;

    // MUST EXIST — accessed by all semantics
    public int lineNumber;

    public void printMe() {}

    public Type semantMe() throws Exception {
        return null;
    }

    public static void error(int line, String msg) {
        throw new RuntimeException("ERROR(" + line + ")");
    }
}
