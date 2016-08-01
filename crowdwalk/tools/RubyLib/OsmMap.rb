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
## OSM の json ファイルを、CrowdWalk 用 XML マップファイルに変換する。
## OSM の feature の内、 geoType が LineString で、property に "highway" を
## 含むものを、 Road として取り出す。
## また、Road を構成する LineString の構成点は、各々、MapNode となり、
## LineString の構成線分 (LineSegment) は、各々 MapLink になる。
## Road に付加されている、"cw:tag" という property は、
## MapLink のタグとして付与される。
## その際、";" をセパレータとして、順に suffix を取り除いたものも、
## タグとして付与される。
## さらに、"cw:tag" の値に、LineSegment のしての序数を付加したものも、
## タグとして付与される。
## つまり、
## "foo;bar;baz" という "cw:tag" を持つロードの3番目の LineSegment は、
## "foo", "foo;bar", "foo;bar;baz", "foo;bar;baz:2" というタグが付与される。
class OsmMap < MapTown
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## description of DefaultValues.
  DefaultValues = { :foo => :bar } ;
  ## description of DefaultOptsions.
  DefaultConf = {
    :cartOrigin => :jp09, # 平面直角原点
    :cwTagName => "cw:tag", # OSM の Road に付与されている CrowdWalk 用タグの
                            # property 名。
    :cwTagSep => ';',       # 上記タグの suffix を切っていく時のセパレータ。
    :cwTagNthSep => ':',    # 上記タグの末尾につける序数のセパレータ
    :redundantMargin => 0.5, # redundant node を判定する距離。
                             # ノードを取り去った時、形状がの距離以下しか
                             # 変化しなければOK。
  } ;

  ## 日本の19座標系の原点リスト。
  ## 経度(lon)緯度(lat)から平面直角座標系への変換原点。
  CartesianLonLatOrigin = {
    :jp01 => Geo2D::Point.new(129.0 + (30.0/60.0), 33.0), # [129度30分, 33度]
    :jp02 => Geo2D::Point.new(131.0 +  (0.0/60.0), 33.0), # [131度00分, 33度]
    :jp03 => Geo2D::Point.new(132.0 + (10.0/60.0), 36.0), # [132度10分, 36度]
    :jp04 => Geo2D::Point.new(133.0 + (30.0/60.0), 33.0), # [133度30分, 33度]
    :jp05 => Geo2D::Point.new(134.0 + (20.0/60.0), 36.0), # [134度20分, 36度]
    :jp06 => Geo2D::Point.new(136.0 +  (0.0/60.0), 36.0), # [136度00分, 36度]
    :jp07 => Geo2D::Point.new(137.0 + (10.0/60.0), 36.0), # [137度10分, 36度]
    :jp08 => Geo2D::Point.new(138.0 + (30.0/60.0), 36.0), # [138度30分, 36度]
    :jp09 => Geo2D::Point.new(139.0 + (50.0/60.0), 36.0), # [139度50分, 36度]
    :jp10 => Geo2D::Point.new(140.0 + (50.0/60.0), 40.0), # [140度50分, 40度]
    :jp11 => Geo2D::Point.new(140.0 + (15.0/60.0), 44.0), # [140度15分, 44度]
    :jp12 => Geo2D::Point.new(142.0 + (15.0/60.0), 44.0), # [142度15分, 44度]
    :jp13 => Geo2D::Point.new(144.0 + (15.0/60.0), 44.0), # [144度15分, 44度]
    :jp14 => Geo2D::Point.new(142.0 +  (0.0/60.0), 26.0), # [142度00分, 26度]
    :jp15 => Geo2D::Point.new(127.0 + (30.0/60.0), 26.0), # [127度30分, 26度]
    :jp16 => Geo2D::Point.new(124.0 +  (0.0/60.0), 26.0), # [124度00分, 26度]
    :jp17 => Geo2D::Point.new(131.0 +  (0.0/60.0), 26.0), # [131度00分, 26度]
    :jp18 => Geo2D::Point.new(136.0 +  (0.0/60.0), 20.0), # [136度00分, 20度]
    :jp19 => Geo2D::Point.new(154.0 +  (0.0/60.0), 26.0), # [154度00分, 26度]
  } ;
  ## 経度(lon)から平面直角座標系への変換倍率
  CartesianLatMagnify = 10001960.0/90.0 ;

  ## 角度ラジアン係数。
  Deg2Rad = (Math::PI / 180.0) ;
  
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## original json
  attr_accessor :sourceJson ;
  ## road list ;
  attr_accessor :roadList ;
  ## node table ;
  attr_accessor :nodeTable ;
  ## node id max
  attr_accessor :nodeIdMax ;
  ## link id max
  attr_accessor :linkIdMax ;
  
  #--------------------------------------------------------------
  #++
  ## 初期化。
  ## _conf_:: 設定テーブル
  def initialize(conf = {})
    super(0, 0.0, conf) ;
    @roadList = [] ;
    @nodeTable = Geo2D::RTree.new() ;
    @nodeIdMax = 0 ;
    @linkIdMax = 0 ;
  end

  #--------------------------------------------------------------
  #++
  ## scan json file
  ## _file_:: JSON file
  def scanJsonFromFile(file)
    open(file,"r"){|strm| scanJsonFromStream(strm) ; }
  end

  #--------------------------------------------------------------
  #++
  ## scan json stream
  ## _strm_:: JSON stream
  def scanJsonFromStream(strm)
    scanJson(strm.read) ;
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
    if(tag = road.hasProperty(getConf(:cwTagName))) then
      road.pushTag(tag) ;
      partList = [] ;
      tag.split(getConf(:cwTagSep)).each{|part|
        partList.push(part) ;
        subtag = partList.join(getConf(:cwTagSep));
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
      pos = convertLonLat2Pos(coord) ;
      node = getNodeByPos(pos) ;
      road.pushNode(node) ;
      if(!preNode.nil?) then
        link = OsmRoadLink.new(road, preNode, node) ;
        registerNewLink(link) ;
        road.pushLink(link) ;
        link.assignTagFromRoad(getConf(:cwTagNthSep)) ;
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
    @nodeList.each{|node|
      node.id = genNodeId() ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## assign IDs to links.
  def assignLinkIds()
    @roadList.each{|road|
      road.linkList.each{|link|
        link.id = genLinkId() ;
      }
    }
  end

  #--------------------------------------------------------------
  #++
  ## genNodeId.
  def genNodeId()
    newId = ("nd_%06d" % @linkIdMax) ;
    @linkIdMax += 1 ;
    return newId ;
  end
  
  #--------------------------------------------------------------
  #++
  ## genLinkId.
  def genLinkId()
    newId = ("lk_%06d" % @linkIdMax) ;
    @linkIdMax += 1 ;
    return newId ;
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
  
  #--------------------------------------------------------------
  #++
  ## convert lonlat to pos (x-y for CrowdWalk)。
  ## CrowdWalk は、東が x、南が y。
  ## _lonlat_ :: 経度緯度の配列もしくは Geo2D::Point
  def convertLonLat2Pos(lonlat)
    ll = Geo2D::Point.sureGeoObj(lonlat) ;
    origin = CartesianLonLatOrigin[getConf(:cartOrigin)] ;
    magnify = Geo2D::Point.new(CartesianLatMagnify *
                               Math::cos(Deg2Rad * ll.y),
                               CartesianLatMagnify) ;
    x = (ll.x - origin.x) * magnify.x ;
    y = (ll.y - origin.y) * magnify.y ;
    return Geo2D::Point.new(x, -y) ;
  end

  #--------------------------------------------------------------
  #++
  ## convert OsmMap to CrowdWalk data
  def convertOsm2CrowdWalk()
    extractNodeListFromRoadList() ;
    assignIds() ;
    removeNonConnectedNodesLinks() ;
    reduceRedundantNodes() ;
  end

  #--------------------------------------------------------------
  #++
  ## reduce redundant nodes.
  ## 2リンクのみとつながるノードで、そのノードを取り去っても
  ## 道路の形状がほとんど変化しないものについて、それを取り去る。
  def reduceRedundantNodes()
    @removedNodeList = [] ;
    @removedLinkList = [] ;
    c = 0 ;
    @nodeList.each{|node|
      if(checkRedundantNode(node)) then
        removeRedundantNode(node) ;
        c += 1;
      end
    }
    @removedNodeList.each{|node|
      @nodeList.delete(node) ;
      @nodeTable.delete(node) ;
    }
    @removedLinkList.each{|link|
      @linkList.delete(link) ;
    }
    p [:reducedNode, c] ;
  end

  #--------------------------------------------------------------
  #++
  ## redundant node かどうかのチェック。 
  ## 2リンクのみとつながるノードで、そのノードを取り去っても
  ## 道路の形状がほとんど変化しないかどうか。
  def checkRedundantNode(node)
    if(node.linkList.length == 2) then
      # ノードを取り去った時の、新しい線分へのノードの足までの長さを
      # 求める。
      (link0, link1, node0, node1) = getBothSidesLinksNodes(node) ;
      newLine = Geo2D::LineSegment.new(node0.pos, node1.pos) ;
      dist = newLine.distanceFromPoint(node.pos) ;
      if(dist < getConf(:redundantMargin)) then
        p [:node, node.id, dist] ;
        return true ;
      else
        return false ;
      end
    else
      return false ;
    end
  end

  #--------------------------------------------------------------
  #++
  ## 2リンクノードの両端リンク及びノードの取得。
  ## 2リンクのみとつながるノードの、その２つのリンクと、さらにその先のノードを
  ## まとめて返す。
  def getBothSidesLinksNodes(node)
    link0 = node.linkList[0] ;
    link1 = node.linkList[1] ;
    node0 = link0.getAnotherNode(node) ;
    node1 = link1.getAnotherNode(node) ;
    return [link0, link1, node0, node1] ;
  end
  
  #--------------------------------------------------------------
  #++
  ## redundant node の削除。
  def removeRedundantNode(node)
    (link0, link1, node0, node1) = getBothSidesLinksNodes(node) ;
    newLink = OsmRoadLink.new(link0.road, node0, node1) ;
    newLink.setLength(link0.length + link1.length) ;
    newLink.setWidth(link0.width) ;
    [link0, link1].each{|l|
      l.tagList.each{|tag|
        newLink.addTag(tag) ;
      }
    }
    newLink.children = [node, link0, link1] ;
    newLink.id = genLinkId() ;
    @linkList.push(newLink) ;
    node0.linkList.delete(link0) ;
    node0.linkList.push(newLink) ;
    node1.linkList.delete(link1) ;
    node1.linkList.push(newLink) ;
    @removedNodeList.push(node) ;
    @removedLinkList.push(link0) ;
    @removedLinkList.push(link1) ;
  end

  
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
    ## 元のJSONデータ（連想配列）。
    attr_accessor :sourceJson ;
    
    #------------------------------------------
    #++
    ## initialize
    def initialize(conf = {})
      super(conf) ;
    end

    #------------------------------------------
    #++
    ## json を格納。
    ## 現段階で解析はしない。
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
    ## check a certain property.
    ## _pattern_ :: 属性名のパターン。
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
    ## geo object のタイプ。
    ## "LineString" とか "Point" とかが帰る。
    def geoType()
      return getGeometry()["type"] ;
    end

    #------------------------------------------
    #++
    ## Json に含まれる座標値列。(coordinates)
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
    ## 緯度経度設定。
    ## _lonlat_ :: 経度緯度値
    def setLonLat(lonlat)
      @lonlat = lonlat ;
    end

    #------------------------------------------
    #++
    ## bbox.
    ## BTree のテーブルで必要。
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
    ## children. reduce node した時に消去したノードやリンク。
    attr_accessor :children ;

    #------------------------------------------
    #++
    ## 初期化。
    ## __road_ :: Road データ。
    ## __fromNode_ :: 起点ノード。
    ## __toNode_ :: 終点ノード。
    def initialize(_road, _fromNode, _toNode)
      @road = _road ;
      super(0, _fromNode, _toNode, DefaultWidth) ;
    end

    #------------------------------------------
    #++
    ## Road の属性情報からタグを付与。
    ## @road にはすでに、Road が入っている。
    ## その中の tag (cw:tag の属性値) を使ってタグを付与する。
    ## タグには、そのリンクの序数（RoadLink の何番目のセグメントか）
    ## が付与される。その際、_cwTagNthSep_ （通常 ":"）がセパレータとして
    ## 挿入される。
    ## _cwTagNthSep_ :: 属性名の suffix を追加するときのセパレータ。
    def assignTagFromRoad(cwTagNthSep)
      _nth = @road.linkList.index(self) ;
      if(_nth.nil?) then
        _nth = @road.linkList.length() ;
      end
      if(@road.tagList.length > 0) then
        nthTag = @road.tagList[0] + cwTagNthSep + _nth.to_s ;
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
    ## OSM の JSON ファイルの中身をチェック。
    ## 開発用の作業テスト。
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
      map.scanJsonFromFile(TestJsonData) ;

      p [:roadList, map.roadList.length] ;
      map.extractNodeListFromRoadList() ;
      map.assignIds() ;
      p [:linkList, map.linkList.length] ;
      p [:nodeList, map.nodeList.length] ;
      p [:connect, map.findNonConnectedNodes(map.nodeList.first).size]
      map.removeNonConnectedNodesLinks() ;
      p [:nodeList, map.nodeList.length] ;
      p [:linkList, map.linkList.length] ;

      map.saveXmlToFile(TestCWMapData) ;
    end

    
  end # class TC_OsmMap
end # if($0 == __FILE__)
