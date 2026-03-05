package ir;

import java.util.Set;
import java.util.HashSet;
import temp.*;
import mips.*;

/** Allocate array: length at 0, elements at 4, 8, ... */
public class IrCommandAllocateArray extends IrCommand
{
	Temp dst;
	Temp sizeTemp;

	public IrCommandAllocateArray(Temp dst, Temp sizeTemp) {
		this.dst = dst;
		this.sizeTemp = sizeTemp;
	}
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (sizeTemp != null) s.add(sizeTemp); return s; }
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); if (dst != null) s.add(dst); return s; }

	public void mipsMe() {
		MipsGenerator.getInstance().allocateArray(dst, sizeTemp);
	}
}
