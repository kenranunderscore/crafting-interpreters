JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

CLASSES = $(wildcard de/kenran/jlox/*.java)

.java.class:
	$(JC) $(JFLAGS) $*.java

default: classes

classes: $(CLASSES:.java=.class)

run:
	@java de.kenran.jlox.Main
