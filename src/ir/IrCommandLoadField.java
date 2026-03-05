package ir;

import java.util.Set;
import java.util.HashSet;
import temp.*;
import mips.*;

/** Load from base + offset with null check. */
public class IrCommandLoadField extends IrCommand
{
	Temp dst;
	Temp base;
	int offset;

	public IrCommandLoadField(Temp dst, Temp base, int offset) {
		this.dst = dst;
		this.base = base;
		this.offset = offset;
	}
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); s.add(base); return s; }
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); s.add(dst); return s; }

	public void mipsMe() {
		MipsGenerator.getInstance().loadField(dst, base, offset);
	}
}
