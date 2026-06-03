#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = Sample Agent for CrowdWalk
## Author:: Itsuki Noda
## Version:: 0.0 2015/06/28 I.Noda
##
## === History
## * [2014/06/28]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'RubyAgentBase.rb' ;

#--======================================================================
#++
## SampleAgent class
class SampleAgent3 < RubyAgentBase
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
#                   "calcCostFromNodeViaLink",
#                   "calcSpeed",
#                   "calcAccel",
                   "thinkCycle",
                  ] ;
  
  AgentList = [] ;
  
  #@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## id counter
  attr_accessor :idInClass ;
  ## counter of think cycle
  attr_accessor :nCycle ;
  
  #--------------------------------------------------------------
  #++
  ## 初期化
  def initialize(*arg)
    super(*arg) ;
    AgentList.push(self) ;
    @idInClass = AgentList.length ;
    @nCycle = 0 ;
  end
  
  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの前半に呼ばれる。
  def preUpdate()
    p ['SampleAgent', :preUpdate, getAgentId(), currentTime()] ;
    return super()
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの後半に呼ばれる。
  def update()
    p ['SampleAgent', :update, getAgentId(), currentTime()] ;
    return super() ;
  end

  #--------------------------------------------------------------
  #++
  ## あるwayを選択した場合の目的地(_target)までのコスト。
  ## _way_:: 現在進もうとしている道
  ## _node_:: 現在の分岐点
  ## _target_:: 最終目的地
  def calcCostFromNodeViaLink(link, node, target)
    ## 元のものを呼び出す。
    return super(link, node, target) ;
  end

  #--------------------------------------------------------------
  #++
  ## 速度を計算する。
  ## 波を持たせて減速させてみる。
  ## _previousSpeed_:: 前のサイクルの速度。
  ## *return* 速度。
  def calcSpeed(previousSpeed)
    super(previousSpeed) ;
  end

  #--------------------------------------------------------------
  #++
  ## 加速度を計算する。
  ## _baseSpeed_:: 自由速度。
  ## _previousSpeed_:: 前のサイクルの速度。
  ## *return* 加速度。
  def calcAccel(baseSpeed, previousSpeed)
    p [:calcAccel, getAgentId(), baseSpeed, previousSpeed] ;
    return super(baseSpeed, previousSpeed) ;
  end

  #--------------------------------------------------------------
  #++
  ## 思考ルーチン。
  ## ThinkAgent のサンプルと同じ動作をさせている。
  def thinkCycle()
    @nCycle += 1 ;
#    p [:agentRand, getRandomInt()] ;
#    sleep(1) ;
  end

end # class SampleAgent

