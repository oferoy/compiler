package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

/** Push a temp onto the stack (4 bytes). Used to preserve base across a call. */
public class IrCommandPush extends IrCommand
{
	Temp t;

	public IrCommandPush(Temp t) { this.t = t; }

	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (t != null) s.add(t); return s; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().push(t); }
}
