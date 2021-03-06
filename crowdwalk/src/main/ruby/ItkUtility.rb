#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = ItkUtility
## Author:: Itsuki Noda
## Version:: 0.0 2017/07/19 I.Noda
##
## === History
## * [2017/07/19]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'java';

import 'nodagumi.Itk.Itk' ;

import 'nodagumi.ananPJ.NetworkMap.Link.MapLink' ;

#--======================================================================
#++
## 汎用ユーティリティ
module ItkUtility
  extend self ;
  
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## logger のレベル
  LogLevelTable = ({ :trace => Itk.getLogLevel("Trace"),
                     :debug => Itk.getLogLevel("Debug"),
                     :info  => Itk.getLogLevel("Info"),
                     :warn  => Itk.getLogLevel("Warn"),
                     :error => Itk.getLogLevel("Error"),
                     :fatal => Itk.getLogLevel("Fatal"),
                   }) ;
  
  #--------------------------------------------------------------
  #++
  ## Itkのloggerによるログ出力
  ## _level_ :: ログレベル。:trace, :debug, :info, :warn, :error, :fatal
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logWithLevel(level, label, *data)
    levelObj = LogLevelTable[level] ;
    label = "Ruby" if(label.nil?) ;
    return Itk.logOutput(levelObj, label, *data) ;
  end

  #--------------------------------
  #++
  ## Itkのloggerによるログ出力(trace)
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logTrace(label, *data)
    logWithLevel(:trace, label, *data) ;
  end

  #--------------------------------
  #++
  ## Itkのloggerによるログ出力(debug)
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logDebug(label, *data)
    logWithLevel(:debug, label, *data) ;
  end
  
  #--------------------------------
  #++
  ## Itkのloggerによるログ出力(info)
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logInfo(label, *data)
    logWithLevel(:info, label, *data) ;
  end

  #--------------------------------
  #++
  ## Itkのloggerによるログ出力(warn)
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logWarn(label, *data)
    logWithLevel(:warn, label, *data) ;
  end

  #--------------------------------
  #++
  ## Itkのloggerによるログ出力(error)
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logError(label, *data)
    logWithLevel(:error, label, *data) ;
  end

  #--------------------------------
  #++
  ## Itkのloggerによるログ出力(fatal)
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logFatal(label, *data)
    logWithLevel(:fatal, label, *data) ;
  end

  #--------------------------------------------------------------
  ## 地図オブジェクトへのアクセス。
  #--------------------------------
  #++
  ## ノードから exit したエージェントの数。
  def numOfExitAgentsFromNode(node)
    return node.getNumberOfEvacuatedAgents() ;
  end

  #--------------------------------
  #++
  ## 直前サイクルにノードを通過したエージェントの数。
  ## _simTime_:: 現在時刻（シミュレーション内時刻）
  def numOfPassedAgentsOverNode(node, simTime, margin = 1.5)
    return node.countPassingAgent(simTime, margin) ;
  end

  #--------------------------------------------------------------
  ## Agentオブジェクトへのアクセス。
  #--::::::::::::::::::::::::::::::
  ## Java における Agent クラス
  #++
  ## RubyAgentClass

  AgentClassTable = {}
  def fooo()
  AgentClassTable.update({
    :base => Java::NodagumiAnanPJAgents::AgentBase,
    :walk => Java::NodagumiAnanPJAgents::WalkAgent,
    :await => Java::NodagumiAnanPJAgents::AwaitAgent,
    :naive => Java::NodagumiAnanPJAgents::NaiveAgent,
    :bustle => Java::NodagumiAnanPJAgents::BustleAgent,
    :capricious => Java::NodagumiAnanPJAgents::CapriciousAgent,
    :rational => Java::NodagumiAnanPJAgents::RationalAgent,
    :ruby => Java::NodagumiAnanPJAgents::RubyAgent,
  }) ;
  end
  #--------------------------------
  #++
  ## Agent クラスチェック
  def checkAgentClass(agent, classTag)
    fooo() if (AgentClassTable.size() == 0) ; 
    return agent.is_a?(AgentClassTable[classTag]) ;
  end

  #--------------------------------
  #++
  ## Ruby Agent クラスチェック
  def isRubyAgent(agent)
    return checkAgentClass(agent, :ruby) ;
  end
  
  #--------------------------------
  #++
  ## Ruby Agent の場合に、そのインスタンスを取得。
  def getAgentInstanceInRuby(agent)
    if(isRubyAgent(agent)) then
      return agent.getRubyAgentInstance() ;
    else
      return nil ;
    end
  end
  
  #--------------------------------------------------------------
  ## Simulatorオブジェクト(Java)へのアクセス。
  #--------------------------------
  #++
  ## Simulator オブジェクトの取得。各クラスで再定義必要。
  def getSimulator()
    raise "should be defined in each included class." ;
  end

  #--------------------------------
  #++
  ## Java 側の乱数生成器へのアクセス。
  def getRandom()
    return getSimulator().getRandom() ;
  end

  #------------------------------------------
  #++
  ## Java 側の乱数生成で、int 取得。
  def getRandomInt(mode = nil)
    if(mode.nil?) then
      return getRandom().nextInt() ;
    else
      return getRandom().nextInt(mode) ;
    end
  end

  #------------------------------------------
  #++
  ## Java 側の乱数生成で、int 取得。
  def getRandomDouble()
    return getRandom().nextDouble() ;
  end
  
  #--------------------------------------------------------------
  ## link から agent へのアクセス。
  #::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## レーンの方向
  LaneDirectionFore = MapLink::Direction::Forward ;
  LaneDirectionBack = MapLink::Direction::Backward ;
  
  #------------------------------------------
  #++
  ## 全エージェント。
  def eachAgentOnLink(link, &block)
    link.getAgents().each{|agent|
      block.call(agent) ;
    }
  end

  #------------------------------------------
  #++
  ## 前向きレーンのエージェント。
  def eachAgentOnLinkFore(link, &block)
    link.getLane(LaneDirectionFore).each{|agent|
      block.call(agent) ;
    }
  end

  #------------------------------------------
  #++
  ## 前向きレーンのエージェント。
  def eachAgentOnLinkBack(link, &block)
    link.getLane(LaneDirectionBack).each{|agent|
      block.call(agent) ;
    }
  end

  
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------
  #++

end # module ItkUtility

