/***********/
/* PACKAGE */
/***********/
package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

public class IRcommandConstInt extends IrCommand
{
	Temp t;
	int value;

	public IRcommandConstInt(Temp t, int value)
	{
		this.t = t;
		this.value = value;
	}

	@Override
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); if (t != null) s.add(t); return s; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().li(t, value); }
}
