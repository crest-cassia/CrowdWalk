#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Map Node class
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

#--======================================================================
#++
## Node class for Map
class MapNode
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## tag name of XML element
  XmlElementTag = "Node" ;

  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## id
  attr :id, true ;
  ## x-y pos
  attr :pos, true ;
  ## height
  attr :height, true ;
  ## link list
  attr :linkList, true ;
  ## tag list
  attr :tagList, true ;

  #--------------------------------------------------------------
  #++
  ## description of method initialize
  def initialize(id = 0, pos = Geo2D::Point.new(), height = 0.0)
    @pos = Geo2D::Point.new() ;
    @linkList = [] ;
    @tagList = [] ;
    setId(id) ;
    setPos(pos) ;
    setHeight(height) ;
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
  ## set pos
  ## _pos_:: position in Geo2D::Point 
  ## *return*:: self
  def setPos(pos)
    return setXY(pos.x, pos.y) ;
  end

  #--------------------------------------------------------------
  #++
  ## set XY pos
  ## _x_:: X position
  ## _y_:: Y position
  ## *return*:: self
  def setXY(x, y)
    @pos.set(x, y) ;
    return self ;
  end

  #--------------------------------------------------------------
  #++
  ## set height
  ## _height_:: height
  ## *return*:: self
  def setHeight(height)
    @height = height ;
    return self ;
  end

  #--------------------------------------------------------------
  #++
  ## add link
  ## _link_:: an instance of MapLink
  ## *return*:: self
  def addLink(link)
    @linkList.push(link) ;
    return self ;
  end

  #--------------------------------------------------------------
  #++
  ## add tag
  ## _tag_:: tag string
  ## *return*:: self
  def addTag(tag, topP = false)
    if(!@tagList.member?(tag)) then
      if(topP) then
        @tagList.unshift(tag) ;
      else
        @tagList.push(tag) ;
      end
    end
    return self ;
  end

  #--------------------------------------------------------------
  #++
  ## inspect
  def inspect()
    ("\#<MapNode:" +
     "id=#{@id}," +
     "pos=#{@pos}," +
     "height=#{@height}," +
     "tagList=#{@tagList.inspect}," +
     "linkList=#{linkList.map{|link| "link:#{link.id}"}.inspect}>")
  end

  #--------------------------------------------------------------
  #++
  ## gen Arrayed Xml
  ## *return*:: arrayed xml
  def to_ArrayedXml()
    # head part
    axml = [[nil, XmlElementTag, ({ :id => @id,
                                    :x => @pos.x,
                                    :y => @pos.y,
                                    :height => @height })]] ;
    ## tag part
    @tagList.each{|tag|
      axml.push([:tag, tag]) ;
    }

    ## link part
    @linkList.each{|link|
      axml.push([[nil, :link, { :id => link.id } ]]) ;
    }

    return axml ;
  end

end # class MapNode

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

      axml0 = node0.to_ArrayedXml() ;
      p axml0 ;
      xml0 = ItkXml.to_Xml(axml0) ;
      ItkXml::ppp(xml0) ;
    end

  end # class TC_Foo < Test::Unit::TestCase
end # if($0 == __FILE__)
