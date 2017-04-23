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
    ## return nil
  end

  #--------------------------------------------------------------
  #++
  ## 
  def tryUpdateAndGenerate()
    ## return nil
  end

  #--------------------------------------------------------------
  #++
  ## 文字列の java 内でのintern.
  def intern(str)
    return @javaFactory.intern(str) ;
  end
  
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class RubyAgentBase

