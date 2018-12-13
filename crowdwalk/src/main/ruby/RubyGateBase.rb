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

require 'NetworkMap.rb' ;

#--======================================================================
#++
## CrowdWalk の RubyGate での Ruby 側の制御のインターフェース
class RubyGateBase
  include ItkUtility ;
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## RubyGate を格納するリスト
  GateList = [] ;

  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の Gate オブジェクト
  attr_accessor :javaGate ;
  ## NetworkMap の Ruby オブジェクト
  attr_accessor :map ;

  ## initial cycle check
  attr_accessor :isInitialCycle ;

  #--------------------------------------------------------------
  #++
  ## 初期化。
  ## 設定等は、getEventDef() で取得できる。
  ## _gate_:: Gate の java インスタンス。
  def initialize(_gate) ;
    GateList.push(self) ;
    @javaGate = _gate ;
    @map = NetworkMap.new(getMap()) ;
    # pp [:createRubyBase] ;
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
  ## イベント定義取得。
  ## Itk::Term の形で返す。
  ## なので、ItkTerm.getArg(obj, slot) などで変換。
  ## さらに、ItkTerm.toRuby(value) で ruby object に変換。
  def getEventDef()
    return @javaGate.getEvent().getEventDef() ;
  end
  
  #------------------------------------------
  #++
  ## イベント定義取得
  def getMap()
    return @javaGate.getPlace().getMap() ;
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

