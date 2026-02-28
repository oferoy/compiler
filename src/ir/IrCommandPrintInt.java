/***********/
/* PACKAGE */
/***********/
package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

public class IrCommandPrintInt extends IrCommand
{
	Temp t;

	public IrCommandPrintInt(Temp t)
	{
		this.t = t;
	}

	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (t != null) s.add(t); return s; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().printInt(t); }
}
