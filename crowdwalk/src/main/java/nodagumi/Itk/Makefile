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
CWJAR = $(BASEDIR)/build/libs/crowdwalk.jar
#JARLIST = junit-4.12 hamcrest-core-1.3 jsonic-1.3.7 jruby
JARLIST = hamcrest-core-1.3 jsonic-1.3.7 jruby
JARS = $(subst $( ),:,$(foreach jar,$(JARLIST),$(JARDIR)/$(jar).jar))
CLASSPATH = $(CLASSDIR):$(JARS)
JUNITCLASS = org.junit.runner.JUnitCore # for JUnit v.4
#JUNITCLASSPATH = $(CLASSPATH):/usr/share/java/junit4.jar
JUNITCLASSPATH = $(CLASSPATH):/usr/share/java/junit4.jar:$(CWJAR)

## [2021.11.16 I.Noda] to use java-11 when build.
JAVA_HOME_FOR_GRADLEW = /usr/lib/jvm/java-11-openjdk-amd64

##------------------------------------------------------------
doc :: rdoc
	rm -f $(BASEDIR)/doc/javadoc/nodagumi/ananPJ/package-summary.html
	( cd $(BASEDIR) ; ./make_javadoc.sh )
#	( cd $(BASEDIR) ; gradle --offline javadoc  )

doc-old ::
	(cd ../.. ;\
	javadoc -d ../../../doc/javadoc -encoding utf-8 \
	-charset utf-8 -subpackages nodagumi)
#	-charset utf-8 -private -subpackages nodagumi)
#	javadoc -d doc -encoding utf-8 -charset utf-8 $(SRCFILES)

RUBYDIR = ../../../ruby
RUBYDOC = $(BASEDIR)/doc/rdoc
RUBYDOCABS = $(abspath $(RUBYDOC))

rdoc ::
	rm -rf $(RUBYDOC)
	(cd $(RUBYDIR) ; rdoc --all --main docRubyForCrowdWalk.rb --hyperlink-all --force-update --line-numbers --output $(RUBYDOCABS) *.rb) 

clean ::
	( cd $(BASEDIR) ; JAVA_HOME=$(JAVA_HOME_FOR_GRADLEW) ./gradlew clean )
#	( cd $(BASEDIR) ; gradle clean )

build : build_v

## quiet
build_q :
	( cd $(BASEDIR) ; JAVA_HOME=$(JAVA_HOME_FOR_GRADLEW) ./gradlew -q )
#	( cd $(BASEDIR) ; ./gradlew -q )
#	( cd $(BASEDIR) ; gradle -q )

## off-line (do not check java lib version via internet)
build_ol :
	( cd $(BASEDIR) ; gradle -q --offline)

## verbose
build_v :
	( cd $(BASEDIR) ; gradle)

build_vv :
	( cd $(BASEDIR) ; gradle --info -Xdiag:verbose)

##------------------------------------------------------------
#test : test_ClassFinder
test : test_Itk

test_ClassFinder ::
	java -cp $(JUNITCLASSPATH) $(JUNITCLASS) $(PACKAGE).ClassFinder_Test

test_Itk ::
	java -cp $(JUNITCLASSPATH) $(JUNITCLASS) $(PACKAGE).Itk_Test

test_Lexicon ::
	java -cp $(JUNITCLASSPATH) $(JUNITCLASS) $(PACKAGE).Lexicon_Test

test_Term ::
	java -cp $(JUNITCLASSPATH) $(JUNITCLASS) $(PACKAGE).Term_Test

test_CsvFormatter ::
	java -cp $(JUNITCLASSPATH) $(JUNITCLASS) $(PACKAGE).CsvFormatter_Test

test_JsonFormatter ::
	java -cp $(JUNITCLASSPATH) $(JUNITCLASS) $(PACKAGE).JsonFormatter_Test

test_ItkRuby ::
	java -cp $(JUNITCLASSPATH) $(JUNITCLASS) $(PACKAGE).ItkRuby_Test

test_RingBuffer ::
	java -cp $(JUNITCLASSPATH) $(JUNITCLASS) $(PACKAGE).RingBuffer_Test

test_all ::
	java -cp $(JUNITCLASSPATH) $(JUNITCLASS) $(ALLTESTS)


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
##SAMPLE0s2 = sample/2links/properties-width-100.xml	#stop (2link)
SAMPLE0s2 = sample/2links/properties-width-1.xml	#stop (2link)
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
SAMPLE0sg2pn = sample/simpleGrid2/properties-plain-node.xml #kawaguchi's sample
run0sg2pn ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE0sg2pn))
run0sg2pn_cui ::
	(cd $(BASEDIR) ; bash quickstart.sh --cui $(SAMPLE0sg2pn))

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
	(cd $(BASEDIR) ; bash quickstart.sh -l Info --use-2d-simulator $(SAMPLE3gt00))
run3gt00_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh -l Info --cui $(SAMPLE3gt00))

##-----
SAMPLE3gt00a = sample/generatedTown/gridTown00.array.prop.json   # array formula
run3gt00a ::
	(cd $(BASEDIR) ; bash quickstart.sh -l Info $(SAMPLE3gt00a))
#	(cd $(BASEDIR) ; bash quickstart.sh -l Warn $(SAMPLE3gt00a))


run3gt00a_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh -l Info --cui $(SAMPLE3gt00a))

##-----
SAMPLE3gt00a2 = sample/generatedTown/gridTown00.array2.prop.json   # change mentalMode
run3gt00a2 ::
	(cd $(BASEDIR) ; bash quickstart.sh -l Info $(SAMPLE3gt00a))

##-----
SAMPLE3gt00i = sample/generatedTown/gridTown00.indivConf.prop.json   # array formula individual
run3gt00i ::
	(cd $(BASEDIR) ; bash quickstart.sh -l Info $(SAMPLE3gt00i))
#	(cd $(BASEDIR) ; bash quickstart.sh -l Warn $(SAMPLE3gt00i))


run3gt00a_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh -l Info --cui $(SAMPLE3gt00a))

##-----
SAMPLE3gt00r = sample/generatedTown/gridTown00.ruby.prop.json   # ruby agent
run3gt00r ::
	(cd $(BASEDIR) ; bash quickstart.sh -l Info --use-2d-simulator $(SAMPLE3gt00r))
run3gt00r_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE3gt00r))
run3gt00r_fb ::
	(cd $(BASEDIR) ; bash quickstart.sh --fallback '{"aho":1.0}' --fallback '{"link":{"gathering_location_width":12.345}}' $(SAMPLE3gt00r))

##-----
SAMPLE3gt00ri = sample/generatedTown/gridTown00.rubyIrb.prop.json   # irb
run3gt00ri ::
	(cd $(BASEDIR) ; bash quickstart.sh -l Info --use-2d-simulator $(SAMPLE3gt00ri))
run3gt00ri_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE3gt00ri))

##-----
SAMPLE3gt00r2 = sample/generatedTown/gridTown00.ruby2.prop.json   # ruby agent
run3gt00r2 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3gt00r2))

##-----
SAMPLE3gt02 = sample/generatedTown/gridTown02.prop.json   # ruby factory
run3gt02 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3gt02))
run3gt02_cui ::
	(cd $(BASEDIR) ; bash quickstart.sh --cui $(SAMPLE3gt02))

##-----
SAMPLE3gt02i = sample/generatedTown/gridTown02i.prop.json   # individual
run3gt02i ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3gt02i))

##-----
SAMPLE3gt02d = sample/generatedTown/gridTown02d.prop.json   # dump event
run3gt02d ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3gt02d))

##-----
SAMPLE3gt02ds = sample/generatedTown/gridTown02ds.prop.json   # dump event (restart)
run3gt02ds ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3gt02ds))

##-----
SAMPLE3gt02rr = sample/generatedTown/gridTown02rr.prop.json   # ruby factory/agent
run3gt02rr ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3gt02rr))

##-----
SAMPLE3gt03 = sample/generatedTown/gridTown03.prop.json   # ruby gate
run3gt03 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3gt03))

##-----
SAMPLE3kw01 = sample/kawaguchi-S2B/property-S2B.xml # kawaguchi
run3kw01 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3kw01))
run3kw01_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE3kw01))

##-----
SAMPLE3mj00 = sample/stop-sample/properties_24-ruby.json # moji, ruby
run3mj00 ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3mj00))
run3mj00_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE3mj00))

##-----
SAMPLE3mj00a = sample/stop-sample/properties_24-ruby-adaptive.json # moji, ruby
run3mj00a ::
	(cd $(BASEDIR) ; bash quickstart.sh $(SAMPLE3mj00a))
run3mj00a_cui ::
	(cd $(BASEDIR) ; time bash quickstart.sh --cui $(SAMPLE3mj00a))

##-----
SAMPLE3bench00naive = sample/generatedTown/gridTown00_bench.naive.prop.json   # bench, naive
run3bc00n ::
	(cd $(BASEDIR) ; time -p bash quickstart.sh --cui --log-level Error $(SAMPLE3bench00naive))

##-----
SAMPLE3bench00rational = sample/generatedTown/gridTown00_bench.rational.prop.json   # bench, rational
run3bc00ra ::
	(cd $(BASEDIR) ; time -p bash quickstart.sh --cui --log-level Error $(SAMPLE3bench00rational))
##-----
SAMPLE3bench00rational2 = sample/generatedTown/gridTown00_bench.rational2.prop.json   # bench, rational
run3bc00ra2 ::
	(cd $(BASEDIR) ; time -p bash quickstart.sh --cui --log-level Error $(SAMPLE3bench00rational2))

##-----
SAMPLE3bench00rubyWrapper = sample/generatedTown/gridTown00_bench.ruby.wr.prop.json   # bench, ruby wrapper
run3bc00rbw ::
	(cd $(BASEDIR) ; time -p bash quickstart.sh --cui --log-level Error $(SAMPLE3bench00rubyWrapper))

##-----
SAMPLE3bench00rubyAgent = sample/generatedTown/gridTown00_bench.ruby.ag.prop.json   # bench, ruby agent
run3bc00rba ::
	(cd $(BASEDIR) ; time -p bash quickstart.sh --cui --log-level Error $(SAMPLE3bench00rubyAgent))

##-----
SAMPLE3crs4x = sample/crossing/crossing4x.prop.json   # crossing. cross.
run3crs4x ::
	(cd $(BASEDIR) ; time -p bash quickstart.sh --log-level Info $(SAMPLE3crs4x))

##-----
SAMPLE3crs4p = sample/crossing/crossing4p.prop.json   # crossing. parallel
run3crs4p ::
	(cd $(BASEDIR) ; time -p bash quickstart.sh --log-level Info $(SAMPLE3crs4p))

##-----
SAMPLE4gnz0 = sample/ginza/ginza00.prop.json   # Ginza
run4gnz0 ::
	(cd $(BASEDIR) ; cd `dirname $(SAMPLE4gnz0)` ; time -p bash ../../quickstart.sh --log-level Info `basename $(SAMPLE4gnz0)`)

##-----
SAMPLE4gnz0d = sample/ginza/ginza00d.prop.json   # Ginza
run4gnz0d ::
	(cd $(BASEDIR) ; cd `dirname $(SAMPLE4gnz0d)` ; time -p bash ../../quickstart.sh --log-level Info --cui `basename $(SAMPLE4gnz0d)`)

##-----
SAMPLE5loop0 = sample/generatedTown/gridTown01s.prop.json   # stay loop
run5loop0 ::
	(cd $(BASEDIR) ; cd `dirname $(SAMPLE5loop0)` ; time -p bash ../../quickstart.sh --log-level Info `basename $(SAMPLE5loop0)`)

##-----
SAMPLE_G4 = sample/generatedTown/gridTown04.prop.json   # directed restricted speed rules
runG4 ::
	(cd $(BASEDIR) ; cd `dirname $(SAMPLE_G4)` ; time -p bash ../../quickstart.sh --log-level Info `basename $(SAMPLE_G4)`)

##-----
SAMPLE_G5 = sample/generatedTown/gridTown05.prop.json   # ruby color model
runG5 ::
	(cd $(BASEDIR) ; cd `dirname $(SAMPLE_G5)` ; time -p bash ../../quickstart.sh --log-level Info `basename $(SAMPLE_G5)`)



##------------------------------------------------------------
FOO = a b c
foo ::
	echo $(ALLTESTS)
