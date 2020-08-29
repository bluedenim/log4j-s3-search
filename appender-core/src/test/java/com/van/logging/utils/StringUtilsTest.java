package com.van.logging.utils;


import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testIsTruthyString() {
        Assert.assertTrue(StringUtils.isTruthy("abc"));
        Assert.assertTrue(StringUtils.isTruthy("  abc\t\n"));
    }

    @Test
    public void testIsTruthyWhitespaces() {
        Assert.assertFalse(StringUtils.isTruthy("   "));
        Assert.assertFalse(StringUtils.isTruthy("\t\n"));
    }

    @Test
    public void testIsTruthyNull() {
        Assert.assertFalse(StringUtils.isTruthy(null));
    }

    @Test
    public void testaddTrailingIfNeededNull() {
        Assert.assertNull(StringUtils.addTrailingIfNeeded(null, "blah"));
    }

    @Test
    public void testaddTrailingIfNeededBlank() {
        Assert.assertEquals("", StringUtils.addTrailingIfNeeded("", "blah"));
    }

    @Test
    public void testaddTrailingIfNeededNullTrailing() {
        Assert.assertEquals("hello", StringUtils.addTrailingIfNeeded("hello", null));
    }

    @Test
    public void testaddTrailingIfNeeded() {
        Assert.assertEquals("hello/", StringUtils.addTrailingIfNeeded("hello", "/"));
        Assert.assertEquals("hello/", StringUtils.addTrailingIfNeeded("hello/", "/"));
    }
}
