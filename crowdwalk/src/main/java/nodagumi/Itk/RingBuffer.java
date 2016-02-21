// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk RingBuffer
 * @author:: Itsuki Noda
 * @version:: 0.0 2016/02/21 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2016/02/21]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.util.ArrayList ;
import java.util.Iterator;
import java.lang.reflect.*;

//======================================================================
/**
 * リングバッファ。
 */
public class RingBuffer<E> implements Iterable<E> {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //============================================================
    static public enum ExpandType {
	/** 自動拡張 */
	Auto,
	/** 固定長(満杯チェックあり) */
	Fixed,
	/** 固定長(満杯チェックなし) */
	Overwrite,
	/** 固定長(オブジェクト再利用) */
	Recycle ;
    }
	
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 拡張タイプ。
     */
    private ExpandType _type ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 繰り返しタイプ
     */
    private boolean _iterateBackward ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    static public double defaultExpandRatio = 2.0 ;
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 拡張係数。
     */
    private double _expandRatio = defaultExpandRatio ;

    /**
     * バッファサイズ。
     */
    private int _size ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 新たにデータを入れる先の index。
     */
    private int _head ;

    /**
     * 次にデータを取り出す index。
     */
    private int _tail ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * データの格納先。
     */
    private ArrayList<E> _buffer ;

    //------------------------------------------------------------
    /**
     * constructor。
     * @param initSize: リングバッファの初期サイズ。
     */
    public RingBuffer(int initSize){
	this(initSize, ExpandType.Auto) ;
    }

    /**
     * constructor。
     * @param initSize: リングバッファの初期サイズ。
     * @param type: 拡張タイプ。
     */
    public RingBuffer(int initSize, ExpandType type){
        this(initSize, type, false) ;
    }
    
    /**
     * constructor。
     * @param initSize: リングバッファの初期サイズ。
     * @param type: 拡張タイプ。
     * @param iterateBackward: 繰り返し方向を逆にするかどうか。
     */
    public RingBuffer(int initSize, ExpandType type, boolean iterateBackward){
	_type = type ;
        _iterateBackward = iterateBackward ;
	_size = initSize ;
        _buffer = allocate(_size) ;
	_head = 0 ;
	_tail = 0 ;
    }

    //------------------------------------------------------------
    // アクセス関係。
    //--------------------
    /** 拡張タイプ取得。*/
    public ExpandType getType() { return _type ; }
    /** 拡張タイプ設定。*/
    public void setType(ExpandType type) { _type = type ; }

    //--------------------
    /** 繰り返しタイプ取得。*/
    public boolean getIterateBackward() { return _iterateBackward ; }
    /** 繰り返しタイプ設定。*/
    public void setIterateBackward() { setIterateBackward(true) ; } ;
    /** 繰り返しタイプ設定。*/
    public void setIterateForward() { setIterateBackward(false) ; } ;
    /** 繰り返しタイプ設定。*/
    public void setIterateBackward(boolean backward) {
        _iterateBackward = backward ;
    }

    //--------------------
    /** 拡張係数取得。 */
    public double getExpandRatio() { return _expandRatio ; }
    /** 拡張係数設定。*/
    public void setExpandRatio(double ratio) { _expandRatio = ratio ; }
    
    //--------------------
    /** サイズ取得。*/
    public int getSize() { return _size ; }

    //--------------------
    /** head index取得。*/
    public int getHeadIndex() { return _head ; }
    /** tail index取得。*/
    public int getTailIndex() { return _tail ; }

    //--------------------
    /** バッファー取得。*/
    public ArrayList<E> getBuffer() { return _buffer ; }

    //------------------------------------------------------------
    /**
     * メモリ確保。
     * _buffer にはセットしない。
     */
    private ArrayList<E> allocate(int size) {
        ArrayList<E> newBuf = new ArrayList<>(size) ;
        for(int i = 0 ; i < size ; i++) {
            newBuf.add(null) ;
        }
        return newBuf ;
    }

    /**
     * _buffer の null 要素にインスタンスを埋める。
     * Recycle の場合に、インスタンス生成後、呼ばれるべきである。
     * その他のタイプの場合、あまり意味はない。 
     */
    public void fillElements(Class<?> klass) {
        try {
            for(int i = 0 ; i < _size ; i++) {
                if(_buffer.get(i) == null) {
                    E element = (E)klass.newInstance() ;
                    _buffer.set(i, element) ;
                }
            }
        } catch(Exception e) {
            throw new RuntimeException(e) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 空チェック。
     * @return true if empty buffer
     */
    public boolean isEmpty() {
        return ((_head - _tail + _size) % _size == 0) ;
    }

    //------------------------------------------------------------
    /**
     * 満杯 チェック。
     * @return true if full buffer
     */
    public boolean isFull() {
	return (((_tail - _head + _size) % _size) == 1) ;
    }

    //------------------------------------------------------------
    /**
     * index が、head と tail の間かチェック。
     * @return true if index is between head and tail
     */
    public boolean isBody(int index) {
        return (index >= _tail) && (index < _head) ;
    }

    //------------------------------------------------------------
    /**
     * index の縮退表現。
     * @param index
     * @return 正規化されたindex。
     */
    public int reducedIndex(int index) {
	return (index % _size) ;
    }

    //------------------------------------------------------------
    /**
     * インデックスの縮約。
     */
    public void reduceIndexes() {
	_head = _head % _size ;
	_tail = _tail % _size ;
	if(_head < _tail) {
	    _head = _head + _size ;
	}
    }
    
    //------------------------------------------------------------
    /**
     * head の前進。
     */
    public void advanceHead() {
	_head += 1 ;
    }

    //------------------------------------------------------------
    /**
     * tail の前進。
     */
    public void advanceTail() {
	_tail += 1 ;
	if(_tail >= _size) {
	    reduceIndexes() ;
	}
    }

    //------------------------------------------------------------
    /**
     * index の部分の取り出し。
     * @param index
     * @return index を取り出し。
     */
    public E get(int index) {
	return _buffer.get(reducedIndex(index)) ;
    }

    //------------------------------------------------------------
    /**
     * index への代入。
     * @param index
     * @param value
     * @return index を取り出し。
     */
    public E set(int index, E value) {
	return _buffer.set(reducedIndex(index), value) ;
    }

    //------------------------------------------------------------
    /**
     * buffer の head 側に入れる。
     * @param value.
     * @return true if succeed to enqueue the value.
     */
    public boolean enqueue(E value) {
	if(isFull()) {
	    if(_type == ExpandType.Auto) {
		expand(_expandRatio) ;
		return enqueue(value) ;
	    } else if(_type == ExpandType.Overwrite) {
		dequeue() ;
		return enqueue(value) ;
	    } else {
		return false ;
	    }
	} else {
	    set(_head, value) ;
	    advanceHead() ;
	    return true ;
	}
    }
    
    //------------------------------------------------------------
    /**
     * buffer の tail 側から取り出す。
     * @return null if empty。
     */
    public E dequeue() {
	if(isEmpty()) {
	    return null ;
	} else {
	    E value = get(_tail) ;
	    if(_type != ExpandType.Recycle) {
		set(_tail, null) ;
	    }
	    advanceTail() ;
	    return value ;
	}
    }

    //------------------------------------------------------------
    /**
     * buffer の tail を覗く。
     * @return null if empty。
     */
    public E peekTail() {
        if(isEmpty()) {
            return null ;
        } else {
            return get(_tail) ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * buffer の head を shift を移動する。
     * もし、head がすでに tail に接しているなら、まず tail を shift してから
     * head を shift する。
     * Recycle の場合に使われる。（他では意味がない。）
     * @return 現状の head 位置にある element を返す。
     */
    public E shiftHead() {
        if(isFull()) {
            advanceTail() ;
        } 
        E value = get(_head) ;
        advanceHead() ;
        return value ;
    }
    
    //------------------------------------------------------------
    /**
     * 拡張
     */
    public boolean expand(double ratio) {
        if(ratio <= 1.0) {
            return false ;
        } else {
            int newSize = (int)Math.ceil(((double)_size) * ratio) ;
            ArrayList<E> newBuffer = allocate(newSize) ;
            for(int i = 0 ; i < _size ; i++) {
                newBuffer.set(i, get(_tail + i)) ;
            }
            int newHead = _head - _tail ;
            while(newHead < 0) { newHead += _size ;}
            _tail = 0 ;
            _head = newHead ;
            _size = newSize ;
            _buffer = newBuffer ;
            return true ;
        }
    }

    //------------------------------------------------------------
    /**
     * 繰り返し演算子。
     */
    public Iterator<E> iterator() {
        if(_iterateBackward) {
            return backwardIterator() ;
        } else {
            return forwardIterator() ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * 反対方向繰り返し演算子。
     */
    public Iterator<E> reverseIterator() {
        if(_iterateBackward) {
            return forwardIterator() ;
        } else {
            return backwardIterator() ;
        }
    }

    //------------------------------------------------------------
    /**
     * 繰り返し演算子。（順方向）
     */
    public Iterator<E> forwardIterator() {
        RingBuffer<E> ring = this ;
        return new Iterator<E>(){
            private RingBuffer<E> _ringBuffer = ring ;
            private int _index = ring.getTailIndex() ;
            public boolean hasNext() {
                return _index < _ringBuffer.getHeadIndex() ; }
            public E next() {
                E value = _ringBuffer.get(_index) ;
                _index += 1 ;
                return value ;
            }
            public void remove() {} ;
        } ;
    }
    
    //------------------------------------------------------------
    /**
     * 繰り返し演算子。（逆方向）
     */
    public Iterator<E> backwardIterator() {
        RingBuffer<E> ring = this ;
        return new Iterator<E>(){
            private RingBuffer<E> _ringBuffer = ring ;
            private int _index = ring.getHeadIndex() ;
            public boolean hasNext() {
                return _index > _ringBuffer.getTailIndex() ; }
            public E next() {
                _index -= 1 ;
                return _ringBuffer.get(_index) ;
            }
            public void remove() {} ;
        } ;
    }
    
    //------------------------------------------------------------
    /**
     * 文字列化。
     */
    public String toString() {
	return ("#RingBuffer" +
		"[type=" + _type +
		",size=" + _size +
		",head=" + _head +
		",tail=" + _tail +
                ",nextVal=" + peekTail() +
		"]") ;
    }

    //------------------------------------------------------------
    /**
     * ArrayList 化。
     * 順序は、iterateBackward の値に依存する。
     */
    public ArrayList<E> toArrayList() {
        ArrayList<E> array = new ArrayList<>() ;
        for(E value : this) {
            array.add(value) ;
        }
        return array ;
    }

    //============================================================
    //------------------------------------------------------------
    /* [2016.02.22 I.Noda]
     * 以下、テスト用。Java ではパラメータ化されたクラスの
     * new T() とか、T.class() とかが呼べない。
     * ある特殊な場合だけ、それを知ることができるが、
     * 現状の RingBuffer の実装ではうまく行かない。
     * object には、RingBuffer のインスタンスを渡すつもり。
     */
    static Class<?> ___guessElementClass(Object object) {
        try {
            Class<?> clazz = object.getClass() ;
            Itk.dbgVal("clazz", clazz) ;
            Type type = clazz.getGenericSuperclass() ;
            Itk.dbgVal("type", type) ;
            ParameterizedType ptype = (ParameterizedType)type ;
            Itk.dbgVal("ptype", ptype) ;
            Type[] actualTypeArguments = ptype.getActualTypeArguments() ;
            Itk.dbgVal("atype", actualTypeArguments[0]) ;
            // ここのキャストでエラーが起きる。
            Class<?> elementClass = (Class<?>)actualTypeArguments[0] ;
            Itk.dbgVal("cClass", elementClass) ;
            return elementClass ;
        } catch(Exception e) {
            throw new RuntimeException(e) ;
        }
    } 

} // class RingBuffer

