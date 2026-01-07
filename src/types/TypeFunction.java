package types;

public class TypeFunction extends Type
{
	/***********************************/
	/* The return type of the function */
	/***********************************/
	public Type returnType;

	/*************************/
	/* types of input params */
	/*************************/
	public TypeList params;
	
	/****************/
	/* CTROR(S) ... */
	/****************/
	public TypeFunction(Type returnType, String name, TypeList params)
	{
		this.name = name;
		this.returnType = returnType;
		this.params = params;
	}
}
