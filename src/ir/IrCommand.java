/***********/
/* PACKAGE */
/***********/
package ir;

/*******************/
/* GENERAL IMPORTS */
/*******************/

/*******************/
/* PROJECT IMPORTS */
/*******************/

public abstract class IrCommand
{
	/*****************/
	/* Label Factory */
	/*****************/
	protected static int label_counter=0;
	public    static String getFreshLabel(String msg)
	{
		return String.format("Label_%d_%s",label_counter++,msg);
	}

	/***************/
	/* MIPS me !!! */
	/***************/
	public abstract void mipsMe();
}
