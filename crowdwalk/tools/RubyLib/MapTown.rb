#! /usr/bin/env ruby
# coding: utf-8
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
require 'RTree.rb' ;

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

  ## node RTree (table for pos => node/link object)
  attr :nodeTree, true ;
  ## link RTree (table for pos => node/link object)
  attr :linkTree, true ;

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
  def addObject(object, forceNewId = true)
    if(forceNewId) then
      if(!isNewId(object.id)) then
        object.id = getNewId() ;
      end
    end
    @objectTable[sureIdString(object.id)] = object ;
    return object ;
  end

  #--------------------------------------------------------------
  #++
  ## make sure id string
  def sureIdString(id)
    return id.to_s ;
  end
  #--------------------------------------------------------------
  #++
  ## add object
  def getObject(id)
    return @objectTable[sureIdString(id)] ;
  end

  #--------------------------------------------------------------
  #++
  ## check id is new or used.
  def isNewId(id)
    return !@objectTable.has_key?(sureIdString(id)) ;
  end

  #--------------------------------------------------------------
  #++
  ## get new id
  def getNewId()
    while(!isNewId(@maxId)) do
      @maxId += 1 ;
    end
    return @maxId ;
  end

  #--------------------------------------------------------------
  #++
  ## new node
  def newNode(pos = nil, height = @defaultHeight)
    newId = getNewId() ;
    node = nil ;
    if(pos.nil?) then
      node = MapNode.new(newId) ;
    else
      node = MapNode.new(newId, pos, height) ;
      registerNewNode(node) ;
    end
    return node ;
  end

  #--------------------------------------------------------------
  #++
  ## register new node
  def registerNewNode(node)
    @nodeList.push(node) ;
    addObject(node) ;
    return node ;
  end

  #--------------------------------------------------------------
  #++
  ## new link
  def newLink(fromNode = nil, toNode = nil, width = 0.0)
    newId = getNewId() ;
    link = nil ;
    if(fromNode.nil?) then
      link = MapLink.new(newId) ;
    else
      link = MapLink.new(newId, fromNode, toNode, width) ;
      registerNewLink(link) ;
    end
    return link ;
  end

  #--------------------------------------------------------------
  #++
  ## register new link
  def registerNewLink(link)
    @linkList.push(link) ;
    addObject(link) ;
    return link ;
  end

  #--------------------------------------------------------------
  #++
  ## rebind nodes and links by ID
  def rebindNodesLinksById()
    @nodeList.each{|node|
      node.rebindLinksById(self) ;
    }
    @linkList.each{|link|
      link.rebindNodesById(self) ;
    }
    self ;
  end

  #--------------------------------------------------------------
  #++
  ## build RTree for Nodes and Links.
  def buildRTree()
    @nodeTree = Geo2D::RTree.new() ;
    @nodeList.each{|node|
      @nodeTree.insert(node) ;
    }
    @linkTree = Geo2D::RTree.new() ;
    @linkList.each{|link|
      @linkTree.insert(link) ;
    }
    self ;
  end
  
  #--------------------------------------------------------------
  #++
  # minimum distance to start to find link/node
  MinDistToStart = 1.0 ;
  #++
  ## find nearrest nodes.
  def findNearestNode(pos)
    d = MinDistToStart ;
    nodes = nil ;
    # 指定されたposを中心に、d を徐々に増やしながら（＝bbox を大きくしながら)
    # @nodeTree を探す。
    begin
      bbox = pos.bbox.growByMargin(d) ;
      d *= 2.0 ;
      nodes = @nodeTree.searchByBBox(bbox) ;
    end while(nodes.size == 0) ;
    # nodes の中で最近ノードを探す。
    minNode = nil ;
    minDist = nil ;
    nodes.each{|node|
      dist = pos.distanceTo(node.pos) ;
      if(minNode.nil? || dist < minDist) then
        minNode = node ;
        minDist = dist ;
      end
    }
    return minNode ;
  end
  
  #--------------------------------------------------------------
  #++
  # minimum distance to start to find link/node
  #++
  ## find almost nearrest link.
  ## 計算上、もしかしたら違う場合があるかもしれない。
  def findNearestLink(pos)
    d = MinDistToStart ;
    links = nil ;
    # 指定されたposを中心に、d を徐々に増やしながら（＝bbox を大きくしながら)
    # @linkTree を探す。
    begin
      bbox = pos.bbox.growByMargin(d) ;
      d *= 2.0 ;
      links = @linkTree.searchByBBox(bbox) ;
    end while(links.size == 0) ;
    # links の中で最近リンクを探す。
    minLink = nil ;
    minDist = nil ;
    links.each{|link|
      line = link.lineSegment() ;
      dist = line.distanceFromPoint(pos) ;
      if(minLink.nil? || dist < minDist) then
        minLink = link ;
        minDist = dist ;
      end
    }
    return minLink ;
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
  ## check connectivity
  def checkConnectivity(startNode = @nodeList.first)
    connectedNodeTable = findConnectedNodes(startNode) ;

#    return @nodeList.size == closeList.size ;
    # 全ノードに対して、close リストに含まれているか、nodeのリンクが無いなら、
    # 単連結
    @nodeList.each{|node|
      if(connectedNodeTable[node].nil? && node.linkList.size > 0) then
        return false ;
      end
    }
    return true ;
  end

  #--------------------------------------------------------------
  #++
  ## 単連結の範囲を求める。
  ## _startNode_ : 探索開始点
  ## *return* : _startNode_ から繋がっているノードのテーブル。
  ##            Node => 探索で辿った一つ手前のノードというテーブル。
  def findConnectedNodes(startNode)
    openList = [startNode] ;
    closeList = {} ;
    until(openList.empty?)
      currentNode = openList.pop() ;
      closeList[currentNode] = currentNode ;
      currentNode.linkList.each{|link|
        anotherNode = link.getAnotherNode(currentNode) ;
        if(!closeList[anotherNode])
          openList.push(anotherNode) ;
        end
      }
    end
    return closeList ;
  end
  
  #--------------------------------------------------------------
  #++
  ## 単連結でないノードを集める。
  ## _startNode_ : 探索開始点
  ## *return* : _startNode_ から繋がっていないノードの配列。
  def findNonConnectedNodes(startNode)
    connectTable = findConnectedNodes(startNode) ;
    restList = [] ;
    @nodeList.each{|node|
      restList.push(node) if (connectTable[node].nil?) ;
    }
    return restList ;
  end
  
  #--------------------------------------------------------------
  #++
  ## n本リンクを削除する。
  def pruneLinks(n)
    retryCount = 0 ;
    (0...n).each{|i|
      r = rand(@linkList.size) ;
      link = @linkList[r] ;
      link.fromNode.linkList.delete(link) ;
      link.toNode.linkList.delete(link) ;
      ## 単連結チェック。単連結でなければ、やり直し。
      if(!checkConnectivity())
        retryCount += 1 ;
        p [:pruneLinks, :retry, retryCount] ;
        link.setFromToNode(link.fromNode, link.toNode) ;
        redo ;
      end
      @linkList.delete(link) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## 孤立ノードを削除
  def reduceIsolatedNode()
    (0...@nodeList.size).each{|index|
      node = @nodeList[index] ;
      if(node.linkList.size() == 0) then
        @nodeList[index] = nil ;
      end
    }
    @nodeList.compact!() ;
    return @nodeList ;
  end

  #--------------------------------------------------------------
  #++
  ## gen Arrayed Xml
  ## *return*:: arrayed xml
  def to_ArrayedXml()
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
      axml.push(node.to_ArrayedXml()) ;
    }

    ## link part
    @linkList.each{|link|
      axml.push(link.to_ArrayedXml()) ;
    }

    return axml ;
  end

  #--------------------------------------------------------------
  #++
  ## gen Xml Object
  ## *return*:: xml
  def to_Xml()
    axml = to_ArrayedXml() ;
    return ItkXml.to_Xml(axml) ;
  end

  #--------------------------------------------------------------
  #++
  ## gen Xml Object
  ## *return*:: xml
  def to_XmlString(withHeader = true)
    xml = to_Xml() ;
    str = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>' ;
    str << "\n" ;
    ItkXml::ppp(xml, str) ;
    str.gsub!(/\<tag\>\s*([^\s]*)\s*\<\/tag\>/, '<tag>\1</tag>') ;
    return str ;
  end

  #--------------------------------------------------------------
  #++
  ## save XML to stream
  ## _strm_ :: output stream
  def saveXmlToStream(strm)
    strm << to_XmlString() ;
  end

  #--------------------------------------------------------------
  #++
  ## save XML to file
  ## _file_ :: output file
  def saveXmlToFile(file)
    open(file,"w") {|strm|
      saveXmlToStream(strm) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## load XML map file
  ## _file_ :: map file
  ## _verboseP_ :: show progress or not
  def loadXmlMapFile(file, verboseP = false)
    open(file,"r"){|strm|
      loadXmlMapStream(strm, verboseP) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## load XML map stream
  ## _strm_ :: xml map stream
  ## _verboseP_ :: show progress or not
  def loadXmlMapStream(strm, verboseP = false)
    fparser = ItkXml::FilterParser.new(strm) ;
    m = 1000 ;
    nodeC = 0 ;
    fparser.listenQName("Node"){|xml, str|
      node = newNode() ;
      node.scanXml(xml) ;
      registerNewNode(node) ;
      nodeC += 1 ;
      STDERR.putc("n") if(verboseP && (nodeC % m == 0)) ;
    }

    linkC = 0 ; 
    fparser.listenQName("Link"){|xml, str|
      link = newLink() ;
      link.scanXml(xml) ;
      registerNewLink(link) ;
      linkC += 1 ;
      STDERR.putc("l") if(verboseP && (linkC % m == 0)) ;
    }
    STDERR << "Loading:" if (verboseP) ;
    fparser.parse ;
    STDERR.puts("") if (verboseP) ;
    rebindNodesLinksById() ;
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

      axml = town.to_ArrayedXml() ;
      p axml ;
      xml = ItkXml.to_Xml(axml) ;
      ItkXml::ppp(xml) ;
    end

    #----------------------------------------------------
    #++
    ## XML map read test
    require 'Stat/Uniform.rb' ;
    TestBSampleFile = "../../sample/ginza/ginza00.map.xml" ;
    def test_b
      town = MapTown.new() ;
      town.loadXmlMapFile(TestBSampleFile)
      pp [:map, [:node, town.nodeList.size], [:link, town.linkList.size]] ;
      town.buildRTree() ;
#      town.linkTree.showTree() ;
      bbox = town.linkTree.root.bbox() ;
      p [:bbox, bbox] ;
      mx = (bbox.minX() + bbox.maxX())/2.0 ;
      my = (bbox.minY() + bbox.maxY())/2.0 ;
      d = 200.0 ;
      randX = Stat::Uniform.new(mx - d, mx + d) ;
      randY = Stat::Uniform.new(my - d, my + d) ;
      (0...10).each{|i|
        x = randX.value() ;
        y = randY.value() ;
        pos = Geo2D::Point.new(x,y) ;
        node = town.findNearestNode(pos) ;
        link = town.findNearestLink(pos) ;
        pp [i, pos, node, link] ;
      }
    end
    
  end # class TC_Foo < Test::Unit::TestCase
end # if($0 == __FILE__)
