package ast;

import types.*;
import temp.*;

public abstract class AstStmt extends AstNode
{
	/***********************************************/
	/* The default semantic action for an AST node */
	/***********************************************/
	public Type semantMe()
	{
		return null;
	}

	/*******************************************/
	/* Abstract method - must be implemented  */
	/* by all statement subclasses            */
	/*******************************************/
	public abstract Temp irMe();
}