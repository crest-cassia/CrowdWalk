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
class GateOperationWrapper < CrowdWalkWrapper
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  attr_accessor :taggedNodeList ;
  #--------------------------------------------------------------
  #++
  ## 初期化
  ## _simulator_:: java のシミュレータ(EvacuationSimulator)
  def initialize(simulator)
    super(simulator) ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション前処理
  def prepareForSimulation()
    @offset = 300 ;
    @phaseLen0 = 120 ;
    @phaseLen1 = 100 ;
    @phaseLen2 = 110 ;
    @cycleLen = @phaseLen0 + @phaseLen1 + @phaseLen2 ;
    @phase = nil ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション後処理
  def finalizeSimulation()
    # do nothing
  end

  #--------------------------------------------------------------
  #++
  ## update の先頭で呼び出される。
  ## _simTime_:: シミュレーション内相対時刻
  def preUpdate(simTime)
    relTime = simTime.getRelativeTime() ;

    return if(relTime - @offset < 0) ;

    prePhase = @phase ;

    remain = (relTime - @offset) % @cycleLen ;
    if((remain -= @phaseLen0) < 0) then
      # open route 1
      if(@phase != :route1) then
        @phase = :route1 ;
        @networkMap.eachLink() {|link|
          openCloseGate(link, "R1_CHECK3", ["R2_CHECK3", "R3_CHECK2"]) ;
        }
        @networkMap.eachNode() {|node|
          openCloseGate(node, "R1_CHECK3", ["R2_CHECK3", "R3_CHECK2"]) ;
        }
      end
    elsif((remain -= @phaseLen1) < 0) then
      # open route 2
      if(@phase != :route2) then
        @phase = :route2 ;
        @networkMap.eachLink() {|link|
          openCloseGate(link, "R2_CHECK3", ["R3_CHECK2", "R1_CHECK3"]) ;
        }
        @networkMap.eachNode() {|node|
          openCloseGate(node, "R2_CHECK3", ["R3_CHECK2", "R1_CHECK3"]) ;
        }
      end
    else
      # open route 3
      if(@phase != :route3) then
        @phase = :route3 ;
        @networkMap.eachLink() {|link|
          openCloseGate(link, "R3_CHECK2", ["R1_CHECK3", "R2_CHECK3"]) ;
        }
        @networkMap.eachNode() {|node|
          openCloseGate(node, "R3_CHECK2", ["R1_CHECK3", "R2_CHECK3"]) ;
        }
      end
    end

    if(@phase != prePhase) then
      p ['GateOperationWrapper', :phaseChange, @phase, relTime] ;
    end
  end

  #--------------------------------------------------------------
  #++
  ## open close gate
  ## _object_:: 対象となる link, node
  ## _openGate_:: 開けるゲート
  ## _closeGateList_:: 閉めるゲートリスト
  def openCloseGate(object, openGate, closeGateList)
    object.openGate(openGate) if(object.hasTag(openGate)) ;
    closeGateList.each{|closeGate|
      object.closeGate(closeGate) if(object.hasTag(closeGate)) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## update の最後に呼び出される。
  ## _relTime_:: シミュレーション内相対時刻
  def postUpdate(simTime)
    # do nothing
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class GateOperationWrapper

