package com.ctf.autotest;

import com.ctf.ass_codec.binary.SpBinaryMsg.CmdId;
import com.ctf.ass_codec.sp_body.*;
import com.ctf.ass_codec.struct.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

class SpMsgHandler extends ChannelInboundHandlerAdapter {
    private static LogWrapper logger = LogWrapper.getLogger(SpMsgHandler.class.getName());

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CmdUtils.init(ctx);
        MockEnv.pushCtx(ctx);

        logger.debug(ctx, "channelActive");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {
        if (object instanceof Message) {
            Message message = (Message) object;
            CmdUtils cmdUtils = CmdUtils.get(ctx);
            MockSp mockSp = MockEnv.getMockSp(ctx);

            if (cmdUtils.isTermRsp(message)) {   //如果是对当前命令的回复消息，则取消命令重发。
                cmdUtils.cancelSendingCmd();
                cmdUtils.setLastTermRsp(message);

                procMsg(ctx, message);
            } else if (cmdUtils.isTermReq(message)) {  //收到请求消息

                //保存接收到的seq_no，以便回复时使用同样的seq_no
                CmdUtils.get(ctx).setLastTermReq(message);

                if (!mockSp.getIsAutoRsp()) {    //如果是忙状态，则不响应命令
                    logger.debug("Sp client isAutoRsp==False! Won't process recvived message:" + message);
                    return;
                }

                //SimPool不判断是否为重复消息，直接处理
                procMsg(ctx, message);
            } else {
                logger.warn(ctx, "[Sp] Unexpected message:" + message);
            }
        }
        else {
            logger.error(ctx, "Not a valid Message! fireChannelRead!!!");
            ctx.fireChannelRead(object);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        MockSp mockSp = MockEnv.getMockSp(ctx);

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent)evt;
            if (event.state() == IdleState.ALL_IDLE && mockSp.getIsAutoHeartBeat()) {
                logger.debug(MockSp.SP_CLIENT_HEARTBEAT_TIMEOUT_SECONDS  + " seconds heartbeat timeout!");
                mockSp.heartbeat(true, "");
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn(ctx, "SpMsgHandler channelInactive!");
        ctx.fireChannelInactive();

        MockSp mockSp = MockEnv.getMockSp(ctx);
        if (mockSp != null) {
            mockSp.onClose();
        } else {
            logger.debug("mockSp is null!");
        }

        CmdUtils.deInit(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn(ctx, "SpMsgHandler exceptionCaught!", cause.fillInStackTrace());

        ctx.close();
        //ctx.fireExceptionCaught(cause);
    }

        /*
    private static Map<String, Boolean> nodeCheck = new ConcurrentHashMap<String, Boolean>();
    private static String[] whiteList = { "127.0.0.1", "192.168.37.241", "192.168.37.156" };

    String nodeIndex = ctx.channel().remoteAddress().toString();
    Message login_ack = null;

    short msg_id = message.getHead().getTermReqSeqNo();
    // 重复登陆，拒绝
    if (nodeCheck.containsKey(nodeIndex)) {
        login_ack = buildSbLoginAck(msg_id, (byte) 1);
    } else {
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = address.getAddress().getHostAddress();
        boolean isOK = false;
        for (String WIP : whiteList) {
            if (WIP.equals(ip)) {
                isOK = true;
                break;
            }
        }

        SpLoginReq spLoginInd = (SpLoginReq)message.getBody();

        login_ack = buildSbLoginAck(msg_id, isOK ? (byte) 0 : (byte) 1);
        if (isOK) {
            nodeCheck.put(nodeIndex, true);
        }
    }

    ctx.writeAndFlush(login_ack);
    */

    //消息处理
    public void procMsg(ChannelHandlerContext ctx, Message message) {
        //打印log
        StringBuffer sb = new StringBuffer();
        sb.append("Server=>Sp ");
        sb.append(message);
        logger.debug(ctx, sb.toString());

        CmdId cmd_id = CmdId.valueOf(message.getCmdId());
        switch(cmd_id) {
            case CMD_SP_LOGIN:          //SimPool 登录回复
                procSpLoginRsp(ctx, message);
                break;

            case CMD_SP_SIM_INFO:       //SimPool SIM卡 信息 上传 回复
                procSpSimInfoRsp(ctx, message);
                break;

            case CMD_SP_SIM_INFO_UPDATE:    //处理SimPool SIM卡信息 更新 回复
                procSpSimInfoRsp(ctx, message);
                break;

            case CMD_SP_SIM_IMG:        //SimPool SIM卡镜像请求
                procSpSimImgReq(ctx, message);
                break;

            case CMD_SP_SIM_APDU:       //SimPool SIM卡APDU 请求
                procSpSimApduReq(ctx, message);
                break;

            case CMD_SP_HEARTBEAT:     //SimPool 心跳pong包
                procSpHeartBeatRsp(ctx, message);
                break;
            case CMD_SP_LOG_UPLOAD: //SimPool 上传日志
                procSpLogUploadReq(ctx, message);

            default:
                procSpBadMsg(ctx, message);
                break;
        }
    }

    //----------------------------------------------------------------------------
    //登录回复
    //------------------
    private void procSpLoginRsp(ChannelHandlerContext ctx, Message message) {
        SpLoginRsp spLoginRsp = (SpLoginRsp)message.getBody();

        if (spLoginRsp.getResult() == 0) {
            /*
            MockSp mockSp = MockEnv.getMockSp(ctx);
            mockSp.simInfo(null);
            */
        }
    }

    //----------------------------------------------------------------------------
    //SIM卡信息
    //------------------
    private void procSpSimInfoRsp(ChannelHandlerContext ctx, Message message) {
        SpSimInfoRsp simInfoRsp = (SpSimInfoRsp)message.getBody();

        if (simInfoRsp.getResult() == 0) {
            if (simInfoRsp.hasInvalidSimNos()) {
            }
        }
    }

    //----------------------------------------------------------------------------
    //SIM卡镜像
    //------------------
    private void procSpSimImgReq(ChannelHandlerContext ctx, Message message) {
        SpTermSimImgReq simImgReq = (SpTermSimImgReq)message.getBody();

        Message simImgRsp = buildSpSimImgRsp(message.getSeqNo(),
                (byte)0,
                simImgReq.getSimNo(),
                simImgReq.getImsi(),
                MockEnv.getMockSp(ctx).getMockSim(simImgReq.getSimNo()).getImgMd5());
        CmdUtils.get(ctx).sendRsp(simImgRsp);
    }

    //生成sim卡镜像回复
    public Message buildSpSimImgRsp(short seq_no, byte result, short sim_no, String imsi, byte[] sim_image) {
        return new Message(Message.SpTag,
                CmdId.CMD_SP_SIM_IMG.value(),
                seq_no,
                (byte)0,
                new SpTermSimImgRsp(result, sim_no, imsi, sim_image));
    }

    //----------------------------------------------------------------------------
    //处理SimBank SIM卡APDU Rsp
    //------------------
    private void procSpSimApduReq(ChannelHandlerContext ctx, Message message) {
        SpTermSimApduReq simApduReq = (SpTermSimApduReq)message.getBody();

        //模拟回复simNo,imsi,<Apdu Data>
        byte[] apdu_data = simApduReq.getApduReq();
        String info = String.format("%d,%s,", simApduReq.getSimNo(), simApduReq.getImsi());
        byte[] info_bytes = info.getBytes();
        byte[] rsp_bytes = new byte[info_bytes.length + apdu_data.length];
        System.arraycopy(info_bytes, 0, rsp_bytes, 0, info_bytes.length);
        System.arraycopy(apdu_data, 0, rsp_bytes, info_bytes.length, apdu_data.length);

        Message apdu_rsp = buildSpSimApduRsp(message.getSeqNo(), simApduReq.getSimNo(), simApduReq.getImsi(), apdu_data);
        CmdUtils.get(ctx).sendRsp(apdu_rsp);
    }

    public static Message buildSpSimApduRsp(short seq_no, short sim_no, String imsi, byte[] apdu_cmd) {
        byte result = 0;
        SpTermSimApduRsp simApduRsp = new SpTermSimApduRsp(result, sim_no, imsi, apdu_cmd);
        return new Message(Message.SpTag,
                CmdId.CMD_SP_SIM_APDU.value(),
                seq_no,
                (byte)0,
                simApduRsp);
    }
    //----------------------------------------------------------------------------
    //处理SimBank 心跳Pong包
    //------------------
    private void procSpHeartBeatRsp(ChannelHandlerContext ctx, Message message) {

    }

    //----------------------------------------------------------------------------
    //处理SimBank 错误消息
    //-----------------
    private void procSpBadMsg(ChannelHandlerContext ctx, Message message) {
        logger.error(ctx, "Simbank bad msg found:" + message);
    }

    private void procSpLogUploadReq(ChannelHandlerContext ctx, Message message){
        SpTermLogUploadRsp spTermLogUploadRsp = new SpTermLogUploadRsp((byte) 0);
        Message log_updaload_rsp = new Message(Message.SpTag,
                CmdId.CMD_SP_LOG_UPLOAD.value(),
                message.getSeqNo(),
                (byte)0,
                spTermLogUploadRsp);
        CmdUtils.get(ctx).sendRsp(log_updaload_rsp);
    }

}
