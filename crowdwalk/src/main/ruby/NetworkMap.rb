#! /usr/bin/env ruby
# coding: utf-8
## -*- mode: ruby -*-
## = NetworkMap access
## Author:: Itsuki Noda
## Version:: 0.0 2015/07/11 I.Noda
##
## === History
## * [2015/07/11]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'CrowdWalkWrapper.rb' ;

#--======================================================================
#++
## description of class Foo.
class NetworkMap
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## Java 側の NetworkMap のオブジェクト
  attr_accessor :mapObject ;

  #--------------------------------------------------------------
  #++
  ## コンストラクタ
  ## _mapObject_:: java 側のオブジェクトを渡す。
  def initialize(mapObject)
    @mapObject = mapObject ;
  end

  #--------------------------------------------------------------
  #++
  ## 全リンクに対し、ある処理を行う。
  def eachLink(&block)
    @mapObject.getLinks().each{|link|
      block.call(link) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## 全ノードに対し、ある処理を行う。
  def eachNode(&block)
    @mapObject.getNodes().each{|node|
      block.call(node) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## あるタグを持つリンクに対し、ある処理を行う。
  def eachLinkWithTag(tag, &block)
    tagString = (tag.is_a?(Term) ? tag : intern(tag.to_s)) ;
    @mapObject.getLinks().each{|link|
      block.call(link) if(link.hasTag(tagString)) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## あるタグを持つノードに対し、ある処理を行う。
  def eachNodeWithTag(tag, &block)
    tagString = (tag.is_a?(Term) ? tag : intern(tag.to_s)) ;
    @mapObject.getNodes().each{|node|
      block.call(node) if(node.hasTag(tagString)) ;
    }
  end

end # class NetworkMap
