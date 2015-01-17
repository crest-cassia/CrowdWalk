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
SAMPLE0 = sample/simpleGrid/properties-lane.xml		#simple
SAMPLE0d = sample/simpleGrid/properties-density.xml	#density model
SAMPLE0w = sample/basic-sample/properties.xml		#wait
SAMPLE0s2 = sample/2links/properties-width-100.xml	#stop (2link)
SAMPLE0s4 = sample/4links/properties.xml		#stop (4link)
SAMPLE0sm = sample/stop-sample/properties_24.json	#stop (門司港)
SAMPLE0sg2 = sample/simpleGrid2/properties.xml		#kawaguchi's sample
SAMPLE0sg2s = sample/simpleGrid2/properties-strait.xml	#kawaguchi's sample
SAMPLE1 = sample/simpleGrid/properties-lane.v1.xml	#simple, version1
SAMPLE2 = sample/simpleGrid/properties-lane.v2.xml	#simple, version2
SAMPLE2d = sample/simpleGrid/properties-density.v2.xml	#simple, version2
SAMPLE2s = sample/simpleGrid/properties-strait.v2.xml	#simple, version2
SAMPLE2w = sample/basic-sample/properties.v2.xml	#wait, version2
SAMPLE2i = sample/simpleGrid/properties-itk.v2.xml	#itk, version2
SAMPLE2id = sample/simpleGrid/properties-itk-density.v2.xml	#itk, version2
SAMPLE2is = sample/simpleGrid/properties-itk-strait.v2.xml	#itk, version2

run : run2

run_base ::
	(cd $(BASEDIR) ; bash ./quickstart.sh)

run0 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0))

run0d ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0d))

run_wait :: run0w
run0w ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0w))

run0s2 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0s2))

run0s4 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0s4))
run0sm ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0sm))
run0sg2 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0sg2))
run0sg2s ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0sg2s))

run1 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE1))

run1_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh $(SAMPLE1) cui)

run2 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2))
run2_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh $(SAMPLE2) cui)
run2d ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2d))
run2d_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh $(SAMPLE2d) cui)
run2s ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2s))
run2s_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh $(SAMPLE2s) cui)
run2w ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2w))
run2i ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2i))
run2id ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2id))
run2is ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2is))


##------------------------------------------------------------
FOO = a b c
foo ::
	echo $(ALLTESTS)
