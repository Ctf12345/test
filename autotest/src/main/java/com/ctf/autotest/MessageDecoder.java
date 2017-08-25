package com.ctf.autotest;

import com.ctf.ass_codec.binary.SpBinaryMsg;
import com.ctf.ass_codec.mt_body.*;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_codec.sp_body.*;
import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_codec.struct.MessageBody;
import com.ctf.ass_public.utils.ConvUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.IOException;
import java.nio.ByteOrder;

/*
 BinaryMsg解码器(TLV)
 */
public class MessageDecoder extends LengthFieldBasedFrameDecoder {
    private static LogWrapper logger = LogWrapper.getLogger(MessageDecoder.class.getName());

    private int mTag;

    private MessageDecoder(ByteOrder byteOrder,
                           int maxFrameLength,
                           int lengthFieldOffset, int lengthFieldLength,
                           int lengthAdjustment, int initialBytesToStrip,
                           boolean failFast) throws IOException {
        super(byteOrder, maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
    }

    public MessageDecoder(int mTag) throws IOException {
        /*
            | ------------- | --------------------- | -------------- | ------------------- | --------------- |
            | TAG (4 bytes) | body length (2 bytes) | prop (1 bytes) | body data (N bytes) | CRC16 (2 bytes) |
            | ------------- | --------------------- | -------------- | ------------------- | --------------- |
        */
        this(ByteOrder.BIG_ENDIAN,        //byteOrder
                64 * 1024,                  //maxFrameLength
                4,                          //lengthFieldOffset
                2,                          //lengthFiledLength
                7,                          //lengthAdjustment
                0,                          //initialBytesToStrip
                false                      //failFast
        );
        this.mTag = mTag;
    }

    private MessageBody decodeMessageBody(ByteBuf buf, int tag, short cmd_id_value, short len) throws Exception {
        MessageBody body = null;
        switch (tag) {
            case Message.ToSpTag:
            {
                SpBinaryMsg.CmdId cmdId = SpBinaryMsg.CmdId.valueOf(cmd_id_value);
                switch (cmdId) {
                    case CMD_SP_LOGIN:
                        body = new SpLoginRsp(buf);
                        break;
                    case CMD_SP_SIM_INFO:
                        body = new SpSimInfoRsp(buf);
                        break;
                    case CMD_SP_SIM_INFO_UPDATE:
                        body = new SpSimInfoRsp(buf);
                        break;
                    case CMD_SP_SIM_IMG:
                        body = new SpTermSimImgReq(buf);
                        break;
                    case CMD_SP_SIM_APDU:
                        body = new SpTermSimApduReq(buf);
                        break;
                    case CMD_SP_HEARTBEAT:
                        body = new SpHeartBeatRsp(buf);
                        break;
                }
            }
            break;

            case Message.ToMtTag:
            {
                if (len == 0) {
                    break;
                }
                byte[] body_bytes = new byte[len];
                buf.readBytes(body_bytes);

                MtProtoMsg.CMDID cmdId = MtProtoMsg.CMDID.forNumber(cmd_id_value);
                switch(cmdId) {
                    case LOGIN:
                    {
                        body = MtLoginRsp.parseFrom(body_bytes);
                    }
                    break;

                    case LOGOUT:
                    {
                        body = MtLogoutRsp.parseFrom(body_bytes);
                    }
                    break;

                    case REQUIM:
                    {
                        body = MtReqUimRsp.parseFrom(body_bytes);
                    }
                    break;

                    case REVUIM:
                    {
                        body = MtRevUimRsp.parseFrom(body_bytes);
                    }
                    break;

                    case LOGINBYID:
                    {
                        body = MtLoginByIdRsp.parseFrom(body_bytes);
                    }
                    break;

                    case SIMIMG:
                    {
                        body = MtSimImgRsp.parseFrom(body_bytes);
                    }
                    break;

                    case SIMPOOL:
                    {
                        body = MtSimPoolRsp.parseFrom(body_bytes);
                    }
                    break;

                    case HEARTBEAT:
                    {
                        body = MtHeartBeatRsp.parseFrom(body_bytes);
                    }
                    break;

                    case LOGUPLOADRET:
                    {
                        body = MtLogUploadRetRsp.parseFrom(body_bytes);
                    }
                    break;

                    //=============================================================

                    case TERMINFO:
                    {
                        body = MtTermInfoReq.parseFrom(body_bytes);
                    }
                    break;

                    case TERMREVUIM:
                    {
                        body = MtTermRevUimReq.parseFrom(body_bytes);
                    }
                    break;

                    case TERMLOGOUT:
                    {
                        body = MtTermLogoutReq.parseFrom(body_bytes);
                    }
                    break;

                    case TERMASSIGNUIM:
                    {
                        body = MtTermAssignUimReq.parseFrom(body_bytes);
                    }
                    break;

                    case TERMTRACE:
                    {
                        body = MtTermTraceReq.parseFrom(body_bytes);
                    }
                    break;

                    case TERMLOGUPLOAD:
                    {
                        body = MtTermLogUploadReq.parseFrom(body_bytes);
                    }
                    break;

                    default:
                    {

                    }
                    break;
                }
            }
            break;

            default:
                break;
        }

        return body;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf)super.decode(ctx, in);
        if (frame != null) {
            int tag = frame.readInt();
            if (tag == mTag) {
                short len = frame.readShort();
                short cmd_id_value = frame.readShort();
                short seq_no = frame.readShort();
                byte prop = frame.readByte();
                frame.skipBytes(len);
                int crc16 = (frame.readShort() & 0xFFFF);
                int calc_crc16 = Message.calcCrc16(frame, 0, len + 11);
                if (crc16 == calc_crc16) {
                    frame.readerIndex(11);    //reset readerIndex to body start

                    try {
                        MessageBody body = decodeMessageBody(frame, mTag, cmd_id_value, len);
                        return new Message(mTag, cmd_id_value, seq_no, prop, body);
                    } catch (Exception e) {
                        frame.readerIndex(11);    //reset readerIndex to body start
                        byte[] body_bytes = new byte[len];
                        frame.readBytes(body_bytes);

                        logger.error(ctx, "[decode]:BODY ERROR! cmd_id_value:" + cmd_id_value + "," + e.getMessage() + ", body_bytes:" + ConvUtils.bytesToHexStr(body_bytes));
                    }
                } else {
                    logger.error(ctx, "[decode]:CRC ERROR! Read:" + crc16 + ", Calced:" + calc_crc16);
                }
            } else {
                logger.error(ctx, "[decode]:TAG ERROR! Read:" + tag + ", Expected:" + mTag);
            }
        } else {
            //frame in-completed(decode is executed immediately when data received)
        }

        return null;
    }
}
