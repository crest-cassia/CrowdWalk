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

require './GateOperationWrapper.rb'

#--======================================================================
#++
## CrowdWalk の EvacuationSimulator の制御のwrapper
class GateOperationAdaptiveWrapper < GateOperationWrapper
  #--------------------------------------------------------------
  #++
  ## 一番
  def getPhase(relTime)
    if(relTime - @offset < 0) then
      # offset 前なら何もしない。
      @preSwitchTime = relTime ;
      return nil ;
    elsif(relTime - @preSwitchTime < 50)
      # 直前の変更からあまり立っていなければ、何もしない。
      return nil ;
    else
      chosenPhase = nil ;
      chosenCount = nil ;
      @phaseInfo.each{|phase|
        slowAgentCount = 0 ;
        @gateNodeList[phase[:openGateTag]].each{|node|
          node.getLinks().each{|link|
            slowAgentCount += link.countSlowAgentRelative_Forward(0.1) ;
            slowAgentCount += link.countSlowAgentRelative_Backward(0.1) ;
          }
        }
        if(chosenPhase.nil? || chosenCount < slowAgentCount) then
          chosenPhase = phase ;
          chosenCount = slowAgentCount ;
        end
      }
      if(@phase != chosenPhase) then
        @preSwitchTime = relTime
        p [:maxRoute, chosenPhase[:name], chosenCount, relTime] ;
      end
      return chosenPhase ;
    end
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class GateOperationWrapper

