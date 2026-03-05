package ir;

import java.util.Set;
import java.util.HashSet;
import temp.Temp;
import mips.MipsGenerator;

/** Emit MIPS to print a constant int (for DEBUG_EVAL_ORDER: see arg eval order at runtime). */
public class IrCommandDebugPrintConstInt extends IrCommand {
	private final int value;

	public IrCommandDebugPrintConstInt(int value) { this.value = value; }

	@Override public Set<Temp> getUse() { return new HashSet<>(); }
	@Override public Set<Temp> getDef() { return new HashSet<>(); }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().emitDebugPrintConstInt(value); }
}
