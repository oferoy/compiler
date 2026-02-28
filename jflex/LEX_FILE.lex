/***************************/
/* FILE NAME: LEX_FILE.lex */
/***************************/

/*************/
/* USER CODE */
/*************/

import java_cup.runtime.*;

/******************************/
/* DOLLAR DOLLAR - DON'T TOUCH! */
/******************************/

%%

/************************************/
/* OPTIONS AND DECLARATIONS SECTION */
/************************************/
   
%class Lexer
%line
%column
%cupsym TokenNames
%cup

%{
private Symbol symbol(int type)               { return new Symbol(type, yyline, yycolumn); }
private Symbol symbol(int type, Object value) { return new Symbol(type, yyline, yycolumn, value); }

public int getLine() { return yyline + 1; }
public int getTokenStartPosition() { return yycolumn + 1; }
%}

/***********************/
/* MACRO DECLARATIONS  */
/***********************/
LineTerminator = \r|\n|\r\n
WhiteSpace    = {LineTerminator} | [ \t\f]
INTEGER       = [0-9]+
ID            = [a-zA-Z][a-zA-Z0-9]*
STRING        = \"[A-Za-z]*\"

/******************************/
/* DOLLAR DOLLAR - DON'T TOUCH! */
/******************************/

%%

/************************************************************/
/* LEXER matches regular expressions to actions (Java code) */
/************************************************************/

<YYINITIAL> {

    /* ============================================
       COMMENTS (C-style, strict Figure 2 rules)
       ============================================ */

    // --- TYPE 1 COMMENT ---
    // valid (only allowed chars, ends with newline)
    "//"([a-zA-Z0-9\(\)\[\]\{\}\?\!\+\-\*\/\.\;\ \t\f\r])* \n   { /* skip */ }

    // invalid line comment
    "//".* \n { throw new RuntimeException("ERROR"); }

    // --- TYPE 2 COMMENT ---
    // valid block comment
    "/*"([a-zA-Z0-9\(\)\[\]\{\}\?\!\+\-\/\.\;\ \t\f\r\n]
         | \*+[a-zA-Z0-9\(\)\[\]\{\}\?\!\+\-\.\;\ \t\f\r\n])*\*+"/" { /* skip */ }

    // unterminated block comment
    "/*"    { throw new RuntimeException("ERROR"); }
    
    /* ============================================
       SYMBOLS & OPERATORS
       ============================================ */

    "{"         { return symbol(TokenNames.LBRACE); }
    "}"         { return symbol(TokenNames.RBRACE); }
    "["         { return symbol(TokenNames.LBRACK); }
    "]"         { return symbol(TokenNames.RBRACK); }
    ","         { return symbol(TokenNames.COMMA); }
    "."         { return symbol(TokenNames.DOT); }
    ";"         { return symbol(TokenNames.SEMICOLON); }
    ":="        { return symbol(TokenNames.ASSIGN); }
    "="         { return symbol(TokenNames.EQ); }
    "<"         { return symbol(TokenNames.LT); }
    ">"         { return symbol(TokenNames.GT); }

    /* ============================================
       KEYWORDS
       ============================================ */
    "class"     { return symbol(TokenNames.CLASS); }
    "array"     { return symbol(TokenNames.ARRAY); }
    "while"     { return symbol(TokenNames.WHILE); }
    "int"       { return symbol(TokenNames.TYPE_INT); }
    "void"      { return symbol(TokenNames.TYPE_VOID); }
    "extends"   { return symbol(TokenNames.EXTENDS); }
    "return"    { return symbol(TokenNames.RETURN); }
    "new"       { return symbol(TokenNames.NEW); }
    "if"        { return symbol(TokenNames.IF); }
    "else"      { return symbol(TokenNames.ELSE); }
    "string"    { return symbol(TokenNames.TYPE_STRING); }
    "nil"       { return symbol(TokenNames.NIL); }

    /* ============================================
       ARITHMETIC OPERATORS & PARENS
       ============================================ */
    "+"         { return symbol(TokenNames.PLUS); }
    "-"         { return symbol(TokenNames.MINUS); }
    "*"         { return symbol(TokenNames.TIMES); }
    "/"         { return symbol(TokenNames.DIVIDE); }
    "("         { return symbol(TokenNames.LPAREN); }
    ")"         { return symbol(TokenNames.RPAREN); }

    /* ============================================
       LITERALS
       ============================================ */
    
    {INTEGER} {
        int val = Integer.parseInt(yytext());
   
        // disallow leading zeros except for single "0"
        if (yytext().length() > 1 && yytext().startsWith("0")) {
            throw new RuntimeException("ERROR");
        }
   
        // disallow integers out of range [0, 32767]
        if (val < 0 || val > 32767) {
            throw new RuntimeException("ERROR");
        }
   
        return symbol(TokenNames.INT, val);
    }

    {STRING}    { return symbol(TokenNames.STRING, yytext()); }
    {ID}        { return symbol(TokenNames.ID, yytext()); }

    /* ============================================
       WHITESPACE
       ============================================ */
    {WhiteSpace} { /* skip whitespace */ }

    /* ============================================
       ANY OTHER ILLEGAL CHARACTER
       ============================================ */
    . { throw new RuntimeException("ERROR"); }

    /* ============================================
       END OF FILE
       ============================================ */
    <<EOF>> { return symbol(TokenNames.EOF); }
}
