package com.ctf.ass.server.netty.proto;

import com.ctf.ass.server.component.MongoDbUtil;
import com.ctf.ass.server.component.SpringContextHandler;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_codec.binary.SpBinaryMsg;
import com.ctf.ass_codec.mt_body.MtHeartBeatReq;
import com.ctf.ass_codec.mt_body.MtLogUploadRetReq;
import com.ctf.ass_codec.mt_body.MtLoginByIdReq;
import com.ctf.ass_codec.mt_body.MtLoginReq;
import com.ctf.ass_codec.mt_body.MtLogoutReq;
import com.ctf.ass_codec.mt_body.MtRTraceInd;
import com.ctf.ass_codec.mt_body.MtReqUimReq;
import com.ctf.ass_codec.mt_body.MtRevUimReq;
import com.ctf.ass_codec.mt_body.MtSimImgReq;
import com.ctf.ass_codec.mt_body.MtSimPoolReq;
import com.ctf.ass_codec.mt_body.MtTermAssignUimRsp;
import com.ctf.ass_codec.mt_body.MtTermInfoRsp;
import com.ctf.ass_codec.mt_body.MtTermLogUploadRsp;
import com.ctf.ass_codec.mt_body.MtTermLogoutRsp;
import com.ctf.ass_codec.mt_body.MtTermRevUimRsp;
import com.ctf.ass_codec.mt_body.MtTermTraceRsp;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_codec.sp_body.SpHeartBeatReq;
import com.ctf.ass_codec.sp_body.SpLoginReq;
import com.ctf.ass_codec.sp_body.SpSimInfoReq;
import com.ctf.ass_codec.sp_body.SpTermSimApduRsp;
import com.ctf.ass_codec.sp_body.SpTermSimImgRsp;
import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_codec.struct.MessageBody;
import com.ctf.ass_public.utils.ConvUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.IOException;
import java.nio.ByteOrder;

/*
 BinaryMsg解码器(TLV)
 */
//@Scope("prototype")
//@Component
public class MessageDecoder extends LengthFieldBasedFrameDecoder {
    //    @Autowired
//    private MongoDbUtil mongoDbUtil;
    private static LogWrapper logger = LogWrapper.getLogger(MessageDecoder.class);

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
            case Message.SpTag: {
                SpBinaryMsg.CmdId cmdId = SpBinaryMsg.CmdId.valueOf(cmd_id_value);
                switch (cmdId) {
                    case CMD_SP_LOGIN:
                        body = new SpLoginReq(buf);
                        break;
                    case CMD_SP_SIM_INFO:
                        body = new SpSimInfoReq(buf);
                        break;
                    case CMD_SP_SIM_INFO_UPDATE:
                        body = new SpSimInfoReq(buf);
                        break;
                    case CMD_SP_SIM_IMG:
                        body = new SpTermSimImgRsp(buf);
                        break;
                    case CMD_SP_SIM_APDU:
                        body = new SpTermSimApduRsp(buf);
                        break;
                    case CMD_SP_HEARTBEAT:
                        body = new SpHeartBeatReq(buf);
                        break;
                }
            }
            break;

            case Message.MtTag: {
                if (len == 0) {
                    break;
                }
                byte[] body_bytes = new byte[len];
                buf.readBytes(body_bytes);

                MtProtoMsg.CMDID cmdId = MtProtoMsg.CMDID.forNumber(cmd_id_value);
                switch (cmdId) {
                    case LOGIN: {
                        body = MtLoginReq.parseFrom(body_bytes);
                    }
                    break;

                    case LOGOUT: {
                        body = MtLogoutReq.parseFrom(body_bytes);
                    }
                    break;

                    case REQUIM: {
                        body = MtReqUimReq.parseFrom(body_bytes);
                    }
                    break;

                    case REVUIM: {
                        body = MtRevUimReq.parseFrom(body_bytes);
                    }
                    break;

                    case LOGINBYID: {
                        body = MtLoginByIdReq.parseFrom(body_bytes);
                    }
                    break;

                    case SIMIMG: {
                        body = MtSimImgReq.parseFrom(body_bytes);
                    }
                    break;

                    case SIMPOOL: {
                        //add temp for APDU debug
                        //System.out.println("Recv apdu data:" + ConvUtils.bytesToHexStr(body_bytes));
                        body = MtSimPoolReq.parseFrom(body_bytes);
                    }
                    break;

                    case HEARTBEAT: {
                        body = MtHeartBeatReq.parseFrom(body_bytes);
                    }
                    break;

                    case LOGUPLOADRET: {
                        body = MtLogUploadRetReq.parseFrom(body_bytes);
                    }
                    break;

                    //=============================================================

                    case TERMINFO: {
                        body = MtTermInfoRsp.parseFrom(body_bytes);
                    }
                    break;

                    case TERMREVUIM: {
                        body = MtTermRevUimRsp.parseFrom(body_bytes);
                    }
                    break;

                    case TERMLOGOUT: {
                        body = MtTermLogoutRsp.parseFrom(body_bytes);
                    }
                    break;

                    case TERMASSIGNUIM: {
                        body = MtTermAssignUimRsp.parseFrom(body_bytes);
                    }
                    break;

                    case TERMTRACE: {
                        body = MtTermTraceRsp.parseFrom(body_bytes);
                    }
                    break;

                    case TERMLOGUPLOAD: {
                        body = MtTermLogUploadRsp.parseFrom(body_bytes);
                    }
                    break;

                    //==========================================================

                    case RTRACE: {
                        body = MtRTraceInd.parseFrom(body_bytes);
                    }
                    break;

                    default: {

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
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
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
                        //yuanyuefeng add temp for debug with SimPool
                        if (cmd_id_value == MtProtoMsg.CMDID.SIMPOOL_VALUE ||
                                cmd_id_value == SpBinaryMsg.CmdId.CMD_SP_SIM_APDU.value()) {
                            frame.readerIndex(0);
                            byte[] bytes = new byte[frame.readableBytes()];
                            frame.readBytes(bytes);
                            logger.debug("Got apdu msg bytes from " +
                                    (cmd_id_value == MtProtoMsg.CMDID.SIMPOOL_VALUE ? "MT" : "SP") +
                                    ":[" + ConvUtils.bytesToHexStr(bytes) + "]");
                        }
                        //add end
                        return new Message(mTag, cmd_id_value, seq_no, prop, body);
                    } catch (Exception e) {
                        frame.readerIndex(11);    //reset readerIndex to body start
                        byte[] body_bytes = new byte[len];
                        frame.readBytes(body_bytes);

                        logger.error(ctx, "[decode]:BODY ERROR! cmd_id_value:" + cmd_id_value + "," + e.getMessage() + ", body_bytes:" + ConvUtils.bytesToHexStr(body_bytes));
                        e.printStackTrace();
                    }
                } else {
                    logger.error(ctx, "[decode]:CRC ERROR! Read:" + crc16 + ", Calced:" + calc_crc16);
                }
            } else {
                logger.error(ctx, "[decode]:TAG ERROR! Read:" + tag + ", Expected:" + mTag);
            }
            //如果多次协议包出错，就断开连接
          /*  mongoDbUtil.doSaveBlackUsers(ctx.channel().remoteAddress().toString());
            if (mongoDbUtil.isBlackUser(ctx.channel().remoteAddress().toString())) {
                logger.error(ctx, "[server]:A BlackUser,will close connection.please contact server administrator!");
                ctx.close();
            }*/
            MongoDbUtil mongoDbUtil = SpringContextHandler.getBean(MongoDbUtil.class);
            if (mongoDbUtil != null) {
                mongoDbUtil.doSaveBlackUsers(ctx.channel().remoteAddress().toString());
                if (mongoDbUtil.isBlackUser(ctx.channel().remoteAddress().toString())) {
                    logger.error(ctx, "[server]:A BlackUser,will close connection.please contact server administrator!");
                    ctx.close();
                }
            }
        } else {
            //frame in-completed(decode is executed immediately when data received)
        }

        return null;
    }
}