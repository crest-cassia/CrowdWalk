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
## CrowdWalk の RubyGate での Ruby 側の制御のインターフェース。
## 
## シナリオ設定ファイル("*.scnr.json")に記述するCloseGateイベントの制御を、
## Ruby で記述することを可能とする。
## このクラスを継承した Ruby のクラスのインスタンスがGateに割り当てられる。
##
## ユーザは、RubyGateBase を継承した Ruby のクラスを継承し、
## そのクラス名や定義ファイル(Rubyプログラム)を以下のように、
## property 設定ファイル("*.prop.json")
## およびシナリオ設定ファイル("*.scnr.json")で指定しなければならない。
## 
## <B>"*.prop.json"</B>
##       ...
##       "ruby_init_script":[ ...
##          "require './SampleGate.rb'",
##          ...],
##       ...
## <B>"*.scnr.json"</B>
##       ...
##       { "type":"CloseGate",
##         "gateClass":"RubyGate",
##         "rubyClass":"SampleGate",
##         "atTime":"18:02:15",
##         "placeTag":"gate_foo",
##         "gateTag":"foo",
##         "param1": [1,2,3],
##         ...},
##       ...
## この例では、+SampleGate+ が、ユーザが定義したクラスであり、
## "+SampleGate.rb+" にそのプログラムが格納されているとしている。
## この例では、18:02:15 にこの CloseGate イベントが生成され、
## SampleGate クラスのインスタンスが割り当てられる。
## そのインスタンスの変数 @eventDef には
## このイベントの定義自体は Hash の形で代入されるので、
## この定義に書かれた "param1" など任意のキーの値を参照することができる。
##
## 以下は、+SampleGate+ の例である。
## この例では、closed かどうかのチェックで、
## 確率 1/2 で止める（残り半分はgateを通過させる）ような制御を行っている。
##
## <B>SampleGate.rb</B>
##    require 'RubyGateBase.rb' ;
##    
##    class SampleGate < RubyGateBase
##      
##      def initialize(_gate)
##        super ;
##        @conf = ItkTerm.toRuby(getEventDef()) ;
##      end
##      
##      def isClosed(agent, currentTime)
##        ## close のときも、半分通す。
##        r = super ;
##        if(r) then
##          r = (getRandomInt(2) == 0) ;
##        end
##
##        return r ;
##      end
##    
##      def switchGate(event, closed)
##        # do nothing
##        super
##      end
##    end # class SampleGate

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
  ## Scenario に記述されたイベント定義を、Ruby のデータに変換したもの。
  attr_accessor :eventDef ;

  ## initial cycle check
  attr_accessor :isInitialCycle ;

  #--------------------------------------------------------------
  #++
  ## 初期化。
  ## 設定等は、Ruby のデータに変換され、@eventDef で取得できる。
  ## _gate_:: Gate の java インスタンス。
  def initialize(_gate) ;
    GateList.push(self) ;
    @javaGate = _gate ;
    @map = NetworkMap.new(getMap()) ;
    @eventDef = ItkTerm.toRuby(getEventDef()) ;
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
  
  #------------------------------------------
  #++
  ## イベント定義取得
  def getSimulator()
    return @javaGate.getEvent().getSimulator() ;
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
  
  #--------------------------------------------------------------
  #++
  ## Itkのloggerによるログ出力
  ## ItkUtility のものを override.
  ## _level_ :: ログレベル。:trace, :debug, :info, :warn, :error, :fatal
  ## _label_ :: ログのラベル。nil なら、Agent ID などに置き換えられる。
  ## _*data_ :: データの並び。
  def logWithLevel(level, label, *data)
    label = "RubyGate" if label.nil? ;
    super(level, label, *data) ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class RubyAgentBase

