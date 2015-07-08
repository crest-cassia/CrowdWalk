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
JARLIST = junit-4.12 hamcrest-core-1.3 jsonic-1.3.7 jruby
JARS = $(subst $( ),:,$(foreach jar,$(JARLIST),$(JARDIR)/$(jar).jar))
CLASSPATH = $(CLASSDIR):$(JARS)
JUNITCLASS = org.junit.runner.JUnitCore # for JUnit v.4

##------------------------------------------------------------
doc ::
	( cd $(BASEDIR) ; gradle javadoc  )

doc-old ::
	(cd ../.. ;\
	javadoc -d ../../../doc/javadoc -encoding utf-8 \
	-charset utf-8 -subpackages nodagumi)
#	-charset utf-8 -private -subpackages nodagumi)
#	javadoc -d doc -encoding utf-8 -charset utf-8 $(SRCFILES)

build : build_v

## quiet
build_q :
	( cd $(BASEDIR) ; gradle -q )

## verbose
build_v :
	( cd $(BASEDIR) ; gradle)

build_vv :
	( cd $(BASEDIR) ; gradle --info -Xdiag:verbose)

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

test_CsvFormatter ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).CsvFormatter_Test

test_ItkRuby ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(PACKAGE).ItkRuby_Test


test_all ::
	java -cp $(CLASSPATH) $(JUNITCLASS) $(ALLTESTS)


##------------------------------------------------------------
pull ::
	(cd ../../../../../.. ; gitPullAll -c -r)

##------------------------------------------------------------
list ::
	awk '/^(SAMPLE|run)/ {print}' ./Makefile

##------------------------------------------------------------
run : run2

run_base ::
	(cd $(BASEDIR) ; bash ./quickstart.sh)

##-----
SAMPLE0 = sample/simpleGrid/properties-lane.xml		#simple
run0 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0))

##-----
SAMPLE0d = sample/simpleGrid/properties-density.xml	#density model
run0d ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0d))

##-----
SAMPLE0w = sample/basic-sample/properties.xml		#wait
run_wait :: run0w
run0w ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0w))

##-----
SAMPLE0s2 = sample/2links/properties-width-100.xml	#stop (2link)
run0s2 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0s2))

##-----
SAMPLE0s4 = sample/4links/properties.xml		#stop (4link, gas)
run0s4 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0s4))

##-----
SAMPLE0s4t = sample/4links/properties-tagPol.xml	#stop (4link, gas, tag指定)
run0s4t ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0s4t))

##-----
SAMPLE0sm = sample/stop-sample/properties_24.json	#stop (門司港)
run0sm ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0sm))

##-----
SAMPLE0smp = sample/stop-sample/properties_24-plain.json #stop (門司港)
run0smp ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0smp))
run0smp_cui ::
	(cd $(BASEDIR) ; bash quickstart.sh --cui $(SAMPLE0smp))

##-----
SAMPLE0sg2 = sample/simpleGrid2/properties.xml		#kawaguchi's sample
run0sg2 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0sg2))
run0sg2_cui ::
	(cd $(BASEDIR) ; bash quickstart.sh --cui $(SAMPLE0sg2))

##-----
SAMPLE0sg2p = sample/simpleGrid2/properties-plain.xml	#kawaguchi's sample
run0sg2p ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0sg2p))
run0sg2p_cui ::
	(cd $(BASEDIR) ; bash quickstart.sh --cui $(SAMPLE0sg2p))

##-----
SAMPLE1 = sample/simpleGrid/properties-lane.v1.xml	#simple, version1
run1 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE1))
run1_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE1))

##-----
SAMPLE2 = sample/simpleGrid/properties-lane.v2.xml	#simple, version2,fallback
run2 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2))
run2_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE2))

##-----
SAMPLE2d = sample/simpleGrid/properties-density.v2.xml	#simple, version2
run2d ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2d))
run2d_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE2d))

##-----
SAMPLE2p = sample/simpleGrid/properties-plain.v2.xml	#simple, version2
run2p ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2p))
run2p_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE2p))

##-----
SAMPLE2w = sample/basic-sample/properties.v2.xml	#wait, version2
run2w ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2w))

##-----
SAMPLE2i = sample/simpleGrid/properties-itk.v2.xml	#itk, version2
run2i ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2i))

##-----
SAMPLE2id = sample/simpleGrid/properties-itk-density.v2.xml	#itk, version2
run2id ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2id))

##-----
SAMPLE2ip = sample/simpleGrid/properties-itk-plain.v2.xml	#itk, version2
run2ip ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2ip))
run2ip_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE2ip))

##-----
SAMPLE2it = sample/simpleGrid/properties-itk-think.v2.xml	#think
run2it ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE2it))
run2it_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE2it))

##-----
SAMPLE3gt00 = sample/generatedTown/gridTown00.prop.json   # fallback
run3gt00 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3gt00))
run3gt00_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE3gt00))

##-----
SAMPLE3gt00r = sample/generatedTown/gridTown00.ruby.prop.json   # ruby agent
run3gt00r ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3gt00r))
run3gt00r_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE3gt00r))


##------------------------------------------------------------
FOO = a b c
foo ::
	echo $(ALLTESTS)
