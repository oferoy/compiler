package ir;

import java.util.Set;
import java.util.HashSet;
import temp.Temp;
import mips.*;

/** Pop stack into $s1 (restore index after RHS). */
public class IrCommandPopS1 extends IrCommand
{
	@Override
	public Set<Temp> getUse() { return new HashSet<>(); }
	@Override
	public Set<Temp> getDef() { return new HashSet<>(); }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().popS1(); }
}
