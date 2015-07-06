#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = CrowdWalk の EvacuationSimulator の wrapper
## Author:: Itsuki Noda
## Version:: 0.0 2015/06/27 I.Noda
##
## === History
## * [2015/06/27]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

#--======================================================================
#++
## CrowdWalk の EvacuationSimulator の制御のwrapper
class CrowdWalkWrapper
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の EvacuationSimulator
  attr_accessor :simulator ;

  #--------------------------------------------------------------
  #++
  ## 初期化
  ## _simulator_:: Java の EvacuationSimulator のインスタンス。
  def initialize(simulator)
    @simulator = simulator ;
  end

  #--------------------------------------------------------------
  #++
  ## update の先頭で呼び出される。
  ## _relTime_:: シミュレーション内相対時刻
  def preUpdate(relTime)
    p [:preUpdate, relTime, :notImplemented] ;
  end

  #--------------------------------------------------------------
  #++
  ## update の最後に呼び出される。
  ## _relTime_:: シミュレーション内相対時刻
  def postUpdate(relTime)
    p [:postUpdate, relTime, :notImplemented] ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class CrowdWalkWrapper

