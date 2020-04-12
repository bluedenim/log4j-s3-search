package com.van.logging.utils;


import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testTruthyString() {
        Assert.assertTrue(StringUtils.isTruthy("abc"));
        Assert.assertTrue(StringUtils.isTruthy("  abc\t\n"));
    }

    @Test
    public void testWhitespaces() {
        Assert.assertFalse(StringUtils.isTruthy("   "));
        Assert.assertFalse(StringUtils.isTruthy("\t\n"));
    }

    @Test
    public void testNull() {
        Assert.assertFalse(StringUtils.isTruthy(null));
    }

}
