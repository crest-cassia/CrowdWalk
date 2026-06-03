#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Radial-type Town class
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
class RadialTown < MapTown
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## description of DefaultOptsions.
  DefaultConf = {
    :gridLength => 100.0,
    :size => 10,		# 半径
    :angleRatio => 1.0,		# 角度方向の grid unit の半径方向に対する比率
    :divideFactor => 2,		# 角度方向に分割していく時の倍率
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
  ## generate Radial Town
  def generateTown()
    generateNodes() ;
    generateLinks() ;
  end

  #--------------------------------------------------------------
  #++
  ## generate Node in Town
  def generateNodes()
    size = getConf(:size) ;
    angleRatio = getConf(:angleRatio) ;
    divideFactor = getConf(:divideFactor) ;

    @nodeTable = [] ; # MapNode の 2次元 Array

    (0...size).each{|idxR|
      nodeList = [] ;
      @nodeTable[idxR] = nodeList ;
      if(idxR == 0) then
        node = generateOneNode(0, 0, 1) ;
        nodeList.push(node) ;
      else
        circleLen = 2.0 * Math::PI * idxR ;
        angleUnitLen = angleRatio ;
        idxMaxA = 1 ;
        while(circleLen / idxMaxA > angleUnitLen)
          idxMaxA *= divideFactor ;
        end
        (0...idxMaxA).each{|idxA|
          node = generateOneNode(idxR, idxA, idxMaxA) ;
          nodeList.push(node) ;
        }
      end
    }
  end

  #--------------------------------------------------------------
  #++
  ## generate one Node
  def generateOneNode(idxR, idxA, idxMaxA)
    offset = getConf(:offset) ;
    gridLength = getConf(:gridLength) ;
    randomSize = getConf(:randomSize) ;
    nodeTagFormat = getConf(:nodeTagFormat) ;

    pos = Geo2D::Point::newByPolar(gridLength * idxR,
                            2.0 * Math::PI * (idxA.to_f / idxMaxA.to_f),
                            offset) ;
    pos.x += (2.0 * randomSize * rand()) - randomSize ;
    pos.y += (2.0 * randomSize * rand()) - randomSize ;
    node = newNode(pos) ;
    tag = nodeTagFormat % [idxR, idxA] ;
    node.addTag(tag) ;
    return node ;
  end

  #--------------------------------------------------------------
  #++
  ## generate Links in Town
  ## @nodeList/@nodeTable にはすでに入っているとする。
  def generateLinks()
    size = getConf(:size) ;
    divideFactor = getConf(:divideFactor) ;

    (1...size).each{|idxR|
      if(idxR == 1) then
        idxMaxA = @nodeTable[idxR].size ;
        (0...idxMaxA).each{|idxA|
          generateOneLink(@nodeTable[idxR-1][0],@nodeTable[idxR][idxA]) ;
        }
      else
        idxMaxA = @nodeTable[idxR-1].size ;
        idxMaxOuterA = @nodeTable[idxR].size ;
        (0...idxMaxA).each{|idxA|
          fromNode = @nodeTable[idxR-1][idxA] ;
          toNode = (idxMaxA == idxMaxOuterA ?
                    @nodeTable[idxR][idxA] :
                    @nodeTable[idxR][idxA * divideFactor]) ;
          generateOneLink(fromNode, toNode) ;
        }
      end
      idxMaxA = @nodeTable[idxR].size ;
      (0...idxMaxA).each{|idxA|
        generateOneLink(@nodeTable[idxR][idxA],
                        @nodeTable[idxR][(idxA + 1 ) % idxMaxA]) ;
      }
    }
  end

  #--------------------------------------------------------------
  #++
  ## generate one Link
  def generateOneLink(fromNode, toNode)
    linkTagFormat = getConf(:linkTagFormat) ;
    linkWidth = getConf(:linkWidth) ;

    link = newLink(fromNode, toNode, linkWidth) ;
    tag = linkTagFormat % [fromNode.tagList.first, toNode.tagList.first] ;
    link.addTag(tag) ;
    return link ;
  end

  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------
  #--============================================================
end # class RadialTown

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
      town = RadialTown.new() ;

      axml = town.to_ArrayedXml() ;
      p axml ;
      xml = ItkXml.to_Xml(axml) ;
      ItkXml::ppp(xml) ;
    end

  end # class TC_Foo < Test::Unit::TestCase
end # if($0 == __FILE__)



