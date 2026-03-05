package ir;

import mips.*;

/** Pop stack into $s1 (used after call so base doesn't overwrite result). */
public class IrCommandPopToS1 extends IrCommand
{
	@Override
	public void mipsMe() { MipsGenerator.getInstance().popToS1(); }
}
