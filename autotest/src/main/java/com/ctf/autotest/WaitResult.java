package com.ctf.autotest;

public enum WaitResult {  //断言结果
    WAIT_RESULT_UNKNOWN,        //未知
    WAIT_RESULT_ASSERT_PASS,   //断言成功
    WAIT_RESULT_ASSERT_FAIL,   //断言失败
    WAIT_RESULT_TIMEOUT,        //超时
    WAIT_RESULT_DISCONN,        //连接断开
    WAIT_RESULT_EXCEPTION;      //其它异常
}
