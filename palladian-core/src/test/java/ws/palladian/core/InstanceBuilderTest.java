package ws.palladian.core;

import org.junit.Before;
import org.junit.Test;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class InstanceBuilderTest {

    private FeatureVector featureVector;

    @Before
    public void setup() {
        InstanceBuilder builder = new InstanceBuilder();
        builder.set("testFeature1", "test");
        builder.set("testFeature2", 1);
        builder.set("testFeature3", true);
        featureVector = builder.create();
    }

    @Test
    public void testSize() {
        assertEquals(3, featureVector.size());
    }

    @Test
    public void testKeys() {
        assertEquals(new HashSet<>(Arrays.asList("testFeature1", "testFeature2", "testFeature3")), featureVector.keys());
    }

    @Test
    public void testGetExistingFeature() {
        Value value = featureVector.get("testFeature1");
        assertNotNull(value);
        assertTrue(value instanceof NominalValue);
        NominalValue nominalValue = (NominalValue) value;
        assertEquals("test", nominalValue.getString());
    }

    @Test
    public void testGetNonExistingFeature() {
        Value value = featureVector.get("nonExistingFeature");
        assertNotNull(value);
        assertTrue(value instanceof NullValue);
    }

    @Test
    public void testEquals() {
        InstanceBuilder builder = new InstanceBuilder();
        builder.set("testFeature3", true);
        builder.set("testFeature2", 1);
        builder.set("testFeature1", "test");
        FeatureVector other = builder.create();
        assertTrue(featureVector.equals(other));
    }

    @Test
    public void testCopyFeatureVector() {
        FeatureVector copy = new InstanceBuilder().add(featureVector).create();
        assertEquals(3, copy.size());
        assertTrue(featureVector.equals(copy));
    }

}
