package com.ctf.autotest;

import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_public.globals.ErrCode.ServerCode;
import com.ctf.ass_public.utils.CheckUtils;
import com.ctf.ass_public.utils.ConvUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class AssertBodyParam {
    private static LogWrapper logger = LogWrapper.getLogger(CmdUtils.class.getName());

    private String param;           //参数
    private String expect;          //期望值
    private String got_expr;       //获取实际值的表达式，并将结果统一格式化为字符串。如：ServerCode.valueOf(msgBody.getResult().getCode())

    public AssertBodyParam(String param, String expect, String got_expr) {
        this.param = param;
        this.expect = expect;
        this.got_expr = got_expr;
    }

    //执行断言
    public boolean eval(Message message) throws TestException {
        Binding binding = new Binding();
        GroovyShell shell = new GroovyShell(binding);
        //添加对类的引用
        binding.setProperty("ConvUtils", ConvUtils.class);
        binding.setProperty("ServerCode", ServerCode.class);
        binding.setProperty("CheckUtils", CheckUtils.class);

        //添加msgBody的定义
        binding.setVariable("msgBody", message.getBody());
        //添加所有ServerCode的定义
        for (ServerCode code : ServerCode.values()) {
            binding.setVariable(code.name(), code.value());
        }

        String got_script = String.format("got = (%s);", got_expr);
        String got;
        try {
            shell.evaluate(got_script);
            got = (String) binding.getVariable("got");
        } catch (Exception e) {
            throw new TestException("Assert", param, expect,
                    String.format("Exception while eval script[%s]:%s", got_script, e.getMessage()));
        }

        if (!got.equals(expect)) {
            throw new TestException("Assert", param, expect, got);
        }

        logger.debug(String.format("got_script:%s, got:%s, expect:%s", got_script, got, expect));

        return true;
    }
}
