/**
Copyright (c) 2018-present, Walmart, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.walmartlabs.x12.standard.txset.asn856;

import com.walmartlabs.x12.X12Segment;
import com.walmartlabs.x12.X12TransactionSet;
import com.walmartlabs.x12.common.segment.DTMDateTimeReference;
import com.walmartlabs.x12.exceptions.X12ErrorDetail;
import com.walmartlabs.x12.exceptions.X12ParserException;
import com.walmartlabs.x12.standard.X12Group;
import com.walmartlabs.x12.standard.X12Loop;
import com.walmartlabs.x12.standard.txset.asn856.loop.Order;
import com.walmartlabs.x12.standard.txset.asn856.loop.Shipment;
import com.walmartlabs.x12.standard.txset.asn856.loop.Tare;
import com.walmartlabs.x12.standard.txset.asn856.segment.MANMarkNumber;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultAsn856TransactionSetParserTest {

    private DefaultAsn856TransactionSetParser txParser;
    
    @Before
    public void init() {
        txParser = new DefaultAsn856TransactionSetParser();
    }
    
    @Test
    public void test_handlesTransactionSet() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = this.getTestSegments();
        assertTrue(txParser.handlesTransactionSet(segments, x12Group));
    }
    
    @Test
    public void test_handlesTransactionSet_fails_invalid_envelope() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = this.getTestSegments();
        // remove the last segment (SE)
        segments.remove(segments.size() - 1);
        assertFalse(txParser.handlesTransactionSet(segments, x12Group));
    }
    
    @Test
    public void test_handlesTransactionSet_OnlyEnvelope() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = this.getEnvelopeOnly();
        assertTrue(txParser.handlesTransactionSet(segments, x12Group));
    }
    
    @Test
    public void test_handlesTransactionSet_empty() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = Collections.emptyList();
        assertFalse(txParser.handlesTransactionSet(segments, x12Group));
    }
    
    @Test
    public void test_handlesTransactionSet_null() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = null;
        assertFalse(txParser.handlesTransactionSet(segments, x12Group));
    }
    
    @Test
    public void test_doParse_null() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = null;
        X12TransactionSet txSet = txParser.doParse(segments, x12Group);
        assertNull(txSet);
    }
    
    @Test
    public void test_doParse_empty() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = Collections.emptyList();
        X12TransactionSet txSet = txParser.doParse(segments, x12Group);
        assertNull(txSet);
    }
    
    @Test
    public void test_doParse_OnlyEnvelope() {
        try {
            X12Group x12Group = new X12Group();
            List<X12Segment> segments = this.getSegmentsOnlyEnvelope();
            txParser.doParse(segments, x12Group);
            fail("expected parsing exception");
        } catch (X12ParserException e) {
            assertEquals("expected BSN segment but found SE", e.getMessage());
        }
    }
    
    @Test
    public void test_doParse_Missing_SE() {
        try {
            X12Group x12Group = new X12Group();
            List<X12Segment> segments = this.getTestSegments();
            // remove SE
            segments.remove(segments.size() - 1);
            txParser.doParse(segments, x12Group);
            fail("expected parsing exception");
        } catch (X12ParserException e) {
            assertEquals("expected SE segment but found SN1", e.getMessage());
        }
    }
    
    @Test
    public void test_doParse_NoHierarchicalLoops() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = this.getSegmentsNoHierarchicalLoops();
        X12TransactionSet txSet = txParser.doParse(segments, x12Group);
        assertNotNull(txSet);
        
        AsnTransactionSet asnTx = (AsnTransactionSet) txSet;
        
        // looping check
        assertFalse(asnTx.isLoopingValid());
        List<X12ErrorDetail> loopErrors = asnTx.getLoopingErrors();
        assertNotNull(loopErrors);
        assertEquals(1, loopErrors.size());
        assertEquals("missing shipment loop", loopErrors.get(0).getMessage());
        
        // BSN
        assertEquals("05755986", asnTx.getShipmentIdentification());
    }
    
    @Test
    public void test_doParse_FirstLoop_NotShipment() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = this.getTestSegments("X", "O");
        X12TransactionSet txSet = txParser.doParse(segments, x12Group);
        assertNotNull(txSet);
        
        AsnTransactionSet asnTx = (AsnTransactionSet) txSet;
        
        // looping check
        assertFalse(asnTx.isLoopingValid());
        List<X12ErrorDetail> loopErrors = asnTx.getLoopingErrors();
        assertEquals(1, loopErrors.size());
        assertEquals("first HL is not a shipment", loopErrors.get(0).getMessage());
        
        // BSN
        assertEquals("05755986", asnTx.getShipmentIdentification());
    }
    
    @Test
    public void test_doParseSecondLoop_NotOrder() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = this.getTestSegments("S", "X");
        X12TransactionSet txSet = txParser.doParse(segments, x12Group);
        assertNotNull(txSet);
        
        AsnTransactionSet asnTx = (AsnTransactionSet) txSet;
        
        // looping check
        assertFalse(asnTx.isLoopingValid());
        List<X12ErrorDetail> loopErrors = asnTx.getLoopingErrors();
        assertEquals(1, loopErrors.size());
        assertEquals("expected Order HL but got X", loopErrors.get(0).getMessage());
        
        // BSN
        assertEquals("05755986", asnTx.getShipmentIdentification());
    }

    @Test
    public void test_doParse_TwoTopLevelHierarchicalLoops() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = this.getTwoShipmentLoops();
        X12TransactionSet txSet = txParser.doParse(segments, x12Group);
        assertNotNull(txSet);
        
        AsnTransactionSet asnTx = (AsnTransactionSet) txSet;
        
        // looping check
        assertFalse(asnTx.isLoopingValid());
        List<X12ErrorDetail> loopErrors = asnTx.getLoopingErrors();
        assertEquals(1, loopErrors.size());
        assertEquals("expected one top level HL", loopErrors.get(0).getMessage());
        
        // BSN
        assertEquals("05755986", asnTx.getShipmentIdentification());
    }
    
    @Test
    public void test_doParse_UnexpectedSegmentBeforeHierarchicalLoops() {
        X12Group x12Group = new X12Group();
        // has DTM and REF segments 
        // before the HL shipment loop
        List<X12Segment> segments = this.getUnexpectedSegmentBeforeHierarchicalLoops();
        X12TransactionSet txSet = txParser.doParse(segments, x12Group);
        assertNotNull(txSet);
        
        AsnTransactionSet asnTx = (AsnTransactionSet) txSet;
        
        // looping check
        assertTrue(asnTx.isLoopingValid());
        List<X12ErrorDetail> loopErrors = asnTx.getLoopingErrors();
        assertNull(loopErrors);
        
        // BSN
        assertEquals("05755986", asnTx.getShipmentIdentification());
        
        List<DTMDateTimeReference> dtms = asnTx.getDtmReferences();
        assertEquals(1, dtms.size());
        assertEquals("20210323", dtms.get(0).getDate());
        
        List<X12Segment> unexpectedSegments = asnTx.getUnexpectedSegmentsBeforeLoop();
        assertEquals(1, unexpectedSegments.size());
        assertEquals("REF", unexpectedSegments.get(0).getIdentifier());
    }
    
    @Test
    public void test_doParse_DTM_BeforeFirstLoop() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = this.getDTMBeforeFirstLoopSegments();
        txParser.doParse(segments, x12Group);
        X12TransactionSet txSet = txParser.doParse(segments, x12Group);
        assertNotNull(txSet);
        
        AsnTransactionSet asnTx = (AsnTransactionSet) txSet;
        
        // looping check
        assertTrue(asnTx.isLoopingValid());
        List<X12ErrorDetail> loopErrors = asnTx.getLoopingErrors();
        assertNull(loopErrors);
        
        // BSN
        assertEquals("05755986", asnTx.getShipmentIdentification());
        
        List<DTMDateTimeReference> dtms = asnTx.getDtmReferences();
        assertEquals(2, dtms.size());
    }

    @Test
    public void test_doParse() {
        X12Group x12Group = new X12Group();
        List<X12Segment> segments = this.getTestSegments();
        X12TransactionSet txSet = txParser.doParse(segments, x12Group);
        assertNotNull(txSet);
        
        AsnTransactionSet asnTx = (AsnTransactionSet) txSet;
        
        // looping check
        assertTrue(asnTx.isLoopingValid());
        List<X12ErrorDetail> loopErrors = asnTx.getLoopingErrors();
        assertNull(loopErrors);
        
        // BSN
        assertEquals("05755986", asnTx.getShipmentIdentification());
        
        List<DTMDateTimeReference> dtms = asnTx.getDtmReferences();
        assertNull(dtms);
        
        // shipment
        Shipment shipment = asnTx.getShipment();
        List<X12Loop> orders = shipment.getParsedChildrenLoops();
        assertNotNull(orders);
        assertEquals(1, orders.size());
        
        // order
        X12Loop orderLoop = orders.get(0);
        assertNotNull(orderLoop);
        assertTrue(orderLoop instanceof Order);
        assertEquals("O", orderLoop.getCode());

        List<X12Loop> tares = ((Order)orderLoop).getParsedChildrenLoops();
        assertNotNull(tares);
        assertEquals(1, tares.size());

        // tare
        X12Loop tareLoop = tares.get(0);
        assertNotNull(tareLoop);
        assertTrue(tareLoop instanceof Tare);
        assertEquals("T", tareLoop.getCode());

        Tare tare = (Tare) tareLoop;
        List<MANMarkNumber> manList = tare.getManList();
        MANMarkNumber man = manList.get(0);
        assertNotNull(man);
        assertEquals("GM", man.getQualifier());
        assertEquals("00100700302232310393", man.getNumber());
        
        List<X12Segment> unparsedSegments = tare.getUnparsedSegments();
        assertNotNull(unparsedSegments);
        assertEquals(1, unparsedSegments.size());
        assertEquals("TEST", unparsedSegments.get(0).getElement(2));

        List<X12Loop> tareChildLoops = tare.getParsedChildrenLoops();
        assertNotNull(tareChildLoops);
        assertEquals(1, tareChildLoops.size());
    }
    
    private List<X12Segment> getSegmentsOnlyEnvelope() {
        List<X12Segment> txSegments = new ArrayList<>();
        
        txSegments.add(new X12Segment("ST*856*368090001"));
        txSegments.add(new X12Segment("SE*296*368090001"));
        
        return txSegments;
    }
    
    private List<X12Segment> getSegmentsNoHierarchicalLoops() {
        List<X12Segment> txSegments = new ArrayList<>();
        
        txSegments.add(new X12Segment("ST*856*368090001"));
        txSegments.add(new X12Segment("BSN*00*05755986*20190523*171543*0002"));
        txSegments.add(new X12Segment("SE*296*368090001"));
        
        return txSegments;
    }
    
    private List<X12Segment> getTwoShipmentLoops() {
        List<X12Segment> txSegments = new ArrayList<>();
        
        txSegments.add(new X12Segment("ST*856*368090001"));
        txSegments.add(new X12Segment("BSN*00*05755986*20190523*171543*0002"));
        txSegments.add(new X12Segment("HL*1**S"));
        txSegments.add(new X12Segment("HL*2**S"));
        txSegments.add(new X12Segment("SE*296*368090001"));
        
        return txSegments;
    }
    
    private List<X12Segment> getUnexpectedSegmentBeforeHierarchicalLoops() {
        List<X12Segment> txSegments = new ArrayList<>();
        
        txSegments.add(new X12Segment("ST*856*368090001"));
        txSegments.add(new X12Segment("BSN*00*05755986*20190523*171543*0002"));
        txSegments.add(new X12Segment("DTM*067*20210323"));
        txSegments.add(new X12Segment("REF*ZZ*420554090"));
        txSegments.add(new X12Segment("HL*1**S"));
        txSegments.add(new X12Segment("SE*296*368090001"));
        
        return txSegments;
    }
    
    private List<X12Segment> getDTMBeforeFirstLoopSegments() {
        List<X12Segment> txSegments = new ArrayList<>();
        
        txSegments.add(new X12Segment("ST*856*368090001"));
        txSegments.add(new X12Segment("BSN*00*05755986*20190523*171543*0002"));
        txSegments.add(new X12Segment("DTM*011*20210323"));
        txSegments.add(new X12Segment("DTM*067*20210323"));
        txSegments.add(new X12Segment("HL*1**S"));
        txSegments.add(new X12Segment("SE*296*368090001"));
        
        return txSegments;
    }

    private List<X12Segment> getEnvelopeOnly() {
        List<X12Segment> txSegments = new ArrayList<>();

        txSegments.add(new X12Segment("ST*856*368090001"));
        txSegments.add(new X12Segment("SE*296*368090001"));

        return txSegments;
    }
    
    private List<X12Segment> getTestSegments() {
        return getTestSegments("S", "O");
    }
    
    private List<X12Segment> getTestSegments(String firstLoopCode, String secondLoopCode) {
        List<X12Segment> txSegments = new ArrayList<>();

        //
        // ASN 856
        txSegments.add(new X12Segment("ST*856*368090001"));
        txSegments.add(new X12Segment("BSN*00*05755986*20190523*171543*0002"));

        //
        // shipment
        //
        txSegments.add(new X12Segment("HL*1**" + firstLoopCode));

        //
        // order
        //
        txSegments.add(new X12Segment("HL*2*1*" + secondLoopCode));

        // Tare
        txSegments.add(new X12Segment("HL*3*2*T"));
        txSegments.add(new X12Segment("MAN*GM*00100700302232310393"));
        txSegments.add(new X12Segment("REF*XX*TEST"));

        // Pack
        txSegments.add(new X12Segment("HL*4*3*P"));
        txSegments.add(new X12Segment("MAN*UC*10081131916933"));

        // Item
        txSegments.add(new X12Segment("HL*5*4*I"));
        txSegments.add(new X12Segment("LIN**UP*039364170623"));
        txSegments.add(new X12Segment("SN1**18*EA"));

        //
        // SE
        //
        txSegments.add(new X12Segment("SE*296*368090001"));

        return txSegments;
    }

}
