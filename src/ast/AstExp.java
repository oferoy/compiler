package ast;

import types.*;
import temp.*;

public abstract class AstExp extends AstNode
{
    public AstExp() {
        super();   // sets serialNumber and lineNumber
    }

    @Override
    public Type semantMe()
    {
        return null;
    }

    /*******************************************/
    /* Abstract method - must be implemented  */
    /* by all expression subclasses           */
    /*******************************************/
    public abstract Temp irMe();
}