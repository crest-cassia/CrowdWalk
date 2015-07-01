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
    return @javaAgent.super_preUpdate(relTime) ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの後半に呼ばれる。
  ## _relTime_:: 相対シミュレーション時刻
  def update(relTime)
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
  #++
  ## Java から Ruby を呼び出すTriggerでのFilter。
  ## この配列に Java のメソッド名（キーワード）が入っていると、
  ## Ruby 側が呼び出される。入っていないと、無視される。
  ## RubyAgentBase を継承するクラスは、このFilterを持つことが望ましい。
  ## このFilterは、クラスをさかのぼってチェックされる。
  TriggerFilter = [
#                   "preUpdate",
#                   "update",
#                   "calcWayCostTo",
                  ] ;

  #--============================================================
  #--------------------------------------------------------------
  #++
  ## RubyAgentBase を継承するクラスにおいて、TriggerFilter に methodName 
  ## 含まれているかをチェックする。
  ## このクラスメソッドが true を返すもののみ、
  ## java から ruby のメソッドが呼ばれる。
  ## java と ruby の行き来のオーバーヘッドを軽くするための措置。
  def self.checkTriggerFilter(methodName)
    # TriggerFilter が定義されていて、methodName を含めば、true を返す
    if(self.const_defined?(:TriggerFilter)) then
      if(self::TriggerFilter.include?(methodName)) then
        return true ;
      end
    end
    # RubyAgentBase までさかのぼっていれば、探索撃ち切って false を返す。
    if(self == RubyAgentBase) then
      return false ;
    end
    # それ以外は、親クラスを探しに行く。
    return self.superclass().checkTriggerFilter(methodName) ;
  end




  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class RubyAgentBase

