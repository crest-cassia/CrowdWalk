## -*- Mode: ruby -*-
##Header:
##Title: Generic 2D Geometrical Operations with GML facility
##Author: Itsuki Noda
##Date: 2005/11/22
##EndHeader:
##
##Usage:
#
##EndUsage:

require 'Geo2D.rb' ;
require 'rexml/document' ;
module XML 
  include REXML ;
end

######################################################################

module Geo2D

  ##============================================================
  ## Constants for namespace

  NameSpaceURI_Gml = "http://www.opengis.net/gml" ;
  NameSpacePrefix_Gml = "gml" ;
  GmlTagForm = "#{NameSpacePrefix_Gml}:%s" ;

  GmlVersion = 2.0 ;
  # GmlVersion = 3.0 ;

  if(GmlVersion < 3.0) then
    GmlTagName_exterior = "outerBoundaryIs" ;
    GmlTagName_interior = "innerBoundaryIs" ;
  else
    GmlTagName_exterior = "exterior" ;
    GmlTagName_interior = "interior" ;
  end

  GmlTagName_coordinates = 'coordinates' ;
  GmlTagName_coord = 'coord' ;
  GmlTagName_X = 'X' ;
  GmlTagName_Y = 'Y' ;

  GmlAttrName_srs = 'srsName' ;

  ##============================================================
  ## GeoObjUtil

  module GeoObjUtil

    ##----------------------------------------
    def to_Gml(withNameSpaceUriP = false)
      node = genGmlNode(gmlLocalName(), true, withNameSpaceUriP) ;

      ## add srsName attribute
      node.add_attribute(GmlAttrName_srs, @srsName) if(!@srsName.nil?) ;

      body = gmlBody() ;
      if(body.is_a?(XML::Element)) 
	node.add(body) ;
      else	# in the case body is a list of elements
	body.each{|child|
	  node.add(child) ;
	}
      end
      node ;
    end

    ##----------------------------------------
    def to_GmlStr(indent = 0, withNameSpaceUriP = false)
      gml = to_Gml(withNameSpaceUriP) ;
      str = '' ;
      gml.write(str, indent) ;
      str ;
    end

    ##----------------------------------------
    def gmlLocalName()
      self.class::GmlLocalName ;
    end

    ##----------------------------------------
    def gmlBody()
      raise "gmlBody() is undefined for class #{self.class.to_s}." ;
    end

    ##---------------------------------------
    def genCoordinatesNode()
      genGmlNode(GmlTagName_coordinates) ;
    end

    ##----------------------------------------
    def genGmlNode(localname, prefix = nil, uri = nil)

      prefix = NameSpacePrefix_Gml if (!prefix.is_a?(String)) ;
      uri = NameSpaceURI_Gml if(uri == true) ;

      node = XML::Element::new("%s:%s" % [prefix, localname])
      if(uri)
	node.add_namespace(NameSpacePrefix_Gml, NameSpaceURI_Gml) ;
      end

      node ;
    end

    ##----------------------------------------
    def flexibleXPathFirst(node, localname, tollerantP = true)
      elm = XML::XPath::first(node, (GmlTagForm % localname),
                              {NameSpacePrefix_Gml => NameSpaceURI_Gml}) ;
      if(elm.nil? && tollerantP)
        elm = XML::XPath::first(node, (GmlTagForm % localname)) ;
      end
      elm ;
    end

    ##----------------------------------------
    def flexibleXPathEach(node, localname, tollerantP = true, &block)
      existP = false ;
      XML::XPath::each(node, (GmlTagForm % localname),
                       {NameSpacePrefix_Gml => NameSpaceURI_Gml}){|elm|
        existP = true ;
        block.call(elm) ;
      }

      if(!existP && tollerantP)
        XML::XPath::each(node, (GmlTagForm % localname)) { |elm|
          block.call(elm) ;
        }
      end
    end


    ##----------------------------------------
    def to_Wkt()
      "#{wktTagName()}(#{wktBody()})" ;
    end

    ##----------------------------------------
    def wktTagName() 
      self.class::WktTagName ;
    end

    ##----------------------------------------
    def wktBody() 
      raise "wktBody() is undefined for class #{self.class.to_s}." ;
    end

    ##----------------------------------------
    def scanWktBody(bodyStr) 
      raise "scanWktBody(bodyStr) is undefined for class #{self.class.to_s}." ;
    end

    ##----------------------------------------
    def scanGml(gmlNode)
      raise "scanGml(gmlNode) is undefined for class #{self.class.to_s}." ;
    end

    ##----------------------------------------
    def to_MySQL()
      "GeomFromText(#{to_Wkt()})" ;
    end

  end

  ##============================================================
  ## GeoObject

  class GeoObject
    attr :srsName, true ;

    ##----------------------------------------
    def scanGml(node)
      @srsName = node.attributes[GmlAttrName_srs] ;
      self ;
    end

  end

  ##==================================================
  ## class methods for GeoObject

  class << GeoObject
    attr :gmlNodeTable, true ;
    attr :wktTagTable, true ;
    
    ##----------------------------------------
    def registerAsGmlObj(klass)
      @gmlNodeTable[klass::GmlLocalName] = klass ;
    end

    ##----------------------------------------
    def scanGml(gmlNode)
      prefix = gmlNode.prefix() ;
      namespace = gmlNode.namespace(prefix) ;
      name = gmlNode.name() ;
      if( namespace == NameSpaceURI_Gml || prefix = NameSpacePrefix_Gml)
        klass = @gmlNodeTable[name] ;
        if(!klass.nil?)
          obj = klass::new() ;
          obj.scanGml(gmlNode) ;
          return obj ;
        else
          raise("unknown class name:" + name) ;
        end
      else
        raise("unknown namespace or prefix:" + prefix.to_s + 
                "(" + namespace.to_s + ")") ;
      end
    end

    ##----------------------------------------
    def scanGmlStr(gmlStr)
      doc = XML::Document::new(gmlStr) ;
      scanGml(doc.root) ;
    end

    ##----------------------------------------
    def registerAsWktObj(klass)
      @wktTagTable[klass::WktTagName] = klass ;
    end
    
    ##----------------------------------------
    def scanWkt(text)
      matchp = (text =~ /^\s*([a-zA-Z]+)\s*\((.*)\)\s*$/) ;

      raise("unknown data format for Wkt: " + text) if(!matchp) ;

      tagStr = $1 ;
      bodyStr = $2 ;

      klass = @wktTagTable[tagStr] ;
      if(!klass.nil?)
        obj = klass::new() ;
        obj.scanWktBody(bodyStr) ;
        return obj ;
      else
        raise("unknown class name:" + tagStrK) ;
      end
    end

  end

  GeoObject::gmlNodeTable = {} ;
  GeoObject::wktTagTable = {} ;

  ##============================================================
  ## Point
  ##   GML:
  ##     <gml:Point srsName="A-KIND-OF-SRS">
  ##       <gml:coordinates>XXX.XX,YYY,YY</gml:coordinates>
  ##     </gml:Point>
  ##   Wkt:
  ##     POINT(XXX.XX YYY.YY)


  class Point 

    GmlLocalName = 'Point' ;
    WktTagName = 'POINT' ;

    ##----------------------------------------
    def gmlBody()
      coord = genCoordinatesNode() ;
      coord.add(XML::Text::new(gmlCoordBodyText())) ;
      coord ;
    end

    ##----------------------------------------
    def gmlCoordBodyText()
      "#{@x},#{@y}" ;
    end

    ##----------------------------------------
    def scanGml(node)
      super ;
      coord = flexibleXPathFirst(node, GmlTagName_coordinates) ;

      ## Error if no coord node.
      if(coord.nil?)
        str = '' ; node.write(str) ;
        raise "Illegal XML for #{GmlLocalName}:" + str ;
      end

      scanCoordStr(coord.texts.to_s,',') ;

      self ;
    end

    ##----------------------------------------
    def scanCoordStr(str, separator)
      v = str.split(separator) ;
      set(v[0].to_f, v[1].to_f) ;
      self ;
    end

    ##----------------------------------------
    def wktBody() 
      return "#{@x} #{@y}" ;
    end

    ##----------------------------------------
    def scanWktBody(body) 
      scanCoordStr(body.gsub(/^\s+/,''),/\s+/)
      self ;
    end

  end

  GeoObject::registerAsGmlObj(Point) ;
  GeoObject::registerAsWktObj(Point) ;

  ##============================================================
  ## LineString
  ##   GML:
  ##     <gml:LineString srsName="A-KIND-OF-SRS">
  ##       <gml:coordinates>XXX.XX,YYY,YY XXX.XX,YYY.YY ...</gml:coordinates>
  ##     </gml:LineString>
  ##   WKT:
  ##     LINESTRING(XXX.XX YYY.YY, XXX.XX YYY.YY, ...)

  class LineString
    
    GmlLocalName = 'LineString' ;
    WktTagName = 'LINESTRING' ;
    
    ##----------------------------------------
    def gmlBody()
      coord = genCoordinatesNode() ;
      coord.add(XML::Text::new(gmlCoordBodyText())) ;
      coord ;
    end

    ##----------------------------------------
    def gmlCoordBodyText()
      str = '' ;
      eachPoint{|point|
	str += ' ' if (str != '') ;
	str += point.gmlCoordBodyText() ;
      }
      str ;
    end

    ##----------------------------------------
    def scanGml(node)
      super ;
      coord = flexibleXPathFirst(node, GmlTagName_coordinates) ;

      ## Error if no coord node.
      if(coord.nil?)
        str = '' ; node.write(str) ;
        raise "Illegal XML for #{GmlLocalName}:" + str ;
      end

      coordStr = coord.texts.to_s() ;
      scanCoordStr(coordStr, /\s+/, ',') ;
    end

    ##----------------------------------------
    def scanGml_obsolete(node) ## obsolete
      super ;
      coord = flexibleXPathFirst(node, GmlTagName_coordinates) ;

      ## Error if no coord node.
      if(coord.nil?)
        str = '' ; node.write(str) ;
        raise "Illegal XML for #{GmlLocalName}:" + str ;
      end

      coordStr = coord.texts.to_s() ;
      coordStr.split(/\s+/).each{|cstr|
        point = self.class::PointClass::new() ;
        point.scanCoordStr(cstr, ',') ;
        pushPoint(point) ;
      }

      self ;
    end

    ##----------------------------------------
    def wktBody()
      ret =  "";
      eachPoint{|point|
        ret += "," if(ret != "") ;
        ret += point.wktBody() ;

      }
      ret ;
    end

    ##----------------------------------------
    def wktBodyWithEnv()
      "(" + wktBody() + ")";
    end

    ##----------------------------------------
    def scanWktBody(body)
      scanCoordStr(body, /\s*,\s*/, /\s/) ;
    end

    ##----------------------------------------
    def scanWktBody_obsolete(body) ## obsolete
      body.split(/\s*,\s*/).each{|cstr|
        point = self.class::PointClass::new() ;
        point.scanCoordStr(cstr, /\s/) ;
        pushPoint(point) ;
      }

      self ;
    end

    ##----------------------------------------
    def scanCoordStr(str, pointSep, coordSep)
      str.split(pointSep).each{|pointCoord|
        point = self.class::PointClass::new() ;
        point.scanCoordStr(pointCoord,coordSep) ;
        pushPoint(point) ;
      }
      self ;
    end

  end

  GeoObject::registerAsGmlObj(LineString) ;
  GeoObject::registerAsWktObj(LineString) ;

  ##============================================================
  ## LinearRing
  ##   GML:
  ##     <gml:LineRing srsName="A-KIND-OF-SRS">
  ##       <gml:coordinates>XXX.XX,YYY,YY XXX.XX,YYY.YY ...</gml:coordinates>
  ##     </gml:LineRing>

  class LinearRing < LineString

    GmlLocalName = 'LinearRing' ;
    WktTagName = 'LINEARRING' ;
    
    ##----------------------------------------
    def gmlCoordBodyText()
      str = super() ;
      str += ' ' + @pointList[0].gmlCoordBodyText() if @pointList[0] ;
      str ;
    end

    ##----------------------------------------
    def scanGml(node)
      super ;
      omitRedundantPoint() ;
      self ;
    end

    ##----------------------------------------
    def wktBody()
      ret = super() ;
      ret += ',' + @pointList[0].wktBody() ;
      ret ;
    end

    ##----------------------------------------
    def scanWktBody(body)
      super ;
      omitRedundantPoint() ;
      self ;
    end

  end

  GeoObject::registerAsGmlObj(LinearRing) ;
  GeoObject::registerAsWktObj(LinearRing) ;

  ##============================================================
  ## Polygon
  ##   GML:
  ##     <gml:Polygon srsName="A-KIND-OF-SRS">
  ##       <gml:outerBoundaryIs>
  ##         <gml:LinearRing>
  ##           <gml:coordinates>...<gml:coordinates>
  ##         </gml:LinearRing>
  ##       </gml:outerBoundaryIs>
  ##       <gml:innerBoundaryIs>
  ##         <gml:LinearRing> ... </gml:LinearRing>
  ##       </gml:innerBoundaryIs>
  ##       <gml:innerBoundaryIs> ... </gml:innerBoundaryIs>
  ##     </gml:Polygon>
  ##   WKT:
  ##     POLYGON((XXX.XX YYY.YY, XXX.XX YYY.YY, ...),   # <- exterior
  ##	         (XXX.XX YYY.YY, XXX.XX YYY.YY, ...),   # <- interior1
  ##	         (XXX.XX YYY.YY, XXX.XX YYY.YY, ...),   # <- interior2
  ##	         ...)

  class Polygon

    GmlLocalName = 'Polygon' ;
    WktTagName = 'POLYGON' ;
    
    ##----------------------------------------
    def gmlBody()
      ret = [] ;

      if(@exterior) ;
        exterior = genGmlNode(GmlTagName_exterior) ;
        exterior.add(@exterior.to_Gml) ;
        ret.push(exterior) ;
      end

      eachInterior(){|ring|
	interior = genGmlNode(GmlTagName_interior) ;
	interior.add(ring.to_Gml) ;
	ret.push(interior) ;
      }
      
      ret ;
    end

    ##----------------------------------------
    def scanGml(node)
      super ;

      exterior = flexibleXPathFirst(node, GmlTagName_exterior) ;
      ring = GeoObject::scanGml(exterior.elements[1]) ;
      setExterior(ring) ;

      flexibleXPathEach(node, GmlTagName_interior){|interior|
        ring = GeoObject::scanGml(interior.elements[1]) ;
        pushInteriorRing(ring) ;
      }

      self ;
    end

    ##----------------------------------------
    def wktBody()
      ret = '' ;
      
      ret =  @exterior.wktBodyWithEnv() if(@exterior) ;

      eachInterior(){|ring|
        ret += ","  + ring.wktBodyWithEnv() ;
      }
      ret ;
    end

    ##----------------------------------------
    def scanWktBody(body)
      ringList = body.split(/\)\s*,\s*\(/) ;

      ring = RingClass::new() ;
      ring.scanWktBody(ringList[0].gsub(/^\s*\(/,'')) ;
      
      setExterior(ring) ;

      ringList.shift() ;
      ringList.each{|ringBodyStr|
        ring = RingClass::new() ;
        ring.scanWktBody(ringBodyStr.gsub(/^\)/,'')) ;
        pushInteriorRing(ring) ;
      }

      self ;
    end


  end
  GeoObject::registerAsGmlObj(Polygon) ;
  GeoObject::registerAsWktObj(Polygon) ;


  ##============================================================
  ## Box
  ##   GML:
  ##     <gml:Box srsName="A-KIND-OF-SRS">
  ##       <gml:coord>
  ##         <gml:X>XXX.XX</gml:X>
  ##         <gml:Y>YYY.YY</gml:Y>
  ##       </gml:coord>
  ##       <gml:coord>
  ##         <gml:X>XXX.XX</gml:X>
  ##         <gml:Y>YYY.YY</gml:Y>
  ##       </gml:coord>
  ##     </gml:Box>

  class Box

    GmlLocalName = 'Box' ;

    
    ##----------------------------------------
    def gmlBody_old()

      coord0 = genGmlNode(GmlTagName_coord) ;
      x = coord0.add(genGmlNode(GmlTagName_X)) ;
      x.add(XML::Text::new(@minPos.x.to_s)) ;
      y = coord0.add(genGmlNode(GmlTagName_Y)) ;
      y.add(XML::Text::new(@minPos.y.to_s)) ;

      coord1 = genGmlNode(GmlTagName_coord) ;
      x = coord1.add(genGmlNode(GmlTagName_X)) ;
      x.add(XML::Text::new(@maxPos.x.to_s)) ;
      y = coord1.add(genGmlNode(GmlTagName_Y)) ;
      y.add(XML::Text::new(@maxPos.y.to_s)) ;

      return [coord0, coord1] ;
    end

    ##----------------------------------------
    def gmlBody()

      coord = genGmlNode(GmlTagName_coordinates) ;
      coord.add(XML::Text::new(("%f,%f %f,%f" % 
                                  [@minPos.x, @minPos.y, 
                                   @maxPos.x, @maxPos.y]))) ;
      return [coord] ;
    end

    ##----------------------------------------
    def scanGml(node)
      super ;

      c = 0 ;
      flexibleXPathEach(node, GmlTagName_coord){|coord|
        xnode = flexibleXPathFirst(coord,GmlTagName_X) ;
        ynode = flexibleXPathFirst(coord,GmlTagName_Y) ;
        case(c)
        when 0 ;
          @minPos.set(xnode.texts.to_s.to_f, ynode.texts.to_s.to_f) ;
        when 1 ;
          @maxPos.set(xnode.texts.to_s.to_f, ynode.texts.to_s.to_f) ;
        end
        c += 1 ;
      }

      self ;
    end

  end
  GeoObject::registerAsGmlObj(Box);

end


