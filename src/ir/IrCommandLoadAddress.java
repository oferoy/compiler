package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

public class IrCommandLoadAddress extends IrCommand
{
	Temp dst;
	String label;

	public IrCommandLoadAddress(Temp dst, String label) {
		this.dst = dst;
		this.label = label;
	}

	@Override
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); if (dst != null) s.add(dst); return s; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().loadAddress(dst, label); }
}
