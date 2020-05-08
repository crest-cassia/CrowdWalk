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
## CrowdWalk の RubyEvent での Ruby 側の制御のインターフェース。
##
## シナリオ設定ファイル("*.scnr.json")に記述するイベントの制御を、
## Ruby で記述することを可能とする。
## このクラスを継承した Ruby のクラスのインスタンスがイベントに割り当てられる。
##
## ユーザは、RubyEventBase を継承した Ruby のクラスを継承し、
## そのクラス名や定義ファイル(Rubyプログラム)を以下のように、
## property 設定ファイル("*.prop.json")
## およびシナリオ設定ファイル("*.scnr.json")で指定しなければならない。
## 
## <B>"*.prop.json"</B>
##       ...
##       "ruby_init_script":[ ...
##          "require './SampleEvent.rb'",
##          ...],
##       ...
## <B>"*.scnr.json"</B>
##       ...
##       { "type":"Ruby",
##         "atTime":"18:00:10",
##         "rubyClass": "SampleEvent",
##         "name": "hogehoge",
##         "param1": 1234,
##         ... },
##       ...
## この例では、+SampleEvent+ が、ユーザが定義したクラスであり、
## "+SampleEvent.rb+" にそのプログラムが格納されているとしている。
## この例では、18:00:10 にこの RubyEvent が生成され、
## SampleEvent クラスのインスタンスが割り当てられる。
## そのインスタンスの変数 @eventDef には
## このイベントの定義自体は Hash の形で代入されるので、
## この定義に書かれた "name" や "param1" の値を参照することができる。
##
## 以下は、+SampleEvent+ の例である。
## この例では、表示だけ行い、何もシミュレーションを変化させていないが、
## NetworkMap や ItkUtility などの機能を用いて、
## 地図やエージェント状態をいろいろ変更することができる。
##
## <B>SampleEvent.rb</B>
##    require 'RubyEventBase.rb' ;
##    
##    class SampleEvent < RubyEventBase
##      
##      def initialize(_event)
##        super ;
##        pp [:rubyEventConf, @eventDef] ;
##      end
##      
##      def occur(currentTime, map)
##        p [:eventOccur, currentTime, map] ;
##        p [:eventRand, getRandomInt(), getRandomDouble()] ;
##        return true ;
##      end
##    end # class SampleEvent

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

