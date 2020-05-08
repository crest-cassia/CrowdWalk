#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = Irb Utility for CrowdWalk
## Author:: Itsuki Noda
## Version:: 0.0 2020/05/07 I.Noda
##
## === History
## * [2020/05/07]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

def $LOAD_PATH.addIfNeed(path)
  self.unshift(path) if(!self.include?(path)) ;
end

# $LOAD_PATH.addIfNeed("~/lib/ruby");
# $LOAD_PATH.addIfNeed(File.dirname(__FILE__));

require 'java';

import 'nodagumi.Itk.Itk' ;

#--======================================================================
#++
## Irb Utility module for CrowdWalk.
##
## CrowdWalk を、irb で対話的に制御する機能を提供する。
##
## irb 対話モードで CrowdWalk を起動するには、
## property設定ファイル(*.prop.json)において、以下のように設定する。
##
## <B> *.prop.json </B>
##   {
##     ...
##     "use_ruby": true,
##     "use_irb": true,
##     ...
##   }
##
## 対話モードで CrowdWalk が起動され、シミュレーションが始まると、
## 最初のシミュレーションサイクルの開始時点で一旦停止し
## 起動した端末に irb のプロンプトが表示される。
## この時点で、CwIrb や ItkUtility などの機能を用いて、
## シミュレーションの状態などの参照や変更を、irb を通じて行える。
##
## また、irb のプロンプトで
##    > CwIrb.runCycle(100)
## とすると、100 サイクル、シミュレーションが進み、再び irb に制御が戻ってくる。
## runCycle の引数は省略可能で、既定値が 1 なので、
##    > CwIrb.runCycle
## とすると、1サイクル、シミュレーションが進む。

module CwIrb
  extend CwIrb ;
  #--------------------------------------------------------------
  #++
  ## get CrowdWalk simulator entity.
  ## *return*:: Java Object of the CrowdWalk simulator.
  def getSimulator()
    return $crowdwalk ;
  end

  #--------------------------------------------------------------
  #++
  ## get CrowdWalk simulator entity.
  ## *return*:: Java Object of agent handler in CrowdWalk.
  def getAgentHandler()
    return getSimulator().getAgentHandler() ;
  end

  #--------------------------------------------------------------
  #++
  ## get CrowdWalk simulator entity.
  ## *return*:: Ruby Object of agent as an instance of NetworkMap.
  def getMap()
    if($crowdwalkMap.nil?) then
      $crowdwalkMap = NetworkMap.new(getSimulator().getMap()) ;
    end
    return $crowdwalkMap ;
  end

  #--------------------------------------------------------------
  #++
  ## call a _block_ with each agent.
  ## _status_:: specify type of agents. :all or :walking.
  def eachAgent(status = :all, &block) # :yield: _agent_
    case(status)
    when :all ;
      getSimulator().getAllAgentCollection().each{|agent|
        block.call(agent) ;
      } ;
    when :walking ;
      getSimulator().getWalkingAgentCollection().each{|agent|
        block.call(agent) ;
      } ;
    else
      raise "unknown agent status for eachAgent(): " + status.inspect ;
    end
  end

  #--------------------------------------------------------------
  #++
  ## run N cycle.
  ## _n_:: cycle count to run the simulation.
  def runCycle(n = 1)
    getSimulator().irbWaitCycleN(n) ;
  end

  #--////////////////////////////////////////////////////////////
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------
end # module CwIrb

