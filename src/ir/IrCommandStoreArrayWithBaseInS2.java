package ir;

import java.util.Set;
import java.util.HashSet;
import temp.*;
import mips.*;

/** Store src at array[index]; base must already be in $s2 (call moveBaseToS2 first). */
public class IrCommandStoreArrayWithBaseInS2 extends IrCommand
{
	Temp index;
	Temp src;

	public IrCommandStoreArrayWithBaseInS2(Temp index, Temp src) {
		this.index = index;
		this.src = src;
	}
	public Set<Temp> getUse() {
		Set<Temp> s = new HashSet<>();
		if (index != null) s.add(index);
		s.add(src);
		return s;
	}

	public void mipsMe() {
		MipsGenerator.getInstance().storeArrayWithBaseInS2(index, src);
	}
}
