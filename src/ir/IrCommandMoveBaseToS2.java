package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

/** Copy base temp into $s2 so base survives arg evaluation and call (for base.field := call()). */
public class IrCommandMoveBaseToS2 extends IrCommand
{
	Temp base;

	public IrCommandMoveBaseToS2(Temp base) { this.base = base; }
	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (base != null) s.add(base); return s; }
	@Override
	public void mipsMe() { MipsGenerator.getInstance().moveBaseToS2(base); }
}
