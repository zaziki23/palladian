package ws.palladian.helper.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

public class StatsTest {

    @Test
    public void testStats() {
        Collection<Double> numbers = Arrays.asList(2., 1., 6., 10., 23., 7.);
        Stats stats = new Stats(numbers);
        assertEquals(6, stats.getCount());
        assertEquals(8.167, stats.getMean(), 0.001);
        assertEquals(6.5, stats.getMedian(), 0);
        assertEquals(23, stats.getMax(), 0);
        assertEquals(1, stats.getMin(), 0);
        assertEquals(7.985, stats.getStandardDeviation(), 0.001);
        assertEquals(119.833, stats.getMse(), 0.001);
        assertEquals(10.947, stats.getRmse(), 0.001);
        assertEquals(0.5, stats.getCumulativeProbability(6), 0.001);
        // System.out.println(stats);
    }

    @Test
    public void testRunningStats() {
        Stats stats = new Stats(3);
        stats.add(1);
        assertEquals(1, stats.getMean(), 0);
        stats.add(2);
        assertEquals(1.5, stats.getMean(), 0);
        stats.add(3);
        assertEquals(2, stats.getMean(), 0);
        stats.add(4);
        assertEquals(3, stats.getMean(), 0);
        stats.add(5);
        assertEquals(4, stats.getMean(), 0);
    }

    @Test
    public void testMedian() {
        assertEquals(2.5, new Stats(Arrays.asList(1., 1., 2., 3., 1035., 89898.68)).getMedian(), 0);
        assertEquals(2., new Stats(Arrays.asList(0., 1., 2., 3., 4.)).getMedian(), 0);
        assertEquals(2.5, new Stats(Arrays.asList(0., 1., 2., 3., 4., 5.)).getMedian(), 0);
        assertEquals(7., new Stats(Arrays.asList(9., 7., 2.)).getMedian(), 0.00001);
        assertEquals(0., new Stats(Arrays.asList(0., 0., 0., 1.)).getMedian(), 0);
        assertEquals(3948348538l, new Stats(Arrays.asList(1l, 2l, 3948348538l, 3948348539l, 3948348540l)).getMedian(),
                0);
    }

    @Test
    public void testStandardDeviation() {
        assertEquals(2.14, new Stats(Arrays.asList(2., 4., 4., 4., 5., 5., 7., 9.)).getStandardDeviation(), 0.01);
        assertEquals(2.24, new Stats(Arrays.asList(4, 2, 5, 8, 6)).getStandardDeviation(), 0.01);
        assertEquals(0, new Stats(Arrays.asList(1)).getStandardDeviation(), 0);
        assertTrue(Double.isNaN(new Stats().getStandardDeviation()));
    }

    @Test
    public void testNoValues() {
        Stats stats = new Stats();
        assertEquals(0, stats.getCount());
        assertTrue(Double.isNaN(stats.getMax()));
        assertTrue(Double.isNaN(stats.getMin()));
        assertTrue(Double.isNaN(stats.getMedian()));
        assertTrue(Double.isNaN(stats.getMean()));
        assertTrue(Double.isNaN(stats.getStandardDeviation()));
        assertEquals(0, stats.getSum(), 0);
        assertTrue(Double.isNaN(stats.getMse()));
        assertTrue(Double.isNaN(stats.getRmse()));
    }

}
