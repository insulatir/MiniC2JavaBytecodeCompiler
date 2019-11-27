package listener.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ParamsContext;
import static listener.main.BytecodeGenListenerHelper.*;
import static listener.main.SymbolTable.*;

public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {
	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	SymbolTable symbolTable = new SymbolTable();
	
	int tab = 0;
	int label = 0;
	
	// program	: decl+
	
	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
		// 심볼테이블 초기화
		symbolTable.initFunDecl();
		
		// 함수 이름
		String fname = getFunName(ctx);
		ParamsContext params;
		
		// main 함수
		if (fname.equals("main")) {
			// 심볼테이블에 'String[] args' 추가
			symbolTable.putLocalVar("args", Type.INTARRAY);
		// main을 제외한 함수
		} else {
			// 심볼테이블에 함수 spec 추가 
			symbolTable.putFunSpecStr(ctx);
			params = (MiniCParser.ParamsContext) ctx.getChild(3);
			// 심볼테이블에 인자들 추가
			symbolTable.putParams(params);
		}		
	}
	
	// var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'| type_spec IDENT '[' LITERAL ']' ';'
	@Override
	public void enterVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		
		// 전역변수가 배열 타입
		if (isArrayDecl(ctx)) {
			symbolTable.putGlobalVar(varName, Type.INTARRAY);
		}
		// 전역변수가 초기값을 가짐
		else if (isDeclWithInit(ctx)) {
			symbolTable.putGlobalVarWithInitVal(varName, Type.INT, initVal(ctx));
		}
		else  { // simple decl
			symbolTable.putGlobalVar(varName, Type.INT);
		}
	}
	
	@Override
	public void enterLocal_decl(MiniCParser.Local_declContext ctx) {
		// 지역변수가 배열 타입
		if (isArrayDecl(ctx)) {
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INTARRAY);
		}
		// 지역변수가 초기값을 가짐
		else if (isDeclWithInit(ctx)) {
			symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), Type.INT, initVal(ctx));	
		}
		else  { // simple decl
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);
		}	
	}

	@Override
	public void exitProgram(MiniCParser.ProgramContext ctx) {
		// 공통의 초기 상태
		String classProlog = getFunProlog();
		
		String fun_decl = "", var_decl = "";
		
		String program = "";
		
		for(int i = 0; i < ctx.getChildCount(); i++) {
			if(isFunDecl(ctx, i))
				// 함수 decl
				fun_decl += newTexts.get(ctx.decl(i));
			else
				// 변수 decl
				var_decl += newTexts.get(ctx.decl(i));
		}
		
		// 최종 문장
		program = classProlog + var_decl + fun_decl;
		
		// 빈 줄 삭제
		program = deleteEmptyLines(program);
		
		// 트리에 최종 문장 추가
		newTexts.put(ctx, program);
		
		// 'Test.j' 파일
		File file = new File("Test.j");
		
		try {
			FileWriter fw = new FileWriter(file);
			// 'Test.j' 파일에 program 쓰기
			fw.write(newTexts.get(ctx));
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// program 출력
		System.out.println(newTexts.get(ctx));
	}	
	
	// decl	: var_decl | fun_decl
	@Override
	public void exitDecl(MiniCParser.DeclContext ctx) {
		String decl = "";
		if(ctx.getChildCount() == 1)
		{
			if(ctx.var_decl() != null)		// var_decl
				decl += newTexts.get(ctx.var_decl());
			else							// fun_decl
				decl += newTexts.get(ctx.fun_decl());
		}
		newTexts.put(ctx, decl);
	}
	
	// stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
	@Override
	public void exitStmt(MiniCParser.StmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() > 0)
		{
			if(ctx.expr_stmt() != null)				// expr_stmt
				stmt += newTexts.get(ctx.expr_stmt());
			else if(ctx.compound_stmt() != null)	// compound_stmt
				stmt += newTexts.get(ctx.compound_stmt());
			else if (ctx.if_stmt() != null)			// if_stmt
				stmt += newTexts.get(ctx.if_stmt());
			else if (ctx.while_stmt() != null)		// while_stmt
				stmt += newTexts.get(ctx.while_stmt());
			else if (ctx.return_stmt() != null)		// return_stmt
				stmt += newTexts.get(ctx.return_stmt());
		}
		newTexts.put(ctx, stmt);
	}
	
	// expr_stmt : expr ';'
	@Override
	public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() == 2)
		{
			stmt += newTexts.get(ctx.expr());	// expr
		}
		newTexts.put(ctx, stmt);
	}
	
	// while_stmt : WHILE '(' expr ')' stmt
	@Override
	public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
		String stmt = "";
		// while 조건문
		String condExpr = newTexts.get(ctx.expr());
		// while 실행문
		String thenStmt = newTexts.get(ctx.stmt());
		
		// while 라벨
		String lwhile = symbolTable.newLabel();
		// end 라벨
		String lend = symbolTable.newLabel();
		
		stmt = "\t" + lwhile + ":" + "\n"
				+ condExpr + "\n"
				// ifeq인 이유 : condExpr 실행 시 참이면 1이고 거짓이면 0인데 
				// ifeq는 이것을 0과 비교하기 때문에 거짓인 0과 같아야 while을 빠져나오기 때문
				+ "\t" + "ifeq " + lend + "\n"
				+ thenStmt + "\n"
				+ "\t" + "goto " + lwhile + "\n"
				+ "\t" + lend + ":" + "\n";
		
		newTexts.put(ctx, stmt);
	}
	
	// fun_decl	: type_spec IDENT '(' params ')' compound_stmt
	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
		String decl = "";
		// 함수 이름
		String fname = getFunName(ctx);
		
		// 함수 시작 부분
		decl += funcHeader(ctx, fname);
		// 함수 내부 문장들
		decl += newTexts.get(ctx.compound_stmt());
		// 리턴 존재 여부 판별
		decl += noReturn(ctx);
		// 함수의 끝
		decl += ".end method" + "\n";
		
		newTexts.put(ctx, decl);
	}

	// 함수 시작 부분
	private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
		// public static 메소드 
		return ".method public static " + symbolTable.getFunSpecStr(fname) + "\n"	
				+ "\t" + ".limit stack " 	+ getStackSize(ctx) + "\n"
				+ "\t" + ".limit locals " 	+ getLocalVarSize(ctx) + "\n";
	}
	
	@Override
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		// 전역변수 이름
		String varName = ctx.IDENT().getText();
		String varDecl = "";
		
		if (isDeclWithInit(ctx)) {
			varDecl += "putfield " + varName + "\n";  
			// v. initialization => Later! skip now..: 
		}
		newTexts.put(ctx, varDecl);
	}
	
	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		String varDecl = "";
		
		// 지역변수가 초기값을 가지고 있는 경우
		if (isDeclWithInit(ctx)) {
			// 지역변수 id
			String vId = symbolTable.getVarId(ctx);
					// 초기값 스택 추가
			varDecl += "\t" + "ldc " + ctx.LITERAL().getText() + "\n"
					// 지역변수에 스택 값 저장
					+ "\t" + "istore_" + vId + "\n"; 			
		}
		
		newTexts.put(ctx, varDecl);
	}
	
	// compound_stmt : '{' local_decl* stmt* '}'
	@Override
	public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
		String stmt = "";
		
		// 지역변수 추가
		if (ctx.local_decl() != null) {
			for (int i = 0; i < ctx.local_decl().size(); i++) {
				stmt += newTexts.get(ctx.local_decl(i)) + "\n";
			}
		}
		// 문장 추가
		if (ctx.stmt() != null) {
			for (int i = 0; i < ctx.stmt().size(); i++) {
				stmt += newTexts.get(ctx.stmt(i)) + "\n";
			}
		}
		
		newTexts.put(ctx, stmt);
	}

	// if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
	@Override
	public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
		String stmt = "";
		// if 조건문
		String condExpr = newTexts.get(ctx.expr());
		// if 실행문
		String thenStmt = newTexts.get(ctx.stmt(0));
		
		// end 라벨
		String lend = symbolTable.newLabel();
		// else 라벨
		String lelse = symbolTable.newLabel();
		
		// else가 없는 경우
		if(noElse(ctx)) {		
			stmt += condExpr + "\n"
				+ "\t" + "ifeq " + lend + "\n"
				+ thenStmt + "\n"
				+ "\t" + lend + ":" + "\n";	
		}
		// else가 있는 경우
		else {
			// else 실행문
			String elseStmt = newTexts.get(ctx.stmt(1));
			stmt += condExpr + "\n"
					+ "\t" + "ifeq " + lelse + "\n"
					+ thenStmt + "\n"
					+ "\t" + "goto " + lend + "\n"
					+ "\t" + lelse + ":" + "\n" 
					+ elseStmt + "\n"
					+ "\t" + lend + ":" + "\n";	
		}
		
		newTexts.put(ctx, stmt);
	}
	
	// return_stmt : RETURN ';' | RETURN expr ';'
	@Override
	public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
		String stmt = "";
		
		// return 타입이 void인 경우
		if(isVoidReturn(ctx)) {
			stmt += "\t" + "return";
		} 
		// return 타입이 int인 경우
		else {
			// return 값
			String returnExpr = newTexts.get(ctx.expr());
			stmt += returnExpr 
					+ "\t" + "ireturn";
		}
		
		newTexts.put(ctx, stmt);
	}
	
	@Override
	public void exitExpr(MiniCParser.ExprContext ctx) {
		String expr = "";

		if(ctx.getChildCount() <= 0) {
			newTexts.put(ctx, ""); 
			return;
		}		
		
		if(ctx.getChildCount() == 1) { // IDENT | LITERAL
			// IDENT인 경우
			if(ctx.IDENT() != null) {
				String idName = ctx.IDENT().getText();
				// 변수타입이 int인 경우
				if(symbolTable.getVarType(idName) == Type.INT) {
					expr += "\t" + "iload_" + symbolTable.getVarId(idName) + " \n";
				}
				//else	// Type int array => Later! skip now..
				//	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
			// LITERAL인 경우
			} else if (ctx.LITERAL() != null) {
					String literalStr = ctx.LITERAL().getText();
					expr += "\t" + "ldc " + literalStr + " \n";
			}
		} else if(ctx.getChildCount() == 2) { // UnaryOperation
			expr = handleUnaryExpr(ctx, expr);			
		}
		else if(ctx.getChildCount() == 3) {	 
			if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
				expr = newTexts.get(ctx.expr(0));
			} else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
				expr = newTexts.get(ctx.expr(0))
						+ "\t" + "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + "\n";
			} else { 											// binary operation
				expr = handleBinExpr(ctx, expr);
			}
		}
		// IDENT '(' args ')' |  IDENT '[' expr ']'
		else if(ctx.getChildCount() == 4) {
			if(ctx.args() != null){		// function calls
				expr = handleFunCall(ctx, expr);
			} else { // expr
				// Arrays: TODO  
			}
		}
		// IDENT '[' expr ']' '=' expr
		else { // Arrays: TODO			*/
		}
		newTexts.put(ctx, expr);
	}

	private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
		String l1 = symbolTable.newLabel();
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		
		expr += newTexts.get(ctx.expr(0));
		switch(ctx.getChild(0).getText()) {
		case "-":
			expr += "\t" + "ineg" + "\n"; 
			break;
		case "--":
			expr += "\t" + "ldc 1" + "\n"
					+ "\t" + "isub" + "\n"
					+ "\t" + "istore_" + symbolTable.getVarId(ctx.getChild(1).getText()) + "\n";
			break;
		case "++":
			expr += "\t" + "ldc 1" + "\n"
					+ "\t" + "iadd" + "\n"
					+ "\t" + "istore_" + symbolTable.getVarId(ctx.getChild(1).getText()) + "\n";
			break;
		case "!":
			expr += "\t" + "ifeq " + l2 + "\n"
					+ "\t" + l1 + ":" + "\n" 
					// 거짓
					+ "\t" + "ldc 0" + "\n"
					+ "\t" + "goto " + lend + "\n"
					+ "\t" + l2 + ":" + "\n" 
					// 참
					+ "\t" + "ldc 1" + "\n"
					+ "\t" + lend + ":" + "\n";
			break;
		}
		return expr;
	}


	private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		
		expr += newTexts.get(ctx.expr(0));
		expr += newTexts.get(ctx.expr(1));
		
		switch (ctx.getChild(1).getText()) {
			case "*":		// expr(0) expr(1) imul
				expr += "\t" + "imul" + "\n"; break;
			case "/":		// expr(0) expr(1) idiv
				expr += "\t" + "idiv" + "\n"; break;
			case "%":		// expr(0) expr(1) irem
				expr += "\t" + "irem" + "\n"; break;
			case "+":		// expr(0) expr(1) iadd
				expr += "\t" + "iadd" + "\n"; break;
			case "-":		// expr(0) expr(1) isub
				expr += "\t" + "isub" + "\n"; break;
				
			case "==":
				expr += "\t" + "isub " + "\n"
						// 'a == 0'면 참
						+ "\t" + "ifeq " + l2 + "\n"
						// 거짓
						+ "\t" + "ldc 0" + "\n"
						+ "\t" + "goto " + lend + "\n"
						+ "\t" + l2 + ":" + "\n"
						// 참
						+ "\t" + "ldc 1" + "\n"
						+ "\t" + lend + ":" + "\n";
				break;
			case "!=":
				expr += "\t" + "isub " + "\n"
						// 'a != 0'면 참
						+ "\t" + "ifne " + l2 + "\n"
						+ "\t" + "ldc 0" + "\n"
						+ "\t" + "goto " + lend + "\n"
						+ "\t" + l2 + ":" + "\n" 
						+ "\t" + "ldc 1" + "\n"
						+ "\t" + lend + ":" + "\n";
				break;
			case "<=":
				expr += "\t" + "isub " + "\n"
						// 'a <= 0'면 참
						+ "\t" + "ifle " + l2 + "\n"
						+ "\t" + "ldc 0" + "\n"
						+ "\t" + "goto " + lend + "\n"
						+ "\t" + l2 + ":" + "\n" 
						+ "\t" + "ldc 1" + "\n"
						+ "\t" + lend + ":" + "\n";
				break;
			case "<":
				expr += "\t" + "isub " + "\n"
						// 'a < 0'면 참
						+ "\t" + "iflt " + l2 + "\n"
						+ "\t" + "ldc 0" + "\n"
						+ "\t" + "goto " + lend + "\n"
						+ "\t" + l2 + ":" + "\n" 
						+ "\t" + "ldc 1" + "\n"
						+ "\t" + lend + ":" + "\n";
				break;

			case ">=":
				expr += "\t" + "isub " + "\n"
						// 'a >= 0'면 참
						+ "\t" + "ifge " + l2 + "\n"
						+ "\t" + "ldc 0" + "\n"
						+ "\t" + "goto " + lend + "\n"
						+ "\t" + l2 + ":" + "\n" 
						+ "\t" + "ldc 1" + "\n"
						+ "\t" + lend + ":" + "\n";
				break;

			case ">":
				expr += "\t" + "isub " + "\n"
						// 'a > 0'면 참
						+ "\t" + "ifgt " + l2 + "\n"
						+ "\t" + "ldc 0" + "\n"
						+ "\t" + "goto " + lend + "\n"
						+ "\t" + l2 + ": " + "\n" 
						+ "\t" + "ldc 1" + "\n"
						+ "\t" + lend + ": " + "\n";
				break;

			case "and":
						// 거짓이면 바로 빠져나옴
				expr +=  "\t" + "ifne "+ lend + "\n"
						+ "\t" + "pop" + "\n" 
						+ "\t" + "ldc 0" + "\n"
						+ "\t" + lend + ": " + "\n"; 
				break;
			case "or":
						// 참이면 바로 빠져나옴
				expr +=  "\t" + "ifeq "+ lend + "\n"
						+ "\t" + "pop" + "\n" 
						+ "\t" + "ldc 0" + "\n"
						+ "\t" + lend + ": " + "\n";
				break;

		}
		return expr;
	}
	private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
		// 함수 이름
		String fname = getFunName(ctx);		

		if (fname.equals("_print")) {		// System.out.println	
			expr = "\t" + "getstatic java/lang/System/out Ljava/io/PrintStream; " + "\n"
			  		+ newTexts.get(ctx.args()) 
			  		+ "\t" + "invokevirtual " + symbolTable.getFunSpecStr("_print") + "\n";
		} else {							// println이 아닌 함수
			expr = newTexts.get(ctx.args()) 
					+ "\t" + "invokestatic " + getCurrentClassName()+ "/" + symbolTable.getFunSpecStr(fname) + "\n";
		}	
		
		return expr;
	}

	// args	: expr (',' expr)* | ;
	@Override
	public void exitArgs(MiniCParser.ArgsContext ctx) {
		String argsStr = "\n";
		
		for (int i=0; i < ctx.expr().size() ; i++) {
			// 인자 추가
			argsStr += newTexts.get(ctx.expr(i)) ; 
		}		

		newTexts.put(ctx, argsStr);
	}

}
