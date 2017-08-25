package com.ctf.autotest;

import com.ctf.ass_codec.binary.SpBinaryMsg;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_public.globals.ErrCode;

import java.util.ArrayList;

public class AssertMessage {
    private String cmdId;
    private Integer seqNo;
    private ArrayList<AssertBodyParam> assertBodyParams;
    private String desc;

    public AssertMessage(String cmdId, Integer seqNo, ArrayList<AssertBodyParam> assertBodyParams, String desc) {
        this.cmdId = cmdId;
        this.seqNo = seqNo;
        this.assertBodyParams = assertBodyParams;
        this.desc = desc;
    }

    public AssertMessage(String cmdId, Integer seqNo, String desc) {
        this(cmdId, seqNo, null, desc);
    }

    public AssertMessage(String cmdId, short seqNo, String desc) {
        this(cmdId, Integer.valueOf(seqNo), null, desc);
    }

    //--------------------------------------------------------------

    public AssertMessage addMtExpectRet(String expect_ret) {
        AssertBodyParam assertBodyParam = new AssertBodyParam("expect_ret",
                expect_ret,
                "ServerCode.valueOf((short)msgBody.getResult().getCode()).name()");
        return this.addAssertBodyParam(assertBodyParam);
    }


    public AssertMessage addSpExpectRet(String expect_ret) {
        AssertBodyParam assertBodyParam = new AssertBodyParam("expect_ret",
                expect_ret,
                "String.valueOf(msgBody.getResult())");
        return this.addAssertBodyParam(assertBodyParam);
    }

    public AssertMessage addAssertBodyParam(AssertBodyParam assertBodyParam) {
        if (this.assertBodyParams == null) {
            this.assertBodyParams = new ArrayList<>();
        }
        this.assertBodyParams.add(assertBodyParam);
        return this;
    }

    //--------------------------------------------------------------
    public String getCmdId() {
        return cmdId;
    }

    public Integer getSeqNo() {
        return seqNo;
    }

    public ArrayList<AssertBodyParam> getAssertBodyParams() {
        return assertBodyParams;
    }

    public boolean isMatch(Message message) {
        String cmdIdName = "";
        switch(message.getTag()) {
            case Message.ToMtTag:
                cmdIdName = MtProtoMsg.CMDID.forNumber(message.getCmdId()).name();
                break;

            case Message.ToSpTag:
                cmdIdName = SpBinaryMsg.CmdId.valueOf(message.getCmdId()).name();
                break;

            default:
                break;
        }

        return ( cmdIdName.equals(cmdId) &&
                    (seqNo == null || seqNo == message.getSeqNo()) );
    }

    public boolean doAssert(Message message) throws TestException {
        boolean bRet = true;
        if (assertBodyParams != null) {
            for (AssertBodyParam assertBodyParam : assertBodyParams) {
                if (!assertBodyParam.eval(message)) {
                    bRet = false;
                    break;
                }
            }
        }

        return bRet;
    }
}