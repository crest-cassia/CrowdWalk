# coding: utf-8
#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = RubyGateBase for RubyGate class
## Author:: Itsuki Noda
## Version:: 0.0 2018/09/21 I.Noda
##
## === History
## * [2018/09/21]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

#--======================================================================
#++
## CrowdWalk の RubyGate での Ruby 側の制御のインターフェース
class RubyGateBase
  include ItkUtility ;
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の Gate オブジェクト
  attr_accessor :javaGate ;

  ## initial cycle check
  attr_accessor :isInitialCycle ;

  #--------------------------------------------------------------
  #++
  ## 初期化
  ## _gate_:: Gate の java インスタンス。
  ## _tag_:: Gate の tag 名。
  ## _eventDef_:: scenario でのイベント設定。
  ## _closed_:: 当面、無視。java で制御。
  def initialize(_gate) ;
    @javaGate = _gate ;
    pp [:createRubyBase] ;
  end

  #--------------------------------------------------------------
  # アクセス関係
  #------------------------------------------
  #++
  ## イベント定義取得
  def getGateTag()
    return @javaGate.getTag() ;
  end
  
  #------------------------------------------
  #++
  ## イベント定義取得
  def getGateTag()
    return @javaGate.getTag() ;
  end
  
  #------------------------------------------
  #++
  ## 場所取得
  def getPlace()
    return @javaGate.getPlace() ;
  end
  
  #------------------------------------------
  #++
  ## イベント定義取得
  def getEventDef()
    return @javaGate.getEvent().getEventDef() ;
  end
  
  #--------------------------------------------------------------
  #++
  ## エージェント通過チェック。
  def isClosed(agent, currentTime)
    return @javaGate.isClosed() ;
  end
  
  #--------------------------------------------------------------
  #++
  ## 状態変化
  def switchGate(event, closed)
    return @javaGate.super_switchGate(event, closed) ;
  end
  
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class RubyAgentBase

