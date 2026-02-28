package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

/** Load receiver from $s2 (set at method entry from $a0). Use in methods instead of Load("this") to avoid slot mixups. */
public class IrCommandLoadThis extends IrCommand
{
	Temp dst;

	public IrCommandLoadThis(Temp dst) {
		this.dst = dst;
	}

	@Override
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); if (dst != null) s.add(dst); return s; }

	@Override
	public void mipsMe() {
		MipsGenerator.getInstance().loadThis(dst);
	}
}
