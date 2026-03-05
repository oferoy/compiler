package ir;

import java.util.Set;
import java.util.HashSet;
import temp.Temp;
import mips.*;

/** Push $s0 onto stack so it survives nested eval (e.g. outer addPairs preserves arg, inner addPairs overwrites $s0). */
public class IrCommandPushS0 extends IrCommand
{
	@Override
	public Set<Temp> getUse() { return new HashSet<>(); }
	@Override
	public Set<Temp> getDef() { return new HashSet<>(); }
	@Override
	public void mipsMe() { MipsGenerator.getInstance().pushS0(); }
}
