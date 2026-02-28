package ir;

import java.util.Set;
import java.util.HashSet;
import temp.*;
import mips.*;

/** Load from array[index] with null and bounds check. */
public class IrCommandLoadArray extends IrCommand
{
	Temp dst;
	Temp arrayBase;
	Temp index;

	public IrCommandLoadArray(Temp dst, Temp arrayBase, Temp index) {
		this.dst = dst;
		this.arrayBase = arrayBase;
		this.index = index;
	}
	public Set<Temp> getUse() {
		Set<Temp> s = new HashSet<>();
		s.add(arrayBase);
		if (index != null) s.add(index);
		return s;
	}
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); s.add(dst); return s; }

	public void mipsMe() {
		MipsGenerator.getInstance().loadArray(dst, arrayBase, index);
	}
}
