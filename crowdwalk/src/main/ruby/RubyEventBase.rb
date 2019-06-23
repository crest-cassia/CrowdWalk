# coding: utf-8
#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = RubyGateBase for RubyEventBase class
## Author:: Itsuki Noda
## Version:: 0.0 2019/06/23 I.Noda
##
## === History
## * [2018/06/23]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'NetworkMap.rb' ;

#--======================================================================
#++
## CrowdWalk の RubyEvent での Ruby 側の制御のインターフェース
class RubyEventBase
  include ItkUtility ;

  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の Gate オブジェクト
  attr_accessor :javaEvent ;
  ## Scenario に記述されたイベント定義を、Ruby のデータに変換したもの。
  attr_accessor :eventDef ;

  #--------------------------------------------------------------
  #++
  ## 初期化。
  ## 設定等は、Ruby のデータに変換され、@eventDef で取得できる。
  ## _gate_:: Gate の java インスタンス。
  def initialize(_event) ;
    @javaEvent = _event ;
    @eventDef = ItkTerm.toRuby(getEventDef()) ;
    # pp [:createRubyBase] ;
  end

  #--------------------------------------------------------------
  # アクセス関係
  #------------------------------------------
  #++
  ## イベント定義取得。
  ## Itk::Term の形で返す。
  ## なので、ItkTerm.getArg(obj, slot) などで変換。
  ## さらに、ItkTerm.toRuby(value) で ruby object に変換。
  def getEventDef()
    return @javaEvent.getEventDef() ;
  end
  
  #------------------------------------------
  #++
  ## イベント定義取得
  def getSimulator()
    return @javaEvent.getScenario().getSimulator() ;
  end
  
  #--------------------------------------------------------------
  #++
  ## イベント発生。なにか再定義されないといけない。
  def occur(currentTime, map)
    return raise "occur() should be defined in the inherited Ruby Event class."
  end
  
  #--------------------------------------------------------------
  #++
  ## Itkのloggerによるログ出力
  ## ItkUtility のものを override.
  ## _level_ :: ログレベル。:trace, :debug, :info, :warn, :error, :fatal
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logWithLevel(level, label, *data)
    label = "RubyEvent" if label.nil? ;
    super(level, label, *data) ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class RubyAgentBase

