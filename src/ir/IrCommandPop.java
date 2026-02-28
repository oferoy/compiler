package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

/** Pop a temp from the stack (4 bytes). */
public class IrCommandPop extends IrCommand
{
	Temp t;

	public IrCommandPop(Temp t) { this.t = t; }

	@Override
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); if (t != null) s.add(t); return s; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().pop(t); }
}
