package ast;

public class AstExpVarSubscript extends AstExpVar
{
	public AstExpVar var;
	public AstExp subscript;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstExpVarSubscript(AstExpVar var, AstExp subscript)
	{
		System.out.print("====================== var -> var [ exp ]\n");
		this.var = var;
		this.subscript = subscript;
	}

	/*****************************************************/
	/* The printing message for a subscript var AST node */
	/*****************************************************/
	public void printMe()
	{
		/*************************************/
		/* AST NODE TYPE = AST SUBSCRIPT VAR */
		/*************************************/
		System.out.print("AST NODE SUBSCRIPT VAR\n");

		/****************************************/
		/* RECURSIVELY PRINT VAR + SUBSRIPT ... */
		/****************************************/
		if (var != null) var.printMe();
		if (subscript != null) subscript.printMe();
	}
}
