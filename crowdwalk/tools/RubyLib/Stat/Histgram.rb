#! /usr/bin/env ruby
## -*- mode: ruby -*-

require 'Stat/StatInfo.rb' ;

module Stat

  ##======================================================================
=begin

== Histgram

   * Cumulate histgram infomation

=end

  class Histgram < StatInfo

    attr_accessor :step ;
    attr_accessor :table ;
    attr_accessor :bottom ;
    attr_accessor :centered ;

    ##--------------------------------------------------
    def initialize(step, centered = false, historySize = 1) ;
      super(historySize) ;

      @step = step ;
      @centered = centered ;
      @table = [] ;
      @bottom = nil ;
    end
    
    ##--------------------------------------------------
    ## index, offset, boundary and center values of steps

    ##----------
    def offset()
      (@centered ? @step/2.0 : 0.0) ;
    end

    ##----------
    def index(value, bottom = @bottom)
#      Integer((value + offset() - bottom) / @step) ;
      Integer((value - bottom) / @step) ;
    end

    ##----------
    def maxIndex()
      @table.length() ;
    end

    ##----------
    def stepBoundary(index, bottom=@bottom)
      bottom + @step * index ;
    end

    ##----------
    def stepCenter(index, bottom=@bottom)
      stepBoundary(index,bottom) + @step/2.0 ;
    end

    ##----------
    def stepMajorValue(index, bottom=@bottom)
      if(@centered)
        return stepCenter(index, bottom) ;
      else
        return stepBoundary(index, bottom) ;
      end
    end

    ##--------------------------------------------------
    ## get frequency values

    ##----------
    def freqByIndex(index)
      @table[index] || 0 ;
    end

    ##----------
    def freqByValue(value)
      freqByIndex(index(value)) ;
    end

    ##--------------------------------------------------
    ## bottom managemenr

    ##----------
    def setBottom(value)
      k = index(value, -offset()) ;
      k = k - 1 if (value < stepBoundary(k, -offset())) ;
      @bottom = stepBoundary(k, -offset()) ;
    end

    ##----------
    def reviseBottom(value)
      oldBottom = @bottom ;
      setBottom(value) ;
      diff = index(oldBottom) ;
      (0...diff).each{ @table.unshift(nil) }
    end

    ##--------------------------------------------------
    ## main facility to put actual values to stat info

    ##----------
    def put(value)
      setBottom(value) if (@bottom.nil?) ;

      reviseBottom(value) if(value < @bottom)  ;

      incFreqByIndex(index(value)) ;

      super(value) ;
      
    end

    ##----------
    def incFreqByIndex(index)
      @table[index] = freqByIndex(index) + 1 ;
    end

    ##----------
    def total()
      sum = 0 ;
      (0...maxIndex()).each{|i|
        sum += freqByIndex(i) ;
      }
      sum ;
    end

    ##--------------------------------------------------
    ## make array
    def to_a()
      array = [] ;
      (0...maxIndex()).each{|i|
        array.push([stepMajorValue(i),freqByIndex(i)]) ;
      }
      array ;
    end

    ##--------------------------------------------------
    ## gnuplot
    def plot(gplot = nil, normalized = false)

      if (Object::const_defined?("Gnuplot"))
        case(normalized)
        when(false)
          sum = 1.0 ;
        when(:prob)
          sum = total().to_f ;
        when(true)
          sum = total().to_f ;
        when(:percent)
          sum= total().to_f / 100 ;
        when(:density)
          sum = total().to_f * @step ;
        else
          raise "unknown normalize type: " + normalized ;
        end

        flist = to_a();

        if(gplot.nil?)
          Gnuplot::directMultiPlot([:histgram]){|gplot|
            flist.each{|r|
              gplot.dmpXYPlot(:histgram,r[0], r[1]/sum) ;
            }
          }
        else
          flist.each{|r|
            gplot.dpXYPlot(r[0], r[1]/sum) ;
          }
        end

      else
        raise "Gnuplot is not included." 
      end
    end

  end

end
