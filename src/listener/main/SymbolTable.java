package listener.main;

import java.util.HashMap;
import java.util.Map;
import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.Var_declContext;
import static listener.main.BytecodeGenListenerHelper.*;

public class SymbolTable {
	enum Type {
		INT, INTARRAY, VOID, ERROR
	}
	
	static public class VarInfo {
		Type type; 
		int id;
		int initVal;
		
		public VarInfo(Type type, int id, int initVal) {
			this.type = type;
			this.id = id;
			this.initVal = initVal;
		}
		public VarInfo(Type type, int id) {
			this.type = type;
			this.id = id;
			this.initVal = 0;
		}
	}
	
	static public class FInfo {
		public String sigStr;
	}
	
	private Map<String, VarInfo> _lsymtable = new HashMap<>();	// local v.
	private Map<String, VarInfo> _gsymtable = new HashMap<>();	// global v.
	private Map<String, FInfo> _fsymtable = new HashMap<>();	// function 
	
		
	private int _globalVarID = 0;
	private int _localVarID = 0;
	private int _labelID = 0;
	private int _tempVarID = 0;
	
	SymbolTable(){
		initFunDecl();
		initFunTable();
	}
	
	void initFunDecl(){		// at each func decl
		_lsymtable.clear();
		_localVarID = 0;
		_labelID = 0;
		_tempVarID = 32;		
	}
	
	void putLocalVar(String varname, Type type){
		// type, id를 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _localVarID++);
		// 지역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_lsymtable.put(varname, varinfo);
	}
	
	void putGlobalVar(String varname, Type type){
		// type, id를 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _globalVarID++);
		// 전역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_gsymtable.put(varname, varinfo);
	}
	
	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		// type, id와 초기값을 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _localVarID++, initVar);
		// 지역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_lsymtable.put(varname, varinfo);
	}
	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		// type, id와 초기값을 가지고 변수정보 생성
		VarInfo varinfo = new VarInfo(type, _globalVarID++, initVar);
		// 전역변수테이블에 변수이름과 변수정보를 쌍으로 하여 추가
		_gsymtable.put(varname, varinfo);
	}
	
	void putParams(MiniCParser.ParamsContext params) {
		for(int i = 0; i < params.param().size(); i++) {
			// 인자 이름 
			String pname = getParamName(params.param(i));
			// 인자 타입
			Type type = Type.valueOf(params.param(i).type_spec().getText().toUpperCase());
			// 지역변수테이블에 인자 이름, 인자 타입을 쌍으로 하여 추가
			putLocalVar(pname, type);
		}
	}
	
	private void initFunTable() {
		// println 함수정보
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "java/io/PrintStream/println(I)V";
		// main 함수정보
		FInfo maininfo = new FInfo();
		maininfo.sigStr = "main([Ljava/lang/String;)V";
		// 함수테이블에 println은 "_print"라는 이름을 가지게 하여 추가
		_fsymtable.put("_print", printlninfo);
		// 함수테이블에 main은 "main"라는 이름을 가지게 하여 추가 
		_fsymtable.put("main", maininfo);
	}
	
	public String getFunSpecStr(String fname) {		
		String res = "";
		
		FInfo finfo = new FInfo();
		// 함수테이블에서 함수이름에 해당하는 함수정보
		finfo = _fsymtable.get(fname);
		// 함수의 spec
		res = finfo.sigStr;
		
		return res;
	}

	public String getFunSpecStr(Fun_declContext ctx) {
		// 함수 이름
		String fname = getFunName(ctx);
		String res = "";
		
		FInfo finfo = new FInfo();
		// 함수테이블에서 함수이름에 해당하는 함수정보
		finfo = _fsymtable.get(fname);
		// 함수의 spec
		res = finfo.sigStr;
		
		return res;
	}
	
	public String putFunSpecStr(Fun_declContext ctx) {
		// 함수 이름
		String fname = getFunName(ctx);
		String argtype = "";	
		String rtype = "";
		String res = "";
		
		// 인자들의 타입
		argtype = getParamTypesText(ctx.params());
		// 리턴 타입
		rtype = getTypeText(ctx.type_spec());
		// 바이트 코드 형식의 함수 spec
		res =  fname + "(" + argtype + ")" + rtype;
		
		FInfo finfo = new FInfo();
		// 함수 정보에 함수 spec 저장
		finfo.sigStr = res;
		// 함수테이블에 함수 이름과 함수 정보를 쌍으로 하여 추가
		_fsymtable.put(fname, finfo);
		
		return res;
	}
	
	String getVarId(String name){
		// 이름을 가지고 지역변수테이블에서 지역변수정보 탐색
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		// 지역변수정보가 존재한다면
		if (lvar != null) {
			// 지역변수의 id 반환
			return lvar.id+"";
		}
		
		// 이름을 가지고 전역변수테이블에서 전역변수정보 탐색
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		// 전역변수정보가 존재한다면
		if (gvar != null) {
			// 전역변수의 id 반환
			return gvar.id+"";
		}
		
		// 두 테이블 모두에서 존재하지 않는다면 ERROR
		return Type.ERROR+"";
	}
	
	Type getVarType(String name){
		// 이름을 가지고 지역변수테이블에서 지역변수정보 탐색
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		// 지역변수정보가 존재한다면
		if (lvar != null) {
			// 지역변수의 타입 반환
			return lvar.type;
		}
		
		// 이름을 가지고 전역변수테이블에서 전역변수정보 탐색
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		// 전역변수정보가 존재한다면
		if (gvar != null) {
			// 전역변수의 타입 반환
			return gvar.type;
		}
		
		// 두 테이블 모두에서 존재하지 않는다면 ERROR
		return Type.ERROR;	
	}
	String newLabel() {
		// 새로운 라벨 생성 후 _labelID 증가
		return "label" + _labelID++;
	}
	
	String newTempVar() {
		String id = "";
		// 새로운 임시변수 생성 후 _tempVarID 감소
		return id + _tempVarID--;
	}

	// global
	public String getVarId(Var_declContext ctx) {
		String sname = "";
		// 전역변수의 이름을 가지고 전역변수의 id 획득
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}

	// local
	public String getVarId(Local_declContext ctx) {
		String sname = "";
		// 지역변수의 이름을 가지고 지역변수의 id 획득
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}
	
}
