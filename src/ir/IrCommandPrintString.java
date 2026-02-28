package ir;

import java.util.Set;
import java.util.HashSet;
import temp.*;
import mips.*;

public class IrCommandPrintString extends IrCommand
{
	Temp t;
	public IrCommandPrintString(Temp t) { this.t = t; }
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<Temp>(); if (t != null) s.add(t); return s; }
	public void mipsMe() { MipsGenerator.getInstance().printString(t); }
}
