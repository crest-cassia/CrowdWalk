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
    setupGatePhaseInfo() ;
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

    prePhase = @phase ;
    @phase = getPhase(relTime) ;

    if(!@phase.nil?) then
      if(@phase != prePhase) then
        openCloseGate(@phase) ;
        p ['GateOperationWrapper', :phaseChange, @phase[:name], relTime] ;
      end
    end
  end

  #--------------------------------------------------------------
  #++
  ## update の最後に呼び出される。
  ## _relTime_:: シミュレーション内相対時刻
  def postUpdate(simTime)
    # do nothing
  end

  #--------------------------------------------------------------
  #++
  ## phase および gate の情報の設定。
  ## @gateTagList は、gate のタグ（文字列）のリスト。
  ## @gateObjectList は、gateTag => [MapObject...] というマップ
  ## @phaseInfo は、{ :name => <phase の名前>,
  ##                  :duration => <phase の長さ>,
  ##                  :openGateTag => <gate のタグ> } というハッシュのリスト。
  def setupGatePhaseInfo()
    @gateTagList = ["R1_CHECK3", "R2_CHECK3", "R3_CHECK2"] ;
    ## gateTag を持つ MapObject をリストアップ。
    @gateObjectList = {} ;
    @gateTagList.each{|gateTag|
      @gateObjectList[gateTag] = [] ;
      @networkMap.eachLinkWithTag(gateTag){|link|
        @gateObjectList[gateTag].push(link) ;
      }
      @networkMap.eachNodeWithTag(gateTag){|node|
        @gateObjectList[gateTag].push(node) ;
      }
    }
    ## 切り替えタイミング設定
    @offset = 300 ;
    @phaseInfo = ([
                   { :name => :route1,
                     :duration => 120,
                     :openGateTag => @gateTagList[0],
                   },
                   { :name => :route2,
                     :duration => 100,
                     :openGateTag => @gateTagList[1],
                   },
                   { :name => :route3,
                     :duration => 110,
                     :openGateTag => @gateTagList[2],
                   },
                  ]) ;
    @cycleLen = 0 ;
    @phaseInfo.each{|phase|
      @cycleLen += phase[:duration] ;
    }
    @phase = nil ;
  end

  #--------------------------------------------------------------
  #++
  ## 相対時刻から phase を取得
  def getPhase(relTime)
    if(relTime - @offset < 0) then
      return nil ;
    else
      remain = ((relTime - @offset) % @cycleLen) ;
      @phaseInfo.each{|phase|
        remain -= phase[:duration] ;
        if(remain < 0)
          return phase ;
        end
      }
      return @phaseInfo.last ;
    end
  end

  #--------------------------------------------------------------
  #++
  ## open close gate
  ## _phase_:: phase の情報。
  def openCloseGate(phase)
    @gateObjectList.each{|tag, objList|
      if(tag == phase[:openGateTag]) then
        objList.each{|obj| obj.openGate(tag)} ;
      else
        objList.each{|obj| obj.closeGate(tag)} ;
      end
    }
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class GateOperationWrapper

