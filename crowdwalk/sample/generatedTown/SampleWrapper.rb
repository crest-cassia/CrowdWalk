#! /usr/bin/env ruby
# coding: utf-8
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
  ## 初期化
  ## _simulator_:: java のシミュレータ(EvacuationSimulator)
  def initialize(simulator)
    super(simulator) ;
    @taggedNodeList = [] ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション前処理
  def prepareForSimulation()
    p ['SampleWrapper', :prepareForSimulation]
    width = @simulator.filterFetchFallbackDouble("link",
                                                 "gathering_location_width",
                                                 40.0) ;
    ## 一時避難所道路の道幅変更
    @networkMap.eachLinkWithTag("TEMPORARY_GATHERING_LOCATION_LINK"){|link|
      link.setWidth(width) ;
      p ['link.setWidth', link.getID(), link.getTagString(), link.getWidth()] ;
    }
    ## 中央通りを major 道路に。
    @networkMap.eachLink(){|link|
      tag = link.getNthTag(0) ;
      if(!tag.nil? && tag =~ /link_node_04/ && tag =~ /__node_04/) then
        link.addTag(Term_major) ;
        p ['link.addTag', link.getID(), link.getTagString()] ;
      end
      if(!tag.nil? && tag =~ /04__/ && tag =~ /04$/) then
        link.addTag("major") ;
        p ['link.addTag', link.getID(), link.getTagString()] ;
      end
    }
    rebuildRoutes() ;
    p ['rebuildRoutes()'] ;
  end

  Term_major = ItkTerm.intern("major") ;

  #--------------------------------------------------------------
  #++
  ## シミュレーション後処理
  def finalizeSimulation()
    super
  end

  #--------------------------------------------------------------
  #++
  ## update の先頭で呼び出される。
  ## _simTime_:: シミュレーション内相対時刻
  def preUpdate(simTime)
    p ['SampleWrapper', :preUpdate, simTime] ;
#    @networkMap.eachLink(){|link| p [:link, link]} ;
#    @networkMap.eachNode(){|node| p [:node, node]} ;
    @networkMap.eachNode(){|node|
      if(rand(100) == 0) then
        node.addTag(ItkTerm.intern("foo")) ;
        @taggedNodeList.push(node) ;
      end
    }
    while(@taggedNodeList.size > 10)
      node = @taggedNodeList.shift ;
      node.removeTag(ItkTerm.intern("foo")) ;
    end
  end

  #--------------------------------------------------------------
  #++
  ## update の最後に呼び出される。
  ## _simTime_:: シミュレーション内相対時刻
  def postUpdate(simTime)
    p ['SampleWrapper', :postUpdate, simTime] ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class SampleWrapper

