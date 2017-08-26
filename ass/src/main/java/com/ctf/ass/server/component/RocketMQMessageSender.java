package com.ctf.ass.server.component;

import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_codec.binary.SpBinaryMsg;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_public.struct.OpHistory;
import com.ctf.ass_public.struct.UserLoginHistory;
import com.ctf.ass_public.struct.UserReqUimHistory;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Date;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Charles
 * @create 2017/7/21 14:55
 */
@Component
@PropertySource(value = "classpath:/rocketmq.properties")
public class RocketMQMessageSender {
    private static final LogWrapper logger = LogWrapper.getLogger(RocketMQMessageSender.class);
    @Autowired
    private DefaultMQProducer defaultMQProducer;
    @Value("${rocketmq.topic.name}")
    private String topicName;
    @Value("${rocketmq.tag.TagOpHistory}")
    private String tagOpHistory;
    @Value("${rocketmq.tag.TagUserLoginHistory}")
    private String tagUserLoginHistory;
    @Value("${rocketmq.tag.TagUserReqUimHistory}")
    private String tagUserReqUimHistory;

    private boolean sendMessage(String tags, byte[] body) {
        if (defaultMQProducer == null) {
            return false;
        }
        if (body != null) {
            Message msg = new Message(topicName, tags, body);
            try {
                SendResult sendResult = defaultMQProducer.send(msg);
                logger.debug(sendResult.toString());
                return (sendResult.getSendStatus() == SendStatus.SEND_OK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean sendOpHistory(OpHistory opHistory) {
        if (opHistory != null) {
            try {
                return sendMessage(tagOpHistory, opHistory.toBytes());
            } catch (Exception e) {
                logger.warn("Error while encode&send OpHistory!" + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean sendUserLoginHistory(UserLoginHistory userLoginHistory) {
        if (userLoginHistory != null) {
            try {
                return sendMessage(tagUserLoginHistory, userLoginHistory.toBytes());
            } catch (Exception e) {
                logger.warn("Error while encode&send UserLoginHistory!" + e.getMessage());
                e.printStackTrace();
            }
        }

        return false;
    }

    public boolean sendUserReqUimHistory(UserReqUimHistory userReqUimHistory) {
        if (userReqUimHistory != null) {
            try {
                return sendMessage(tagUserReqUimHistory, userReqUimHistory.toBytes());
            } catch (Exception e) {
                logger.warn("Error while encode&send UserReqUimHistory!" + e.getMessage());
                e.printStackTrace();
            }
        }

        return false;
    }

    public boolean sendOpHistory(ChannelHandlerContext ctx, com.ctf.ass_codec.struct.Message message) {
        String ipAddr = ctx.channel().remoteAddress().toString();
        OpHistory.OpType type = null;
        String sCmdId = "Unknown";
        switch (message.getTag()) {
            case com.ctf.ass_codec.struct.Message.MtTag: {
                short cmdId = message.getCmdId();
                if (cmdId < MtProtoMsg.CMDID.END_TERM2SERVER_VALUE) {
                    type = OpHistory.OpType.MtToSvr_Req;
                } else if (cmdId < MtProtoMsg.CMDID.END_SERVER2TERM_VALUE) {
                    type = OpHistory.OpType.MtToSvr_Rsp;
                } else {
                    type = OpHistory.OpType.MtToSvr_Ind;
                }

                sCmdId = MtProtoMsg.CMDID.forNumber(cmdId).name();
            }
            break;

            case com.ctf.ass_codec.struct.Message.ToMtTag: {
                short cmdId = message.getCmdId();
                if (cmdId < MtProtoMsg.CMDID.END_TERM2SERVER_VALUE) {
                    type = OpHistory.OpType.SvrToMt_Rsp;
                } else /*if (cmdId < MtProtoMsg.CMDID.END_SERVER2TERM_VALUE)*/ {
                    type = OpHistory.OpType.SvrToMt_Req;
                }

                sCmdId = MtProtoMsg.CMDID.forNumber(cmdId).name();
            }
            break;

            case com.ctf.ass_codec.struct.Message.SpTag: {
                short cmdId = message.getCmdId();
                switch (SpBinaryMsg.CmdId.valueOf(cmdId)) {
                    case CMD_SP_LOGIN:
                    case CMD_SP_SIM_INFO:
                    case CMD_SP_SIM_INFO_UPDATE:
                    case CMD_SP_HEARTBEAT:
                    case CMD_SP_LOG_UPLOAD_RES:
                        type = OpHistory.OpType.SpToSvr_Req;
                        break;

                    case CMD_SP_SIM_IMG:
                    case CMD_SP_SIM_APDU:
                    case CMD_SP_LOG_UPLOAD:
                    case CMD_SP_TRACE:
                    case CMD_SP_UPGRADE:
                    case CMD_SP_REBOOT:
                        type = OpHistory.OpType.SpToSvr_Rsp;
                        break;
                }

                sCmdId = SpBinaryMsg.CmdId.valueOf(cmdId).name();
            }
            break;

            case com.ctf.ass_codec.struct.Message.ToSpTag: {
                short cmdId = message.getCmdId();
                switch (SpBinaryMsg.CmdId.valueOf(cmdId)) {
                    case CMD_SP_LOGIN:
                    case CMD_SP_SIM_INFO:
                    case CMD_SP_SIM_INFO_UPDATE:
                    case CMD_SP_HEARTBEAT:
                    case CMD_SP_LOG_UPLOAD_RES:
                        type = OpHistory.OpType.SvrToSp_Rsp;
                        break;

                    case CMD_SP_SIM_IMG:
                    case CMD_SP_SIM_APDU:
                    case CMD_SP_LOG_UPLOAD:
                    case CMD_SP_TRACE:
                    case CMD_SP_UPGRADE:
                    case CMD_SP_REBOOT:
                        type = OpHistory.OpType.SvrToSp_Req;
                        break;
                }

                sCmdId = SpBinaryMsg.CmdId.valueOf(cmdId).name();
            }
            break;
        }

        return sendOpHistory(new OpHistory(new Date(), ipAddr, type, sCmdId, message.getSeqNo(), 0, message.toString(), 0));
    }
}
