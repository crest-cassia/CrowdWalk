# coding: utf-8
1#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = AgentFactoryBase for AgentFactoryByRuby
## Author:: Itsuki Noda
## Version:: 0.0 2017/04/23 I.Noda
##
## === History
## * [2017/04/23]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

#--======================================================================
#++
## CrowdWalk の AgentFactoryByRuby での Ruby 側の制御のインターフェース
class AgentFactoryBase
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の Agent オブジェクト
  attr_accessor :javaFactory ;

  ## config 情報。generation ファイルでの設定がそのまま渡る。
  attr_accessor :config ;

  ## fallback 情報。
  attr_accessor :fallback ;

  ## initial cycle check
  attr_accessor :isInitialCycle ;

  #--------------------------------------------------------------
  #++
  ## 初期化
  ## _agent_:: Java の RubyAgent のインスタンス。
  ## _initOption_:: 初期化のためのオプション引数。
  ##                generation file で指定できる。指定しなければ nil。
  def initialize(factory, config, fallback)
    @javaFactory = factory ;
    @config = config ;
    @fallback = fallback ;
    @isInitialCycle = true ;
  end

  #--------------------------------------------------------------
  #++
  ## 
  def placeAgent()
    ## return nil ;
  end

  #--------------------------------------------------------------
  #++
  ## 
  def getGoal()
    ## return nil
  end

  #--------------------------------------------------------------
  #++
  ## 
  def clonePlannedPath()
    ## return nil 
  end

  #--------------------------------------------------------------
  #++
  ## 
  def getTags()
    ##return nil
  end

  #--------------------------------------------------------------
  #++
  ## 
  def getAgentConfig()
    return nil ;
  end

  #--------------------------------------------------------------
  #++
  ## 
  def isFinished()
    return @javaFactory.super_isFinished() ;
  end

  #--------------------------------------------------------------
  #++
  ## 
  def tryUpdateAndGenerate()
    initCycle() if(@isInitialCycle) ;
    @isInitCycle = false ;
    
    cycle() ;
  end

  #--------------------------------------------------------------
  #++
  ## 
  def initCycle()
    ## do nothing
  end
  
  #--------------------------------------------------------------
  #++
  ## 
  def enable()
    @javaFactory.enable()
  end
  
  #--------------------------------------------------------------
  #++
  ## 
  def disable()
    @javaFactory.disable()
  end
  
  #--------------------------------------------------------------
  #++
  ## 
  def cycle()
    ##do nothing
  end
  
  #--------------------------------------------------------------
  #++
  ## 文字列の java 内でのinternを行った Term を作る。
  ## 毎サイクルなど頻繁に呼び出すと、効率が悪い。(intern に時間かかる)
  ## なので、初回に1回呼ぶようにすべき。
  def makeSymbolTerm(str)
    return @javaFactory.makeSymbolTerm(str) ;
  end

  #--------------------------------------------------------------
  #++
  ## get SimTime object
  ## _timeStr_ :: 時刻を表す文字列 ("HH:MM:SS") ;
  ## *return* :: SimTime のインスタンス。
  def getSimTime(timeStr)
    return @javaFactory.getSimTime(timeStr) ;
  end
  
  #--------------------------------------------------------------
  #++
  ## tag で指定されたリンクの取得。
  ## _tag_ :: tag を表す Term。 makeSymbolTerm() で作成されるべき。
  ## *return* :: リンクのリスト(MapLinkTable)
  def getLinkTableByTag(tag)
    @javaFactory.getLinkTableByTag(tag) ;
  end

  #--------------------------------------------------------------
  #++
  ## tag で指定されたノードの取得。
  ## _tag_ :: tag を表す Term。 makeSymbolTerm() で作成されるべき。
  ## *return* :: ノードのリスト(MapLinkTable)
  def getNodeTableByTag(tag)
    @javaFactory.getNodeTableByTag(tag) ;
  end

  #--------------------------------------------------------------
  #++
  ## エージェント生成。
  ## _agentClassName_ :: class name の文字列
  ## _startPlace_ :: 出発地点(MapLink or MapNode)
  ## _goalTag_ :: ゴールタグ (Term)
  ## _route_ :: 経由点。Term の配列。
  def launchAgentWithRoute(agentClassName, startPlace,
                           goalTag, route)
    @javaFactory.launchAgentWithRoute(agentClassName, startPlace,
                                      goalTag, route) ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class RubyAgentBase

