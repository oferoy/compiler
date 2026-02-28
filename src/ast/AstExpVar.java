package ast;

import types.*;
import temp.*;

public abstract class AstExpVar extends AstExp
{
    /*******************************************/
    /* Abstract method - must be implemented  */
    /* by all variable expression subclasses  */
    /* (AstExpVarSimple, AstExpVarField,      */
    /*  AstExpVarSubscript)                   */
    /*******************************************/
    @Override
    public abstract Temp irMe();
}