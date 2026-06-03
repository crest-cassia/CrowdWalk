#! /usr/bin/env ruby
## -*- mode: ruby -*-

require 'Stat/RandomValue' ;

module Stat
  ##======================================================================
  class Binomial < RandomValue
    attr_accessor :p ;
    attr_accessor :n ;

    def initialize(n, p = 0.5)
      @n = n ;
      @p = p ;
    end

    def density(k)
      Binomial.density(k,@n,@p) ;
    end

    def value()
      rand()
    end

    def rand()
      return Binomial.rand(@n,@p) ;
    end

    def nrand()  ## normalized random
      return Binomial.nrand(@n,@p) ;
    end

    def wrand(a,b)  ## weighted random = a * k + b * (n-k)
      k = self.rand() ;
      return a * k + b * (@n - k)
    end

    def mean()
      return @n * @p ;
    end

    def var() ## ???
      return @n * @p * (1.0 - @p) ;
    end

    def std() 
      Math::sqrt(var()) ;
    end

    def to_s()
      ("#Binomial[n=%d, p=%f]" % [@n, @p]) ;
    end
  end

  ##========================================
  class << Binomial
    ##--------------------------------------------------
    def density(k, n, p)
      return Stat.comb(n,k) * (p ** k) * ((1-p) ** (n-k))
    end

    ##--------------------------------------------------
    def rand(n,p)
      k = 0 ;
      (0...n).each{ k += 1 if Kernel::rand() < p ; }
      return k ;
    end

    ##--------------------------------------------------
    def nrand(n,p) ## normalized random
      return Float(rand(n,p)) / Float(n) ;
    end
  end

  ##------------------------------------------------------------
  ## utility
  def fact(n, base=2)
    f = 1 ;
    (base..n).each { |k| f *= k }
    return f ;
  end

  def comb(n,k)
    if(k > n-k)
      return (fact(n,k+1) / fact(n-k))
    else
      return (fact(n,n-k+1) / fact(k))
    end
  end

  extend Stat ;
  
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
    p = 0.1 ;

    binList = [] ;
    [3,10,30,100,300,1000].each{|n|
      bin = Binomial.new(n,p) ;
      p([bin,bin.mean()/bin.n,bin.std()/bin.n]) ;
      binList.push(bin) ;
    }

    Gnuplot::directMultiPlot(binList){|gplot|
      binList.each{|bin|
        (0..bin.n()).each{|k|
          d = bin.density(k) ;
          gplot.dmpXYPlot(bin, k.to_f/bin.n.to_f , d) ;
        }
      }
    }
  end

  ##------------------------------------------------------------
  ## test Histgram and Gaussian.rand
  def test1()
    p = 0.1 ;
    n = 100 ;
    bin = Binomial.new(n, p) ;

    hist = Histgram.new(1,true)
    
    n = 100000 ;
    (0...n).each{|i|
      v = bin.rand() ;
      hist.put(v) ;
    }

    p([hist.average(), hist.sdiv()]) ;
  
    Gnuplot::directPlot("[][0:]","w boxes"){|gplot|
      hist.plot(gplot, :percent) ;
    }
  end

  ##------------------------------------------------------------
  ## test Histgram and Gaussian.rand
  def test2()
    p = 0.1 ;
    n = 100 ;
    bin = Binomial.new(n, p) ;

    hist = Histgram.new(0.01,true)
    
    n = 100000 ;
    (0...n).each{|i|
      v = bin.nrand() ;
      hist.put(v) ;
    }

    p([hist.average(), hist.sdiv()]) ;
  
    Gnuplot::directPlot("[][0:]","w boxes"){|gplot|
      hist.plot(gplot, :percent) ;
    }
  end

  ##======================================================================
  ##======================================================================
  ##======================================================================
  #test0() ;
  #test1() ;
  test2() ;

end


