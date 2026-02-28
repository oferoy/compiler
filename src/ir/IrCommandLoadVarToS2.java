package ir;

import mips.*;

/** Load variable (by IR name) directly into $s2 for base.field := call(); no temp so base cannot be clobbered. */
public class IrCommandLoadVarToS2 extends IrCommand
{
	String varIrName;

	public IrCommandLoadVarToS2(String varIrName) { this.varIrName = varIrName; }
	@Override
	public void mipsMe() { MipsGenerator.getInstance().loadVarToS2(varIrName); }
}
