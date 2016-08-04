#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Statistics Utility
## Author:: Itsuki Noda
## Version:: 0.0 2010/??/?? I.Noda
##
## === History
## * [2010/??/??]: Create This File.
## * [2014/08/24]: reform
## * [2014/08/29]: add exponential moving average
## == Usage
## * ...

#--======================================================================
#++
module Stat

  #--======================================================================
  #++
  ## Cumulate statical infomation and calculate average, variance and
  ## standard diviation.
  ## It also record history.

  class StatInfo
    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## minimum in given values.
    attr_accessor :min ;
    ## maximum in given values.
    attr_accessor :max ;
    ## sum of given values.
    attr_accessor :sum ;
    ## squared sum of given values.
    attr_accessor :qsum ;
    ## the number of given values.
    attr_accessor :countN ;
    ## history of given values.
    attr_accessor :history ;
    ## history maximum size.
    attr_accessor :historySize ;

    ## stepsize (alpha) for exponential moving average
    attr_accessor :alpha ;
    ## exponential moving average
    attr_accessor :ema ;

    #--------------------------------------------------------------
    #++
    def initialize(historySize = 1, alpha = 0.01)
      # historySize = 0 means infinite history

      @sum = 0.0 ;
      @qsum = 0.0 ;
      @countN = 0 ;

      @min = nil ;
      @max = nil ;

      @historySize = historySize ;
      @history = Array.new(@historySize) ;

      @alpha = alpha ;
      @ema = 0.0 ;
    end

    #--------------------------------------------------------------
    #++
    def put(value)
      @min = value if (@min.nil? || value < @min) ;
      @max = value if (@max.nil? || value > @max) ;

      @sum += value ;
      @qsum += value * value ;

      if(@historySize > 0)
        @history[@countN % @history.size] = value ;
      else
        @history.push(value) ;
      end

      @ema = ((@countN == 0) ? 
              value : 
              ((1.0-@alpha) * @ema + @alpha * value)) ;

      @countN += 1 ;
    end

    #--------------------------------------------------------------
    #++
    ## == access to history

    #------------------------------------------
    #++
    def last(n = 1)
      return @history[-n] ;
    end

    #------------------------------------------
    #++
    def nth(n)
      return @history[n] ;
    end

    #--------------------------------------------------------------
    #++
    ## == calc stat info

    #------------------------------------------
    #++
    def average()
      if(@countN > 0) then
        return @sum / @countN ;
      else
        return 0.0 ;
      end
    end

    #----------------------
    #++
    alias ave average ;

    #------------------------------------------
    #++
    def variance()
      if(@countN > 0 && @min < @max) then
        ave = average() ;
        return @qsum / @countN - ave * ave ;
      else
        return 0.0 ;
      end
    end

    #----------------------
    #++
    alias var variance ;

    #------------------------------------------
    #++
    def sdev()
      return Math::sqrt(variance()) ;
    end

    #----------------------
    #++
    alias sdiv sdev ;  # for backward compatibility.
    #----------------------
    #++
    alias std sdev ;

    #--------------------------------------------------------------
    #++
    ## convert to Hash
    def to_h()
      return ({
                :average => average(),
                :variance => variance(),
                :sdev => sdev(),
                :n => @countN,
                :min => @min,
                :max => @max,
              }) ;
    end

  end # class StatInfo

  #--======================================================================
  #++
  ## Correlation2.
  ## cumulate correlation information of two variables

  class Correlation2
    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## statistics of X values
    attr_accessor :xStat ;
    ## statistics of Y values
    attr_accessor :yStat ;
    ## statistics of X-Y values
    attr_accessor :xyStat ;

    #--------------------------------------------------------------
    #++
    def initialize()
      @xStat = StatInfo.new()
      @yStat = StatInfo.new() ;
      @xyStat = StatInfo.new() ;
    end

    #--------------------------------------------------------------
    #++
    def put(xVal, yVal)
      @xStat.put(xVal) ;
      @yStat.put(yVal) ;
      @xyStat.put(xVal * yVal) ;
    end

    #--------------------------------------------------------------
    #++
    def covariance()
      return @xyStat.average() - @xStat.average() * @yStat.average() ;
    end

    #--------------------------------------------------------------
    #++
    def correlation()
      return covariance() / (@xStat.sdiv() * @yStat.sdiv()) ;
    end

  end # class Correlation2
    
  #--======================================================================
  #++
  ## cumulate correlation information of two arrays.

  class TwoArrayStatInfo
    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## size of X vectors
    attr_accessor :sizeX ;
    ## size of Y vectors
    attr_accessor :sizeY ;
    ## array of statistics info for X
    attr_accessor :statInfoX ;
    ## array of statistics info for Y
    attr_accessor :statInfoY ;
    ## squared sum matrix
    attr_accessor :qMatrix ;
    ## counting
    attr_accessor :count ;
    ## average array for X
    attr_accessor :averageX ;
    ## average array for Y
    attr_accessor :averageY ;
    ## covariance of X-Y
    attr_accessor :covariance ;

    #--------------------------------------------------------------
    #++
    def initialize(sizeY, sizeX, historySize = 1)
      setup(sizeY, sizeX, historySize) ;
    end

    #--------------------------------------------------------------
    #++
    def setup(sizeY, sizeX, historySize) ;
      @sizeX = sizeX ;
      @sizeY = sizeY ;
      @historySize = historySize ;

      @statInfoX = Array.new(@sizeX){ StatInfo.new(historySize) ; }
      @statInfoY = Array.new(@sizeY){ StatInfo.new(historySize) ; }
      @qMatrix = Array.new(@sizeY){ Array.new(@sizeX,0.0) ; } ;
      @count = 0 ;
    end

    #--------------------------------------------------------------
    #++
    def put(valueY, valueX)
      (0...@sizeY).each{|i|
        @statInfoY[i].put(valueY[i]) ;
        (0...@sizeX).each{|j|
          @statInfoX[j].put(valueX[j]) if(i == 0) ;
          @qMatrix[i][j] += valueY[i] * valueX[j] ;
        }
      }
      @averageX = nil ;
      @averageY = nil ;
      @covariance = nil ;
      @count += 1 ;
    end

    #--------------------------------------------------------------
    #++
    def averageX()
      if(@averageX.nil?) then
        @averageX = Array.new(@sizeX) ;
        (0...@sizeX).each{|i|
          @averageX[i] = @statInfoX[i].average() ;
        }
      end
      return @averageX ;
    end

    #--------------------------------------------------------------
    #++
    def averageY()
      if(@averageY.nil?) then
        @averageY = Array.new(@sizeY) ;
        (0...@sizeY).each{|i|
          @averageY[i] = @statInfoY[i].average() ;
        }
      end
      return @averageY ;
    end

    #--------------------------------------------------------------
    #++
    def covariance() 
      if(@covariance.nil?) then
        aveX = averageX() ;
        aveY = averageY() ;
        @covariance = Array.new(@sizeY){ Array.new(@sizeX,0.0) }  ;
        (0...@sizeY).each{|i|
          (0...@sizeX).each{|j|
            @covariance[i][j] = @qMatrix[i][j]/@count.to_f - aveY[i] * aveX[j] ;
          }
        }
      end
      return @covariance ;
    end

  end # class TwoArrayStatInfo

  #--======================================================================
  #++
  ## cumulate correlation information of two variables

  class ArrayStatInfo < TwoArrayStatInfo
    #--------------------------------------------------------------
    #++
    def initialize(size, historySize = 1)
      setup(size, size, historySize) ;
    end

    #--------------------------------------------------------------
    #++
    def put(value)
      super(value, value) ;
    end

    #--------------------------------------------------------------
    #++
    def averageY()
      averageX() ;
      @averageY = @averageX ;
    end

    #--------------------------------------------------------------
    #++
    def average()
      averageX() ;
    end


  end

  #--======================================================================
  #++
  ## cumulate correlation information of two variables

  class ArrayStatInfo_old
    attr_accessor :size ;
    attr_accessor :statInfo ;
    attr_accessor :qMatrix ;
    attr_accessor :count ;

    attr_accessor :average ;
    attr_accessor :covariance ;

    ##--------------------
    def initialize(size, historySize = 1)
      setup(size, historySize) ;
    end

    ##--------------------
    def setup(size, historySize) ;
      @size = size ;
      @historySize = historySize ;

      @statInfo = Array.new(@size){ StatInfo.new(historySize) ; }
      @qMatrix = Array.new(@size){ Array.new(@size,0.0) ; } ;
      @count = 0 ;
    end

    ##--------------------
    def put(value)
      (0...@size).each{|i|
        @statInfo[i].put(value[i]) ;
        (0...@size).each{|j|
          @qMatrix[i][j] += value[i] * value[j] ;
        }
      }
      @count += 1 ;
      @average = nil ;
      @covariance = nil ;
    end

    ##--------------------
    def average()
      if(@average.nil?) then
        @average = Array.new(@size) ;
        (0...@size).each{|i|
          @average[i] = @statInfo[i].average() ;
        }
      end
      return @average ;
    end

    ##--------------------
    def covariance() 
      if(@covariance.nil?) then
        ave = average() ;
        @covariance = Array.new(@size){ Array.new(@size,0.0) }  ;
        (0...@size).each{|i|
          (0...@size).each{|j|
            @covariance[i][j] = @qMatrix[i][j]/@count.to_f - ave[i] * ave[j] ;
          }
        }
      end
      return @covariance ;
    end

  end # class ArrayStatInfo_old

end # module Stat

########################################################################
########################################################################
########################################################################
if($0 == __FILE__) then

  require 'test/unit'

  #--============================================================
  #++
  ## unit test for this file.
  class TC_StatInfo < Test::Unit::TestCase
    #--::::::::::::::::::::::::::::::::::::::::::::::::::
    #++
    ## desc. for TestData
    TestData = nil ;

    #----------------------------------------------------
    #++
    ## show separator and title of the test.
    def setup
#      puts ('*' * 5) + ' ' + [:run, name].inspect + ' ' + ('*' * 5) ;
      name = "#{(@method_name||@__name__)}(#{self.class.name})" ;
      puts ('*' * 5) + ' ' + [:run, name].inspect + ' ' + ('*' * 5) ;
      super
    end

    #----------------------------------------------------
    #++
    ## about test_a
    def test_a
      p [:test_a] ;
    end

  end # class TC_StatInfo
end # if($0 == __FILE__)

