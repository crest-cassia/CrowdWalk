# coding: utf-8
#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = RubyColorBase
## Author:: Itsuki Noda
## Version:: 0.0 2022/09/14 I.Noda
##
## === History
## * [2022/09/14]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

#--======================================================================
#++
## CrowdWalk の Agent Appearance の色ぎめインターフェース。
##
## <B>"*.prop.json"</B>
##       ...
##       "agent_appearance_file":"sample.agentApp.json",
##       "use_ruby": true,
##       "ruby_init_script":[ ...
##          "require './SampleRubyColor.rb'",
##          ...],
##       ...
## この例では、+SampleRubyColor+ が、ユーザが定義したクラスであり、
## "+SampleRubyColor.rb+" にそのプログラムが格納されているとしている。
##
## 以下は、+SampleRubyColor+ の例である。
##
## <B>SampleFactory.rb</B>
##    require 'RubyColorBase.rb' ;
##    class SampleRubyColor < RubyColorBase
##
##      def initialize()
##        super ;
##      end
##
##      def getAgentColorRGB(agent)
##        pp [:agent, getAgentPos(agent)] ;
##        pos = getAgentPos(agent) ;
##        intPos = [(pos[0] / 4.0).to_i, (pos[1] / 4.0).to_i, pos[2].to_i] ;
##        return intPos ;
##      end
##    end # class SampleRubyColor

class RubyColorBase
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の Agent オブジェクト
#  attr_accessor :javaFactory ;

  #--------------------------------------------------------------
  #++
  ## 初期化
  def initialize()
  end

  #--------------------------------------------------------------
  #++
  ## RGBで色を返す。
  def getAgentColorRGB(agent)
    return [0, 255, 0] ;
  end

  #--------------------------------------------------------------
  #++
  ## get link (MapLink) where the agent exist.
  def getAgentLink(agent)
    return agent.getCurrentLink() ;
  end
  
  #--------------------------------------------------------------
  #++
  ## get pos [x,y,z] where the agent exist.
  def getAgentPos(agent)
    place = agent.getCurrentPlace() ;
    posXY = place.getPosition() ;
    posZ = place.getHeight() ;
    pos = [posXY.getX(), posXY.getY(), posZ] ;
    return pos ;
  end
  
  
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------

end # class RubyAgentBase

