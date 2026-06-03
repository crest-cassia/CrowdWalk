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
## CrowdWalk の EvacuationSimulator の制御のwrapper の base class。
##
## シミュレーションのメインルーチンのうち、
## 以下に上げる method ポイントで、一旦 Ruby への制御が渡される。
## Ruby の method の実行が終わると、
## もとの Java の対応する method に実行が戻っていく。
##
## ユーザは CrowdWalkWrapper クラスを継承した Ruby のクラスを定義し、
## そのクラス名を以下のように property 設定ファイル("*.prop.json")で
## 指定しなければならない。
##
## <B>"*.prop.json"</B>
##   ...
##   "ruby_init_script": [...,
##     "require './SampleWrapper.rb'",
##     ...],
##   "ruby_simulation_wrapper_class":"SampleWrapper",
##   ...
##
## ただし、+SampleWrapper+ がユーザが定義したクラス、
## +./SampleWrapper.rb+ は、
## そのクラスを定義した Ruby のプログラムファイルである。
##
## CrowdWalkWrapper を継承したクラスに対して、
## 以下にあげるメソッドが、シミュレーションの実行の各段階で呼ばれる。
## * prepareForSimulation () : シミュレーションの開始直前に呼ばれる。
## * finalizeSimulation () : シミュレーション終了後に呼ばれる。
## * preUpdate () : シミュレーションの各サイクルの先頭
##   (移動先位置の計算の前)に呼ばれる。
## * postUpdate () : シミュレーションの各サイクルの最後
##   (エージェント移動後)に呼ばれる。
## * setupSimulationLoggers () : シミュレーション開始前、
##   ログ出力の準備のタイミングで呼ばれる。
##
## 以下は、SampleWrapper の例である。
##
## <B>"SampleWrapper.rb"</B>
##    require 'CrowdWalkWrapper.rb' ;
##
##    class SampleWrapper < CrowdWalkWrapper
##      attr_accessor :taggedNodeList ;
##    
##      def initialize(simulator)
##        super(simulator) ;
##        @taggedNodeList = [] ;
##      end
##    
##      def setupSimulationLoggers()
##        addMemberToAgentTrailLogFormatter("foo") {
##          |agent, currentTime, handler|
##          [1, 2, 3, "hogehoge", {"a" => 10, "b" => nil}] ;
##        }
##        addMemberToAgentTrailLogFormatter("bar") {
##          |agent, currentTime, handler|
##          {"time" => currentTime.getAbsoluteTimeString(),
##           "agent" => agent.getID()} ;
##        }
##      end
##      
##      def prepareForSimulation()
##        width = @simulator.filterFetchFallbackDouble("link",
##                                                     "gathering_location_width",
##                                                     40.0) ;
##        @networkMap.eachLinkWithTag("TEMPORARY_GATHERING_LOCATION_LINK"){
##          |link|
##          link.setWidth(width) ;
##        }
##
##        @networkMap.eachLink(){|link|
##          tag = link.getNthTag(0) ;
##          if(!tag.nil? && tag =~ /link_node_04/ && tag =~ /__node_04/) then
##            link.addTag(Term_major) ;
##            logInfo(nil, 'link.addTag', link.getID(), link.getTagString()) ;
##          end
##          if(!tag.nil? && tag =~ /04__/ && tag =~ /04$/) then
##            link.addTag("major") ;
##            logInfo(nil, 'link.addTag', link.getID(), link.getTagString()) ;
##          end
##        }
##        rebuildRoutes() ;
##      end
##    
##      Term_major = ItkTerm.intern("major") ;
##    
##      def finalizeSimulation()
##        super
##      end
##    
##      def preUpdate(simTime)
##        @networkMap.eachNode(){|node|
##          if(rand(100) == 0) then
##            node.addTag(ItkTerm.intern("foo")) ;
##            @taggedNodeList.push(node) ;
##          end
##        }
##        while(@taggedNodeList.size > 10)
##          node = @taggedNodeList.shift ;
##          node.removeTag(ItkTerm.intern("foo")) ;
##        end
##      end
##    
##      def postUpdate(simTime)
##        ## do nothing.
##      end
##    end # class SampleWrapper

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

  ## Properties in Term ;
  attr_accessor :propertiesTerm ;

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
    @propertiesTerm = simulator.getPropertiesTerm() ;

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
  ## _block_ :: a procedure to generage the value of the member.
  ##            The block should receive three args
  ##            (agent, timeObj, handler).
  def addMemberToAgentTrailLogFormatter(name, &block) # :yield: _agent_, _currentTime_, _handler_
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

