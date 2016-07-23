#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = Itk Template for Ruby
## Author:: Itsuki Noda
## Version:: 0.0 2016/06/20 I.Noda
##
## === History
## * [2016/06/20]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'pp' ;
require 'json' ;

$LOAD_PATH.push(File::dirname(__FILE__)) ;
require 'WithConfParam.rb' ;
require 'Geo2D.rb' ;
require 'RTree.rb' ;
require 'ItkXml.rb' ;

require 'MapTown.rb' ;

#--======================================================================
#++
## description of class Foo.
class OsmMap < MapTown
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## description of DefaultValues.
  DefaultValues = { :foo => :bar } ;
  ## description of DefaultOptsions.
  DefaultConf = { :bar => :baz } ;

  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## original json
  attr_accessor :sourceJson ;
  ## road list ;
  attr_accessor :roadList ;
  ## node table ;
  attr_accessor :nodeTable ;
  
  #--------------------------------------------------------------
  #++
  ## description of method initialize
  ## _baz_:: about argument baz.
  def initialize(conf = {})
    super(0, 0.0, conf) ;
    @roadList = [] ;
    @nodeTable = Geo2D::RTree.new() ;
  end

  #--------------------------------------------------------------
  #++
  ## scan json file
  ## _file_:: JSON file
  def scanJsonFile(file)
    open(file,"r"){|strm| scanJson(strm.read) ; }
  end

  #--------------------------------------------------------------
  #++
  ## scan json file
  ## _json_:: JSON string
  def scanJson(json)
    @sourceJson = JSON::Parser.new(json).parse ;
    scanRoadJson(@sourceJson) ;
  end

  #--------------------------------------------------------------
  #++
  ## scan json road
  ## _json_:: JSON parts
  def scanRoadJson(json)
    json["features"].each{|fjson|
      feature = OsmRoad.new().scanJson(fjson) ;
      if(feature.geoType() == "LineString" && feature.hasProperty("highway"))
        addRoad(feature) ;
      end
    }
  end

  #--------------------------------------------------------------
  #++
  ## add road
  ## _road_:: added road
  def addRoad(road)
    @roadList.push(road) ;

    tag = nil ;
    if(tag = road.hasProperty("cw:tag")) then
      road.pushTag(tag) ;
      partList = [] ;
      tag.split(';').each{|part|
        partList.push(part) ;
        subtag = partList.join(';');
        p [:tag, subtag] ;
        road.pushTag(subtag) if(subtag != tag) ;
      }
    end
  end

  #--------------------------------------------------------------
  #++
  ## extract node list from road list
  def extractNodeListFromRoadList()
    @roadList.each{|road|
      extractNodeListFromRoad(road) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## extract node list from a road
  ## _road_:: road objects
  def extractNodeListFromRoad(road)
    preNode = nil ;
    road.coordinatesJson.each{|coord|
      pos = OsmMap.convertLonLat2Pos(coord) ;
      node = getNodeByPos(pos) ;
      road.pushNode(node) ;
      if(!preNode.nil?) then
        link = OsmRoadLink.new(road, preNode, node) ;
        registerNewLink(link) ;
        road.pushLink(link) ;
        link.assignTagFromRoad() ;
      end
      preNode = node ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## find or create Node by position or 
  ## _pos_:: position.  should be a Geo2D::
  ## _margin_:: find margin
  ## *return*:: node
  def getNodeByPos(pos, margin = 1.0)
    bbox = pos.bbox().growByMargin(1.0) ;
    nlist = @nodeTable.searchByBBox(bbox) ;
    node = nil ;
    if(nlist.length > 0) then
      node = nlist[0] ;
    else
      node = OsmNode.new() ;
      node.setPos(pos) ;
      registerNewNode(node) ;
      @nodeTable.insert(node) ;
    end
    return node ;
  end
  
  #--------------------------------------------------------------
  #++
  ## assign IDs to nodes and links ;
  def assignIds()
    assignNodeIds() ;
    assignLinkIds() ;
  end
  
  #--------------------------------------------------------------
  #++
  ## assign IDs to nodes.
  def assignNodeIds()
    id = 0 ;
    @nodeList.each{|node|
      node.id = ("nd_%06d" % id) ;
      id += 1 ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## assign IDs to links.
  def assignLinkIds()
    id = 0 ;
    @roadList.each{|road|
      road.linkList.each{|link|
        link.id = ("lk_%06d" % id) ;
        id += 1 ;
      }
    }
  end

  #--------------------------------------------------------------
  #++
  ## remove nodes and links that is not connected from _startNode_.
  def removeNonConnectedNodesLinks(startNode = @nodeList.first)
    nonConnectedNodes = findNonConnectedNodes(startNode) ;
    nonConnectedLinks = {} ;
    nonConnectedNodes.each{|node|
      node.linkList.each{|link|
        nonConnectedLinks[link] = true ;
      }
      @nodeTable.delete(node) ;
    }
    @nodeList = @nodeList - nonConnectedNodes ;
    @linkList = @linkList - nonConnectedLinks.keys ;
  end

  #--============================================================
  #--------------------------------------------------------------
  #++
  ## convert lonlat to pos (x-y for CrowdWalk)
  def self.convertLonLat2Pos(lonlat)
    ll = Geo2D::Point.sureGeoObj(lonlat) ;
    x = (ll.x - CartesianJp09LonLat.x) *  CartesianJp09Multi.x ;
    y = (ll.y - CartesianJp09LonLat.y) *  CartesianJp09Multi.y ;
    return Geo2D::Point.new(x, -y) ;
  end

  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  CartesianJp09LonLat = Geo2D::Point.sureGeoObj([139.83333,
                                                 36.0]) ; # [139度50分, 36度]
  CartesianJp09Multi = Geo2D::Point.sureGeoObj([(10000000/90.0) * Math::cos(Math::PI * 36.0/180.0),
                                                10000000/90.0]) ;

  #--============================================================
  #++
  ## GeoFeature in OSM
  class OsmFeature < WithConfParam
    #--::::::::::::::::::::::::::::::::::::::::
    #++
    ## description of DefaultValues.
    DefaultValues = { :foo => :bar } ;
    ## description of DefaultOptsions.
    DefaultConf = { :bar => :baz } ;

    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## description of DefaultValues.
    attr_accessor :sourceJson ;
    
    #------------------------------------------
    #++
    ## initialize
    def initialize(conf = {})
      super(conf) ;
    end

    #------------------------------------------
    #++
    ## initialize
    def scanJson(json)
      @sourceJson = json ;
      return self ;
    end

    #------------------------------------------
    #++
    ## get properties
    def getProperties()
      return @sourceJson["properties"] ;
    end

    #------------------------------------------
    #++
    ## check a certain property
    def hasProperty(pattern)
      if(pattern.is_a?(String)) then
        return getProperties()[pattern] ;
      else
        getProperties().keys.each{|key|
          return getProperties()[key] if(key =~ pattern) ;
        }
        return nil ;
      end
    end
    
    #------------------------------------------
    #++
    ## get geometry
    def getGeometry()
      return @sourceJson["geometry"] ;
    end

    #------------------------------------------
    #++
    ## get geometry
    def geoType()
      return getGeometry()["type"] ;
    end

    #------------------------------------------
    #++
    ## get geometry
    def coordinatesJson()
      return getGeometry()["coordinates"] ;
    end
    
    
  end # class OsmFeature

  
  #--============================================================
  #++
  ## GeoRoad in OSM
  class OsmRoad < OsmFeature
    
    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## node list
    attr_accessor :nodeList ;
    ## link list
    attr_accessor :linkList ;
    ## tag list
    attr_accessor :tagList ;
    
    #------------------------------------------
    #++
    ## initialize
    def initialize(conf = {})
      super(conf) ;
      @nodeList = [] ;
      @linkList = [] ;
      @tagList = [] ;
    end

    #------------------------------------------
    #++
    ## add new node
    def pushNode(node)
      @nodeList.push(node) ;
    end
    
    #------------------------------------------
    #++
    ## add new link
    def pushLink(link)
      @linkList.push(link) ;
    end
    
    #------------------------------------------
    #++
    ## add new tag
    def pushTag(tag)
      @tagList.push(tag) ;
    end
    
  end # class OsmRoad
  
  #--============================================================
  #++
  ## GeoNode in OSM
  class OsmNode < MapNode
    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## longitude and latitude
    attr_accessor :lonlat ;
    
    #------------------------------------------
    #++
    ## setPos
    def setLonLat(lonlat)
      @lonlat = lonlat ;
    end

    #------------------------------------------
    #++
    ## bbox
    def bbox()
      @pos.bbox() ;
    end
    
  end # class OsmNode

  #--============================================================
  #++
  ## GeoRoadLink in OSM
  class OsmRoadLink < MapLink
    #--::::::::::::::::::::::::::::::::::::::::
    #++
    ## Default Width
    DefaultWidth = 2.0 ;
    
    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## to Road
    attr_accessor :road ;

    #------------------------------------------
    #++
    ## bbox
    def initialize(_road, _fromNode, _toNode)
      @road = _road ;
      super(0, _fromNode, _toNode, DefaultWidth) ;
    end

    #------------------------------------------
    #++
    ## bbox
    def assignTagFromRoad()
      nth = @road.linkList.index(self) ;
      if(nth.nil?) then
        nth = @road.linkList.length() ;
      end
      if(tagList.length > 0) then
        nthTag = tagList[0] + "_" + nth.to_s ;
        addTag(nthTag) ;
      end
      @road.tagList.each{|tag|
        addTag(tag) ;
      }
    end

  end # class OsmRoadLink
    
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------
end # class OsmMap

########################################################################
########################################################################
########################################################################
if($0 == __FILE__) then

  require 'test/unit'

  #--============================================================
  #++
  ## unit test for this file.
  class TC_OsmMap < Test::Unit::TestCase
    #--::::::::::::::::::::::::::::::::::::::::::::::::::
    #++
    ## desc. for TestData
    TestDir = "/home/noda/work/gis/OSM/Data/" ;
    TestJsonData = TestDir + "Ginza.Tokyo.Japan.json"  ;
    TestCWMapData = "/home/noda/tmp/foo.xml" ;

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
      json = nil ;
      open(TestJsonData,"r"){|strm|
        json = JSON::Parser.new(strm.read).parse ;
      }
      featureList = json["features"] ;
      propKeyTable = [] ;
      featureList.each{|feature|
        properties = feature["properties"] ;
        geo = feature["geometry"] ;
        next if (geo["type"] != "LineString") ;
        next if (properties["highway"]) ;
        next if (properties["building"]) ;
        next if (properties["building:part"]) ;
        next if (properties["building:height"]) ;
        next if (properties["railway"]) ;
        next if (properties["parking"]) ;
        next if (properties["landuse"]) ;
        next if (properties["destructed:building"]) ;
        next if (properties["aeroway"]) ;
        next if (properties["waterway"]) ;
        next if (properties["sport"]) ;
        next if (properties["leisure"]) ;
        next if (properties["boundary"]) ;
        next if (properties["barrier"]) ;
        next if (properties["natural"]) ;
        next if (properties["religion"]) ;
        next if (properties["area"]) ;
        if(properties["height"]) then
          pp feature ;
        end
        properties.keys.each{|propKey|
          next if (propKey =~ /name/) ;
          next if (propKey =~ /source/) ;
          next if (propKey =~ /KSJ/) ;
          next if (propKey =~ /note/) ;
          next if (propKey =~ /amenity/) ;
          next if (propKey =~ /social_facility/) ;
          next if (propKey =~ /wikipedia/) ;
          next if (propKey =~ /addr/) ;
          next if (propKey =~ /addr/) ;
          propKeyTable.push(propKey) if !propKeyTable.member?(propKey) ;
        }
      }
      p propKeyTable ;
    end

    #----------------------------------------------------
    #++
    ## about test_b
    def test_b
      map = OsmMap.new() ;
      map.scanJsonFile(TestJsonData) ;
      p [:roadList, map.roadList.length] ;
      map.extractNodeListFromRoadList() ;
      map.assignIds() ;
      p [:linkList, map.linkList.length] ;
      p [:nodeList, map.nodeList.length] ;
      p [:connect, map.findNonConnectedNodes(map.nodeList.first).size]
      map.removeNonConnectedNodesLinks() ;
      p [:nodeList, map.nodeList.length] ;
      p [:linkList, map.linkList.length] ;
      
      open(TestCWMapData,"w"){|strm|
        strm << map.to_XmlString() ;
      }
    end

    
  end # class TC_OsmMap
end # if($0 == __FILE__)
