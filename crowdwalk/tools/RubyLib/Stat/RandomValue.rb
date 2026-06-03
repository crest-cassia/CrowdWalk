#! /usr/bin/env ruby
## -*- Mode: ruby -*-
##Header:
##Title: Random Value Base
##Author: Itsuki Noda
##Date: 2006/06/21
##EndHeader:

##--======================================================================
module Stat
  #--======================================================================
  #++
  ## RandomValue class
  class RandomValue
    #--------------------------------------------------------------
    #++
    ## get random value
    def value()
      raise "RandomValue\$value() is not implemented for : " + self.class.name ;
    end

    #--------------------------------------------------------------
    #++
    ## string expression
    def to_s()
      "\#<#{self.class.name()}>" ;
    end

  end

end

#--======================================================================
#++
## Override Build-in Numeric class
class Numeric
  #--------------------------------------------------------------
  #++
  ## for the compatibility with RandomValue
  def value()
    self
  end
end


