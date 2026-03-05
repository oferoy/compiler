package ir;

import java.util.Set;
import java.util.HashSet;
import temp.Temp;
import mips.*;

/** Move temp into $s1 (preserve index across RHS that may call). */
public class IrCommandMoveToS1 extends IrCommand
{
	Temp src;

	public IrCommandMoveToS1(Temp src) { this.src = src; }

	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (src != null) s.add(src); return s; }
	@Override
	public Set<Temp> getDef() { return new HashSet<>(); }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().moveToS1(src); }
}
