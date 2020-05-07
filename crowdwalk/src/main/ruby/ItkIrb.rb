#! /usr/bin/env ruby
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
## Irb Utility module for CrowdWalk
module ItkIrb
  extend ItkIrb ;
  #--------------------------------------------------------------
  #++
  ## get CrowdWalk simulator entity
  def getSimulator()
    return $crowdwalk ;
  end

  #--------------------------------------------------------------
  #++
  ## run N cycle.
  def runCycle(_n = 1)
    getSimulator().irbWaitCycleN(_n) ;
  end

  #--////////////////////////////////////////////////////////////
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------
end # module ItkIrb

