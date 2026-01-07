package types;

public class TypeList
{
	/****************/
	/* DATA MEMBERS */
	/****************/
	public Type head;
	public TypeList tail;

	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public TypeList(Type head, TypeList tail)
	{
		this.head = head;
		this.tail = tail;
	}
}
