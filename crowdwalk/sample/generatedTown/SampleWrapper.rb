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
  #--------------------------------------------------------------
  #++
  ## update の先頭で呼び出される。
  ## _relTime_:: シミュレーション内相対時刻
  def preUpdate(relTime)
    p ['SampleWrapper', :preUpdate, relTime] ;
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

