#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = RubyAgentBase for CrowdWalk
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
## CrowdWalk の RubyAgent に対応する Ruby 側の AgentBase
class RubyAgentBase
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の Agent オブジェクト
  attr_accessor :javaAgent ;

  #--------------------------------------------------------------
  #++
  ## 初期化
  ## _agent_:: Java の RubyAgent のインスタンス。
  def initialize(agent)
    @javaAgent = agent ;
  end

  #--------------------------------------------------------------
  #++
  ## エージェント id
  def getAgentId()
    return @javaAgent.getID() ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの前半に呼ばれる。
  ## _relTime_:: 相対シミュレーション時刻
  def preUpdate(relTime)
#    p [:preUpdate, getAgentId(), relTime] ;
    return @javaAgent.super_preUpdate(relTime) ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの後半に呼ばれる。
  ## _relTime_:: 相対シミュレーション時刻
  def update(relTime)
#    p [:update, getAgentId(), relTime] ;
    return @javaAgent.super_update(relTime) ;
  end

  #--------------------------------------------------------------
  #++
  ## あるwayを選択した場合の目的地(_target)までのコスト。
  ## _way_:: 現在進もうとしている道
  ## _node_:: 現在の分岐点
  ## _target_:: 最終目的地
  def calcWayCostTo(way, node, target)
#    p [:calcWayCostTo, getAgentId(), way, node, target] ;
    return @javaAgent.super_calcWayCostTo(way, node, target);
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class RubyAgentBase

