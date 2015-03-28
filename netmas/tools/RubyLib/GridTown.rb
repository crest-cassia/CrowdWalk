#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Grid-type Town class
## Author:: Itsuki Noda
## Version:: 0.0 2015/03/26 I.Noda
##
## === History
## * [2015/03/26]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'pp' ;

$LOAD_PATH.push(File::dirname(__FILE__)) ;

require 'MapTown.rb' ;

#--======================================================================
#++
## Town class for Map
class GridTown < MapTown
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## description of DefaultOptsions.
  DefaultConf = {
    :gridLength => 100.0,
    :sizeX => 10,
    :sizeY => 10,
    :offset => Geo2D::Point.new(0.0,0.0),
    :nodeTagFormat => "node_%02d_%02d",
    :linkTagFormat => "link_%s__%s",
    :linkWidth => 1.0,
    :randomSize => 0.0,
    nil => nil
  } ;

  #--------------------------------------------------------------
  #++
  ## initialize
  def initialize(conf = {})
    super(0, 0.0, conf) ;

    generateTown() ;
  end

  #--------------------------------------------------------------
  #++
  ## generate Grid Town
  def generateTown()
    @offset = getConf(:offset) ;
    @sizeX = getConf(:sizeX) ;
    @sizeY = getConf(:sizeY) ;
    @gridLength = getConf(:gridLength) ;
    @randomSize = getConf(:randomSize) ;
    @nodeTagFormat = getConf(:nodeTagFormat) ;

    @nodeTable = [] ;

    (0...@sizeX).each{|x|
      @nodeTable[x] = [] ;
      (0...@sizeY).each{|y|
        rx = (2.0 * @randomSize * rand()) - @randomSize ;
        ry = (2.0 * @randomSize * rand()) - @randomSize ;
        node = newNode(Geo2D::Point.new(@offset.x + @gridLength * x + rx,
                                        @offset.y + @gridLength * y + ry)) ;
        tag = @nodeTagFormat % [x, y] ;
        node.addTag(tag) ;
        @nodeTable[x][y] = node ;
      }
    }

    @linkTagFormat = getConf(:linkTagFormat) ;
    @linkWidth = getConf(:linkWidth) ;

    (0...@sizeX).each{|x|
      (0...@sizeY).each{|y|
        if(x > 0) then
          fromNode = @nodeTable[x-1][y] ;
          toNode = @nodeTable[x][y] ;
          link = newLink(fromNode, toNode, @linkWidth) ;
          tag = @linkTagFormat % [fromNode.tagList.first, toNode.tagList.first] ;
          link.addTag(tag) ;
        end
        if(y > 0) then
          fromNode = @nodeTable[x][y-1] ;
          toNode = @nodeTable[x][y] ;
          link = newLink(fromNode, toNode, 1.0) ;
          tag = @linkTagFormat % [fromNode.tagList.first, toNode.tagList.first] ;
          link.addTag(tag) ;
        end
        }
      }
  end

  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------
  #--============================================================
end # class GridTown

########################################################################
########################################################################
########################################################################
if($0 == __FILE__) then

  require 'test/unit'

  #--============================================================
  #++
  ## unit test for this file.
  class TC_Foo < Test::Unit::TestCase
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
      town = GridTown.new() ;

      axml = town.to_ArrayedXml() ;
      p axml ;
      xml = ItkXml.to_Xml(axml) ;
      ItkXml::ppp(xml) ;
    end

  end # class TC_Foo < Test::Unit::TestCase
end # if($0 == __FILE__)



