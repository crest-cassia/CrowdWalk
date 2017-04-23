#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = CrowdWalk の EvacuationSimulator の AgentFactoryByRuby のサンプル
## Author:: Itsuki Noda
## Version:: 0.0 2017/04/24 I.Noda
##
## === History
## * [2017/04/24]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

#--======================================================================
#++
## AgentFactoryByRubyの制御インターフェース
class SampleFactory < AgentFactoryBase
  
  #--------------------------------------------------------------
  #++
  ## 
  def tryUpdateAndGenerate()
    pp [:javaFactory, @javaFactory] ;
    pp [:config, @config] ;
    pp [:fallback, @fallback] ;
    ## return nil
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class SampleWrapper

