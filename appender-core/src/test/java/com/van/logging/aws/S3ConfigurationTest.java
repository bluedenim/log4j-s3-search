package com.van.logging.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class S3ConfigurationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testResolveRegionNegative() {
        thrown.expect(IllegalArgumentException.class);
        S3Configuration.resolveRegion("asdf");
    }

    @Test
    public void testResolveRegionNullOrBlank() {
        assertNull("null input should return null Region",
            S3Configuration.resolveRegion(null));

        assertNull("blank input should return null Region",
            S3Configuration.resolveRegion(""));
    }

    @Test
    public void testResolveRegionUsWest1() {
        Region expected = Region.getRegion(Regions.US_WEST_1);
        assertEquals("resolve from public region name",
            expected, S3Configuration.resolveRegion("us-west-1"));

        assertEquals("resolve from Regions ordinal name",
            expected, S3Configuration.resolveRegion("US_WEST_1"));
    }
}
