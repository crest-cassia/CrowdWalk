#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Map Town class
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

require 'ItkXml.rb' ;
require 'WithConfParam.rb' ;
require 'MapNode.rb' ;
require 'MapLink.rb' ;

#--======================================================================
#++
## Town class for Map
class MapTown < WithConfParam
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## tag name of XML element
  XmlElementTag = "Group" ;

  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## id
  attr :id, true ;
  ## max id for node/link objects
  attr :maxId, true ;
  ## default height
  attr :defaultHeight, true ;
  ## imageFileName
  attr :imageFileName, true ;
  ## max height
  attr :maxHeight, true ;
  ## min height
  attr :minHeight, true ;
  ## NorthWest point (Geo2D::Point)
  attr :northWestCorner, true ;
  ## SouthEast point (Geo2D::Point)
  attr :southEastCorner, true ;
  ## theta
  attr :pTheta, true ;
  ## r
  attr :r, true ;
  ## scale
  attr :scale, true ;
  ## s point (?)
  attr :sPoint, true ;
  ## t point (?)
  attr :tPoint, true ;
  ## tag list
  attr :tagList, true ;
  ## node list
  attr :nodeList, true ;
  ## link list
  attr :linkList, true ;
  ## object table (table for id => node/link object)
  attr :objectTable, true ;

  #--------------------------------------------------------------
  #++
  ## description of method initialize
  def initialize(id = 0, defaultHeight = 0.0, conf = {})
    super(conf) ;

    @tagList = [] ;
    @nodeList = [] ;
    @linkList = [] ;
    @objectTable = {} ;

    @imageFileName = "" ;
    @maxHeight = nil ;
    @minHeight = nil ;
    @northWestCorner = Geo2D::Point.new() ;
    @southEastCorner = Geo2D::Point.new() ;
    @pTheta  = 0.0 ;
    @r = 0.0 ;
    @scale = 1.0 ;
    @sPoint = Geo2D::Point.new(1.0, 1.0) ;
    @tPoint = Geo2D::Point.new(0.0, 0.0) ;
    setId(id) ;
    @maxId = @id ;
    @defaultHeight = defaultHeight ;

    addObject(self) ;
  end

  #--------------------------------------------------------------
  #++
  ## set ID
  ## _id_:: ID
  ## *return*:: self
  def setId(id)
    @id = id ;
    return self ;
  end

  #--------------------------------------------------------------
  #++
  ## add object
  def addObject(object)
    @objectTable[object.id] = object ;
    return object ;
  end

  #--------------------------------------------------------------
  #++
  ## new node
  def newNode(pos, height = @defaultHeight)
    @maxId += 1 ;
    node = MapNode.new(@maxId, pos, height) ;
    @nodeList.push(node) ;
    addObject(node) ;
    return node ;
  end

  #--------------------------------------------------------------
  #++
  ## new link
  def newLink(fromNode, toNode, width)
    @maxId += 1 ;
    link = MapLink.new(@maxId, fromNode, toNode, width) ;
    @linkList.push(link) ;
    addObject(link) ;
    return link ;
  end

  #--------------------------------------------------------------
  #++
  ## setup max/min height
  def setupMinMaxHeight()
    @maxHeight = nil ;
    @minHeight = nil ;
    @nodeList.each{|node|
      if(@maxHeight.nil? || @maxHeight < node.height) then
        @maxHeight = node.height ;
      end
      if(@minHeight.nil? || @minHeight > node.height) then
        @minHeight = node.height ;
      end
    }
  end

  #--------------------------------------------------------------
  #++
  ## setup boundary box
  def setupBoundaryBox()
    setupMinMaxHeight() ;

    @northWestCorner = @nodeList.first.pos.dup ;
    @southEastCorner = @nodeList.first.pos.dup ;
    @nodeList.each{|node|
      @northWestCorner.x = node.pos.x if(@northWestCorner.x > node.pos.x) ;
      @northWestCorner.y = node.pos.y if(@northWestCorner.y > node.pos.y) ;
      @southEastCorner.x = node.pos.x if(@southEastCorner.x < node.pos.x) ;
      @southEastCorner.y = node.pos.y if(@southEastCorner.y < node.pos.y) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## gen Arrayed Xml
  ## *return*:: arrayed xml
  def toArrayedXml()
    setupBoundaryBox() ;
    # head part
    axml = [[nil, XmlElementTag,
             ({ :id => @id,
                :imageFileName => @imageFileName,
                :defaultHeight => @defaultHeight,
                :maxHeight => @maxHeight,
                :minHeight => @minHeight,
                :pNorthWestX => @northWestCorner.x,
                :pNorthWestY => @northWestCorner.y,
                :pSouthEastX => @southEastCorner.x,
                :pSouthEastY => @southEastCorner.y,
                :pTheta => @pTheta,
                :r => @r,
                :scale => @scale,
                :sx => @sPoint.x,
                :sy => @sPoint.y,
                :tx => @tPoint.x,
                :ty => @tPoint.y })]] ;
    ## tag part
    @tagList.each{|tag|
      axml.push([:tag, tag]) ;
    }

    ## node part
    @nodeList.each{|node|
      axml.push(node.toArrayedXml()) ;
    }

    ## link part
    @linkList.each{|link|
      axml.push(link.toArrayedXml()) ;
    }

    return axml ;
  end

end # class MapTown

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
      town = MapTown.new() ;

      node0 = town.newNode(Geo2D::Point.new(10.0,15.0)) ;
      node0.addTag("foo") ;
      node0.addTag("bar") ;

      node1 = town.newNode(Geo2D::Point.new(20.0,25.0)) ;
      node1.addTag("aho") ;
      node1.addTag("baka") ;

      link2 = town.newLink(node0, node1, 1.0) ;
      link2.addTag("bbb") ;
      link2.addTag("123") ;

      axml = town.toArrayedXml() ;
      p axml ;
      xml = ItkXml.to_Xml(axml) ;
      ItkXml::ppp(xml) ;
    end

  end # class TC_Foo < Test::Unit::TestCase
end # if($0 == __FILE__)
