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
class SampleAgent2rr < RubyAgentBase
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
                   "update",
#                   "calcCostFromNodeViaLink",
#                   "calcSpeed",
#                   "calcAccel",
#                   "thinkCycle",
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
    p ['SampleAgent', :preUpdate, getAgentId(), getCurrentTime()] ;
    return super()
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの後半に呼ばれる。
  def update()
    p ['SampleAgent2', :update, getAgentId(), getCurrentTime()] ;
    p ['SampleAgent2', :config, ItkTerm.toRuby(@config,true)] ;
    p ['SampleAgent2', :fallback, ItkTerm.toRuby(@fallback,true)] ;

    return super() ;
  end

  #--------------------------------------------------------------
  #++
  ## あるwayを選択した場合の目的地(_target)までのコスト。
  ## _way_:: 現在進もうとしている道
  ## _node_:: 現在の分岐点
  ## _target_:: 最終目的地
  def calcCostFromNodeViaLink(link, node, target)
    ## Term の中身の書き換えテスト。
    v = ItkTerm.getArg(@fallback, "xA_0").getDouble() + 1.0 ;
    ItkTerm.setArg(@fallback, "xA_0", v) ;

    ## 結果出力。
    pp ['SampleAgent', :calcCostFromNodeViaLink, getAgentId(),
        link, node,
        ItkTerm.getHead(target),
        ItkTerm.toRuby(@fallback)] ;

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
    speed = super(previousSpeed) ;

    phase = (getCurrentTime().getRelativeTime() + @idInClass)/ 30.0 ;
    offset = 0.1 ;
    ratio = (1.0 + offset + Math::sin(phase)) / (2.0 + offset) ;
    
    origSpeed = speed ;
    speed *= ratio ;
#    p [:speedDown, @idInClass, getAgentId(), origSpeed, speed] ;

    return speed ;
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
    
    if(listenAlert(Term_Emergency)) then
      setGoal(Term_node_09_05) ;
      clearRoute() ;
      p [:changeGoal, ItkTerm.toRuby(Term_node_09_05)] ;
      clearAlert(Term_Emergency) ;
    end

    if(listenAlert(Term_FooBarBaz)) then
      insertRoute(Term_node_02_00) ;
      p [:insertRoute, ItkTerm.toRuby(Term_node_02_00)] ;
      clearAlert(Term_FooBarBaz) ;
    end

  end

  Term_Emergency = ItkTerm.ensureTerm("emergency") ;
  Term_node_09_05 = ItkTerm.ensureTerm("node_09_05") ;
  Term_FooBarBaz = ItkTerm.ensureTerm("foo-bar-baz") ;
  Term_node_02_00 = ItkTerm.ensureTerm("node_02_00") ;

  #--------------------------------------------------------------
  #++
  ## 思考ルーチン (テスト用)
  def thinkCycle0()
    pp ['SampleAgent', :thinkCycle, getAgentId()] ;
    addAgentTag(Term_hogehoge) ;
    pp [:agentTags, ItkTerm.toRuby(getAgentTags())] ;
    pp [:linkTags, ItkTerm.toRuby(getPlaceTags())] ;
    return super() ;
  end

  Term_hogehoge = ItkTerm.ensureTerm("hogehoge") ;

end # class SampleAgent

