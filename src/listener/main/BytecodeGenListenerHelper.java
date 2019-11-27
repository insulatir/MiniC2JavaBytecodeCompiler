package listener.main;

import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.If_stmtContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;

public class BytecodeGenListenerHelper {
	// 함수인지 판별
	static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
		return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
	}
	
	// type_spec IDENT '[' ']'
	// 인자가 배열 타입인지 판별
	static boolean isArrayParamDecl(ParamContext param) {
		return param.getChildCount() == 4;
	}
	
	// global vars
	static int initVal(Var_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	// var_decl	: type_spec IDENT '=' LITERAL ';'
	static boolean isDeclWithInit(Var_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	// var_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Var_declContext ctx) {
		return ctx.getChildCount() == 6;
	}

	// local vars
	static int initVal(Local_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}
	
	// local_decl : type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Local_declContext ctx) {
		return ctx.getChildCount() == 6;
	}
	// local_decl : type_spec IDENT '=' LITERAL ';'
	static boolean isDeclWithInit(Local_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	// return type of function is void
	static boolean isVoidF(Fun_declContext ctx) {
		return ctx.type_spec().getText().equals("VOID");
	}
	// return_stmt : RETURN ';'
	static boolean isIntReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() == 3;
	}
	// return_stmt : RETURN expr ';'
	static boolean isVoidReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() == 2;
	}
	
	// <information extraction>
	static String getStackSize(Fun_declContext ctx) {
		return "32";
	}
	static String getLocalVarSize(Fun_declContext ctx) {
		return "32";
	}
	static String getTypeText(Type_specContext typespec) {
		// int type
		if (typespec.getText().equals("int")) {
			// 'I' 반환
			return "I";
		// void type
		} else if (typespec.getText().equals("void")) {
			// 'V' 반환
			return "V";
		}
		
		return "";
	}

	// params
	static String getParamName(ParamContext param) {
		// 인자 이름
		return param.IDENT().getText();
	}
	
	static String getParamTypesText(ParamsContext params) {
		String typeText = "";
		
		for(int i = 0; i < params.param().size(); i++) {
			MiniCParser.Type_specContext typespec = (MiniCParser.Type_specContext)  params.param(i).getChild(0);
			// 인자 타입
			typeText += getTypeText(typespec); // + ";";
		}
		// 인자들 타입 반환
		return typeText;
	}
	
	static String getLocalVarName(Local_declContext local_decl) {
		// 지역변수 이름
		return local_decl.IDENT().getText();
	}
	
	// fun_decl	: type_spec IDENT '(' params ')' compound_stmt
	static String getFunName(Fun_declContext ctx) {
		// 함수 이름
		return ctx.IDENT().getText();
	}
	
	// expr : IDENT '(' args ')'
	static String getFunName(ExprContext ctx) {
		// 함수 이름
		return ctx.IDENT().getText();
	}
	
	// if_stmt : IF '(' expr ')' stmt
	static boolean noElse(If_stmtContext ctx) {
		// 자식의 수가 5개 이하이면 else가 없는 if문
		return ctx.getChildCount() <= 5;
	}
	
	static String getFunProlog() {
		// 처음 시작 부분은 언제나 동일 (단, 클래스 이름은 Test)
		return ".class public Test\n"
				+ ".super java/lang/Object\n"
				+ ".method public <init>()V\n"
				+ "\t" + "aload_0\n"
				+ "\t" + "invokenonvirtual java/lang/Object/<init>()V\n"
				+ "\t" + "return\n"
				+ ".end method\n";
	}
	
	static String getCurrentClassName() {
		// 현재 클래스 이름
		return "Test";
	}
	
	static String noReturn(Fun_declContext ctx) {
		int stmtSize = ctx.compound_stmt().stmt().size();
		// return문이 존재하는 함수
		if (ctx.compound_stmt().stmt(stmtSize-1).return_stmt() != null) {
			// 빈 문장 반환
			return "";
		// return문이 존재하지 않는 함수
		} else {
			// 'return' 반환
			return "\t" + "return" + "\n";
		}
	}
	
	// 빈 줄 삭제
	static String deleteEmptyLines(String program) {
		StringBuilder res = new StringBuilder("");
		
		String[] lines = program.split("\n");
		for (String line : lines) {
			if (!line.isEmpty()) {
				res.append(line + "\n");
			}
		}
		
		return res.toString();
	}
}
