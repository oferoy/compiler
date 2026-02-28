package ir;

import mips.*;

/** Store incoming $a0/$a1/$a2/$a3 into param variable at function entry. */
public class IrCommandStoreParam extends IrCommand
{
	String varName;
	int paramIndex;

	public IrCommandStoreParam(String varName, int paramIndex) {
		this.varName = varName;
		this.paramIndex = paramIndex;
	}

	public String getVarName() { return varName; }
	public int getParamIndex() { return paramIndex; }

	@Override
	public void mipsMe() {
		MipsGenerator.getInstance().storeParam(paramIndex, varName);
	}
}
