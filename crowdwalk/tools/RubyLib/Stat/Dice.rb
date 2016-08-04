#! /usr/bin/env ruby
## -*- Mode: ruby -*-
##Header:
##Title: Dice: Uniform Integer Random Number Class
##Author: Itsuki Noda
##Date: 2010/08/29
##EndHeader:

require 'Stat/RandomValue.rb' ;
##======================================================================
module Stat
  class Dice < RandomValue 
    attr_accessor :mode ;  ## :integer | :symbol
    attr_accessor :min ;
    attr_accessor :max ;
    attr_accessor :symbolList ;

    ##------------------------------
    def initialize(arg0, arg1 = nil)
      @mode = :integer ;
      if(arg1.nil?)
        if(arg0.is_a?(Array))
          @mode = :symbol ;
          @min = 0 ;
          @max = arg0.size ;
          @symbolList = arg0 ;
        else
          @min = 0 ;
          @max = arg0 ;
        end
      else
        @min = arg0 ;
        @max = arg1 ;
      end
    end

    ##------------------------------
    def value()
      case(@mode)
      when :integer
        return rand((@max - @min).to_i) + @min ;
      when :symbol
        i = rand((@max - @min).to_i) + @min ;
        return @symbolList[i] ;
      else
        raise "unsupported mode for Stat::Dice : " + @mode.to_s ;
      end
    end

    ##------------------------------
    def to_s()
      case(@mode)
      when :integer ;
        return "#<Stat::Dice:[#{@min}:#{@max}]>" ;
      when :symbol ;
        return "#<Stat::Dice:[#{@symbolList.intern}]>" ;
      end
    end
  end

end
