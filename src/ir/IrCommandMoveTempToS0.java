package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

/** Copy temp into $s0 so it survives evaluation of following args (e.g. nested addPairs(call(), call())). */
public class IrCommandMoveTempToS0 extends IrCommand
{
	Temp t;

	public IrCommandMoveTempToS0(Temp t) { this.t = t; }
	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (t != null) s.add(t); return s; }
	@Override
	public void mipsMe() { MipsGenerator.getInstance().moveTempToS0(t); }
}
