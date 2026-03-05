package ast;

import types.*;
import temp.*;

public abstract class AstDec extends AstNode
{
    public AstDec() {
        // nothing to initialize
    }

    @Override
    public Type semantMe()
    {
        return null;  // subclasses override this
    }

    /*******************************************/
    /* Abstract method - must be implemented  */
    /* by all declaration subclasses          */
    /*******************************************/
    public abstract Temp irMe();
}