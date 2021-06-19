JFLAGS = -g
JC = javac

# FIXME incorporate code generation here

.SUFFIXES: .java .class

CLASSES = $(wildcard de/kenran/jlox/*.java)

.java.class:
	$(JC) $(JFLAGS) $*.java

default: classes

classes: $(CLASSES:.java=.class)

codegen: de/kenran/tools/GenerateAst.java
	$(JC) de/kenran/tools/GenerateAst.java
	@java de.kenran.tools.GenerateAst de/kenran/jlox
.PHONY: codegen

run: classes
	@java de.kenran.jlox.Lox
