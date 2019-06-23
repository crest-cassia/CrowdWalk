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

require 'ItkUtility.rb' ;
require 'ItkTerm.rb' ;
require 'NetworkMap.rb' ;

#--======================================================================
#++
## CrowdWalk の EvacuationSimulator の制御のwrapper
class CrowdWalkWrapper
  include ItkUtility ;

  #--============================================================
  #--------------------------------------------------------------
  #++ Wrapper の（唯一の）インスタンスへのアクセス。
  def self.getInstance()
    return @instance ;
  end
  
  #--============================================================
  #--------------------------------------------------------------
  #++ Wrapper の（唯一の）インスタンスへのを設定。
  def self.setInstance(wrapper)
    @instance = wrapper ;
    return @instance ;
  end
  
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の EvacuationSimulator
  attr_accessor :simulator ;

  ## NetworkMap
  attr_accessor :networkMap ;

  ## Fallback Parameters ;
  attr_accessor :fallbackParameters ;

  ## シミュレーションの AgentTrailLog の Format Member の手続きテーブル
  attr_accessor :agentTrailLogFormatTable ;

  #--------------------------------------------------------------
  #++
  ## 初期化
  ## _simulator_:: Java の EvacuationSimulator のインスタンス。
  def initialize(simulator)
    self.class().setInstance(self) ;
    @simulator = simulator ;
    @networkMap = NetworkMap.new(simulator.getMap()) ;
    @fallbackParameters = simulator.getFallbackParameters() ;

    @agentTrailLogFormatTable = {} ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレータ取得。
  def getSimulator()
    return @simulator ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーションの各種ログの設定。
  ## ログの出力項目などをいじれる。
  ## AgentHandler の setupSimulationLoggers() と
  ## initSimulationLoggers()の間によびだされる。
  def setupSimulationLoggers()
#    p [:setupSimulationLoggers, :doNothing] ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーションの AgentTrailLog の Format Member の追加。
  ## _name_ :: name of the Format Member.
  ## _&block_ :: a procedure to generage the value of the member.
  ##             The block should receive three args
  ##             (agent, timeObj, handler).
  def addMemberToAgentTrailLogFormatter(name, &block)
    @agentTrailLogFormatTable[name] = block ;
    @simulator.getAgentHandler().addMemberToAgentTrailLogFormatterForRuby(name);
  end

  #--------------------------------------------------------------
  #++
  ## AgentTrailLog の Format Member からの呼び戻し。
  ## _name_ :: name of the Format Member.
  ## _agent_ :: agent object in Java.
  ## _currentTime_ :: time obj.
  ## _handler_ :: AgentHandler.
  def callbackAgentTrailLogMember(name, agent, currentTime, handler)
    block = @agentTrailLogFormatTable[name] ;
    if(!block.nil?) 
      return block.call(agent, currentTime, handler) ;
    else
      raise "Uknown callbackAgentTrailLogMember:" + name ; 
    end
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション開始前の処理
  ## AgentHandler の prepareForSimulation の後で呼び出される。
  def prepareForSimulation()
#    logInfo(nil, :prepareForSimulation, :doNothing) ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション終了後の処理
  ## EvacuationSimulator の finalize() で呼び出される。
  def finalizeSimulation()
#    logInfo(nil, :finalizeSimulation, :doNothing) ;
  end

  #--------------------------------------------------------------
  #++
  ## update の先頭で呼び出される。
  ## _simTime_:: シミュレーション内相対時刻
  def preUpdate(simTime)
#    p [:preUpdate, simTime, :doNothing] ;
  end

  #--------------------------------------------------------------
  #++
  ## update の最後に呼び出される。
  ## _simTime_:: シミュレーション内相対時刻
  def postUpdate(simTime)
#    p [:postUpdate, simTime, :doNothing] ;
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

  #------------------------------------------
  #++
  ## 文字列の java 内でのintern.
  def self.intern(str)
    return @simulator.intern(str) ;
  end
  
  #--------------------------------------------------------------
  #++
  ## Java 側の乱数生成器へのアクセス。
  def self.getRandom()
    return getInstance().getRandom() ;
  end
  
  #------------------------------------------
  #++
  ## Java 側の乱数生成で、int 取得。
  def self.getRandomInt(mode = nil)
    return getInstance().getRandomInt(mode) ;
  end

  #------------------------------------------
  #++
  ## Java 側の乱数生成で、int 取得。
  def self.getRandomDouble()
    return getInstance().getRandomDouble() ;
  end

  #--------------------------------------------------------------
  #++
  ## Itkのloggerによるログ出力.
  ## ItkUtility のものを override.
  ## _level_ :: ログレベル。:trace, :debug, :info, :warn, :error, :fatal
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logWithLevel(level, label, *data)
    label = "Wrapper" if label.nil? ;
    super(level, label, *data) ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class CrowdWalkWrapper

