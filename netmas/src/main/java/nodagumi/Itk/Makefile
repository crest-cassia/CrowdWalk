TOP : build

## common tricks
## 文字列中の comma を操作したい場合、$(,) と参照できる。
,:=,
## 文字列中の space を操作したい場合、$(_) または $( ) と参照できる。
_ := 
_ += 
$(_) :=
$(_) +=

## defs
PACKAGE = nodagumi.Itk
CLASSES = ClassFinder Itk
SRCFILES = $(foreach klass,$(CLASSES),$(klass).java)
ALLTESTS = $(foreach klass,$(CLASSES),$(PACKAGE).$(klass)_Test)

BASEDIR = ../../../../..
JARDIR = $(BASEDIR)/libs/jar
CLASSDIR = $(BASEDIR)/build/classes/main
#CLASSDIR = $(BASEDIR)/build/libs/netmas.jar
JARLIST = junit-4.12 hamcrest-core-1.3 jsonic-1.2.11
JARS = $(subst $( ),:,$(foreach jar,$(JARLIST),$(JARDIR)/$(jar).jar))
CLASSPATH = $(CLASSDIR):$(JARS)
JUNITCLASS = org.junit.runner.JUnitCore # for JUnit v.4

##------------------------------------------------------------
doc ::
	javadoc -d doc -encoding utf-8 -charset utf-8 $(SRCFILES)

build :
	( cd $(BASEDIR) ; gradle )

##------------------------------------------------------------
#test : test_ClassFinder
test : test_Itk

test_ClassFinder ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).ClassFinder_Test

test_Itk ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).Itk_Test


test_all ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(ALLTESTS)


##------------------------------------------------------------
SAMPLE0 = sample/simpleGrid/properties-lane.xml
SAMPLE1 = sample/simpleGrid/properties-lane.v2.xml

run : run1

run_base ::
	(cd $(BASEDIR) ; bash ./quickstart.sh)

run0 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0))

run1 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE1))

run1_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh $(SAMPLE1) cui)

##------------------------------------------------------------
FOO = a b c
foo ::
	echo $(JARS)
	echo $(subst $( ),:,$(FOO))
	echo "$( )"
