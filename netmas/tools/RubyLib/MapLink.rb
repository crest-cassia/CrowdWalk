#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Map Link class
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
require 'MapNode.rb' ;

#--======================================================================
#++
## Link class for Map
class MapLink
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## tag name of XML element
  XmlElementTag = "Link" ;

  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## id
  attr :id, true ;
  ## from Node
  attr :fromNode, true ;
  ## to Node
  attr :toNode, true ;
  ## length
  attr :length, true ;
  ## width
  attr :width, true ;
  ## tag list
  attr :tagList, true ;

  #--------------------------------------------------------------
  #++
  ## description of method initialize
  def initialize(id = 0, fromNode = nil, toNode = nil, width = 0.0)
    @tagList = [] ;
    setId(id) ;
    setFromToNode(fromNode, toNode) ;
    setWidth(width) ;
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
  ## set from and to Node
  ## _fromNode_:: from MapNode
  ## _toNode_:: to MapNode
  ## *return*:: self
  def setFromToNode(fromNode, toNode)
    @fromNode = fromNode ;
    @toNode = toNode ;
    if(@fromNode && @toNode) then
      setLength(@fromNode.pos.distanceTo(@toNode.pos)) ;
    end
    @fromNode.addLink(self) ;
    @toNode.addLink(self) ;
    return self ;
  end

  #--------------------------------------------------------------
  #++
  ## set width
  ## _width_:: width
  ## *return*:: self
  def setWidth(width)
    @width = width ;
    return self ;
  end

  #--------------------------------------------------------------
  #++
  ## set width
  ## _width_:: width
  ## *return*:: self
  def setLength(length)
    @length = length ;
    return self ;
  end

  #--------------------------------------------------------------
  #++
  ## add tag
  ## _tag_:: tag string
  ## *return*:: self
  def addTag(tag)
    @tagList.push(tag) ;
    return self ;
  end

  #--------------------------------------------------------------
  #++
  ## fromNode かどうか
  def isFromNode(node)
    node == @fromNode ;
  end

  #--------------------------------------------------------------
  #++
  ## toNode かどうか
  def isToNode(node)
    node == @toNode ;
  end

  #--------------------------------------------------------------
  #++
  ## 反対側のNode
  def getAnotherNode(node)
    if(isFromNode(node))
      return @toNode ;
    elsif(isToNode(node))
      return @fromNode ;
    else
      raise "The link has not this node:" + @toNode ;
    end
  end


  #--------------------------------------------------------------
  #++
  ## inspect
  def inspect()
    ("\#<MapLink:" +
     "id=#{@id}," +
     "fromNode=node:#{@fromNode.id}," +
     "toNode=node:#{@toNode.id}," +
     "length=#{@length}," +
     "width=#{@width}," +
     "tagList=#{@tagList.inspect}>")
  end

  #--------------------------------------------------------------
  #++
  ## gen Arrayed Xml
  ## *return*:: arrayed xml
  def to_ArrayedXml()
    # head part
    axml = [[nil, XmlElementTag, ({ :id => @id,
                                    :from => @fromNode.id,
                                    :to => @toNode.id,
                                    :length => @length,
                                    :width => @width })]] ;
    ## tag part
    @tagList.each{|tag|
      axml.push([:tag, tag]) ;
    }

    return axml ;
  end

end # class MapLink

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
      node0 = MapNode.new(0,Geo2D::Point.new(10.0,15.0)) ;
      node0.addTag("foo") ;
      node0.addTag("bar") ;

      node1 = MapNode.new(1,Geo2D::Point.new(20.0,25.0)) ;
      node1.addTag("aho") ;
      node1.addTag("baka") ;

      link2 = MapLink.new(2,node0, node1, 1.0) ;
      link2.addTag("bbb") ;
      link2.addTag("123") ;

      axml = link2.to_ArrayedXml() ;
      p axml ;
      xml = ItkXml.to_Xml(axml) ;
      ItkXml::ppp(xml) ;
    end

  end # class TC_Foo < Test::Unit::TestCase
end # if($0 == __FILE__)
