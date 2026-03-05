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
	/** When true, args.get(0) (second param in source) was preserved in $s0; use $s0 for $a1. */
	boolean arg0InS0;
	/** When true, second param was pushed after MoveTempToS0; load $a1 from stack before frame. */
	boolean arg0OnStack;
	/** When true, 4+ args and second param (args.get(1)) was pushed; load $a2 from stack at frameBytes($sp). */
	boolean arg1OnStack;
	/** When true, 3-arg global call was evaluated LTR; first two args are on stack, third in args.get(2). */
	boolean argsPushedLTR3;

	public IrCommandCall(Temp dst, String funcName, List<Temp> args) {
		this(dst, funcName, args, null, false, false, -1, false, false, false, false);
	}
	public IrCommandCall(Temp dst, String funcName, List<Temp> args, String receiverVarName) {
		this(dst, funcName, args, receiverVarName, false, false, -1, false, false, false, false);
	}
	public IrCommandCall(Temp dst, String funcName, List<Temp> args, String receiverVarName, boolean firstArgFromS2) {
		this(dst, funcName, args, receiverVarName, firstArgFromS2, false, -1, false, false, false, false);
	}
	public IrCommandCall(Temp dst, String funcName, List<Temp> args, String receiverVarName, boolean firstArgFromS2, boolean isVirtualCall, int methodSlot) {
		this(dst, funcName, args, receiverVarName, firstArgFromS2, isVirtualCall, methodSlot, false, false, false, false);
	}
	public IrCommandCall(Temp dst, String funcName, List<Temp> args, String receiverVarName, boolean firstArgFromS2, boolean isVirtualCall, int methodSlot, boolean arg0InS0) {
		this(dst, funcName, args, receiverVarName, firstArgFromS2, isVirtualCall, methodSlot, arg0InS0, false, false, false);
	}
	public IrCommandCall(Temp dst, String funcName, List<Temp> args, String receiverVarName, boolean firstArgFromS2, boolean isVirtualCall, int methodSlot, boolean arg0InS0, boolean arg0OnStack) {
		this(dst, funcName, args, receiverVarName, firstArgFromS2, isVirtualCall, methodSlot, arg0InS0, arg0OnStack, false, false);
	}

	public String getFuncName() { return funcName; }

	public IrCommandCall(Temp dst, String funcName, List<Temp> args, String receiverVarName, boolean firstArgFromS2, boolean isVirtualCall, int methodSlot, boolean arg0InS0, boolean arg0OnStack, boolean arg1OnStack, boolean argsPushedLTR3) {
		this.dst = dst;
		this.funcName = funcName;
		this.args = args;
		this.receiverVarName = receiverVarName;
		this.firstArgFromS2 = firstArgFromS2;
		this.isVirtualCall = isVirtualCall;
		this.methodSlot = methodSlot;
		this.arg0InS0 = arg0InS0;
		this.arg0OnStack = arg0OnStack;
		this.arg1OnStack = arg1OnStack;
		this.argsPushedLTR3 = argsPushedLTR3;
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
			MipsGenerator.getInstance().callFunc(funcName, args, dst, receiverVarName, firstArgFromS2, arg0InS0, arg0OnStack, arg1OnStack, argsPushedLTR3);
	}
}
