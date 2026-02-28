/***********/
/* PACKAGE */
/***********/
package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

public class IrCommandJumpIfEqToZero extends IrCommand
{
	Temp t;
	String labelName;

	public IrCommandJumpIfEqToZero(Temp t, String labelName)
	{
		this.t          = t;
		this.labelName = labelName;
	}

	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (t != null) s.add(t); return s; }

	@Override
	public String getJumpLabel() { return labelName; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().beqz(t, labelName); }
}
