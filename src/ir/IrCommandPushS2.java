package ir;

import java.util.Set;
import java.util.HashSet;
import temp.Temp;
import mips.*;

/** Push $s2 onto stack (preserve array base across RHS that may call). */
public class IrCommandPushS2 extends IrCommand
{
	@Override
	public Set<Temp> getUse() { return new HashSet<>(); }
	@Override
	public Set<Temp> getDef() { return new HashSet<>(); }

	@Override
	public void mipsMe() {
		MipsGenerator.getInstance().pushS2();
	}
}
