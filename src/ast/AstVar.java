package ast;

import types.*;

public abstract class AstVar extends AstNode
{
    // Base constructor
    public AstVar()
    {
        super();
    }

    // Every variable node must implement printMe()
    @Override
    public abstract void printMe();

    // Every variable node must implement semantMe()
    // This is abstract because only the subclasses know
    // how to perform semantic analysis:
    // - simple var (x)
    // - field var (x.f)
    // - subscript var (x[i])
    public abstract Type semantMe() throws Exception;
}