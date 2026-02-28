package ir;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import temp.*;
import mips.*;

/** Call function and store $v0 at base+fieldOffset; base is already on top of stack (pushed by previous IR). */
public class IrCommandCallAndStoreFieldFromStack extends IrCommand
{
	String funcName;
	List<Temp> args;
	int fieldOffset;

	public IrCommandCallAndStoreFieldFromStack(String funcName, List<Temp> args, int fieldOffset)
	{
		this.funcName = funcName;
		this.args = args;
		this.fieldOffset = fieldOffset;
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
		MipsGenerator.getInstance().callFuncAndStoreResultAtFieldFromStack(funcName, args, fieldOffset);
	}
}
