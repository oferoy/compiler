package ir;

import java.util.Set;
import java.util.HashSet;
import temp.*;
import mips.*;

public class IrCommandAllocateClass extends IrCommand
{
	Temp dst;
	int numBytes;
	String vtableLabel;

	public IrCommandAllocateClass(Temp dst, int numBytes) {
		this(dst, numBytes, null);
	}
	public IrCommandAllocateClass(Temp dst, int numBytes, String vtableLabel) {
		this.dst = dst;
		this.numBytes = numBytes;
		this.vtableLabel = vtableLabel;
	}
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); if (dst != null) s.add(dst); return s; }

	public void mipsMe() {
		MipsGenerator.getInstance().allocateClass(dst, numBytes, vtableLabel);
	}
}
