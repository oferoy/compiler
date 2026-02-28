package ir;

import mips.*;

/** Store $s2 back to variable slot after base.field:=call() so outer activation's local is restored (callee may have overwritten it). */
public class IrCommandStoreS2ToVar extends IrCommand
{
	String varIrName;

	public IrCommandStoreS2ToVar(String varIrName) { this.varIrName = varIrName; }
	@Override
	public void mipsMe() { MipsGenerator.getInstance().storeS2ToVar(varIrName); }
}
