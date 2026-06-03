#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = RTree implement by pure Ruby
## Author:: Itsuki Noda
## Version:: 0.0 2016/03/20 I.Noda
##
## === History
## * [2016/03/20]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

$LOAD_PATH.push(File::dirname(__FILE__)) ;
require 'WithConfParam.rb' ;
require 'Geo2D.rb' ;

#--======================================================================
module Geo2D
  #--======================================================================
  #++
  ## RTree implements by pure ruby
  class RTree < WithConfParam
    #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    #++
    ## description of DefaultOptsions.
    DefaultConf = { :branchN => 4,
                    :reballanceN => 0, ## 1 だとあまりうまく動かない。
                    :useCountCost => true,
                    nil => nil
                  } ;

    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #++
    ## branching factor at node
    attr_accessor :branchN ;
    ## root node
    attr_accessor :root ;
    ## auto reballance
    attr_accessor :reballanceN ;
    ## flag to use count cost.  cost increases if entity count is large.
    attr_accessor :useCountCost ;

    #--------------------------------------------------------------
    #++
    ## description of method initialize
    ## _conf_:: about argument baz.
    def initialize(conf = {})
      super(conf) ;
      setup() ;
    end

    #--------------------------------------------------------------
    #++
    ## setup parameters.
    def setup()
      @branchN = getConf(:branchN) ;
      @reballanceN = getConf(:reballanceN) ;
      @useCountCost = getConf(:useCountCost) ;
      @root = Node.new(self) ;
    end

    #--------------------------------------------------------------
    #++
    ## insert geo object.
    def insert(geo)
      @root.insert(geo) ;
      (0...@reballanceN).each{ @root.reballanceNode() ; }
      return self ;
    end

    #--------------------------------------------------------------
    #++
    ## insert geo object.
    def delete(geo)
      @root.delete(geo) ;
      return self ;
    end

    #--------------------------------------------------------------
    #++
    ## search
    def searchByBBox(bbox)
      return @root.searchByBBox(bbox, []) ;
    end

    #--------------------------------------------------------------
    #++
    ## calculate overlap area
    def calcOverlapArea(recursiveP = true)
      return @root.calcOverlapArea(recursiveP) ;
    end

    #--------------------------------------------------------------
    #++
    ## show tree.
    def showTree(strm = $stdout, &body)
      if(body.nil?) then
        @root.showTree(strm, "", "  ") {|node|
          "*+[#{node.count}]: #{node.bbox}" ;
        } ;
      else
        @root.showTree(strm, "", "  ", &body) ;
      end
    end

    #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    #++
    ## default configulation on in show on canvas
    DefaultShowOnCanvasConf = { :canvasColor => 'white',
                                :nodeColor => 'green',
                                :leafColor => 'red' } ;
    #--------------------------------------------------------------
    #++
    ## show on canvas
    def showOnCanvas(canvas, optConf = {})
      conf = DefaultShowOnCanvasConf.dup.update(optConf) ;
      @root.showOnCanvas(canvas, conf) ;
    end
    
    #--============================================================
    #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    #--------------------------------------------------------------

    #--============================================================
    #++
    ## Node class
    class Node
      #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
      #++
      ## mother tree
      attr_accessor :tree ;
      ## parent node.
      attr_accessor :parent ;
      ## children node.  an array of Node or entity
      attr_accessor :children ;
      ## flag to indicate bottom. the bottom node's children should be entities.
      attr_accessor :isBottom ;
      ## boundary box.
      attr_accessor :bbox ;
      ## object counter
      attr_accessor :count ;

      #------------------------------------------
      #++
      ## initializer/constructor
      def initialize(parent)
        setInitParent(parent) ;
        @children = [] ;
        @isBottom = true ;
        @bbox = nil ;
        @count = 0 ;
      end
      
      #------------------------------------------
      #++
      ## initializer/constructor
      def setInitParent(parent)
        if(parent.is_a?(RTree))
          @parent = nil ;
          @tree = parent ;
        elsif(parent.is_a?(Node))
          @parent = parent ;
          @tree = parent.tree ;
        elsif(parent.nil?)
          @parent = nil ;
          @tree = nil ;
        else
          raise "Illegal parent:" + parent.inspect ;
        end
      end

      #------------------------------------------
      #++
      ## branch N.
      def branchN()
        return @tree.branchN ;
      end

      #------------------------------------------
      #++
      ## fill check.
      def isFull()
        return (@children.size >= branchN()) ;
      end
      
      #------------------------------------------
      #++
      ## fill check.
      def isEmpty()
        return (@children.size == 0) ;
      end
      
      #------------------------------------------
      #++
      ## fill check.
      def isTop()
        return @parent.nil? ;
      end
      
      #------------------------------------------
      #++
      ## insert.
      def insert(geo)
        if(isBottom()) then
          insertToBottom(geo) ;
        else
          insertToMiddle(geo) ;
        end
      end

      #------------------------------------------
      #++
      ## insert to bottom node.
      def insertToBottom(geo) ;
        if(isFull()) then
          bottomDown() ;
          insert(geo) ;
        else
          @children.push(geo) ;
          @count += 1;
          updateBBox(geo) ;
        end
      end

      #------------------------------------------
      #++
      ## update bbox.
      def updateBBox(geo)
        @bbox = insertToBox(@bbox, geo) ;
      end
      
      #------------------------------------------
      #++
      ## update bbox.
      def insertToBox(box, geo)
        if(box.nil?) ;
          box = geo.bbox() ;
        else
          box.insert(geo.bbox()) ;
        end
        return box ;
      end

      #------------------------------------------
      #++
      ## bottom Down.
      def bottomDown()
        newChildren = [] ;
        @children.each{|leaf|
          node = Node.new(self) ;
          node.insert(leaf) ;
          newChildren.push(node) ;
        }
        @children = newChildren ;
        @isBottom = false ;
      end
      
      #------------------------------------------
      #++
      ## insert to middle node.
      def insertToMiddle(geo)
        bestNode = nil ;
        bestCost = nil ;
        @children.each{|node|
          cost = node.calcInsertCost(geo) ;
          if(bestNode.nil? || cost < bestCost) then
            bestNode = node ;
            bestCost = cost ;
          end
        }
        bestNode.insert(geo) ;
        updateBBox(geo) ;
        @count += 1 ;
      end

      #------------------------------------------
      #++
      ## calc insert cost
      def calcInsertCost(geo)
        incArea = calcIncreasingArea(geo) ;
        if(@tree.useCountCost) then
          return incArea * @count ;
        else
          return incArea ;
        end
      end
      
      #------------------------------------------
      #++
      ## calc insert cost
      def calcIncreasingArea(geo)
        if(@bbox.nil?) then
          return geo.bbox.grossArea() ;
        else
          tempBBox = @bbox.dup(true) ;
          origArea = tempBBox.grossArea() ;
          tempBBox.insert(geo.bbox()) ;
          newArea = tempBBox.grossArea() ;
          return newArea - origArea ;
        end
      end
      
      #------------------------------------------
      #++
      ## search by BBox
      def searchByBBox(_bbox, result)
        return result if(@bbox.nil?) ;
        if(_bbox.intersectsWithBox(@bbox)) then
          if(isBottom()) then
            @children.each{|child|
              result.push(child) if(_bbox.intersectsWithBox(child.bbox())) ;
            }
          else
            @children.each{|child|
              child.searchByBBox(_bbox, result) ;
            }
          end
        end
        return result ;
      end

      #------------------------------------------
      #++
      ## delete.
      def delete(geo)
        c = 0 ;
        if(!bbox().nil? && bbox().intersectsWithBox(geo.bbox())) then
          if(isBottom()) then
            c = deleteFromBottom(geo) ;
          else
            c = deleteFromMiddle(geo) ;
          end
          recalcBBox() if(c>0) ;
        end
        return c ;
      end

      #------------------------------------------
      #++
      ## delete from bottom node.
      def deleteFromBottom(geo)
        c = 0 ;
        @children.each{|obj|
          if(geo == obj) then
            @children.delete(obj) ;
            @count -= 1 ;
            c += 1 ;
          end
        }
        return c ;
      end

      #------------------------------------------
      #++
      ## delete from middle node.
      def deleteFromMiddle(geo)
        csum = 0 ;
        @children.each{|child|
          c = child.delete(geo) ;
          @count -= c ;
          csum += c ;
        }
        return csum ;
      end

      #------------------------------------------
      #++
      ## delete from bottom node.
      def recalcBBox()
        @bbox = nil ;
        @children.each{|child|
          childBBox = child.bbox() ;
          if(!childBBox.nil?) then
            if(@bbox.nil?) then
              @bbox = childBBox.dup() ;
            else
              @bbox.insert(childBBox) ;
            end
          end
        }
        return @bbox ;
      end

      #------------------------------------------
      #++
      ## re-ballance node body process (bad performance)
      def reballanceNode(deepP = true)
        r = false ;
        if(!isBottom()) then
          depthRange = getDepthRange() ;
          if(depthRange[1] - depthRange[0] > 1) then
            if(deepP) then
              @children.each{|child|
                child.reballanceNode(deepP) ;
              }
            end
            (deepestChild, childRange) = findDeepestMiddleChild() ;
            if(!deepestChild.nil?) then
              r = swapWithChild(deepestChild, childRange) ;
            end
          end
        end
        return r ;
      end

      #------------------------------------------
      #++
      ## re-ballance node (bad performance)
      def findDeepestMiddleChild()
        deepestChild = nil ;
        deepestRange = nil ;
        @children.each{|child|
          if(!child.isBottom()) then
            childDepth = child.getDepthRange() ;
            if(deepestChild.nil? || deepestRange[1] < childDepth[1]) then
              deepestChild = child ;
              deepestRange = childDepth ;
            end
          end
        }
        return [deepestChild, deepestRange] ;
      end
      
      #------------------------------------------
      #++
      ## re-ballance node (bad performance)
      def swapWithChild(childNode, depthRange)
        box = nil ;
        nth = nil ;
        i = 0 ;
        @children.each{|child|
          if(child == childNode) then
            nth = i ;
          else
            box = insertToBox(box, child.bbox) ;
          end
          i += 1 ;
        }
        bestGChild = nil ;
        bestBox = nil ;
        childNode.children.each{|gchild|
          gchildDepth = gchild.getDepthRange() ;
          if(gchildDepth[1] + 2 < depthRange[1]) then
            newBox = box.dup ;
            newBox.insert(gchild.bbox()) ;
            if(bestGChild.nil? ||
               bestBox.grossArea() > newBox.grossArea()) then 
              bestGChild = gchild ;
              bestBox = newBox ;
            end
          end
        }
        if(!bestGChild.nil?) then
          _parent = @parent ;
          if(isTop()) then
            @tree.root = childNode ;
          else
            _parent.children[_parent.children.index(self)] = childNode ;
          end
          childNode.parent = _parent ;
          #
          childNode.children[childNode.children.index(bestGChild)] = self ;
          self.parent = childNode ;
          #
          @children[@children.index(childNode)] = bestGChild ;
          bestGChild.parent = self ;
          #
          self.recalcBBox() ;
          childNode.recalcBBox() ;
          _parent.recalcBBox() if(!_parent.nil?) ;
          #
          @count += (bestGChild.count - childNode.count) ;
          childNode.count += (@count - bestGChild.count) ;
#          p [:swapNode] ;
          return true ;
        else
          return false ;
        end
      end
      
      #------------------------------------------
      #++
      ## depth range to the leaf
      ## return [minDepth, maxDepth] ;
      def getDepthRange()
        if(isBottom()) then
          if(isEmpty()) then
            return [0, 0] ;
          elsif(isFull()) then
            return [1, 1] ;
          else
            return [0, 1] ;
          end
        else
          range = nil ;
          @children.each{|child|
            childRange = child.getDepthRange() ;
            if(range.nil?) then
              range = childRange ;
            else
              range[0] = childRange[0] if(childRange[0] < range[0]) ;
              range[1] = childRange[1] if(childRange[1] > range[1]) ;
            end
          }
          range[0] += 1 ;
          range[1] += 1 ;
          return range ;
        end
      end

      #------------------------------------------
      #++
      ## calc overlap area in children
      def calcOverlapArea(recursiveP)
        if(isBottom()) then
          return 0.0 ;
        elsif(@bbox.nil?) then
          return 0.0 ;
        else
          bboxList = @children.map(){|child| child.bbox()} ;
          k = bboxList.size() ;
          area = 0.0 ;
          (0...k).each{|i|
            (0...i).each{|j|
              intersection =
                bboxList[i].getIntersectionBoxWith(bboxList[j]) ;
              if(!intersection.nil?) then
                area += intersection.grossArea() ;
              end
            }
          }
          if(recursiveP) then
            @children.each{|child|
              area += child.calcOverlapArea(recursiveP) ;
            }
          end
          return area ;
        end
      end

      #------------------------------------------
      #++
      ## show tree.
      def showTree(strm, indent, nextIndent, &body)
        strm << indent << "+-+" << body.call(self) << "\n" ;
        c = 0 ;
        @children.each{|child|
          c += 1 ;
          if(child.is_a?(Node)) then
            child.showTree(strm, indent + nextIndent,
                           (c < @children.size ? "| " : "  "), &body) ;
          else
            strm << indent + nextIndent ;
            strm << "*===[" << child.to_s << "]" << "\n" ;
          end
        }
      end

      #------------------------------------------
      #++
      ## show tree on canvas
      def showOnCanvas(canvas, conf)
        if(!@bbox.nil?) then
          canvas.drawEmptyRectangle(@bbox.minX(), @bbox.minY(),
                                    @bbox.sizeX(), @bbox.sizeY(),
                                    conf[:nodeColor]) ;
          if(isBottom()) then
            d = 1.0 / canvas.getScaleX() ;
            @children.each{|leaf|
              _bbox = leaf.bbox() ;
              rx = _bbox.sizeX()/2.0 ; rx = d if (rx < d) ;
              ry = _bbox.sizeY()/2.0 ; ry = d if (ry < d) ;
              canvas.drawEllipse(_bbox.midX(), _bbox.midY(),
                                 rx, ry, false, conf[:leafColor]) ;
            }
          else
            @children.each{|child|
              child.showOnCanvas(canvas, conf) ;
            }
          end
        end
      end
      
      #--========================================
      #--::::::::::::::::::::::::::::::::::::::::
      #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
      #------------------------------------------
    end # class Node
    
  end # class RTree

end # module Geo2D

########################################################################
########################################################################
########################################################################
if($0 == __FILE__) then

  require 'myCanvas.rb' ;
  require 'test/unit' ;
  require 'Stat/Uniform.rb' ;
  require 'pp' ;

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
    ## 
    def prepareCanvas(rangeXY, sizeXY = 512)
      scale = sizeXY.to_f / rangeXY.to_f ;
      canvas = MyCanvas.new('gtk',
                            { 'width' => sizeXY,
                              'height' => sizeXY,
                              'scale' => scale,
                              'centerp' => true }) ;
      return canvas ;
    end
      
    #----------------------------------------------------
    #++
    ## show on canvas
    def showRTreeOnCanvas(rtree)
      sizeX = 20.0 ;
      canvas = prepareCanvas(sizeX) ;
      canvas.singlePage('white') {
        rtree.showOnCanvas(canvas) ;
      }
    end
    

    #----------------------------------------------------
    #++
    ## random plot (no reballance / reballance)
    def test_a0
      _test_a(0)
    end

    def test_a1
      _test_a(1)
    end
    
    def _test_a(reballanceN)
      rtree = Geo2D::RTree.new({:reballanceN => reballanceN}) ;
      size = 10.0 ;
      genX = Stat::Uniform.new(-size, size) ;
      genY = Stat::Uniform.new(-size, size) ;
      canvas = prepareCanvas(2.1 * size) ;
      n = 300 ;
      canvas.animation((0...n),0.001){|i|
        x = genX.value() ;
        y = genY.value() ;
        point = Geo2D::Point.new(x,y) ;
        rtree.showOnCanvas(canvas) ;
        rtree.insert(point) ;
#        p [:insert, i, point] ;
#        rtree.showTree() ;
      }
    end

    #----------------------------------------------------
    #++
    ## shifting random plot
    def test_b0
      _test_b(0) ;
    end

    def test_b1
      _test_b(1) ;
    end
    
    def _test_b(reballanceN)
      rtree = Geo2D::RTree.new({:reballanceN => reballanceN}) ;
      size = 10.0 ;
      genX = Stat::Uniform.new(-size, size) ;
      genY = Stat::Uniform.new(-size, size) ;
      r = 10.0 ;
      canvas = prepareCanvas(r * size) ;
      n = 100 ;
      canvas.animation((0...n),0.1){|i|
        offset = r * size * ((i - n/2).to_f / n.to_f) ;
        x = genX.value() + offset ;
        y = genY.value() + offset ;
        p [:offset, offset, x, y] ;
        point = Geo2D::Point.new(x,y) ;
        rtree.insert(point) ;
        rtree.showOnCanvas(canvas) ;
#        p [:insert, i, point] ;
#        rtree.showTree() ;
      }
    end

    #----------------------------------------------------
    #++
    ## search

    def test_c0
      _test_c(0) ;
    end

    def test_c1
      _test_c(1) ;
    end
    
    def _test_c(reballanceN)
      rtree = Geo2D::RTree.new({:reballanceN => reballanceN}) ;
      size = 10.0 ;
      genX = Stat::Uniform.new(-size, size) ;
      genY = Stat::Uniform.new(-size, size) ;
      ##
      n = 100 ;
      (0...n).each{|i|
        x = genX.value() ;
        y = genY.value() ;
        point = Geo2D::Point.new(x,y) ;
        rtree.insert(point) ;
      }
      ##
      s = size/4.0 ;
      box = Geo2D::Box.new([-s, -s],[s, s]) ;
      plist = rtree.searchByBBox(box) ;
      pp plist ;
      ##
      canvas = prepareCanvas(2 * size) ;
      canvas.singlePage('white'){
        rtree.showOnCanvas(canvas) ;
        canvas.drawEmptyRectangle(box.minX(), box.minY(),
                                  box.sizeX(), box.sizeY(),'orange') ;
        d = 2.0 / canvas.getScaleX() ;
        plist.each{|pt|
          canvas.drawFilledRectangle(pt.minX()-d, pt.minY()-d,
                                     2*d, 2*d, 'red') ;
        }
      }
    end
    

    #----------------------------------------------------
    #++
    ## delete
    def test_d0
      _test_d(0) ;
    end

    def test_d1
      _test_d(1) ;
    end

    def _test_d(reballanceN)
      rtree = Geo2D::RTree.new({:reballanceN => reballanceN}) ;
      size = 10.0 ;
      genX = Stat::Uniform.new(-size, size) ;
      genY = Stat::Uniform.new(-size, size) ;
      ##
      n = 500 ;
      m = 100 ;
      plist = [] ;
      canvas = prepareCanvas(2 * size) ;
      canvas.animation((0...n),0.0){|i|
        x = genX.value() ;
        y = genY.value() ;
        point = Geo2D::Point.new(x,y) ;
        plist.push(point) ;
        rtree.insert(point) ;
        if(i >= m) then
          p = plist[rand(plist.size)] ;
          rtree.delete(p) ;
          plist.delete(p) ;
        end
        rtree.showOnCanvas(canvas) ;
      }
    end

    #----------------------------------------------------
    #++
    ## ballance (grid) ;

    def test_e0
      _test_e(0) ;
    end

    def test_e1
      _test_e(1) ;
    end

    def test_e7
      _test_e(7) ;
    end

    def _test_e(reballanceN)
      rtree = Geo2D::RTree.new({:reballanceN => reballanceN}) ;
      size = 100.0 ;
      canvas = prepareCanvas(size) ;
      n = 3000 ;
      m = Math::sqrt(n) ;
      l = 0.5 * (size / m);
      k = 0.1 ;
      canvas.animation((0...n),0.0){|i|
        a = (i / m - (m/2)) * l
        b = (i % m.to_i - (m/2)) * l ;
        x = (1-k)*a + k * b ;
        y = k * a - (1-k) * b ;
        point = Geo2D::Point.new(x,y) ;
        rtree.insert(point) ;
        rtree.showOnCanvas(canvas) ;
        p [reballanceN, i, rtree.root.getDepthRange()] ;
      }
      p rtree.root.bbox() ;
    end

    #----------------------------------------------------
    #++
    ## ballance (along a line)
    def test_f0
      _test_f(0);
    end

    def test_f1
      _test_f(1);
    end

    def test_f7
      _test_f(7);
    end

    def _test_f(reballanceN)
      rtree = Geo2D::RTree.new({:reballanceN => reballanceN}) ;
      size = 100.0 ;
      canvas = prepareCanvas(size) ;
      n = 1000 ;
      canvas.animation((0...n),0.01){|i|
        x = y = (i.to_f/n.to_f - 0.5) * size * 0.9;
        point = Geo2D::Point.new(x,y) ;
        rtree.insert(point) ;
        rtree.showOnCanvas(canvas) ;
        p [reballanceN, i, rtree.root.getDepthRange()] ;
      }
      p rtree.root.bbox() ;
    end

  end # class TC_Foo < Test::Unit::TestCase
end # if($0 == __FILE__)
