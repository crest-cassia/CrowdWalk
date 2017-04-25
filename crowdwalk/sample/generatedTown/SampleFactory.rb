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
    @fromTag = makeSymbolTerm("major") ;
    @fromList = getLinkTableByTag(@fromTag) ;
    @toTag = makeSymbolTerm("node_09_06") ;
    @toList = getNodeTableByTag(@toTag) ;
  end
  
  #--------------------------------------------------------------
  #++
  ## 
  def cycle()
    @c += 1 ;
    disable() if (@c > 10) ;

    @fromList.each{|origin|
      launchAgentWithRoute("RationalAgent", origin, @toTag, []) ;
    }

    pp [:c, @c] ;
    pp [:time0, @time0.to_s] ;
    pp [:links, @linkList.to_s] ;
    pp [:nodes, @nodeList.to_s] ;
    ## return nil
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class SampleWrapper

