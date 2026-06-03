#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Poisson Process Generator
## Author:: Itsuki Noda
## Version:: 0.0 2014/08/28 I.Noda
##
## === History
## * [2014/08/28]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'Stat/RandomValue.rb' ;

##--======================================================================
module Stat
  #--======================================================================
  #++
  ## Poisson Process Generator
  class PoissonProcess < RandomValue
    #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    #++

    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## probability to happen an event. Should be in [0,1]
    attr_accessor :prob ;
    ## the number of occurance of the event
    attr_accessor :nOccur ;
    ## the number of total chanve
    attr_accessor :nChance ;

    #--------------------------------------------------------------
    #++
    ## initialization
    ## _prob_:: probability of occurance
    def initialize(prob)
      @prob = prob ;
      @nOccur = 0 ;
      @nChance = 0 ;
    end

    #--------------------------------------------------------------
    #++
    ## check a chance of occurnace
    ## *return*:: true or false
    def value()
      @nChance += 1 ;
      if(rand() < @prob) then
        @nOccur += 1;
        return true ;
      else
        return false ;
      end
    end

    #--------------------------------------------------------------
    #++
    ## string expression
    ## *return*:: String
    def to_s()
      "\#<#{self.class.name()}:Pr=#{@prob}, Occ=#{@nOccur}/#{@nChance}>" ;
    end


  end # class PoissonProcess
end # module Stat

########################################################################
########################################################################
########################################################################
if($0 == __FILE__) then

  require 'test/unit'
  require 'pp' ;

  #--============================================================
  #++
  ## unit test for this file.
  class TC_Poisson < Test::Unit::TestCase
    #--::::::::::::::::::::::::::::::::::::::::::::::::::
    #++
    ## desc. for TestData
    TestData = nil ;

    #----------------------------------------------------
    #++
    ## show separator and title of the test.
    def setup
      name = "#{(@method_name||@__name__)}(#{self.class.name})" ;
      puts ('*' * 5) + ' ' + [:run, name].inspect + ' ' + ('*' * 5) ;
      super
    end

    #----------------------------------------------------
    #++
    ## check occurance
    def test_a
      pProc = Stat::PoissonProcess.new(0.1) ;
      pp pProc.to_s ;
      p (0...100).map{ pProc.value } ;
      pp pProc.to_s ;
    end

  end # class TC_Foo < Test::Unit::TestCase
end # if($0 == __FILE__)
