/***********/
/* PACKAGE */
/***********/
package symboltable;

/*******************/
/* PROJECT IMPORTS */
/*******************/
import types.*;

/**********************/
/* SYMBOL TABLE ENTRY */
/**********************/
public class SymbolTableEntry
{
	/*********/
	/* index */
	/*********/
	int index;
	
	/********/
	/* name */
	/********/
	public String name;

	/******************/
	/* TYPE value ... */
	/******************/
	public Type type;

	/*********************************************/
	/* prevtop and next symbol table entries ... */
	/*********************************************/
	public SymbolTableEntry prevtop;
	public SymbolTableEntry next;

	/****************************************************/
	/* The prevtopIndex is just for debug purposes ... */
	/****************************************************/
	public int prevtopIndex;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public SymbolTableEntry(
		String name,
		Type type,
		int index,
		SymbolTableEntry next,
		SymbolTableEntry prevtop,
		int prevtopIndex)
	{
		this.index = index;
		this.name = name;
		this.type = type;
		this.next = next;
		this.prevtop = prevtop;
		this.prevtopIndex = prevtopIndex;
	}
}
