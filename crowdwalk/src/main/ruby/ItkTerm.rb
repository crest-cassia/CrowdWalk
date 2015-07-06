#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Term
## Author:: Itsuki Noda
## Version:: 0.0 2015/07/01 I.Noda
##
## === History
## * [2015/07/01]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'java';

import 'nodagumi.Itk.Term' ;

#--======================================================================
#++
## Java における nodagumi.Itk.Term に対応するクラス。
class ItkTerm
  #--============================================================
  #  クラスメソッド
  #--============================================================
  class << self
    #------------------------------------------
    # タイプチェック
    #------------------------------------------
    #++
    ## null タイプチェック
    def isNull(object)
      return object.isNull() ;
    end

    #------------------------------------------
    #++
    ## atom タイプチェック
    def isAtom(object)
      return object.isAtom() ;
    end

    #------------------------------------------
    #++
    ## arg 部分を持つかどうか
    def hasBody(object)
      return object.hasBody() ;
    end

    #------------------------------------------
    #++
    ## array かどうか
    def isArray(object)
      return object.isArray() ;
    end

    #------------------------------------------
    # 部分取得
    #------------------------------------------
    #++
    ## ヘッド取得
    def getHead(object)
      return object.getHead() ;
    end

    #------------------------------------------
    #++
    ## 本体取得
    def getBody(object)
      return object.getBody();
    end

    #------------------------------------------
    #++
    ## 本体スロット取得
    def getArg(object, slot)
      return object.getArg(slot) ;
    end

    #------------------------------------------
    #++
    ## 配列取得
    def getArray(object)
      return object.getArray() ;
    end

    #------------------------------------------
    # 部分変更
    #------------------------------------------
    #++
    ## 作成
    def newTerm(head = nil)
      if(head.nil?)
        return Term.new() ;
      else
        return Term.new(head) ;
      end
    end

    #------------------------------------------
    #++
    ## 確実に Term にする。
    def ensureTerm(value)
      if(value.is_a?(Term)) then
        return value ;
      elsif(value.is_a?(Array)) then
        arrayTerm = newTerm() ;
        value.each{|element|
          addNth(arrayTerm,ensureTerm(element)) ;
        }
        return arrayTerm ;
      elsif(value.is_a?(Hash)) then
        objectTerm = newTerm() ;
        value.each{|slot,val|
          setArg(objectTerm,slot,ensureTerm(val)) ;
        }
      else
        return newTerm(value) ;
      end
    end

    #------------------------------------------
    #++
    ## ヘッド変更
    ## _object_ :: Java の Itk.Term
    ## _head_ :: atom のデータ。String, int, double, boolean
    def setHead(object, head)
      object.setHead(head) ;
    end

    #------------------------------------------
    #++
    ## ボディ変更
    ## _object_ :: Java の Itk.Term
    ## _slot_ :: スロット指定。String。
    ## _value_ :: データ。
    def setArg(object, slot, value)
      object.setArg(slot, ensureTerm(value)) ;
    end

    #------------------------------------------
    #++
    ## 配列要素変更
    ## _object_ :: Java の Itk.Term
    ## _index_ :: インデックス
    ## _value_ :: データ。
    def setNth(object, index, value)
      object.setNth(index, ensureTerm(value)) ;
    end

    #------------------------------------------
    #++
    ## 配列要素追加
    ## _object_ :: Java の Itk.Term
    ## _value_ :: データ。
    def addNth(object, value)
      object.addNth(ensureTerm(value)) ;
    end

    #------------------------------------------
    # 変換
    #------------------------------------------
    ## ruby object への変換
    def toRuby(object, deeply = true)
      if(!object.is_a?(Term)) then	# Term のインスタンスではない場合。
        if(object.is_a?(Array) || object.is_a?(java.util.ArrayList)) then
          return toRubyFromArray(object, deeply) ;
        elsif(object.is_a?(Hash) || object.is_a?(java.util.HashMap)) then
          return toRubyFromHash(object, deeply) ;
        else
          return object ;
        end

      elsif(object.isNull()) then	# null Termの場合
        return nil ;

      elsif(object.isAtom()) then 		## atom の場合
        if(object.isInt()) then
          return object.getInt() ;
        elsif(object.isDouble()) then
          return object.getDouble() ;
        else
          return object.getHead() ;
        end

      elsif(object.isArray()) then		## 配列の場合
        return toRubyFromArray(object.getArray(), deeply) ;

      else					## json の Object の場合
        return toRubyFromHash(object.getBody(), deeply) ;
      end

    end

    #------------------------------------------
    ## ruby object への変換 (配列)
    def toRubyFromArray(originalArray, deeply)
      array = [] ;
      originalArray.each{|element|
        value = (deeply ? toRuby(element, deeply) : element) ;
        array.push(value) ;
      }
      return array ;
    end

    #------------------------------------------
    ## ruby object への変換 (Hash)
    def toRubyFromHash(originalHash, deeply)
      hash = {} ;
      originalHash.each{|key, element|
        value = (deeply ? toRuby(element, deeply) : element) ;
        hash[key] = value ;
      }
      return hash ;
    end

  end

  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #--------------------------------------------------------------
  #++

end # class ItkTerm

