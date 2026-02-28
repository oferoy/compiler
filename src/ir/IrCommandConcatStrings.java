package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

/** Concatenate two strings; result pointer in dst. left and right are string pointers. */
public class IrCommandConcatStrings extends IrCommand
{
	public Temp dst;
	public Temp left;
	public Temp right;

	public IrCommandConcatStrings(Temp dst, Temp left, Temp right) {
		this.dst = dst;
		this.left = left;
		this.right = right;
	}

	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (left != null) s.add(left); if (right != null) s.add(right); return s; }
	@Override
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); if (dst != null) s.add(dst); return s; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().concatStrings(dst, left, right); }
}
