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
  def initialize(factory, config, fallback)
    super
    @c = 0 ;
    @time0 = getSimTime("01:23:45") ;
  end
  
  #--------------------------------------------------------------
  #++
  ## 
  def initCycle()
    @beginTime = getCurrentTime() ;
    @fromTag = makeSymbolTerm("major") ;
    @fromList = getLinkTableByTag(@fromTag) ;
    @toTag = makeSymbolTerm("node_09_06") ;
    @toList = getNodeTableByTag(@toTag) ;
    @agentList = [] ;
    @c = 0 ;
  end
  
  #--------------------------------------------------------------
  #++
  ## 
  def cycle()
    @currentTime = getCurrentTime() ;
    pp [:diff, timeDiffInSec(@currentTime, @beginTime) ] ;
    disable() if(@c >= 10) ;

    finishAllP = true ;
    @agentList.each{|agent|
      finishAllP = false if(isAgentWalking(agent)) ;
    }
    return if(!finishAllP) ;

    @agentList = [] ;
    @fromList.each{|origin|
      agent = launchAgentWithRoute("RationalAgent", origin, @toTag, []) ;
      @agentList.push(agent) ;
    }
    @c += 1 ;

    pp [:c, @c] ;
    ##pp [:time0, @time0.to_s] ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class SampleWrapper

