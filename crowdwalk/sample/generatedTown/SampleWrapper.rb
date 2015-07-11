#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = CrowdWalk の EvacuationSimulator の wrapper のサンプル
## Author:: Itsuki Noda
## Version:: 0.0 2015/06/28 I.Noda
##
## === History
## * [2015/06/28]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

#--======================================================================
#++
## CrowdWalk の EvacuationSimulator の制御のwrapper
class SampleWrapper < CrowdWalkWrapper
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  attr_accessor :taggedNodeList ;
  #--------------------------------------------------------------
  #++
  ## update の先頭で呼び出される。
  ## _relTime_:: シミュレーション内相対時刻
  def initialize(mapObject)
    super(mapObject) ;
    @taggedNodeList = [] ;
  end

  #--------------------------------------------------------------
  #++
  ## update の先頭で呼び出される。
  ## _relTime_:: シミュレーション内相対時刻
  def preUpdate(relTime)
    p ['SampleWrapper', :preUpdate, relTime] ;
#    @networkMap.eachLink(){|link| p [:link, link]} ;
#    @networkMap.eachNode(){|node| p [:node, node]} ;
    @networkMap.eachNode(){|node|
      if(rand(100) == 0) then
        node.addTag("EXIT") ;
        @taggedNodeList.push(node) ;
      end
    }
    while(@taggedNodeList.size > 10)
      node = @taggedNodeList.shift ;
      node.removeTag("EXIT") ;
    end
  end

  #--------------------------------------------------------------
  #++
  ## update の最後に呼び出される。
  ## _relTime_:: シミュレーション内相対時刻
  def postUpdate(relTime)
    p ['SampleWrapper', :postUpdate, relTime] ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class SampleWrapper

