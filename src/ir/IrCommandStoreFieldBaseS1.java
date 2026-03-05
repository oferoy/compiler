package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

/** Store to field at base in $s1 (offset, src). Use after popToS1 for base.field := call(). */
public class IrCommandStoreFieldBaseS1 extends IrCommand
{
	int offset;
	Temp src;

	public IrCommandStoreFieldBaseS1(int offset, Temp src) {
		this.offset = offset;
		this.src = src;
	}
	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (src != null) s.add(src); return s; }
	@Override
	public void mipsMe() { MipsGenerator.getInstance().storeFieldBaseS1(offset, src); }
}
