package com.ctf.ass.server.netty.handler;

import com.cootf.cloudsim.oms.service.api.entities.ExtractionRecord;
import com.ctf.ass.server.component.MongoDbUtil;
import com.ctf.ass.server.component.ToMtMsg;
import com.ctf.ass.server.component.ToSpMsg;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.globals.LogsState;
import com.ctf.ass_public.struct.OnLineUser;
import com.ctf.ass_public.struct.SimPoolLog;
import com.ctf.oss.CRMService;
import com.ctf.oss.SmcService;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Component("httpMsgHandler")
@Scope("prototype")
@ChannelHandler.Sharable
public class HttpMsgHandler extends ChannelInboundHandlerAdapter {
    private static LogWrapper logger = LogWrapper.getLogger(HttpMsgHandler.class);
    @Autowired
    private CRMService crmService;
    @Autowired
    private SmcService smcService;
    @Autowired
    private ToMtMsg toMtMsg;
    @Autowired
    private ToSpMsg toSpMsg;
    @Autowired
    private MongoDbUtil mongoDbUtil;

    private ByteBufToBytes reader = null;
    private String url = null;
    private String body = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void channelRead(ChannelHandlerContext httpCtx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            url = request.uri();
            logger.debug(httpCtx, "uri:" + url);
            reader = null;
            if (request.method().equals(HttpMethod.POST)) {
                if (HttpUtil.isContentLengthSet(request)) {
                    reader = new ByteBufToBytes((int) HttpUtil.getContentLength(request));
                }
            }
        }

        if (reader != null && msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            reader.reading(content);
            content.release();

            if (reader.isEnd()) {
                ErrCode.ServerCode errCode = ErrCode.ServerCode.ERR_ILLEGAL_PARAM;
                body = URLDecoder.decode(new String(reader.readFull()).trim(), "utf-8");
                logger.debug(httpCtx, "body:" + body);

                switch (url) {
                    case "/user_binduim":   //绑卡
                    {
                        String userId = getParameterByName( "userId");
                        String imsi = getParameterByName( "imsi");
                        if (StringUtils.isNotEmpty(userId) && StringUtils.isNotEmpty(imsi)) {
                            errCode = crmService.userBindUim(imsi, userId);
                        }
                        sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                    }
                    break;

                    case "/user_unbinduim":   //解绑
                    {
                        String imsi = getParameterByName( "imsi");
                        if (StringUtils.isNotEmpty(imsi)) {
                            errCode = crmService.userBindUim(imsi, null);
                        }
                        sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                    }
                    break;

                    case "/user_assignuim":  //主动分卡(TODO:终端暂不支持该协议)
                    {
                        String userId = getParameterByName( "userId");
                        String imsi = getParameterByName( "imsi");
                        if (StringUtils.isNotEmpty(userId) && StringUtils.isNotEmpty(imsi)) {
                            errCode = toMtMsg.sendHttpMtTermAssignUimReq(httpCtx, userId, imsi);
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    break;

                    case "/user_disable":   //禁用用户
                    {
                        String userId = getParameterByName( "userId");
                        String disable = getParameterByName( "disable");
                        if (StringUtils.isNotEmpty(userId) && StringUtils.isNotEmpty(disable)) {
                            errCode = crmService.userDisable(userId, "true".equals(disable));
                        }
                        sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                    }
                    break;

                    case "/user_revuim":     //还卡
                    {
                        String userId = getParameterByName( "userId");

                        String imsi = getParameterByName( "imsi");
                        if (StringUtils.isEmpty(userId) && StringUtils.isNotEmpty(imsi)) {
                            OnLineUser onLineUser = mongoDbUtil.getOnLineUserByImsi(imsi);
                            if(onLineUser != null){
                                userId = onLineUser.getUserId();
                            }
                        }
                        if (StringUtils.isNotEmpty(userId)){
                            //发送Http命令给终端
                            errCode = toMtMsg.sendHttpMtTermRevUimReq(httpCtx, userId, ErrCode.ServerCode.RUR_USER_VIP_SNATCH.value());
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    break;

                    case "/user_logout": //用户退录
                    {
                        String userId = getParameterByName( "userId");
                        if (StringUtils.isNotEmpty(userId)){
                            //发送Http命令给终端
                            errCode = toMtMsg.sendHttpMtTermLogoutReq(httpCtx, userId, ErrCode.ServerCode.ULR_USER_KICKED.value());
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    break;

                    case "/user_trace":    //实时trace
                    {
                        String userId = getParameterByName("userId");
                        String level = getParameterByName("level");
                        if (StringUtils.isNotEmpty(userId)) {
                            MtProtoMsg.LEVEL value = null;
                            try {
                                if (level != null) {
                                    value = MtProtoMsg.LEVEL.valueOf(level);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                            }
                            errCode = toMtMsg.sendHttpMtTermTraceReq(httpCtx, userId, value);
                            if (errCode != ErrCode.ServerCode.ERR_OK) {
                                sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                            }
                        }
                        sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                    }
                    break;

                    case "/user_logupload":  //日志上传
                    {
                        String userId = null;
                        long s_second = 0;
                        long e_second = 0;
                        MtProtoMsg.LEVEL level = null;
                        String sStartTime = getParameterByName("startTime");
                        String sEndTime = getParameterByName("endTime");
                        long log_size_limit = 0;
                        try{
                            userId = getParameterByName("userId");
                            String sLevel = getParameterByName("level");
                            String sLogSizeLimit = getParameterByName("logSizeLimit");
                            s_second = new Date(sStartTime).getTime();

                            e_second = new Date(sEndTime).getTime();
                            level = MtProtoMsg.LEVEL.forNumber(Integer.parseInt(sLevel));
                            log_size_limit = Integer.parseInt(sLogSizeLimit);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                        if (StringUtils.isNotEmpty(userId) && level != null && s_second > 0 && e_second > 0 && log_size_limit > 0){
                            OnLineUser user = mongoDbUtil.getOnLineUserByUserId(userId);
                            if(user != null){
                                //初始化数据库数据
                                ExtractionRecord record = mongoDbUtil.dbMtUserInsertLogData(user, s_second, e_second, level);
                                //发送Http命令给终端
                                errCode = toMtMsg.sendHttpMtTermLogUploadReq(httpCtx, userId, s_second, e_second, level, record.getId()+".log", log_size_limit);
                            }else{
                                errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                            }
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    break;

                    case "/sim_disable":    //禁用sim卡
                    {
                        String imsi = getParameterByName("imsi");
                        String disable = getParameterByName("disable");
                        if (StringUtils.isNotEmpty(imsi) && StringUtils.isNotEmpty(disable)) {
                            errCode = smcService.disableSim(imsi, disable.equals("true"));
                        }
                        sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                    }
                    break;

                    case "/simpool_disable":    //禁用simpool卡池
                    {
                        String macAddress = getParameterByName("macAddress");
                        String disable = getParameterByName("disable");
                        if (StringUtils.isNotEmpty(macAddress) && StringUtils.isNotEmpty(disable)) {
                            errCode = smcService.disableSimpool(macAddress, disable.equals("false"));
                        }
                        sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                    }
                    break;

                    case "/simpool_logupload":    //simpool日志上传
                    {
                        //获取毫秒数
                        String macAddress = getParameterByName("macAddress");
                        String dayTime = getParameterByName("dayTime");
                        long day_second = sdf.parse(dayTime).getTime();
                        if (StringUtils.isNotEmpty(macAddress)) {
                            //初始化记录
                            SimPoolLog simPoolLog = mongoDbUtil.dbSpInsertLogData(macAddress,LogsState.INIT.value());
                            errCode = toSpMsg.sendHttpSpLogUploadReq(httpCtx,macAddress,simPoolLog.geId()+".log", day_second);
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    case "/simpool_upgrade":    //simpool软件升级
                    {
                        String macAddress = getParameterByName("macAddress");
                        String url = getParameterByName("url");
                        if (StringUtils.isNotEmpty(macAddress) && StringUtils.isNotEmpty(url)) {
                            errCode = toSpMsg.sendHttpSpUpgradeReq(httpCtx,macAddress,url);
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    break;

                    case "/simpool_trace":    //simpool 控制trace开关
                    {
                        String macAddress = getParameterByName("macAddress");
                        String on_off = getParameterByName("on_off");
                        if (StringUtils.isNotEmpty(macAddress) && StringUtils.isNotEmpty(on_off)) {
                            errCode = toSpMsg.sendHttpSpTermTraceReq(httpCtx, macAddress, on_off.equals("true"));
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    break;

                    case "/simpool_reboot":    //simpool 重新启动
                    {
                        String macAddress = getParameterByName("macAddress");
                        String cause = getParameterByName("cause");
                        if (StringUtils.isNotEmpty(macAddress) && StringUtils.isNotEmpty(cause)) {
                            int cause_int = Integer.parseInt(cause);
                            errCode = toSpMsg.sendHttpSpTermRebootReq(httpCtx, macAddress, (byte)cause_int);
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    break;

                    case "/simpool_clearLogs":    //simpool 清理日志
                    {
                        String macAddress = getParameterByName("macAddress");
                        String fileName = getParameterByName("fileName");
                        if (StringUtils.isNotEmpty(macAddress) && StringUtils.isNotEmpty(fileName)) {
                            ArrayList<String> fileNameList = new ArrayList(Arrays.asList(fileName.split(",")));
                            errCode = toSpMsg.sendHttpSpTermClearLogsReq(httpCtx, macAddress, fileNameList);
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    break;

                    case "/simpool_getLogs":    //simpool 获取日志列表
                    {
                        String macAddress = getParameterByName("macAddress");
                        if (StringUtils.isNotEmpty(macAddress)) {
                            errCode = toSpMsg.sendHttpSpTermGetLogsReq(httpCtx, macAddress);
                        }
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        }
                    }
                    break;

                    default:
                        errCode = ErrCode.ServerCode.ERR_PROTO_NOT_COMPATIABLE;
                        sendHttpRsp(httpCtx, errCode.value(), errCode.cnErrMsg());
                        break;
                }
            }
        }
    }

    //获取参数
    private String getParameterByName(String name) {
        if(body != null){
            QueryStringDecoder decoderQuery = new QueryStringDecoder("some?" + body);
            Map<String, List<String>> uriAttributes = decoderQuery.parameters();
            for (Map.Entry<String, List<String>> attr : uriAttributes.entrySet()) {
                String key = attr.getKey();
                for (String attrVal : attr.getValue()) {
                    if (key.equals(name)) {
                        return attrVal;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    public static void sendHttpRsp(ChannelHandlerContext ctx, int errCode, String msg) {
        String content = String.format("{\"code\":%d, \"msg\":\"%s\"}", errCode, msg);
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        response.headers().set(HttpHeaderNames.ACCEPT_CHARSET, "UTF-8");
        response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "POST");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.write(response);
        ctx.flush();
    }

    class ByteBufToBytes {
        private ByteBuf temp;

        private boolean end = true;

        public ByteBufToBytes(int length) {
            temp = Unpooled.buffer(length);
        }

        public void reading(ByteBuf datas) {
            datas.readBytes(temp, datas.readableBytes());
            if (this.temp.writableBytes() != 0) {
                end = false;
            } else {
                end = true;
            }
        }

        public boolean isEnd() {
            return end;
        }

        public byte[] readFull() {
            if (end) {
                byte[] contentByte = new byte[this.temp.readableBytes()];
                this.temp.readBytes(contentByte);
                this.temp.release();
                return contentByte;
            } else {
                return null;
            }
        }

        public byte[] read(ByteBuf datas) {
            byte[] bytes = new byte[datas.readableBytes()];
            datas.readBytes(bytes);
            return bytes;
        }
    }

}
