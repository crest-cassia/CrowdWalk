TOP : build

PACKAGE = nodagumi.Itk
CLASSES = ClassFinder Itk
SRCFILES = $(foreach klass,$(CLASSES),$(klass).java)
ALLTESTS = $(foreach klass,$(CLASSES),$(PACKAGE).$(klass)_Test)

BASEDIR = ../../../../..
JARDIR = $(BASEDIR)/libs/jar
#CLASSDIR = $(BASEDIR)/build/classes/main/nodagumi/Itk/.
CLASSDIR = $(BASEDIR)/build/libs/netmas.jar
JARS = $(JARDIR)/junit-4.12.jar:$(JARDIR)/hamcrest-core-1.3.jar:$(JARDIR)/jsonic-1.2.11.jar
CLASSPATH = $(CLASSDIR):$(JARS)
JUNITCLASS = org.junit.runner.JUnitCore # for JUnit v.4

doc ::
	javadoc -d doc -encoding utf-8 -charset utf-8 $(SRCFILES)

build :
	( cd $(BASEDIR) ; gradle )

#test : test_ClassFinder
test : test_Itk

test_ClassFinder ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).ClassFinder_Test

test_Itk ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).Itk_Test


test_all ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(ALLTESTS)

foo ::
	echo $(ALLTESTS)
