package ir;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import temp.*;
import mips.*;

/** Call function and store $v0 at base+fieldOffset; base is already in $s2 (set by MoveBaseToS2). */
public class IrCommandCallAndStoreFieldWithBaseInS2 extends IrCommand
{
	String funcName;
	List<Temp> args;
	int fieldOffset;
	boolean isVirtualCall;
	int methodSlot;

	public IrCommandCallAndStoreFieldWithBaseInS2(String funcName, List<Temp> args, int fieldOffset)
	{
		this(funcName, args, fieldOffset, false, -1);
	}
	public IrCommandCallAndStoreFieldWithBaseInS2(String funcName, List<Temp> args, int fieldOffset, boolean isVirtualCall, int methodSlot)
	{
		this.funcName = funcName;
		this.args = args;
		this.fieldOffset = fieldOffset;
		this.isVirtualCall = isVirtualCall;
		this.methodSlot = methodSlot;
	}
	@Override
	public Set<Temp> getUse()
	{
		Set<Temp> s = new HashSet<>();
		if (args != null) for (Temp t : args) if (t != null) s.add(t);
		return s;
	}
	@Override
	public void mipsMe()
	{
		if (isVirtualCall && methodSlot >= 0)
			MipsGenerator.getInstance().callFuncVirtualAndStoreResultAtFieldWithBaseInS2(methodSlot, args, fieldOffset);
		else
			MipsGenerator.getInstance().callFuncAndStoreResultAtFieldWithBaseInS2(funcName, args, fieldOffset);
	}
}
