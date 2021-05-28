JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

CLASSES = $(wildcard de/kenran/jlox/*.java)

.java.class:
	$(JC) $(JFLAGS) $*.java

default: classes

classes: $(CLASSES:.java=.class)

run: classes
	@java de.kenran.jlox.Lox
