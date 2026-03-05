/***********/
/* PACKAGE */
/***********/
package ir;

/*******************/
/* GENERAL IMPORTS */
/*******************/
import java.util.HashSet;
import java.util.Set;
import temp.*;

/*******************/
/* PROJECT IMPORTS */
/*******************/

public abstract class IrCommand
{
	/*****************/
	/* Label Factory */
	/*****************/
	protected static int labelCounter = 0;
	public    static String getFreshLabel(String msg)
	{
		return String.format("Label_%d_%s", labelCounter++,msg);
	}

	/** For liveness: temps read by this command. */
	public Set<Temp> getUse() { return new HashSet<>(); }
	/** For liveness: temps written by this command. */
	public Set<Temp> getDef() { return new HashSet<>(); }
	/** For CFG: label defined by this command (IrCommandLabel only). */
	public String getLabelName() { return null; }
	/** For CFG: jump target (IrCommandJumpLabel, IrCommandJumpIfEqToZero, IrCommandReturn). */
	public String getJumpLabel() { return null; }

	/** Emit MIPS for this command. */
	public abstract void mipsMe();
}
