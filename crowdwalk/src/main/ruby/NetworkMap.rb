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

require 'ItkTerm.rb' ;
require 'CrowdWalkWrapper.rb' ;

#--======================================================================
#++
## シミュレーションで用いる地図(NetworkMap)のJava Objectへのアクセスを
## 管理する。
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
  ## 全リンクに対し、指定した処理を行う。
  def eachLink(&block) # :yield: _link_
    @mapObject.getLinks().each{|link|
      block.call(link) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## 全ノードに対し、指定した処理を行う。
  def eachNode(&block) # :yield: _node_
    @mapObject.getNodes().each{|node|
      block.call(node) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## あるタグを持つリンクに対し、ある処理を行う。
  ## _tag_:: このタグを持つリンクのみが取り出される。
  def eachLinkWithTag(tag, &block) # :yield: _link_
    tagString = (tag.is_a?(Term) ? tag : ItkTerm.intern(tag.to_s)) ;
    @mapObject.getLinks().each{|link|
      block.call(link) if(link.hasTag(tagString)) ;
    }
  end

  #--------------------------------------------------------------
  #++
  ## あるタグを持つノードに対し、ある処理を行う。
  ## _tag_:: このタグを持つノードのみが取り出される。
  def eachNodeWithTag(tag, &block) # :yield: _node_
    tagString = (tag.is_a?(Term) ? tag : ItkTerm.intern(tag.to_s)) ;
    @mapObject.getNodes().each{|node|
      block.call(node) if(node.hasTag(tagString)) ;
    }
  end

end # class NetworkMap
