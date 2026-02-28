package ir;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import temp.*;
import mips.*;

public class IrCommandCall extends IrCommand
{
	Temp dst;
	String funcName;
	List<Temp> args;
	String receiverVarName;
	boolean firstArgFromS2;
	boolean isVirtualCall;
	int methodSlot;

	public IrCommandCall(Temp dst, String funcName, List<Temp> args)
	{
		this(dst, funcName, args, null, false, false, -1);
	}

	public IrCommandCall(Temp dst, String funcName, List<Temp> args, String receiverVarName)
	{
		this(dst, funcName, args, receiverVarName, false, false, -1);
	}

	public IrCommandCall(Temp dst, String funcName, List<Temp> args, String receiverVarName, boolean firstArgFromS2)
	{
		this(dst, funcName, args, receiverVarName, firstArgFromS2, false, -1);
	}

	public String getFuncName() { return funcName; }

	public IrCommandCall(Temp dst, String funcName, List<Temp> args, String receiverVarName, boolean firstArgFromS2, boolean isVirtualCall, int methodSlot)
	{
		this.dst = dst;
		this.funcName = funcName;
		this.args = args;
		this.receiverVarName = receiverVarName;
		this.firstArgFromS2 = firstArgFromS2;
		this.isVirtualCall = isVirtualCall;
		this.methodSlot = methodSlot;
	}
	public Set<Temp> getUse() {
		Set<Temp> s = new HashSet<Temp>();
		if (args != null) for (Temp t : args) if (t != null) s.add(t);
		return s;
	}
	public Set<Temp> getDef() {
		Set<Temp> s = new HashSet<Temp>();
		if (dst != null) s.add(dst);
		return s;
	}
	public void mipsMe()
	{
		if (isVirtualCall && methodSlot >= 0)
			MipsGenerator.getInstance().callFuncVirtual(methodSlot, args, dst, firstArgFromS2);
		else
			MipsGenerator.getInstance().callFunc(funcName, args, dst, receiverVarName, firstArgFromS2);
	}
}
