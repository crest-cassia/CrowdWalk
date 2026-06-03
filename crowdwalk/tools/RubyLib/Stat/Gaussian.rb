#! /usr/bin/env ruby
# coding: euc-jp
## -*- mode: ruby -*-

require 'Stat/RandomValue' ;

module Stat
  ##======================================================================
  class Gaussian < RandomValue
    attr_accessor :mean ;
    attr_accessor :std ;

    ##--------------------------------------------------
    def initialize(mean = 0.0, std = 1.0)
      @mean = mean ;
      @std = std ;
    end

    ##--------------------------------------------------
    def density(x)
      return Gaussian.density(x,@mean, @std) ;
    end

    ##--------------------------------------------------
    def rand()
      return Gaussian.rand(@mean, @std) ;
    end

    ##--------------------------------------------------
    def value()
      rand()
    end

    ##--------------------------------------------------
    def to_s()
      ("#Gaussian[m=%f, s=%f]" % [@mean, @std]) ;
    end
  end

  ##========================================
  class << Gaussian
    ##--------------------------------------------------
    ConstK = Math::sqrt(2.0 * Math::PI) ;

    ##--------------------------------------------------
    def density(x, mean, std)
      dx = (x - mean)/std ;
      return (1.0 / ( ConstK  * std)) * Math::exp(-(dx * dx)/2.0) ;
    end

    ##--------------------------------------------------
    def rand(mean, std)
      (x,y) = randBoxMuller() ;
      return mean + std * x ;
    end

    ##--------------------------------------------------
    ## Box-Muller 変換による、一様乱数から正規乱数への変換。
    ## mean=0, std=1 の乱数のペアを作ってくれる。
    def randBoxMuller()
      a = Kernel::rand() ;
      b = Kernel::rand() ;
      lna = Math::sqrt(-2 * Math::log(a)) ;
      pib = 2 * Math::PI * b ;
      x = lna * Math::sin(pib) ;
      y = lna * Math::cos(pib) ;
      return x,y ;
    end

  end

end

##============================================================
## for test
##============================================================
if($0 == __FILE__)
  $LOAD_PATH.push('~/lib/ruby') ;
  require 'gnuplot.rb' ;
  require 'StatInfo.rb' ;
  require 'Histgram.rb' ;

  include Stat ;

  ##------------------------------------------------------------
  ## check shape of Gaussian function
  def test0()
    m = 0.0 ;
    s = 1.0 ;
    gdist = Gaussian.new(m, s) ;

    n = 1000 ;
    k = 4.0 ;
    r = k * s ;
    d = 2 * r / n ;
  
    Gnuplot::directPlot(){|gplot|
      (0...n).each{|i|
        v = -r + i * d ;
        gplot.dpXYPlot(v, gdist.density(v)) ;
      }
    }
  end

  ##------------------------------------------------------------
  ## test Histgram and Gaussian.rand
  def test1()
    m = 0.0 ;
    s = 1.0 ;
    gdist = Gaussian.new(m, s) ;

    hist = Histgram.new(s/5.0,true) ;
#    hist = Histgram.new(s/5.0,false) ;
    
    n = 100000 ;
    (0...n).each{|i|
      v = gdist.rand() ;
#      v = 10.0 * (rand()) - 5.0 ;
      hist.put(v) ;
    }

    p([hist.average(), hist.sdiv()]) ;
  
    Gnuplot::directPlot("[][0:]","w boxes"){|gplot|
#      hist.plot(gplot, true) ;
#      hist.plot(gplot, false) ;
#      hist.plot(gplot, :density) ;
      hist.plot(gplot, :percent) ;
    }
  end

  ##------------------------------------------------------------
  ## calculate one-side average of Gaussian 
  def test2()
    m = 0.0 ;
    s = 1.0 ;
    gdist = Gaussian.new(m,s) ;

    n = 100000 ;
    k = 10.0 ;
    r = k * s ;
    d = r / n ;

    xsum = 0.0 ;
    nsum = 0.0 ;
    
    (0...n).each{|i|
      x = i * d ;
      y = gdist.density(x) ;
      xsum += x * y * d ;
      nsum += y * d ;
    }

    xave = xsum / nsum ;

    p([xave, 2 * s/Math::sqrt(Math::PI * 2)]) ;

  end

  ##======================================================================
  #test0() ;
  test1() ;
  #test2() ;

end

