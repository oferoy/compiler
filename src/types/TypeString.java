package types;

public class TypeString extends Type
{
	/**************************************/
	/* USUAL SINGLETON IMPLEMENTATION ... */
	/**************************************/
	private static TypeString instance = null;

	/*****************************/
	/* PREVENT INSTANTIATION ... */
	/*****************************/
	protected TypeString() {}

	/******************************/
	/* GET SINGLETON INSTANCE ... */
	/******************************/
	public static TypeString getInstance()
	{
		if (instance == null)
		{
			instance = new TypeString();
			instance.name = "string";
		}
		return instance;
	}
}
