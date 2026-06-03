#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = CrowdWalk の agent_appearance の RubyColorModel のサンプル
## Author:: Itsuki Noda
## Version:: 0.0 2022/09/14 I.Noda
##
## === History
## * [2022/09/14]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'pp' ;

require 'RubyColorBase.rb' ;

#--======================================================================
#++
## Agent Appearance の色制御インターフェース
class SampleRubyColor < RubyColorBase
  
  #--------------------------------------------------------------
  #++
  ## 初期化。
  def initialize()
    super ;
#    pp [:initialize, self] ;
  end
  
  #--------------------------------------------------------------
  #++
  ## RGBで色を返す。
  def getAgentColorRGB(agent)
    pp [:agent, getAgentPos(agent)] ;
    pos = getAgentPos(agent) ;
    intPos = [(pos[0] / 4.0).to_i, (pos[1] / 4.0).to_i, pos[2].to_i] ;
    return intPos ;
#    return [rand(256), rand(256), rand(256)] ;
    ## return nil ;
  end
  
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class SampleRubyColor

