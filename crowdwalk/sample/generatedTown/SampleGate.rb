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

require 'RubyGateBase.rb' ;

#--======================================================================
#++
## RubyGate の制御インターフェース
class SampleGate < RubyGateBase
  
  #--------------------------------------------------------------
  #++
  ## 初期化。
  def initialize(_gate)
    super
  end
  
  #--------------------------------------------------------------
  #++
  ## 通過チェック。
  ## 通過させないなら true を返す。
  def isClosed(agent, currentTime)
    p [:isClosed, agent.getID(), currentTime.toString()] ;

    ## close のときも、半分通す。
    r = super ;
    if(r) then
      return (rand(2) == 0) ;
    else
      return false ;
    end
  end

  #--------------------------------------------------------------
  #++
  ## 状態変化。
  def switchGate(event, closed)
    p [:switchGate, event.getEventDef().toJson(), closed] ;
    p [:getEventDef, getEventDef().toJson()] ;
    p [:place, getGateTag(), getPlace().getID()] ;
    super
  end
  
  
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class SampleWrapper

