package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstExpBinop extends AstExp
{
    public int op;
    public AstExp left;
    public AstExp right;
    /** Result type from semantMe (used in irMe for string concat). */
    public Type resultType;

    public static final int PLUS = 0;
    public static final int MINUS = 1;
    public static final int TIMES = 2;
    public static final int DIVIDE = 3;
    public static final int LT = 4;
    public static final int GT = 5;
    public static final int EQ = 6;

    /******************/
    /* CONSTRUCTOR    */
    /******************/
    public AstExpBinop(AstExp left, AstExp right, int op)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.left = left;
        this.right = right;
        this.op = op;
    }

    /***************/
    /* PRINT ME    */
    /***************/
    @Override
    public void printMe()
    {
        System.out.print("AST NODE BINOP\n");

        if (left != null) left.printMe();
        if (right != null) right.printMe();

        AstGraphviz.getInstance().logNode(serialNumber, "BINOP");

        if (left != null) AstGraphviz.getInstance().logEdge(serialNumber, left.serialNumber);
        if (right != null) AstGraphviz.getInstance().logEdge(serialNumber, right.serialNumber);
    }

    /*****************/
    /* SEMANT ME     */
    /*****************/
    @Override
    public Type semantMe()
    {
        // Evaluate subexpressions
        Type leftType = left.semantMe();
        Type rightType = right.semantMe();

        // Extract constant value if needed for division
        Integer rightConst = (right instanceof AstExpInt)
                ? ((AstExpInt) right).value
                : null;

        // Use global type engine
        Type result = TypesHelper.binopResult(op, leftType, rightType, rightConst);

        if (result == null) {
            AstNode.error(lineNumber,
                    String.format("illegal operation: %s %s %s",
                            leftType.name, opName(), rightType.name));
        }

        this.resultType = result;
        return result;
    }

    /***********************/
    /* HELPER: OPERATOR    */
    /* NAME FOR DEBUGGING  */
    /***********************/
    private String opName()
    {
        return switch (op) {
            case PLUS -> "+";
            case MINUS -> "-";
            case TIMES -> "*";
            case DIVIDE -> "/";
            case LT -> "<";
            case GT -> ">";
            case EQ -> "=";
            default -> "?";
        };
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe()
    {
        /**************************************/
        /* [1] Recursively generate IR for   */
        /*     left and right operands        */
        /**************************************/
        Temp t1 = left.irMe();
        Temp t2 = right.irMe();

        /**************************************/
        /* [2] Allocate a fresh temporary     */
        /*     to hold the result             */
        /**************************************/
        Temp dst = TempFactory.getInstance().getFreshTemp();

        /**************************************/
        /* [3] Emit the appropriate IR command */
        /*     based on the operator          */
        /**************************************/
        switch (op)
        {
            case PLUS:  // 0
                if (resultType != null && resultType instanceof TypeString) {
                    Ir.getInstance().AddIrCommand(
                        new IrCommandConcatStrings(dst, t1, t2));
                } else {
                    Ir.getInstance().AddIrCommand(
                        new IrCommandBinopAddIntegers(dst, t1, t2));
                }
                break;

            case MINUS: // 1
                Ir.getInstance().AddIrCommand(
                    new IrCommandBinopSubIntegers(dst, t1, t2));
                break;

            case TIMES: // 2
                Ir.getInstance().AddIrCommand(
                    new IrCommandBinopMulIntegers(dst, t1, t2));
                break;

            case DIVIDE: // 3
                // DIVIDE not needed for ex4 subset, but included for completeness
                Ir.getInstance().AddIrCommand(
                    new IrCommandBinopDivIntegers(dst, t1, t2));
                break;

            case LT:    // 4
                Ir.getInstance().AddIrCommand(
                    new IrCommandBinopLtIntegers(dst, t1, t2));
                break;

            case GT:    // 5
                // GT not used in ex4 subset, but included for completeness
                Ir.getInstance().AddIrCommand(
                    new IrCommandBinopGtIntegers(dst, t1, t2));
                break;

            case EQ:    // 6
                Ir.getInstance().AddIrCommand(
                    new IrCommandBinopEqIntegers(dst, t1, t2));
                break;

            default:
                System.err.println("ERROR: Unknown operator " + op);
                break;
        }

        /**************************************/
        /* [4] Return the destination temp    */
        /**************************************/
        return dst;
    }
}