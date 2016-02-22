// -*- mode: java; indent-tabs-mode: nil -*-
/** RingBuffer Unit Test 
 * @author:: Itsuki Noda
 * @version:: 0.0 2016/02/21 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2016/02/21]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import static org.junit.Assert.*;
import org.junit.Test;
//import junit.framework.TestCase;

import java.util.ArrayList;

import nodagumi.Itk.Itk;
import nodagumi.Itk.RingBuffer;
import nodagumi.Itk.RingBuffer.ExpandType;

//======================================================================
/**
 * description of Test.
 */
public class RingBuffer_Test {
    //------------------------------------------------------------
    /**
     * test constructor
     */
    //@Test
    public void testConstruct() {
        RingBuffer<Double> rbd0 = new RingBuffer<>(10) ;
        Itk.dbgVal("rbd0", rbd0) ;
        RingBuffer<Double> rbd1 = new RingBuffer<>(10,ExpandType.Fixed) ;
        Itk.dbgVal("rbd1", rbd1) ;
        RingBuffer<Double> rbd2 = new RingBuffer<>(10,ExpandType.Overwrite) ;
        Itk.dbgVal("rbd2", rbd2) ;
        RingBuffer<Double> rbd3 = new RingBuffer<>(10,ExpandType.Recycle) ;
        Itk.dbgVal("rbd3", rbd3) ;
    }
    //------------------------------------------------------------
    /**
     * test iterator
     */
    //@Test
    public void testIteratorAuto() {
        RingBuffer<Double> rb0 = new RingBuffer<>(10,ExpandType.Auto) ;
        subTestIterator(rb0) ;
    }
    //@Test
    public void testIteratorFixed() {
        RingBuffer<Double> rb0 = new RingBuffer<>(10,ExpandType.Fixed) ;
        subTestIterator(rb0) ;
    }
    //@Test
    public void testIteratorOverwrite() {
        RingBuffer<Double> rb0 = new RingBuffer<>(10,ExpandType.Overwrite) ;
        subTestIterator(rb0) ;
    }
    //@Test
    public void testIteratorRecycle() {
        RingBuffer<Double> rb0 = new RingBuffer<>(10,ExpandType.Recycle) ;
        subTestIterator(rb0) ;
    }
    /* */
    public void subTestIterator(RingBuffer<Double> rb0) {
        Itk.dbgVal("rb0",rb0) ;
        for(int i = 0 ; i < 5 ; i++) {
            rb0.enqueue((double)i) ;
            Itk.dbgVal("enqueue:dequeue:i",i) ;
            Itk.dbgVal("rb0.dequeue",rb0.dequeue()) ;
        }
        for(int i = 0 ; i < 8 ; i++) {
            rb0.enqueue((double)(i+10)) ;
            Itk.dbgVal("enqueue::i",i) ;
        }
        for(int i = 0 ; i < 3 ; i++) {
            Itk.dbgVal("dequeue::i",i) ;
            Itk.dbgVal("rb0.dequeue",rb0.dequeue()) ;
        }
        for(int i = 0 ; i < rb0.getSize() ; i++) {
            Itk.dbgVal("rb0.get(i)",rb0.get(i)) ;
        }
        for(Double d : rb0) {
            Itk.dbgVal("d",d) ;
        }
        Itk.dbgVal("buffer",rb0.getBuffer()) ;
        Itk.dbgVal("toArrayList",rb0.toArrayList()) ;
    }

    //------------------------------------------------------------
    /**
     * test expand
     */
    //@Test
    public void testExpand() {
        RingBuffer<Double> rb0 = new RingBuffer<>(10, ExpandType.Auto) ;
        Itk.dbgVal("rb0",rb0) ;
        for(int i = 0 ; i < 5 ; i++) {
            rb0.enqueue((double)i) ;
            rb0.dequeue() ;
        }
        Itk.dbgVal("rb0",rb0) ;

        for(int i = 0 ; i < 15 ; i++) {
            rb0.enqueue((double)(i + 10)) ;
            Itk.dbgVal("rb0", rb0) ;
            Itk.dbgVal("buffer", rb0.getBuffer()) ;
            Itk.dbgVal("array", rb0.toArrayList()) ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * test Fixed
     */
    //@Test
    public void testFixed() {
        RingBuffer<Double> rb0 = new RingBuffer<>(10, ExpandType.Fixed) ;
        Itk.dbgVal("rb0",rb0) ;
        for(int i = 0 ; i < 5 ; i++) {
            Itk.dbgVal("rb0.enqueue", rb0.enqueue((double)i)) ;
            Itk.dbgVal("rb0.dequeue", rb0.dequeue()) ;
        }
        Itk.dbgVal("rb0",rb0) ;

        for(int i = 0 ; i < 15 ; i++) {
            Itk.dbgVal("rb0.enqueue", rb0.enqueue((double)(i + 10))) ;
            Itk.dbgVal("rb0", rb0) ;
            Itk.dbgVal("buffer", rb0.getBuffer()) ;
            Itk.dbgVal("array", rb0.toArrayList()) ;
        }
    }

    //------------------------------------------------------------
    /**
     * test Overwrite
     */
    @Test
    public void testOverwrite() {
        RingBuffer<Double> rb0 = new RingBuffer<>(10, ExpandType.Overwrite) ;
        Itk.dbgVal("rb0",rb0) ;
        for(int i = 0 ; i < 5 ; i++) {
            Itk.dbgVal("rb0.enqueue", rb0.enqueue((double)i)) ;
            Itk.dbgVal("rb0.dequeue", rb0.dequeue()) ;
        }
        Itk.dbgVal("rb0",rb0) ;

        for(int i = 0 ; i < 15 ; i++) {
            Itk.dbgVal("rb0.enqueue", rb0.enqueue((double)(i + 10))) ;
            Itk.dbgVal("rb0", rb0) ;
            Itk.dbgVal("buffer", rb0.getBuffer()) ;
            Itk.dbgVal("array", rb0.toArrayList()) ;
        }
    }

    //------------------------------------------------------------
    /**
     * test recycle
     */
    //@Test
    public void testRecycle() {
        RingBuffer<TestRecycleFoo> rb0 =
            new RingBuffer<>(10, ExpandType.Recycle, true) ;
        rb0.fillElements(TestRecycleFoo.class) ;

        Itk.dbgVal("rb0",rb0) ;
        Itk.dbgVal("buffer", rb0.getBuffer()) ;
        Itk.dbgVal("array", rb0.toArrayList()) ;
        for(int i = 0 ; i < 5 ; i++) {
            rb0.shiftHead().set(i+1,(double)(i * i)) ;
        }
        Itk.dbgVal("rb0",rb0) ;

        for(int i = 0 ; i < 15 ; i++) {
            rb0.shiftHead().set(i + 100, (double)(3 * i)) ;
            Itk.dbgVal("rb0", rb0) ;
            Itk.dbgVal("buffer", rb0.getBuffer()) ;
            Itk.dbgVal("array", rb0.toArrayList()) ;
        }
    }
    static class TestRecycleFoo {
        public int id = 0;
        public double v = 0.0;
        public void set(int _id, double _v) {
            id = _id ; v = _v ;
        }
        public String toString(){ return ("#Foo[" + id + "]") ; }
    } ;

} // class RingBuffer_Test

