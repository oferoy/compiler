package ir;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import temp.*;
import mips.*;

/** Call function and store return value at base+fieldOffset (no dst temp; uses $v0 so result never aliases base). */
public class IrCommandCallAndStoreField extends IrCommand
{
	String funcName;
	List<Temp> args;
	Temp base;
	int fieldOffset;

	public IrCommandCallAndStoreField(String funcName, List<Temp> args, Temp base, int fieldOffset)
	{
		this.funcName = funcName;
		this.args = args;
		this.base = base;
		this.fieldOffset = fieldOffset;
	}
	@Override
	public Set<Temp> getUse()
	{
		Set<Temp> s = new HashSet<>();
		if (base != null) s.add(base);
		if (args != null) for (Temp t : args) if (t != null) s.add(t);
		return s;
	}
	@Override
	public void mipsMe()
	{
		MipsGenerator.getInstance().callFuncAndStoreResultAtField(funcName, args, base, fieldOffset);
	}
}
