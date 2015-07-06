#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = With Configuration Utility
## Author:: Itsuki Noda
## Version:: 0.0 2010/??/?? I.Noda
##
## === History
## * [2010/??/??]: Create This File.
## * [2014/08/01]: reform and add access method to get Conf from class
## == Usage
## * ...

## WithConfParam library

#--======================================================================
#++
## Meta class for configuration facility.
class WithConfParam

  #--::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## default configuration.
  DefaultConf = { nil => nil } ;
  ## default value if missing key
  DefaultValue = nil ;
  ## list of attributes directly set by conf
  DirectConfAttrList = [] ;
  
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## store configuration for each instance
  attr :conf, true ;
  
  #----------------------------------------------------
  #++
  ## class method to access default config
  ## _key_:: key of the configuration
  ## *return*:: the value for the _key_.
  def self.getConf(key)
    if(self::DefaultConf.has_key?(key))
      return self::DefaultConf[key] ;
    elsif(self == WithConfParam) then
      return nil ;
    else
      return self.superclass().getConf(key) ;
    end
  end

  #----------------------------------------------------
  #++
  ## initialize with configuration
  ## _conf_:: configuration for the instance
  def initialize(conf = {}) 
    setPiledConf(conf) ;
    setDirectConfAttrList() ;
  end

  #----------------------------------------------------
  #++
  ## generate instance configuration including class default configurations
  ## _conf_:: configuration for the instance
  def setPiledConf(conf) 
    @conf = genPiledConf(conf) ;
  end

  #----------------------------------------------------
  #++
  ## generate configuration with class default configurations
  ## _conf_:: configuration for the instance
  ## *return*:: the configuration table for the instance
  def genPiledConf(conf = {})
    return genPiledDefaultConf().update(conf) ;
  end

  #----------------------------------------------------
  #++
  ## generate class default configurations recursively
  ## _klass_:: the class now processing
  ## *return*:: default configuration table for the instance
  def genPiledDefaultConf(klass = self.class())
    if(klass == WithConfParam) then
      return klass::DefaultConf.dup() ;
    else
      newConf = genPiledDefaultConf(klass.superclass()) ;
      if(klass.const_defined?(:DefaultConf)) 
        newConf.update(klass::DefaultConf) ;
      end
      
      return newConf ;
    end
  end

  #----------------------------------------------------
  #++
  ## set attribute value by config directly/automatically
  ## _klass_:: the class now processing
  ## *return*:: ?
  def setDirectConfAttrList(klass = self.class())
    setDirectConfAttrList(klass.superclass()) if(klass != WithConfParam) ;
    if(klass.const_defined?(:DirectConfAttrList)) then
      setDirectConfAttr(klass::DirectConfAttrList) ;
    end
  end

  #----------------------------------------------------
  #++
  ## set attribute value by the same name config directly/automatically
  ## _attr_:: attribute name or list of attributes
  ## *return*:: the value
  def setDirectConfAttr(attr)
    if(attr.is_a?(Array)) then
      return attr.map{|at| 
        setDirectConfAttr(at) ;
      }
    else
      return self.instance_variable_set("@#{attr}", self.getConf(attr)) ;
    end
  end

  #----------------------------------------------------
  #++
  ## set configuration value
  ## _key_:: key of the configuration
  ## _value_:: value of the configuration
  def setConf(key, value)
    @conf[key] = value ;
  end

  #----------------------------------------------------
  #++
  ## get configuration value
  ## _key_:: key of the configuration
  ## _defaultValue_:: default value for missing key
  ## _conf_:: temporal configuration
  ## *return*:: the value for the _key_.
  def getConf(key, defaultValue = DefaultValue, conf = @conf)
    if (conf.key?(key)) then
      return conf[key] ;
    elsif(conf != @conf && @conf.key?(key)) then
      return @conf[key] ;
    else
      return defaultValue ;
    end
  end

end ## class WithConfParam


########################################################################
########################################################################
########################################################################
if($0 == __FILE__) then

  require 'test/unit'

  #--============================================================
  #++
  ## unit test for this file.
  class TC_WithConfParam < Test::Unit::TestCase
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
    ## basic check
    class FooA < WithConfParam
      DefaultConf = { :x => 1, :z => 0 } ;
      DirectConfAttrList = [:z] ;
    end

    class BarA < FooA
      DefaultConf = { :y => 2 } ;
    end

    class CooA < BarA
      DefaultConf = { :x => 3 } ;
      DirectConfAttrList = [:x] ;
    end

    def test_a
      f0 = FooA.new() ;
      b0 = BarA.new() ;
      c0 = CooA.new() ;
      c1 = CooA.new({:y => 4}) ;

      p [:f0, :x, f0.getConf(:x)] ;
      p [:f0, :y, f0.getConf(:y)] ;
      p [:b0, :x, b0.getConf(:x)] ;
      p [:b0, :y, b0.getConf(:y)] ;
      p [:c0, :x, c0.getConf(:x)] ;
      p [:c0, :y, c0.getConf(:y)] ;
      p [:c1, :x, c1.getConf(:x)] ;
      p [:c1, :y, c1.getConf(:y)] ;

    end

    #----------------------------------------------------
    #++
    ## getConf for the class
    def test_b
      p [FooA, :x, FooA.getConf(:x)] ;
      p [FooA, :y, FooA.getConf(:y)] ;
      p [BarA, :x, BarA.getConf(:x)] ;
      p [BarA, :y, BarA.getConf(:y)] ;
      p [CooA, :x, CooA.getConf(:x)] ;
      p [CooA, :y, CooA.getConf(:y)] ;
      p [CooA, :z, CooA.getConf(:z)] ;
    end

    #----------------------------------------------------
    #++
    ## getConf for the class
    def test_c
      f0 = FooA.new() ;
      b0 = BarA.new() ;
      c0 = CooA.new() ;
      c1 = CooA.new({:z => 4}) ;

      p [:f0, f0] ;
      p [:b0, b0] ;
      p [:c0, c0] ;
      p [:c1, c1] ;
    end

  end # class TC_Foo < Test::Unit::TestCase
end # if($0 == __FILE__)
