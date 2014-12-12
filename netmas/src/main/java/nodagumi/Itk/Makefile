TOP : build

SRCFILES = ClassFinder.java
BASEDIR = ../../../../..
JARDIR = $(BASEDIR)/libs/jar
#CLASSDIR = $(BASEDIR)/build/classes/main/nodagumi/Itk/.
CLASSDIR = $(BASEDIR)/build/libs/netmas.jar
CLASSPATH = $(CLASSDIR):$(JARDIR)/junit-4.12.jar:$(JARDIR)/hamcrest-core-1.3.jar

doc ::
	javadoc -d doc -encoding utf-8 -charset utf-8 $(SRCFILES)

build :
	( cd $(BASEDIR) ; gradle )

test ::
	java -cp $(CLASSPATH) org.junit.runner.JUnitCore nodagumi.Itk.ClassFinderTest
