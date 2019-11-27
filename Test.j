.class public Test
.super java/lang/Object
.method public <init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method
.method public static func(I)I
	.limit stack 32
	.limit locals 32
	ldc 0
	istore_1
	iload_0 
	ldc 100 
	isub 
	ifeq label0
	ldc 0
	goto label1
	label0:
	ldc 1
	label1:
	ifeq label9
	label7:
	iload_1 
	ldc 100 
	isub 
	ifle label2
	ldc 0
	goto label3
	label2:
	ldc 1
	label3:
	ifeq label8
	iload_1 
	ldc 1
	iadd
	istore_1
	goto label7
	label8:
	label9:
	iload_1 
	ireturn
.end method
.method public static main([Ljava/lang/String;)V
	.limit stack 32
	.limit locals 32
	ldc 100
	istore_1
	getstatic java/lang/System/out Ljava/io/PrintStream; 
	iload_1 
	invokestatic Test/func(I)I
	invokevirtual java/io/PrintStream/println(I)V
	return
.end method
