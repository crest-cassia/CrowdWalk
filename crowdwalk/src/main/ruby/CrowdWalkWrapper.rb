#! /usr/bin/env ruby
# coding: utf-8
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

require 'ItkTerm.rb' ;
require 'NetworkMap.rb' ;

#--======================================================================
#++
## CrowdWalk の EvacuationSimulator の制御のwrapper
class CrowdWalkWrapper
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の EvacuationSimulator
  attr_accessor :simulator ;

  ## NetworkMap
  attr_accessor :networkMap ;

  ## NetworkMap
  attr_accessor :networkMap ;

  ## Fallback Parameters ;
  attr_accessor :fallbackParameters ;

  #--------------------------------------------------------------
  #++
  ## 初期化
  ## _simulator_:: Java の EvacuationSimulator のインスタンス。
  def initialize(simulator)
    @simulator = simulator ;
    @networkMap = NetworkMap.new(simulator.getMap()) ;
    @fallbackParameters = simulator.getFallbackParameters() ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーションの各種ログの設定。
  ## ログの出力項目などをいじれる。
  ## AgentHandler の setupSimulationLoggers() と
  ## initSimulationLoggers()の間によびだされる。
  def setupSimulationLoggers()
    p [:setupSimulationLoggers, :doNothing] ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション開始前の処理
  ## AgentHandler の prepareForSimulation の後で呼び出される。
  def prepareForSimulation()
    p [:prepareForSimulation, :doNothing] ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション終了後の処理
  ## EvacuationSimulator の finalize() で呼び出される。
  def finalizeSimulation()
    p [:finalizeSimulation, :doNothing] ;
  end

  #--------------------------------------------------------------
  #++
  ## update の先頭で呼び出される。
  ## _simTime_:: シミュレーション内相対時刻
  def preUpdate(simTime)
    p [:preUpdate, simTime, :doNothing] ;
  end

  #--------------------------------------------------------------
  #++
  ## update の最後に呼び出される。
  ## _simTime_:: シミュレーション内相対時刻
  def postUpdate(simTime)
    p [:postUpdate, simTime, :doNothing] ;
  end

  #--------------------------------------------------------------
  #++
  ## 経路情報の再構築。
  ## 各ノードの NavigationHints や、
  ## 各リンクの mentalLength を作り直す。
  ## mentalLength に関係するタグなどのマップパラメータを修正した場合、
  ## これを呼び出さないと、修正が routing に反映されない。
  def rebuildRoutes()
    @simulator.rebuildRoutes() ;
  end

  #--------------------------------------------------------------
  #++
  ## 文字列の java 内でのintern.
  def intern(str)
    return @simulator.intern(str) ;
  end

  #--------------------------------------------------------------
  #++
  ## 文字列の java 内でのintern.
  def self.intern(str)
    return @simulator.intern(str) ;
  end
  
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class CrowdWalkWrapper

