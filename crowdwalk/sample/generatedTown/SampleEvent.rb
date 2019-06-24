#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = RubyEvent のサンプル
## Author:: Itsuki Noda
## Version:: 0.0 2019/06/23 I.Noda
##
## === History
## * [2019/06/23]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'RubyEventBase.rb' ;

#--======================================================================
#++
## RubyGate の制御インターフェース
class SampleEvent < RubyEventBase
  
  #--------------------------------------------------------------
  #++
  ## 初期化。
  def initialize(_event)
    super ;
    pp [:rubyEventConf, @eventDef] ;
  end
  
  #--------------------------------------------------------------
  #++
  ## イベント発生
  def occur(currentTime, map)
    p [:eventOccur, currentTime, map] ;
    p [:eventRand, getRandomInt(), getRandomDouble()] ;
    sleep(10) ;
    return true ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class SampleWrapper

