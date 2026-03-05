package ir;

import java.util.Set;
import java.util.HashSet;
import temp.Temp;
import mips.*;

/** Push $s1 onto stack (preserve index across RHS that may call). */
public class IrCommandPushS1 extends IrCommand
{
	@Override
	public Set<Temp> getUse() { return new HashSet<>(); }
	@Override
	public Set<Temp> getDef() { return new HashSet<>(); }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().pushS1(); }
}
