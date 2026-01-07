package ast;

import types.*;

public abstract class AstDec extends AstNode
{
	/***********************************************/
	/* The default semantic action for an AST node */
	/***********************************************/
	public Type semantMe()
	{
		return null;
	}
}
