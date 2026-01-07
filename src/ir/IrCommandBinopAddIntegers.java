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
import temp.*;
import mips.*;

public class IrCommandBinopAddIntegers extends IrCommand
{
	public Temp t1;
	public Temp t2;
	public Temp dst;
	
	public IrCommandBinopAddIntegers(Temp dst, Temp t1, Temp t2)
	{
		this.dst = dst;
		this.t1 = t1;
		this.t2 = t2;
	}
	/***************/
	/* MIPS me !!! */
	/***************/
	public void mipsMe()
	{
		MipsGenerator.getInstance().add(dst,t1,t2);
	}
}
