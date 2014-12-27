TOP : build_q

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
CLASSES = ClassFinder Itk Lexicon
SRCFILES = $(addsuffix .java,$(CLASSES))
##SRCFILES = $(foreach klass,$(CLASSES),$(klass).java)
ALLTESTSSRC = $(shell ls *_Test.java)
ALLTESTS = $(foreach klass,$(ALLTESTSSRC),$(PACKAGE).$(basename $(klass)))

BASEDIR = ../../../../..
JARDIR = $(BASEDIR)/libs/jar
CLASSDIR = $(BASEDIR)/build/classes/main
#CLASSDIR = $(BASEDIR)/build/libs/netmas.jar
JARLIST = junit-4.12 hamcrest-core-1.3 jsonic-1.3.7
JARS = $(subst $( ),:,$(foreach jar,$(JARLIST),$(JARDIR)/$(jar).jar))
CLASSPATH = $(CLASSDIR):$(JARS)
JUNITCLASS = org.junit.runner.JUnitCore # for JUnit v.4

##------------------------------------------------------------
doc ::
	javadoc -d doc -encoding utf-8 -charset utf-8 $(SRCFILES)

build : build_v

## quiet
build_q :
	( cd $(BASEDIR) ; gradle -q )

## verbose
build_v :
	( cd $(BASEDIR) ; gradle )

##------------------------------------------------------------
#test : test_ClassFinder
test : test_Itk

test_ClassFinder ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).ClassFinder_Test

test_Itk ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).Itk_Test

test_Lexicon ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).Lexicon_Test

test_Term ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).Term_Test


test_all ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(ALLTESTS)


##------------------------------------------------------------
SAMPLE0 = sample/simpleGrid/properties-lane.xml
SAMPLE0d = sample/simpleGrid/properties-density.xml
SAMPLE0b = sample/basic-sample/properties.xml
SAMPLE1 = sample/simpleGrid/properties-lane.v1.xml
SAMPLE2 = sample/simpleGrid/properties-lane.v2.xml


run : run2

run_base ::
	(cd $(BASEDIR) ; bash ./quickstart.sh)

run0 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0))

run0d ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0d))

run0b ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2))

run1 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE1))

run1_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh $(SAMPLE1) cui)

run2 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2))


##------------------------------------------------------------
FOO = a b c
foo ::
	echo $(ALLTESTS)
