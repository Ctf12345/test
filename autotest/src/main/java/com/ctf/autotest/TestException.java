package com.ctf.autotest;

public class TestException extends Exception {
    public TestException(String func_name, String field, Object expected, Object got)
    {
        super(String.format("%s failed! [%s] expected:%s, got:%s", func_name, field, expected, got));
    }
}
