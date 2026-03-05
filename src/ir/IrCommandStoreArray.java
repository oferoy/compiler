package ir;

import java.util.Set;
import java.util.HashSet;
import temp.*;
import mips.*;

public class IrCommandStoreArray extends IrCommand
{
	Temp arrayBase;
	Temp index;
	Temp src;

	public IrCommandStoreArray(Temp arrayBase, Temp index, Temp src) {
		this.arrayBase = arrayBase;
		this.index = index;
		this.src = src;
	}
	public Set<Temp> getUse() {
		Set<Temp> s = new HashSet<>();
		s.add(arrayBase);
		if (index != null) s.add(index);
		s.add(src);
		return s;
	}

	public void mipsMe() {
		MipsGenerator.getInstance().storeArray(arrayBase, index, src);
	}
}
