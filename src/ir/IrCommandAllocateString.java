package ir;

import mips.*;

public class IrCommandAllocateString extends IrCommand
{
	String label;
	String value;

	public IrCommandAllocateString(String label, String value) {
		this.label = label;
		this.value = value;
	}

	@Override
	public void mipsMe() { MipsGenerator.getInstance().allocateString(label, value); }
}
