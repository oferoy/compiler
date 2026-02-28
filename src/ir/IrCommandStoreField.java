package ir;

import java.util.Set;
import java.util.HashSet;
import temp.*;
import mips.*;

public class IrCommandStoreField extends IrCommand
{
	Temp base;
	int offset;
	Temp src;

	public IrCommandStoreField(Temp base, int offset, Temp src) {
		this.base = base;
		this.offset = offset;
		this.src = src;
	}
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); s.add(base); s.add(src); return s; }

	public void mipsMe() {
		MipsGenerator.getInstance().storeField(base, offset, src);
	}
}
