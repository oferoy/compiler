package ast;

public class AstClassBody extends AstNode
{
    public AstTypeNameList fields;   // may be null
    public AstDecList      methods;  // may be null

    public AstClassBody(AstTypeNameList fields, AstDecList methods)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.fields  = fields;
        this.methods = methods;
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE CLASS BODY\n");
        if (fields != null)  fields.printMe();
        if (methods != null) methods.printMe();

        AstGraphviz.getInstance().logNode(serialNumber, "CLASS\nBODY");

        if (fields != null)
            AstGraphviz.getInstance().logEdge(serialNumber, fields.serialNumber);
        if (methods != null)
            AstGraphviz.getInstance().logEdge(serialNumber, methods.serialNumber);
    }
}
