# coding: utf-8
#! /usr/bin/env ruby
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
## CrowdWalk の AgentFactoryByRuby での Ruby 側の制御のインターフェース。
##
## エージェントを生成する AgentFactory の一種で、
## 生成するタイミング、場所、目的地や経路、タグ情報、エージェント数を
## Ruby script によって細かく設定できる。
##
## ユーザは、AgentFactoryByRuby を継承した Ruby のクラスを定義し、
## そのクラス名を以下のように property 設定ファイル("*.prop.json")
## およびエージェント生成設定ファイル("*.gen.json")で指定しなければならない。
## 
## <B>"*.prop.json"</B>
##       ...
##       "ruby_init_script":[ ...
##          "require './SampleFactory.rb'",
##          ...],
##       ...
## <B>"*.gen.json"</B>
##       ...
##       { "rule": "RUBY",
##         "ruleClass": "SampleFactory",
##         "config": { ... }
##       },
##       ...
## この例では、+SampleFactory+ が、ユーザが定義したクラスであり、
## "+SampleFactory.rb+" にそのプログラムが格納されているとしている。
## また、生成ルールの中の<tt>"config"</tt> に与えられる値は、
## +SampleFactory+ の new の _config_ 引数に渡される。
##
## 以下は、+SampleFactory+ の例である。
##
## <B>SampleFactory.rb</B>
##    require 'AgentFactoryBase.rb' ;
##
##    class SampleFactory < AgentFactoryBase
##      def initialize(factory, config, fallback)
##        super
##        @c = 0 ;
##        @time0 = getSimTime("01:23:45") ;
##      end
##      
##      def initCycle()
##        @beginTime = getCurrentTime() ;
##        @fromTag = makeSymbolTerm("major") ;
##        @fromList = getLinkTableByTag(@fromTag) ;
##        @toTag = makeSymbolTerm("node_09_06") ;
##        @toList = getNodeTableByTag(@toTag) ;
##        @agentList = [] ;
##        @c = 0 ;
##      end
##      
##      def cycle()
##        @currentTime = getCurrentTime() ;
##        disable() if(@c >= 10) ;
##    
##        finishAllP = true ;
##        @agentList.each{|agent|
##          finishAllP = false if(isAgentWalking(agent)) ;
##        }
##        return if(!finishAllP) ;
##    
##        @agentList = [] ;
##        @fromList.each{|origin|
##          agent = launchAgentWithRoute("RationalAgent", origin, @toTag, []) ;
##          @agentList.push(agent) ;
##        }
##        @c += 1 ;
##      end
##    
##    end # class SampleWrapper

require 'ItkTerm.rb' ;

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
    @isInitCycle = true ;
  end

  #--------------------------------------------------------------
  #++
  ## エージェントの初期位置を返す。
  ## 必要に応じて、子クラスで定義。
  def placeAgent()
    ## return nil ;
  end

  #--------------------------------------------------------------
  #++
  ## エージェントに設定する目的地を返す。
  ## 必要に応じて、子クラスで定義。
  def getGoal()
    ## return nil
  end

  #--------------------------------------------------------------
  #++
  ## エージェントに設定する経路を返す。
  ## 必要に応じて、子クラスで定義。
  def clonePlannedPath()
    ## return nil 
  end

  #--------------------------------------------------------------
  #++
  ## エージェントに初期に設定するタグのリストを返す。
  ## 必要に応じて、子クラスで定義。
  def getTags()
    ##return nil
  end

  #--------------------------------------------------------------
  #++
  ## エージェント設定 config を返す。
  ## 必要に応じて、子クラスで定義。
  def getAgentConfig()
    return nil ;
  end

  #--------------------------------------------------------------
  #++
  ## 終了しているかどうかの確認。
  ## シミュレーションの終了チェック（全生成ルールが終わっているかのチェック）で
  ## 予備出される。
  def isFinished()
    return @javaFactory.super_isFinished() ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレータからの呼び出しのトップレベル。
  ## 初回のみ initCycle () を呼び出し、その後、毎回、cycle () を呼び出す。
  def tryUpdateAndGenerate()
    initCycle() if(@isInitCycle) ;
    @isInitCycle = false ;
    
    cycle() ;
  end

  #--------------------------------------------------------------
  #++
  ## 最初の呼び出しの際の初期化。
  ## インスタンスが作られた際には、まだ、simulator とかがバインド
  ## されていないので、マップなどを使う初期化はこちらで行う。
  ## 
  def initCycle()
    ## do nothing.
  end
  
  #--------------------------------------------------------------
  #++
  ## 生成ルールを有効化する。
  def enable()
    @javaFactory.enable()
  end
  
  #--------------------------------------------------------------
  #++
  ## 生成ルールを無効化する。
  def disable()
    @javaFactory.disable()
  end
  
  #--------------------------------------------------------------
  #++
  ## 各 シミュレーション cycle のエージェント生成のフェーズで呼び出される。
  ## ただし、この生成ルールが enable されている時のみに呼び出しがある。
  ## disable されると、呼び出されなくなる。
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
  ## simulator object (Java) を取得。
  ## *return* :: simulator
  def getSimulator()
    return @javaFactory.getSimulator() ;
  end
  
  #--------------------------------------------------------------
  #++
  ## scenario object (Java) を取得。
  ## *return* :: scenario
  def getScenario()
    return getSimulator().getScenario() ;
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
  ## _fallback_ :: agent に渡すパラメータ。ハッシュ。
  def launchAgentWithRoute(agentClassName, startPlace,
                           goalTag, route, fallback = nil)
    if(!fallback.nil?) then
      fallback = ItkTerm.ensureTerm(fallback) ;
    end
    return @javaFactory.launchAgentWithRoute(agentClassName, startPlace,
                                             goalTag, route, fallback) ;
  end

  #--------------------------------------------------------------
  #++
  ## 現在時刻の取得。
  ## *return* :: 現在時刻。SimTime のインスタンス
  def getCurrentTime()
    return @javaFactory.getCurrentTime() ;
  end
  
  #--------------------------------------------------------------
  #++
  ## 時間の差分計算。(秒)
  ## time0 - time1 を求める。
  ## _time0_ :: 後の時刻。SImTime のインスタンス。
  ## _time1_ :: 前の時刻SImTime のインスタンス。
  ## *return* :: 差を秒で表す。
  def timeDiffInSec(time0, time1)
    return time0.calcDifferenceFrom(time1) ;
  end

  #--------------------------------------------------------------
  #++
  ## エージェントが歩いているかどうか。
  ## _agent_ :: 調べるエージェント。
  ## *return* :: まだ生きていれば（歩いていれば）true。
  def isAgentWalking(agent)
    return !agent.isEvacuated() ;
  end
  
  
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class RubyAgentBase

