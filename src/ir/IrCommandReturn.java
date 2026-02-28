package ir;

import java.util.Set;
import java.util.HashSet;
import temp.*;
import mips.*;

public class IrCommandReturn extends IrCommand
{
	Temp t;
	String endLabel;

	public IrCommandReturn(Temp t, String endLabel)
	{
		this.t = t;
		this.endLabel = endLabel;
	}
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<Temp>(); if (t != null) s.add(t); return s; }
	public String getJumpLabel() { return endLabel; }
	public void mipsMe()
	{
		if (t != null) MipsGenerator.getInstance().moveReg("$v0", t);
		MipsGenerator.getInstance().jump(endLabel);
	}
}
