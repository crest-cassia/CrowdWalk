# coding: euc-jp
## -*- Mode: ruby -*-
##Header:
##Title: Generic 2D Geometrical Operations
##Author: Itsuki Noda
##Date: 2005/11/12
##EndHeader:
##
##Usage:
#
##EndUsage:

######################################################################

module Geo2D

  ##============================================================
  ## constants

  PI = Math::PI ;
  DoublePI = 2.0 * PI ;
  HalfPI = PI / 2.0 ;

  Deg2Rad = PI / 180.0 ;
  Rad2Deg = 180.0 / PI ;

  HugeAngle = 1.0e5 ;		# used in normalizeAngle

  ##============================================================
  ## utilities

  module Utility

    ##------------------------------------------------------------
    def deg2rad(deg)
      deg * Deg2Rad ;
    end

    ##------------------------------------------------------------
    def rad2deg(rad)
      rad * Rad2Deg ;
    end

    ##------------------------------------------------------------

    def normalizeAngle(ang)
      ang = 0.0 if (ang > HugeAngle || ang < -HugeAngle) ;
      ang += DoublePI while(ang < -PI) ;
      ang -= DoublePI while(ang >  PI) ;
      ang ;
    end

    ##------------------------------------------------------------
    def normalizeAngleDeg(ang)
      ang += 360.0 while(ang < -180.0) ;
      ang -= 360.0 while(ang >  180.0) ;
      ang ;
    end

    ##------------------------------------------------------------
    def min(*value)
      minV = nil ;
      value.each{|v|
        minV = v if (!v.nil? && (minV.nil? || minV > v)) ;
      }
      minV ;
    end

    def max(*value)
      maxV = nil ;
      value.each{|v|
        maxV = v if (!v.nil? && (maxV.nil? || maxV < v)) ;
      }
      maxV ;
    end

    ##------------------------------------------------------------
    def abs(value)
      (value >= 0.0) ? value : -value ;
    end

    ##------------------------------------------------------------
    def fltRand(min, max)
      w = max - min ;
      return min + w * rand(0) ;
    end
    
    ##------------------------------------------------------------
    def isAnglesInOrder(first, second, third)
      # suppose each angle is less than PI if angles are in order.
      angleFirstSecond = normalizeAngle(second - first) ;
      angleSecondThird = normalizeAngle(third - second) ;
      return angleFirstSecond >= 0.0 && angleSecondThird >= 0.0 ;
    end

  end

  include Utility ;

  ##============================================================
  ## GeoObjUtil

  module GeoObjUtil
    include Utility ;

    ##----------------------------------------
    ## generate boundary box
    def bbox()
      bbox = Geo2D::Box::new() ;
      bbox.setXY(minX(),minY(),maxX(),maxY()) ;
      bbox ;
    end

    ##----------------------------------------
    ## min/max X/Y

    def minX()
      raise "minX() has not been defined in class : #{self.class().to_s}"
    end

    def maxX()
      raise "maxX() has not been defined in class : #{self.class().to_s}"
    end

    def minY()
      raise "minY() has not been defined in class : #{self.class().to_s}"
    end

    def maxY()
      raise "maxY() has not been defined in class : #{self.class().to_s}"
    end

    ##----------------------------------------
    ## mid/size X/Y

    def midX()
      (minX() + maxX())/2.0 ;
    end

    def sizeX()
      (maxX() - minX()) ;
    end

    def midY()
      (minY() + maxY())/2.0 ;
    end

    def sizeY()
      (maxY() - minY()) ;
    end

    ##----------------------------------------
    ## midPoint, size
    def midPoint
      Geo2D::Point.new(midX(), midY()) ;
    end

    def size
      Geo2D::Vector.new(sizeX(), sizeY()) ;
    end

    ##----------------------------------------
    ## length
    def length()
      raise "length() has not been defined in class : #{self.class().to_s}"
    end

    ##----------------------------------------
    ## aera
    def grossArea()
      raise "grossArea() has not been defined in class : #{self.class().to_s}"
    end

  end

  ##============================================================
  ## GeoObject  
  ##    abstract class for general geometry object

  class GeoObject

    include GeoObjUtil
  end

  ##============================================================
  ## Vector

  class Vector < GeoObject

    attr :x, true ;
    attr :y, true ;

    DefaultOrigin = [0.0, 0.0] ;

    ##----------------------------------------
    ## init
    def initialize(x = 0.0, y = 0.0)
      set(x,y) ;
    end

    ##----------------------------------------
    ## set
    def set(x, y)
      @x = x ; 
      @y = y ;
      self ;
    end

    def setByPolar(rad,ang, origin = DefaultOrigin)
      origin = self.class::sureGeoObj(origin) ;
      set(rad * Math::cos(ang) + origin.x, 
          rad * Math::sin(ang) + origin.y) ;
    end

    def setByPolarDeg(rad,ang, origin = DefaultOrigin)
      setByPolar(rad,deg2rad(ang), origin) ;
    end

    ##----------------------------------------
    def dup()
      clone() ; 
    end

    ##----------------------------------------
    ## inc/dec/amplify/decay
    def inc(v)
      @x += v.x ;
      @y += v.y ;
      self ;
    end

    def dec(v) 
      @x -= v.x ;
      @y -= v.y ;
      self
    end

    def amplify(a)
      if(a.is_a?(Geo2D::Vector))
	@x *= a.x ;
	@y *= a.y ;
      else
	@x *= a ;
	@y *= a ;
      end
      self ;
    end

    def decay(d)
      @x /= d ;
      @y /= d ;
      self ;
    end

    ##----------------------------------------
    ## +,-,*,/

    def +(v)
      u = dup() ;
      u.inc(v) ;
    end

    def -(v)
      u = dup() ;
      u.dec(v) ;
    end

    def *(a)
      u = dup() ;
      u.amplify(a) ;
    end

    def /(d)
      u = dup() ;
      u.decay(d) ;
    end

    ##----------------------------------------
    def innerProd(v)
      @x * v.x + @y * v.y ;
    end

    ##----------------------------------------
    def norm()
      Math::sqrt(@x ** 2 + @y ** 2) ;
    end

    ##----------------------------------------
    def distanceTo(v)
      Math::sqrt((@x - v.x) ** 2 + (@y - v.y) ** 2) ;
    end

    alias distanceFrom distanceTo ;

    ##----------------------------------------
    def unit()
      v = dup() ;
      v.unit!()
    end

    def unit!()
      d = norm() ;
      decay(d) ;
    end

    ##----------------------------------------
    ## diff
    def diffFrom(v)
      self - v ;
    end

    def diffTo(v)
      v - self ;
    end

    ##----------------------------------------
    ## angle
    def angle()
      Math::atan2(@y, @x) ;
    end

    def angleDeg()
      rad2deg(angle()) ;
    end

    ##----------------------------------------
    ## angle From/To
    def angleTo(v, originDir = 0.0)
      normalizeAngle(Math::atan2(v.y - @y, v.x - @x) - originDir) ;
    end

    def angleFrom(v, originDir = 0.0)
      v.angleTo(self, originDir) ;
    end

    def angleFromDeg(v, originDir = 0.0)
      rad2deg(angleFrom(v, originDir)) ;
    end

    def angleToDeg(v, originDir = 0.0)
      rad2deg(angleTo(v, originDir)) ;
    end

    ##----------------------------------------
    def angleDiff(fromPos, toPos)
      angleTo(toPos, angleTo(fromPos)) ;
    end

    ##----------------------------------------
    ## rotate
    def rotate(ang)
      v = dup() ;
      v.rotate!(ang) ;
    end

    def rotate!(ang)
      c = Math::cos(ang) ;
      s = Math::sin(ang) ;
      set((c  * @x - s * @y),(s * @x + c * @x)) ;
    end

    def rotateDeg(ang)
      rotate(deg2rad(ang)) ;
    end

    def rotateDeg!(ang)
      rotate!(deg2rad(ang)) ;
    end
    
    ##----------------------------------------
    ## min/max X/Y

    def minX() ; @x ; end
    def maxX() ; @x ; end
    def minY() ; @y ; end
    def maxY() ; @y ; end

    ##----------------------------------------
    def to_s(simpleP = false)
      if(simpleP)
        "(#{@x},#{@y})" ;
      else
        "#V[#{@x},#{@y}]" ;
      end
    end
    
  end

  ##==================================================
  ## class methods for Vector

  class << Vector
    DefaultOrigin = [0.0, 0.0] ;
    ##----------------------------------------
    def newByPolar(rad = 0.0, ang = 0.0, origin = DefaultOrigin)
      v = self.new() ;
      v.setByPolar(rad, ang, origin) ;
    end

    ##----------------------------------------
    def newByPolarDeg(rad = 0.0, ang = 0.0)
      v = self.new() ;
      v.setByPolarDeg(rad,ang) ;
    end

    ##----------------------------------------
    def sureGeoObj(vec)
      if(vec.is_a?(self))
        return vec ;
      elsif(vec.is_a?(Vector))
        return vec ;
      elsif(vec.is_a?(Array))
        return self.new(vec[0],vec[1]) ;
      else
        raise "Error:#{vec} is not a Vector or Array." ;
      end
    end

  end

  ##============================================================
  ## Point

  class Point < Vector

    ##----------------------------------------
    def to_s(simpleP = false)
      if(simpleP)
        super(simpleP) ;
      else
        "#Point[#{@x},#{@y}]" ;
      end
    end
    
  end

  ##============================================================
  ## class method for Point
  class << Point

    ##----------------------------------------
    ## find ConvexHull point by Graham Scan
    def findConvexHullPointList(pointList)
      ## find base point, which has minimum Y value
      basePoint = pointList[0] ;
      pointList.each{|point|
        basePoint = point if(basePoint.y > point.y) ;
      }

      ## sort by angle from the base point.
      listForSort = [] ;
      pointList.each{|point|
        next if (basePoint == point) ;
        listForSort.push([basePoint.angleTo(point),point]) ;
      } ;

      sortedList = listForSort.sort ;
      sortedList.unshift([0,basePoint]) ;

      ## scan convex
      convexList = [] ;
      sortedList.each{|pointInfo|
        point = pointInfo[1] ;

        ## fill untill 3 points
        if(convexList.length < 3)
          convexList.push(point) ;
          next ;
        end

        ## remove last points if adding new point cover them.
        while(convexList.length >2) 
          point0 = convexList[-2] ;
          point1 = convexList[-1] ;
      
          angle = point0.angleDiff(point1,point) ;
          break if(angle > 0.0) ;  ## negative angle means point1 is inside.
          convexList.pop ;
        end
        convexList.push(point) ;
      }

      return convexList ;
    end
  end

  ##============================================================
  ## LineSegment

  class LineSegment < GeoObject

    attr :u, true ;
    attr :v, true ;

    PointClass = Point ;
    VectorClass = Vector ;

    ##----------------------------------------
    def initialize(u = self.class::PointClass::new(), 
                   v = self.class::PointClass::new(),
                   dupP = false)
      set(u,v,dupP) ;
    end

    ##----------------------------------------
    def set(u,v,dupP = false)
      u = PointClass::sureGeoObj(u) ;
      v = PointClass::sureGeoObj(v) ;
      if(dupP)
        @u = u.dup() ;
        @v = v.dup() ;
      else
        @u = u ;
        @v = v ;
      end
      self ;
    end

    ##----------------------------------------
    def setXY(ux,uy,vx,vy)
      @u.set(ux,uy) ;
      @v.set(vx,vy) ;
      self ;
    end

    ##----------------------------------------
    def dup(deepP = true)
      l = clone() ; 
      if(deepP)
        l.u = @u.dup() ;
        l.v = @v.dup() ;
      end
      l ;
    end

    ##----------------------------------------
    def reverse(dupP = false)
      rev = dup(dupP) ;
      w = rev.u ;
      rev.u = rev.v ;
      rev.v = w ;
      rev ;
    end

    ##----------------------------------------
    def shift(drift, dir = :left)   # dir = :right | :left | angleInRad
      dir = -HalfPI if(dir == :right) ;
      dir =  HalfPI if(dir == :left) ;

      newLine = dup(true) ;

      absDir = angle() + dir ;
      diff = Vector::newByPolar(drift, absDir) ;

      newLine.u.inc(diff) ;
      newLine.v.inc(diff) ;

      newLine ;
    end

    ##----------------------------------------
    def length()
      @u.distanceTo(@v)
    end

    ##----------------------------------------
    def diffVector()
      @v - @u ;
    end

    ##----------------------------------------
    ## angle
    def angle()
      diffVector.angle() ;
    end

    def angleDeg()
      rad2deg(angle()) ;
    end

    ##----------------------------------------
    ## 垂線の足のある位置の u からの比率 k を求める
    def footPointRatioFrom(point)
      dx = @u.x - @v.x ;
      dy = @u.y - @v.y ;
      rx = @u.x - point.x ;
      ry = @u.y - point.y ;
      d = (dx * dx + dy * dy) ;
      if(d == 0.0)	# to avoid zero divide
        k = 0.0 ;
      else
        k = (dx * rx + dy * ry) / d ;
      end
    end

    ##----------------------------------------
    ## 垂線の足のある位置。extendP が false の時は線分としての最近点
    def footPointFrom(point,extendP = false)
      k = footPointRatioFrom(point) ;

      if(extendP)
        foot = @u + diffVector().amplify(k) ;
      else
        if    (k < 0.0)
          foot = @u.dup() ;
        elsif (k > 1.0)
          foot = @v.dup() ;
        else
          foot = @u + diffVector().amplify(k) ;
        end
      end

      foot ;
    end

    ##----------------------------------------
    ## ある位置からの最短距離
    def distanceFromPoint(point,extendP = false)
      foot = footPointFrom(point,extendP) ;
      point.distanceTo(foot) ; 
    end

    ##----------------------------------------
    ## ある線分からの最短距離
    def distanceFromLine(line)
      return 0.0 if(crossWithLine?(line)) ;

      d0 = distanceFromPoint(line.u) ;
      d1 = distanceFromPoint(line.v) ;
      d2 = line.distanceFromPoint(@u) ;
      d3 = line.distanceFromPoint(@v) ;
      return min(d0,d1,d2,d3) ;
    end

    ##----------------------------------------
    ## ある点が線上にあるか？
    def onLine?(point, extendP = false, margin = 0.0)
      abs(distanceFromPoint(point, extendP)) <= margin ;
    end

    ##----------------------------------------
    ## ある点が、線分(方向 u->v)に対し左/右側にあるか？
    def isLeft?(point)
      angle0 = @u.angleTo(@v) ;
      angle1 = @u.angleTo(point) ;
      normalizeAngle(angle1 - angle0) > 0.0 ;
    end

    def isRight?(point)
      angle0 = @u.angleTo(@v) ;
      angle1 = @u.angleTo(point) ;
      normalizeAngle(angle1 - angle0) < 0.0 ;
    end

    ##----------------------------------------
    ## ある点が、線分に対し面しているか？（両端点の内側にあるか？）
    def faceTo?(point)
      k = footPointRatioFrom(point) ;
      (0.0 <= k && k <= 1.0) ;
    end

    ##----------------------------------------
    ## ある線分と交わるか？
    def crossWithLine?(line, 
                       countParaP = true, # count paralel ?
                       countEndP = :both) # :both/:head/:tail/:none
                                 
      dx0 = @u.x - @v.x ;
      dy0 = @u.y - @v.y ;
      dx1 = line.u.x - line.v.x ;
      dy1 = line.u.y - line.v.y ;
      dxv = line.v.x - @v.x ;
      dyv = line.v.y - @v.y ;

      dd = dx0 * dy1 - dx1 * dy0 ;
      if(dd == 0.0)	## 平行線
        ## どちらかの端点が他方の線分上にあるかどうかで判定
        return (onLine?(line.u) || onLine?(line.v) || 
                  line.onLine?(@u) || line.onLine?(@v)) && countParaP
      else
        dp = dy1 * dxv - dx1 * dyv ;
        dq = dy0 * dxv - dx0 * dyv ;
      
        p = dp / dd ; # self 上の交点の位置(uからの比率)
        q = dq / dd ; # line 上の交点の位置(uからの比率)
        return false if !isInRange?(p, countEndP) ; # p が [0,1] になければだめ
        return false if !isInRange?(q, countEndP) ; # q が [0,1] になければだめ
        return true ;
      end
    end

    ##------------------------------
    ## value が [0,1], [0,1), (0,1], (0,1) に入っているか？
    ##   mode = :both, :head, :tail, :none
    def isInRange?(value, mode = :both, head = 0.0, tail = 1.0)
      case mode
      when :both ; return (head <= value && value <= tail) ;
      when :head ; return (head <= value && value <  tail) ;
      when :tail ; return (head <  value && value <= tail) ;
      else       ; return (head <  value && value <  tail) ;
      end
    end

    ##----------------------------------------
    ## min/max X/Y

    def minX() ; min(@u.x,@v.x) ; end
    def maxX() ; max(@u.x,@v.x) ; end
    def minY() ; min(@u.y,@v.y) ; end
    def maxY() ; max(@u.y,@v.y) ; end

    ##----------------------------------------
    def to_s()
      "#LineSegment[#{@u.to_s(true)}:#{@v.to_s(true)}]" ;
    end
    
  end

  ##==================================================
  ## class methods for LineSegment

  class << LineSegment
    ##----------------------------------------
    def newByXY(ux = 0.0, uy = 0.0, vx = 0.0, vy = 0.0)
      l = self.new() ;
      l.setXY(ux, uy, vx, vy) ;
    end

    ##----------------------------------------
    def newByOrgAngle(org = self::PointClass::new(), 
                      ang = 0.0, len = 1.0, dupP = false) 
      dest = self::PointClass::newByPolar(len, ang) ;
      dest.inc(org) ;
      self.new(org,dest,dupP) ;
    end

    ##----------------------------------------
    def newByOrgAngleDeg(org = self::PointClass::new(), 
                         ang = 0.0, len = 1.0, dupP = false) 
      newByOrgAngle(org, deg2rad(ang), len, dupP) ;
    end

  end

  ##============================================================
  ## LineString

  class LineString < GeoObject

    attr :pointList,	true ;

    PointClass = Point ;
    LineClass = LineSegment ;

    ##----------------------------------------
    def initialize(pointList = [], dupP = false)
      set(pointList,dupP) ;
    end

    ##----------------------------------------
    def set(pointList,dupP = false)
      (0...pointList.length).each{|i|
	pointList[i] = PointClass::sureGeoObj(pointList[i]) ;
      }
      if(dupP)
        @pointList = [] ;
        pushPointList(pointList,dupP) ;
      else
        @pointList = pointList ;
      end
      self ;
    end

    ##----------------------------------------
    def setXY(xyList) # xyList = [[x0,y0],[x1,y1]...]
      @pointList = [] ;
      pushPointListXY(xyList) ;
    end

    ##----------------------------------------
    def pushPoint(point, dupP = false)
      if(dupP)
        @pointList.push(point.dup()) ; 
      else
        @pointList.push(point) ;
      end
      self ;
    end

    ##----------------------------------------
    def pushPointList(pointList, dupP = false)
      pointList.each{|point| 
        pushPoint(point, dupP) ;
      }
      self ;
    end

    ##----------------------------------------
    def pushPointXY(x,y)
      pushPoint(self.class::PointClass.new(x,y)) ; 
    end

    ##----------------------------------------
    def pushPointListXY(pointList) # pointList = [[x0,y0],[x1,y1]...]
      pointList.each{|xy|
        pushPointXY(xy[0],xy[1]) ;
      }
      self ;
    end

    ##----------------------------------------
    def dup()
      linestring = clone() ;
      linestring.set(@pointList, true) ;
      linestring ;
    end

    ##----------------------------------------
    def nPoints()
      @pointList.size() ;
    end

    ##----------------------------------------
    def nthPoint(n)
      @pointList[n % nPoints()] ;
    end

    ##----------------------------------------
    def firstPoint()
      nthPoint(0) ;
    end

    ##----------------------------------------
    def lastPoint()
      nthPoint(-1) ;
    end

    ##----------------------------------------
    def eachPoint(&block)
      @pointList.each(&block) ;
    end

    ##----------------------------------------
    def nLines()
      @pointList.size() - 1;
    end

    ##----------------------------------------
    def nthLine(n)
      self.class::LineClass::new(nthPoint(n), nthPoint(n+1)) ;
    end

    ##----------------------------------------
    def firstLine()
      nthLine(0) ;
    end

    ##----------------------------------------
    def lastLine()
      nthLine(-2) ;
    end

    ##----------------------------------------
    def eachLine(&block)
      prePoint = nil ;
      @pointList.each{|point|
        if(!prePoint.nil?)
          block.call(self.class::LineClass::new(prePoint,point)) ;
        end
        prePoint = point ;
      }
    end
    
    ##----------------------------------------
    ## ある位置からの最短距離
    def distanceFrom(object)
      if(object.is_a?(Vector))
        return distanceFromPoint(object) ;
      elsif(object.is_a?(LineSegment))
        return distanceFromLine(object) ;
      elsif(object.is_a?(LineString))
        return distanceFromLineString(object)
      elsif(object.is_a?(Polygon))
        return distanceFromLineString(object.exterior) ;
      else
        raise("unsupported object type for distanceFrom: " + object.to_s) ;
      end
    end

    ##----------------------------------------
    ## ある位置からの最短距離
    def distanceFromPoint(point)
      dist = nil ;
      eachLine{|line|
        d = line.distanceFromPoint(point) ;
        dist = min(dist, d) ;
      }
      dist ;
    end

    ##----------------------------------------
    ## ある線分からの最短距離
    def distanceFromLine(line)
      dist = nil ;
      eachLine{|l|
        d = l.distanceFromLine(line) ;
        dist = min(dist, d) ;
      }
      dist ;
    end

    ##----------------------------------------
    ## ある LineStringからの最短距離
    def distanceFromLineString(lstring)
      dist = nil ;
      eachPoint{|point|
        d = lstring.distanceFromPoint(point) ;
        dist = min(dist, d) ;
      }
      lstring.eachPoint{|point|
        d = distanceFromPoint(point) ;
        dist = min(dist, d) ;
      }
      dist ;
    end

    ##----------------------------------------
    ## ある点が線上にあるか？
    def onLine?(point, extendP = false, margin = 0.0)
      eachLine{|line|
        return true if(line.onLine?(point, extendP, margin)) ;
      }
      return false ;
    end

    ##----------------------------------------
    ## ある線分と交わるか？
    ##    注意：接している場合はカウントしない
    ##          line 上に重なる辺があった場合に間違う場合あり。
    def crossWithLine?(line)
      eachLine{|l|
        return true if(l.crossWithLine?(line, false, :none)) ;
      }
      return false ;
    end

    ##----------------------------------------
    ## ある線分と交わる回数
    ##   注意：line が頂点で接していたり、ある辺と重なっている場合
    ##         間違う場合がある。
    def countCrossingWithLine(line)
      count = 0 ;
      eachLine{|l|
        count += 1 if(l.crossWithLine?(line, false, :head)) ;
      }
      count ;
    end

    ##----------------------------------------
    ## min/max X/Y

    def minX() 
      x = nil ;
      eachPoint(){|point|
        x = min(x, point.x) ;
      }
      x ;
    end

    def maxX() 
      x = nil ;
      eachPoint(){|point|
        x = max(x, point.x) ;
      }
      x ;
    end

    def minY() 
      y = nil ;
      eachPoint(){|point|
        y = min(y, point.y) ;
      }
      y ;
    end

    def maxY() 
      y = nil ;
      eachPoint(){|point|
        y = max(y, point.y) ;
      }
      y
    end

    ##----------------------------------------
    def length()
      len = 0.0 ;
      eachLine(){|line|
        len += line.length() ;
      }
      return len ;
    end

    ##----------------------------------------
    def to_s()
      str = "#LineString[" ;
      c = 0;
      eachPoint{|point|
        str += ":" if (c > 0) ;
        str += point.to_s(true) ;
        c += 1 ;
      }
      str += "]" ;
      str ;
    end

  end

  ##==================================================
  ## class methods for LineString

  class << LineString
    ##----------------------------------------
    def newByXY(pointList = []) # pointList = [[x0,y0],[x1,y1]...]
      l = self.new() ;
      l.setXY(pointList) ;
    end

  end

  ##============================================================
  ## LinearRing

  class LinearRing < LineString 

    ##----------------------------------------
    def nLines()
      @pointList.size() ;
    end

    ##----------------------------------------
    def lastLine()
      nthLine(-1) ;
    end

    ##----------------------------------------
    def eachLine(&block)
      (0...nLines()).each{|i|
        block.call(self.class::LineClass::new(nthPoint(i), nthPoint(i+1))) ;
      }
    end
   
    ##----------------------------------------
    def insidePoint?(point)
      bb = bbox() ;
      d = bb.maxPos() - bb.minPos() ;
      outerPoint = bb.maxPos + d ;
      line = Geo2D::LineSegment.new(point, outerPoint) ;
      crossingCount = countCrossingWithLine(line) ;
      
      return crossingCount % 2 == 1 ;
    end

    ##----------------------------------------
    def omitRedundantPoint()
      omitP = false ;
      firstPoint = @pointList.first() ;
      lastPoint = @pointList.last() ;
      if(@pointList.length > 1 && firstPoint.distanceTo(lastPoint) == 0.0) 
        @pointList.pop() 
        omitRedundantPoint() ;
        omitP = true ;
      end

      return omitP ;
    end

    ##----------------------------------------
    def clockwise?()
      sumAngle = 0.0 ;
      preLineAngle = lastLine().angle() ;
      eachLine(){|line|
        lineAngle = line.angle() ;
        sumAngle += normalizeAngle(lineAngle - preLineAngle) ;
        preLineAngle = lineAngle ;
      }

      (sumAngle > 0.0) ;
    end

    ##----------------------------------------
    ## calc surrounding area by Helon's theory
    def grossArea()
      area = 0.0 ;
      pointA = nthPoint(0) ;
      pointB = nthPoint(1) ;
      lenAB = pointA.distanceTo(pointB) ;
      (2...nPoints()).each{|k|
        pointC = nthPoint(k) ;
        lenBC = pointB.distanceTo(pointC) ;
        lenCA = pointC.distanceTo(pointA) ;
        lenS = (lenAB + lenBC + lenCA)/2.0 ;
        a =Math::sqrt(lenS * (lenS-lenAB) * (lenS-lenBC) * (lenS-lenCA)) ;
        triangle = LinearRing.new([pointA,pointB,pointC]) ;
        area += (triangle.clockwise?() ? a : -a) ;
      }
      return area ;
    end

    ##----------------------------------------
    def to_s()
      str = "#LinearRing[@" ;
      eachPoint{|point|
        str += ":" ;
        str += point.to_s(true) ;
      }
      str += ":@]" ;
      str ;
    end

  end

  ##============================================================
  ## Polygon
  ##	interior の扱いについてはまだ未実装

  class Polygon < GeoObject

    attr :exterior, true ;
    attr :interior, true ;

    RingClass = LinearRing ;
    
    ##----------------------------------------
    def initialize(exterior = [], # array of Point
                   interior = [], # array of Ring
                   dupP = false)
      setExterior(exterior, dupP) ;
      setInterior(interior, dupP) ;
    end

    ##----------------------------------------
    def setExterior(exterior, dupP = false) 
      if(exterior.is_a?(RingClass))
        if(dupP)
          @exterior = exterior.dup() ;
        else
          @exterior = exterior ;
        end
      elsif(exterior.is_a?(Array))
        if(exterior[0].is_a?(RingClass::PointClass))
          @exterior = self.class::RingClass.new(exterior, dupP) ;
        else
          @exterior = self.class::RingClass.newByXY(exterior) ;
        end
      else
        raise("Illegal exterior value: " + exterior.to_s) ;
      end
    end

    ##----------------------------------------
    def setExteriorXY(exterior)
      setExterior(exterior) ;
    end
      
    ##----------------------------------------
    def setInterior(interior, dupP = false)
      if(dupP)
        @interior = [] ;
        pushInterior(interior, dupP) ;
      else
        @interior = interior ;
      end
      self ;
    end

    ##----------------------------------------
    def setInteriorXY(xyList) # xyList=[[[x00,y00],[x01,y01]..],[[x10,y10]..]]
      @interior = [] ;
      pushInteriorXY(xyList) ;
    end
    
    ##----------------------------------------
    def pushInterior(interior, dupP = false)
      interior.each{|ring|
        pushInteriorRing(ring, dupP) ;
      }
      self ;
    end

    ##----------------------------------------
    def pushInteriorXY(xyList)
      xyList.each{|ringXY|
        pushInteriorRingXY(ringXY) ;
      }
      self ;
    end

    ##----------------------------------------
    def pushInteriorRingXY(xyList)
      pushInteriorRing(self.class::RingClass.newByXY(xyList)) ;
    end

    ##----------------------------------------
    def pushInteriorRing(ring, dupP = false)
      if(dupP)
        @interior.push(ring.dup) ;
      else
        @interior.push(ring) ;
      end
      self ;
    end

    ##----------------------------------------
    def dup()
      polygon = super() ;
      polygon.setInterior(@interior, true) ;
      polygon ;
    end

    ##----------------------------------------
    def eachInterior(&block) 
      @interior.each(&block) ; 
    end 

    ##----------------------------------------
    def eachPoint(&block) ; @exterior.eachPoint(&block) ; end
    def nPoints() ; @exterior.nPoints ; end
    def nthPoint(n) ; @exterior.nthPoint(n) ; end

    def eachLine(&block) ; @exterior.eachLine(&block) ; end
    def nLines() @exterior.nLines ; end
    def nthLine(n) @exterior.nthLine(n) ; end

    ##----------------------------------------
    def distanceFrom(object) 
      ## should be check object is inside or not.
      @exterior.distanceFrom(object) ;
    end

    ##----------------------------------------
    ## min/max X/Y

    def minX();  @exterior.minX() ; end
    def maxX();  @exterior.maxX() ; end
    def minY();  @exterior.minY() ; end
    def maxY();  @exterior.maxY() ; end

    ##----------------------------------------
    def length(withInterior = false)
      len = @exterior.length() ;
      if(withInterior) then
        @interior.each{|ring|
          len += ring.length() ;
        }
      end
      return len ;
    end

    ##----------------------------------------
    def grossArea(withInterior = false)
      area = @exterior.grossArea() ;
      if(withInterior) then
        @interior.each{|ring|
          area -= ring.grossArea ;
        }
      end
      return area ;
    end

    ##----------------------------------------
    def to_s()
      str = "#Polygon[" + @exterior.to_s + "/" ;
      c = 0 ;
      eachInterior(){|ring|
        str += ":" if c > 0 ;
        str += ring.to_s ;
        c += 1 ;
      }
      str += "]" ;
      str ;
    end

  end

  ##==================================================
  ## class methods for Polygon

  class << Polygon
    ##----------------------------------------
    def newByXY(exteriorXYList = [], interiorXYList = []) 
		# exteriorXYList = [[x0,y0],[x1,y1],...]
      		# interiorXYList = [[[x00,y00],[x01,y01],..],[[x10,y10]...]]
      poly = self.new() ;
      poly.setExteriorXY(exteriorXYList) ;
      poly.setInteriorXY(interiorXYList) ;
      return poly ;
    end

    ##----------------------------------------
    def newConvexHull(pointList)
      convexList = Point::findConvexHullPointList(pointList) ;
      return self.new(convexList) ;
    end

  end



  ##============================================================
  ## Box

  class Box < GeoObject

    attr :minPos, true ;
    attr :maxPos, true ;

    PointClass = Point ;

    ##----------------------------------------
    def initialize(minPos = self.class::PointClass::new(nil,nil), 
                   maxPos = self.class::PointClass::new(nil,nil),
                   dupP = false)
      set(minPos,maxPos,dupP) ;
    end

    ##----------------------------------------
    def set(minPos,maxPos,dupP = false)
      minPos = PointClass::sureGeoObj(minPos) ;
      maxPos = PointClass::sureGeoObj(maxPos) ;
      if(dupP)
        @minPos = minPos.dup() ;
        @maxPos = maxPos.dup() ;
      else
        @minPos = minPos ;
        @maxPos = maxPos ;
      end
      self ;
    end

    ##----------------------------------------
    def setXY(minX,minY,maxX,maxY)
      @minPos.set(minX,minY) ;
      @maxPos.set(maxX,maxY) ;
      self ;
    end

    ##----------------------------------------
    def dup(deepP = true)
      box = clone() ; 
      if(deepP)
        box.minPos = @minPos.dup() ;
        box.maxPos = @maxPos.dup() ;
      end
      box ;
    end

    ##----------------------------------------
    def to_Polygon()
      poly = Polygon.new([PointClass::sureGeoObj([@minPos.x,@minPos.y]),
                          PointClass::sureGeoObj([@maxPos.x,@minPos.y]),
                          PointClass::sureGeoObj([@maxPos.x,@maxPos.y]),
                          PointClass::sureGeoObj([@minPos.x,@maxPos.y]),
                          PointClass::sureGeoObj([@minPos.x,@minPos.y])]) ;
      return poly ;
    end

    ##----------------------------------------
    ## insert

    def insert(geoObj)
      @minPos.set(min(minX(),geoObj.minX()), 
                  min(minY(),geoObj.minY())) ;
      @maxPos.set(max(maxX(),geoObj.maxX()), 
                  max(maxY(),geoObj.maxY())) ;
      self ;
    end

    ##----------------------------------------
    ## min/max X/Y

    def minX() ; @minPos.x ;  end
    def maxX() ; @maxPos.x ;  end
    def minY() ; @minPos.y ;  end
    def maxY() ; @maxPos.y ;  end

    ##----------------------------------------
    ## check inside
    def includePoint(point)
      return (point.x >= minX() && point.x <= maxX() &&
              point.y >= minY() && point.y <= maxY()) ;
    end

    ##----------------------------------------
    ## check inside
    def intersectsWithBox(box)
      myMinX = minX() ; myMinY = minY() ;
      myMaxX = maxX() ; myMaxY = maxY() ;
      otherMinX = box.minX() ; otherMinY = box.minY() ;
      otherMaxX = box.maxX() ; otherMaxY = box.maxY() ;
      ans = ((! (myMinX > otherMaxX || myMaxX < otherMinX)) &&
             (! (myMinY > otherMaxY || myMaxY < otherMinY))) ;
      return ans ;
    end

    ##----------------------------------------
    ## get intersecting box with a given box
    def getIntersectionBoxWith(box)
      myMinX = max(minX(), box.minX()) ;
      myMinY = max(minY(), box.minY()) ;
      myMaxX = min(maxX(), box.maxX()) ;
      myMaxY = min(maxY(), box.maxY()) ;
      if(myMinX <= myMaxX && myMinY <= myMaxY) then
        return Box.new([myMinX, myMinY], [myMaxX, myMaxY]) ;
      else
        return nil ;
      end
    end

    ##----------------------------------------
    def length()
      return 2.0 * (sizeX() + sizeY()) ;
    end

    ##----------------------------------------
    def grossArea()
      return (sizeX() * sizeY()) ;
    end

    ##----------------------------------------
    def growByMargin(margin)
      @minPos.x -= margin ;
      @maxPos.x += margin ;
      @minPos.y -= margin ;
      @maxPos.y += margin ;
      return self ;
    end

    ##----------------------------------------
    def to_s()
      "#Box[#{@minPos.to_s(true)}:#{@maxPos.to_s(true)}]" ;
    end

  end

  ##============================================================
  ## Collection

  class Collection < Array
    include GeoObjUtil ;

    ##----------------------------------------
    ## min/max X/Y

    def minX() 
      x = nil ;
      self.each{|geo|
        x = min(x, geo.minX()) ;
      }
      x
    end

    def maxX() 
      x = nil ;
      self.each{|geo|
        x = max(x, geo.maxX()) ; 
      }
      x
    end

    def minY() 
      y = nil ;
      self.each{|geo|
        y = min(y, geo.minY()) ; 
      }
      y
    end

    def maxY() 
      y = nil ;
      self.each{|geo|
        y = max(y, geo.maxY()) ;
      }
      y
    end

    ##----------------------------------------
    def length()
      len = 0.0 ;
      self.each{|geo| len += geo.length()} ;
      return len ;
    end

    ##----------------------------------------
    def grossArea()
      area = 0.0 ;
      self.each{|geo| area += geo.grossArea()} ;
      return area ;
    end

    ##----------------------------------------
    def to_s
      "#Collection" + super ;
    end

  end

  ##============================================================
  ## Matrix
  class Matrix
    attr :x, true ;
    attr :y, true ;

    VectorClass = Vector ;

    ##----------------------------------------
    def initialize(xy = nil) # xy = [[xx,xy],[yx,yy]]
      @x = VectorClass::new() ;
      @y = VectorClass::new() ;
      setXY(xy) if(xy) ;
    end
    
    ##----------------------------------------
    def setXY(xy) # xy = [[xx,xy],[yx,yy]]
      @x.set(xy[0][0],xy[0][1]) ;
      @y.set(xy[1][0],xy[1][1]) ;
      self ;
    end

    ##----------------------------------------
    def setUnitMatrix()
      setXY([[1.0,0.0],[0.0,1.0]]) ;
    end

    ##----------------------------------------
    def setRotateMatrix(angle)
      setXY([ [Math::cos(angle), -Math::sin(angle)],
              [Math::sin(angle),  Math::cos(angle)] ]) ;
    end
      
    ##----------------------------------------
    def dup()
      w = clone() ;
      w.x = @x.dup() ;
      w.y = @y.dup() ;
      w ;
    end

    ##----------------------------------------
    def +(w)
      r = dup() ;
      r.x.inc(w.x) ;
      r.y.inc(w.y) ;
      r ;
    end

    ##----------------------------------------
    def -(w)
      r = dup() ;
      r.x.dec(w.x) ;
      r.y.dec(w.y) ;
      r ;
    end

    ##----------------------------------------
    def *(w)
      if(w.is_a?(Geo2D::Matrix))
	r = dup() ;
	r.x.x = @x.x * w.x.x + @x.y * w.y.x ;
	r.x.y = @x.x * w.x.y + @x.y * w.y.y ;
	r.y.x = @y.x * w.x.x + @y.y * w.y.x ;
	r.y.y = @y.x * w.x.y + @y.y * w.y.y ;
      elsif(w.is_a?(Geo2D::Vector))
	r = w.dup() ;
	r.x = @x.x * w.x + @x.y * w.y ;
	r.y = @y.x * w.x + @y.y * w.y ;
      else  # suppose w is a number
	r = dup() ;
	r.x.amplify(w) ;
	r.y.amplify(w) ;
      end
      r ;
    end

    ##----------------------------------------
    def trans()
      r = dup() ;
      r.x.y = @y.x ;
      r.y.x = @x.y ;
      r ;
    end

    ##----------------------------------------
    def det()
      @x.x * @y.y - @x.y * @y.x ;
    end

    ##----------------------------------------
    def inv()
      r = dup() ;
      d = det() ;
      r.x.x =  @y.y / d ;
      r.x.y = -@x.y / d ;
      r.y.x = -@y.x / d ;
      r.y.y =  @x.x / d ;
      r ;
    end

    ##----------------------------------------
    def to_s(simpleP = false)
      if(simpleP)
        "(#{@x.to_s(simpleP)},#{@y.to_s(simpleP)})" ;
      else
        "#M[[#{@x.x},#{@x.y}],[#{@y.x},#{@y.y}]]" ;
      end
    end
    
  end

  ##==================================================
  class << Matrix
    ##----------------------------------------
    def newUnitMatrix
      Matrix::new().setUnitMatrix() ;
    end

    ##----------------------------------------
    def newRotateMatrix(angle)
      Matrix::new().setRotateMatrix(angle) ;
    end

  end

end

Geo2D.extend(Geo2D::Utility) ;

######################################################################
