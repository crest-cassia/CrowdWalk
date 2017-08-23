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
##
## [2017-06-24 I.Noda] POI の追加。
## Point の中で、"cw:poi" という property を持つものを取り出し、Point として
## 登録。
## "cw:poi" の値をタグに追加。
## "cw:tag" の値を、上記の処理と同じようにタグに追加。
## "cw:stayloop" があれば、さらに、 "__StayLoop__" を追加。
## "cw:tsunami" の値を、"tsunami:<値>" というタグで追加。
##
class OsmMap < MapTown
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## description of DefaultValues.
  DefaultValues = { :foo => :bar } ;
  ## description of DefaultOptsions.
  DefaultConf = {
    :cartOrigin => :jp09, # 平面直角原点
    :cwTagName => "cw:tag", # OSM の Road, PoIに付与されている CrowdWalk 用タグの
                            # property 名。
    :cwTagSep => ';',       # 上記タグの suffix を切っていく時のセパレータ。
    :cwTagNthSep => ':',    # 上記タグの末尾につける序数のセパレータ
    :cwPoIName => "cw:poi", # OSM の PoI に付与されている CrowdWalk 用タグの
                            # property 名。
    :cwStayLoopName => "cw:stayloop", # OSM の PoI に付与されている
                            # StayLoop 埋め込み用 用タグのproperty 名。
    :stayLoopTag => "__NeedStayLoop__", # StayLoop の指定タグ
    :cwTsunamiName => "cw:tsunami", # OSM 上の津波関係タグの名前
    :cwTsunamiTagPrefix => "tsunami:", #津波関係タグにつける prefix
    :cwOneWayName => "cw:oneway", # OSM の 一方通行リンク に付与されているproperty
    :cwOneWayFore => "forward", # 一方通行 property の順方向の値。
    :cwOneWayBack => "backward", # 一方通行 property の順方向の値。
    :onewayForeTag => "ONE-WAY-FORWARD", # CW の中での一方通行（順）のタグ
    :onewayBackTag => "ONE-WAY-BACKWARD", # CW の中での一方通行（順）のタグ
    :redundantMargin => 0.5, # redundant node を判定する距離。
                             # ノードを取り去った時、形状がの距離以下しか
                             # 変化しなければOK。
    :linkWidth => 2.0,	    # link の width の規定値。
    :cwWidthName => "cw:width",  # 道路幅指定の property 名。
    :cwLengthName => "cw:length", # 道路の長さ指定のproperty 名。
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
  ## poi list ;
  attr_accessor :poiList ;
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
    @poiList = [] ;
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
    scanPoIJson(@sourceJson) ;
  end

  #--------------------------------------------------------------
  #++
  ## scan json road
  ## _json_:: JSON parts
  def scanRoadJson(json)
    json["features"].each{|fjson|
      feature = OsmFeature.new().scanJson(fjson) ;
      if(feature.geoType() == "LineString" && feature.hasProperty("highway"))
        addRoad(OsmRoad.new().scanJson(fjson)) ;
      end
    }
  end

  #--------------------------------------------------------------
  #++
  ## scan json PoI
  ## _json_:: JSON parts
  def scanPoIJson(json)
    poiPropName = getConf(:cwPoIName)
    json["features"].each{|fjson|
      feature = OsmFeature.new().scanJson(fjson) ;
      if(feature.geoType() == "Point" && feature.hasProperty(poiPropName))
        addPoI(OsmPoI.new().scanJson(fjson)) ;
      end
    }
  end

  #--------------------------------------------------------------
  #++
  ## add road
  ## _road_:: added road
  def addRoad(road)
    @roadList.push(road) ;

    # 一般タグ
    tagGeneral = addGeneralTagToMapPart(road) ;

    # ONE-WAY タグ
    tagOneway = addOnewayTagToRoad(road) ;

    p [:road, road.tagList] if(tagGeneral || tagOneway) ;
 
    return road ;
  end

  #--------------------------------------------------------------
  #++
  ## add one-way tag to link
  def addOnewayTagToRoad(road)
    tag = nil ;
    if(tag = road.hasProperty(getConf(:cwOneWayName))) then
      if(tag == getConf(:cwOneWayFore)) then
        road.addTag(getConf(:onewayForeTag)) ;
      elsif(tag == getConf(:cwOneWayFore)) then
        road.addTag(getConf(:onewayBackTag)) ;
      else
        p [:warning, "illegal oneway tag", tag] ;
      end
    end
    return tag ;
  end
  
  #--------------------------------------------------------------
  #++
  ## add general tag to link/poi
  def addGeneralTagToMapPart(object)
    tag = nil ;
    if(tag = object.hasProperty(getConf(:cwTagName))) then
      object.addTag(tag) ;
      partList = [] ;
      tag.split(getConf(:cwTagSep)).each{|part|
        partList.push(part) ;
        subtag = partList.join(getConf(:cwTagSep));
        object.addTag(subtag) if(subtag != tag) ;
      }
    end
    return tag ;
  end
  
  #--------------------------------------------------------------
  #++
  ## add PoI
  ## _poi_:: added PoI
  def addPoI(poi)
    @poiList.push(poi) ;

    poiLabel = getConf(:cwPoIName) ;
    poiName = poi.hasProperty(poiLabel) ;
    poi.addTag(poiLabel)
    poi.addTag(poiName) ;

    ## check StayLoop
    if(poi.hasProperty(getConf(:cwStayLoopName))) 
      poi.addTag(getConf(:stayLoopTag))
    end

    addGeneralTagToMapPart(poi) ;

    p [:poi, poi.tagList] ;
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
    roadWidth = road.hasProperty(getConf(:cwWidthName)) ;
    roadLength = road.hasProperty(getConf(:cwLengthName)) ;
    nNode = road.coordinatesJson.length ;
    
    preNode = nil ;
    road.coordinatesJson.each{|coord|
      pos = convertLonLat2Pos(coord) ;
      node = getNodeByPos(pos) ;
      road.pushNode(node) ;
      if(!preNode.nil?) then
        link = OsmRoadLink.new(road, preNode, node, getConf(:linkWidth)) ;
        registerNewLink(link) ;
        road.pushLink(link) ;
        link.assignTagFromRoad(getConf(:cwTagNthSep)) ;
        if(roadLength) then
          link.setLength(roadLength.to_f / (nNode-1).to_f) ;
        end
        if(roadWidth) then
          link.setWidth(roadWidth.to_f) ;
        end
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
  ## bind PoI info to nearest Node.
  def bindPoIToNode()
    @poiList.each{|poi|
      coord = poi.coordinatesJson() ;
      pos = convertLonLat2Pos(coord) ;
      node = getNodeByPos(pos) ;
      poi.bindNode(node) ;
    }
  end
  
  #--------------------------------------------------------------
  #++
  ## remove nodes and links that is not connected from _startNode_.
  def removeNonConnectedNodesLinks()
    pivotNode = findMostMajorGroupNode() ;
    nonConnectedNodes = findNonConnectedNodes(pivotNode) ;
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
    addMiscTagsToRoadList() ;
    extractNodeListFromRoadList() ;
    assignIds() ;
    removeNonConnectedNodesLinks() ;
    bindPoIToNode() ;
    reduceRedundantNodes() ;
    reduceLoopedLinks() ;
    addIdTags() ;
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
    removeNodeInList(@removedNodeList) ;
    removeLinkInList(@removedLinkList) ;
    p [:reducedNode, c] ;
  end

  #--------------------------------------------------------------
  #++
  ## 複数の node を削除する。
  def removeNodeInList(nodeList)
    nodeList.each{|node|
      @nodeList.delete(node) ;
      @nodeTable.delete(node) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## 複数の link を削除する。
  def removeLinkInList(linkList)
    linkList.each{|link|
      @linkList.delete(link) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## redundant node かどうかのチェック。 
  ## 2リンクのみとつながるノードで、そのノードを取り去っても
  ## 道路の形状がほとんど変化しないかどうか。
  ## ノードが PoI の場合は、削除対象としない。
  def checkRedundantNode(node)
    if(!isNodePoI(node) && node.linkList.length == 2) then
      # ノードを取り去った時の、新しい線分へのノードの足までの長さを
      # 求める。
      (link0, link1, node0, node1) = getBothSidesLinksNodes(node) ;
      newLine = Geo2D::LineSegment.new(node0.pos, node1.pos) ;
      dist = newLine.distanceFromPoint(node.pos) ;
      if(dist < getConf(:redundantMargin)) then
        # p [:node, node.id, dist] ;
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
  ## Node が PoI かどうかのチェック。
  def isNodePoI(node)
    poiTag = getConf(:cwPoIName) ;
    return node.tagList.include?(poiTag) ;
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

  #--------------------------------------------------------------
  #++
  ## loop link かどうかのチェックして除外。 
  ## おなじノードに繋がるリンクがあれば、
  ## それは無駄なリンクのはずなので。
  def reduceLoopedLinks()
    @removedLinkList = [] ;
    @removedNodeList = [] ;
    c = 0 ;
    @linkList.each{|link|
      if(checkLoopedLink(link)) then
        removeLoopedLink(link) ;
        c += 1;
      end
    }
    removeNodeInList(@removedNodeList) ;
    removeLinkInList(@removedLinkList) ;
    p [:reducedLink, c] ;
  end
  
  #--------------------------------------------------------------
  #++
  ## loop link かどうかのチェック。 
  ## おなじノードに繋がるリンクかどうか。
  def checkLoopedLink(link)
    return link.fromNode == link.toNode ;
  end
  
  #--------------------------------------------------------------
  #++
  ## loop link かどうかのチェック。 
  ## おなじノードに繋がるリンクかどうか。
  def removeLoopedLink(link)
    node = link.fromNode ;
    node.linkList.delete(link) ;
    @removedLinkList.push(link) ;
    @removedNodeList.push(node) if (node.linkList.length == 0) ;
  end

  #--------------------------------------------------------------
  #++
  ## id をタグに追加。
  def addIdTags()
    @nodeList.each{|node|
      node.addTag(node.id, true) ;
    }
    @linkList.each{|link|
      link.addTag(link.id, true) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## Road に予備のタグを追加
  def addMiscTagsToRoadList()
    addedCount = 0 ;
    @roadList.each{|road|
      added = addMiscTagsToRoad(road) ;
      addedCount += added.size ;
    }
    p [:addMiscTagsToRoadList, addedCount] ;
  end
  
  #--------------------------------------------------------------
  #++
  ## １つのロードに予備のタグを追加
  def addMiscTagsToRoad(road)
    addedList = [] ;
    tsunamiPropName = getConf(:cwTsunamiName) ;
    tsunamiTagPrefix = getConf(:cwTsunamiTagPrefix) ;
    value = nil ;
    if(value = road.hasProperty(tsunamiPropName)) then
      tag = tsunamiTagPrefix + value ;
      road.addTag(tsunamiTagPrefix + value) ;
      addedList.push([tsunamiPropName, tag]) ;
    end
    return addedList ;
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
    DefaultConf = { } ;

    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## 元のJSONデータ（連想配列）。
    attr_accessor :sourceJson ;
    ## tag list
    attr_accessor :tagList ;
    
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
    ## "LineString" とか "Point" とかが返る。
    def geoType()
      return getGeometry()["type"] ;
    end

    #------------------------------------------
    #++
    ## Json に含まれる座標値列。(coordinates)
    def coordinatesJson()
      return getGeometry()["coordinates"] ;
    end
    
    #------------------------------------------
    #++
    ## add new tag
    def addTag(tag)
      @tagList.push(tag) ;
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
    def initialize(_road, _fromNode, _toNode, _width = DefaultWidth)
      @road = _road ;
      super(0, _fromNode, _toNode, _width) ;
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
  #++
  ## PoI in OSM。
  ## 地図上でマークした地点情報。
  class OsmPoI < OsmFeature
    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## 対応ノード。最寄りの OsmNode を探しだして入れる。
    attr_accessor :node ;
    
    #------------------------------------------
    #++
    ## initialize
    def initialize(conf = {})
      super(conf) ;
      @node = nil
      @tagList = [] ;
    end

    #------------------------------------------
    #++
    ## bind OsmNode and OsmPoI
    def bindNode(node)
      @node = node ;
      @tagList.each{|tag|
        @node.addTag(tag) ;
      }
    end
    
    
  end # class OsmPoI
    
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
