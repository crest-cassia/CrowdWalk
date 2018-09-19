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
  
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------
  #++

end # class ItkUtility

