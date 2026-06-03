#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Gaussian Mixture
## Author:: Itsuki Noda
## Version:: 0.0 2016/03/18 I.Noda
##
## === History
## * [2016/03/18]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'Stat/Gaussian.rb' ;
require 'pp' ;

#--======================================================================
module Stat
  
  #--============================================================
  #++
  ## Gaussian Mixture Density Function 
  class GaussianMixture < RandomValue
    
    #--========================================
    #++
    ## Weighted Gaussian
    class WeightedGaussian < Gaussian
      #--::::::::::::::::::::::::::::::
      #++
      ## weight
      attr_accessor :weight ;

      #--------------------------------
      #++
      ## constractor
      def initialize(weight = 1.0, mean = 0.0, std = 1.0)
        super(mean, std) ;
        @weight = weight ;
      end

      #-------------------------------
      #++
      ## weighted density
      def weightedDensity(x)
        return @weight * density(x) ;
      end

      #------------------------------
      #++
      ## get string
      def to_s()
        ("#WG[w=%f, m=%f, s=%f]" % [@weight, @mean, @std]) ;
      end

    end ## class WeightedGaussian

    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## Weighted Gaussian List
    attr_accessor :distList ;
    ## epsilon for adjust weight
    attr_accessor :eps ;

    #----------------------------------------------------
    #++
    ## constractor
    ## _param_:: parameters
    ##           if _param_ is integer, generate N weighted gaussian.
    ##           if _param_ is array, each element of the array should be
    ##           a list [weight, mean, std]
    def initialize(param)
      setup(param) ;
      @eps = 0.01 ;
    end

    #----------------------------------------------------
    #++
    ## setup
    ## _param_:: parameters
    ##           if _param_ is integer, generate N weighted gaussian.
    ##           if _param_ is array, each element of the array should be
    ##           a list [weight, mean, std]
    def setup(param)
      @distList = [] ;
      if(param.is_a?(Array)) then
        param.each{|p| ## p should be [weight, mean, std]
          (weight, mean, std) = p ;
          @distList.push(WeightedGaussian.new(weight, mean, std)) ;
        }
      elsif(param.is_a?(Fixnum)) then
        (0...param).each{|i|
          @distList.push(WeightedGaussian.new()) ;
        }
      else
        raise ("param should be an integer or array of [weight, mean, std]:" +
               "param=" + param.inspect) ;
      end
      normalize() ;
    end

    #----------------------------------------------------
    #++
    ## normalize.
    ## make sum of weights to be 1.0
    def normalize()
      return if(@distList.size() == 0) ;
      weightSum = 0.0 ;
      @distList.each{|wg|
        weightSum += wg.weight ;
      }
      if(weightSum > 0.0) then
        @distList.each{|wg|
          wg.weight /= weightSum ;
        }
      else
        raise "sum of weight should be positive.  weight sum =" + weightSum.to_s ;
      end
    end
    
    #----------------------------------------------------
    #++
    ## get density of x
    def density(x)
      v = 0.0 ;
      @distList.each{|wg|
        v += wg.weightedDensity(x) ;
      }
      return v ;
    end

    #----------------------------------------------------
    #++
    ## get random value
    def rand()
      v = Kernel::rand() ;
      @distList.each{|wg|
        v -= wg.weight ;
        if(v <= 0.0) then
          return wg.rand() ;
        end
      }
      return distList.last.rand() ;
    end

    #----------------------------------------------------
    #++
    ## get random value
    def value()
      return rand() ;
    end
    
    #----------------------------------------------------
    #++
    ## adjust by data
    def adjust(data, n = 10, m = 10)
      adjustByQuasiEM(data, n, m)
    end
    
    #----------------------------------------------------
    #++
    ## adjust by data using quasi EM algorithm
    ## n
    def adjustByQuasiEM(data, n = 10, m = 10)
      (0...n).each{
        adjustByQuasiEM_cycle(data, m) ;
      }
    end

    #----------------------------------------------------
    #++
    ## adjust by data using quasi EM algorithm
    ## n
    def adjustByQuasiEM_cycle(data, m = 10)
      # E-step
      gMatrix = getDensityElementForDataList(data) ;
      hArray = getDensityForDataList(data) ;
      # M-step
      adjustByQuasiEM_weight(data, gMatrix, hArray, m) ;
      adjustByQuasiEM_meanStd(data, gMatrix, hArray) ;
    end
    
    #----------------------------------------------------
    #++
    ## gaussian values
    def getDensityElementForDataList(data)
      valueMatrix = [] ;
      data.each{|dt|
        valueArray = [] ;
        @distList.each{|dist|
          valueArray.push(dist.density(dt)) ;
        }
        valueMatrix.push(valueArray) ;
      }
      return valueMatrix ;
    end
        
    #----------------------------------------------------
    #++
    ## gaussian values
    def getDensityForDataList(data)
      valueArray = [] ;
      data.each{|dt|
        valueArray.push(density(dt)) ;
      }
      return valueArray ;
    end

    #----------------------------------------------------
    #++
    ## adjust by data using quasi EM algorithm (weight)
    def adjustByQuasiEM_weight(data, gMatrix, hArray, n)
      jMax = @distList.size ;
      kMax = data.size
      eps = @eps / kMax.to_f ;
      (0...n).each{|l|
        (0...jMax).each{|j|
          w = @distList[j].weight ;
          dw = 0.0 ;
          (0...kMax).each{|k|
            f = gMatrix[k][j] / hArray[k] ;
            dw += (f - 1.0) ;
          }
          w += dw * eps ;
          w = eps  if(w <= 0.0) ;
          @distList[j].weight = w ;
        }
      }
      normalize() ;
    end

    #----------------------------------------------------
    #++
    ## adjust by data using quasi EM algorithm (weight)
    def adjustByQuasiEM_meanStd(data, gMatrix, hArray)
      jMax = @distList.size ;
      kMax = data.size
      (0...jMax).each{|j|
        mSum = 0.0 ;
        vSum = 0.0 ;
        fSum = 0.0 ;
        m = @distList[j].mean ;
        (0...kMax).each{|k|
          f = gMatrix[k][j] / hArray[k] ;
          x = data[k]
          fSum += f ;
          mSum += f * x ;
          vSum += f * (x - m)**2 ;
        }
        @distList[j].mean = mSum / fSum ;
        @distList[j].std = Math::sqrt(vSum/fSum) ;
      }
    end

    #----------------------------------------------------
    #++
    ## duplicate
    def dup()
      newGMix = GaussianMixture.new(0) ;
      @distList.each{|g|
        newGMix.distList.push(g.dup) ;
      }
      return newGMix ;
    end
    
    #----------------------------------------------------
    #++
    ## convert to string
    ## *return*:: string
    def to_s()
      return ('#Mixture[' +
              @distList.map(){|wg| wg.to_s()}.join(",") +
              ']') ;
    end

    #--==================================================
    #--::::::::::::::::::::::::::::::::::::::::::::::::::
    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #----------------------------------------------------
  end # class GaussianMixture

end # module Stat

########################################################################
########################################################################
########################################################################
if($0 == __FILE__) then

  require 'test/unit'
  require 'gnuplot.rb' ;
  require 'Stat/Uniform.rb' ;
  require 'Stat/Histgram.rb' ;

  #--============================================================
  #++
  ## unit test for this file.
  class TC_GaussianMixture < Test::Unit::TestCase
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
    def x_test_a
      gm = Stat::GaussianMixture.new(3) ;
      puts gm ;
      pp gm ;
    end

    #----------------------------------------------------
    #++
    ## about test_b
    def x_test_b
      wGen = Stat::Uniform.new(0.1, 1.0) ;
      mGen = Stat::Uniform.new(-1.0, 1.0) ;
      sGen = Stat::Uniform.new(0.0, 0.3) ;
      param = [] ;
      (0...3).each{|i|
        param.push([wGen.value(), mGen.value(), sGen.value()]) ;
      }
      gm = Stat::GaussianMixture.new(param) ;
      puts gm ;
      pp gm ;
      Gnuplot::directMultiPlot(1){|gplot|
        min = -2.0 ;
        max = 2.0 ;
        d = 0.01 ;
        v = min ;
        while(v <= max)
          gplot.dmpXYPlot(0, v, gm.density(v)) ;
          v += d ;
        end
      }
    end

    #----------------------------------------------------
    #++
    ## about test_c
    def x_test_c
      wGen = Stat::Uniform.new(0.01, 1.0) ;
      mGen = Stat::Uniform.new(-1.5, 1.5) ;
      sGen = Stat::Uniform.new(0.05, 0.2) ;
      param = [] ;
      (0...3).each{|i|
        param.push([wGen.value(), mGen.value(), sGen.value()]) ;
      }
      gm = Stat::GaussianMixture.new(param) ;
      puts gm ;
      pp gm ;

      data = [] ;
      n = 100000 ;
      (0...n).each{|i|
        data.push(gm.value()) ;
      }
      
      hist = Stat::Histgram.new(0.02) ;
      data.each{|value|
        hist.put(value) ;
      }

      Gnuplot::directMultiPlot([:gen, :hist]){|gplot|
        min = -2.0 ;
        max = 2.0 ;
        d = 0.01 ;
        v = min ;
        while(v <= max)
          gplot.dmpXYPlot(:gen, v, gm.density(v)) ;
          v += d ;
        end

        m = hist.total().to_f * hist.step;
        hist.to_a.each{|r|
          gplot.dmpXYPlot(:hist, r[0] + hist.step/2.0, r[1]/m) ;
        }
      }
    end

    #----------------------------------------------------
    #++
    ## about test_d
    def test_d
      wGen = Stat::Uniform.new(0.01, 1.0) ;
      mGen = Stat::Uniform.new(-1.5, 1.5) ;
      sGen = Stat::Uniform.new(0.05, 0.2) ;
      paramA = [] ;
      paramB = [] ;
      (0...3).each{|i|
        paramA.push([wGen.value(), mGen.value(), sGen.value()]) ;
        paramB.push([wGen.value(), mGen.value(), sGen.value()]) ;
      }
      gmA = Stat::GaussianMixture.new(paramA) ;
      gmB = Stat::GaussianMixture.new(paramB) ;
      gmC = gmB.dup() ;

      data = [] ;
      n = 100000 ;
      (0...n).each{|i|
        data.push(gmA.value()) ;
      }
      gmB.adjust(data, 10, 20) ;
      
      Gnuplot::directMultiPlot([:a, :b, :c]){|gplot|
        min = -2.0 ;
        max = 2.0 ;
        d = 0.01 ;
        v = min ;
        while(v <= max)
          gplot.dmpXYPlot(:a, v, gmA.density(v)) ;
          gplot.dmpXYPlot(:b, v, gmB.density(v)) ;
          gplot.dmpXYPlot(:c, v, gmC.density(v)) ;
          v += d ;
        end
      }
    end

  end # class TC_GaussianMixture
end # if($0 == __FILE__)
