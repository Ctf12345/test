package com.ctf.oss;

import com.ctf.ass.server.component.MongoDbUtil;
import com.ctf.ass.server.component.ToMtMsg;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.globals.ErrCode.ServerCode;
import com.ctf.ass_codec.sp_body.SpSimInfo;
import com.ctf.ass_codec.sp_body.SimInfosResult;
import com.ctf.ass_public.struct.OnLineUser;
import com.ctf.ass_public.struct.SimCard;

import io.netty.channel.ChannelHandlerContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;

//Simcard Management Center(Sim卡管理中心)
@Service
public final class SmcService {
    private static LogWrapper logger = LogWrapper.getLogger(SmcService.class);
    @Autowired
    private MongoDbUtil mongoDbUtil;
    @Autowired
    private ToMtMsg toMtMsg;

    //=====================================================================================================
    //更新SimCard的信息
    public SimInfosResult SpUploadSimInfos(ChannelHandlerContext ctx, ArrayList<SpSimInfo> simInfos, boolean isUpload) {
        String macAddress = mongoDbUtil.getOnLineSimPoolId(ctx.channel().remoteAddress().toString());
        SimInfosResult result = new SimInfosResult(ServerCode.ERR_OK);

        if (macAddress != null) {
            //初始化simi_list值，为后面作比较，清除其他的卡
            ArrayList<String> simi_list = new ArrayList<String>();
            if (simInfos != null && simInfos.size() > 0) {
                for (SpSimInfo simInfo : simInfos) {
                    //根据imsi获取SimCard设备
                    SimCard simCard = mongoDbUtil.getDbSimCard(simInfo.getImsi());
                    if (simCard == null) {
                        //TODO: 通知运维，SimBank上有未知的卡。在运维将卡相关信息录入到数据库前，该卡无法使用
                        logger.debug("Unknown Simcard! imsi:" + simInfo.getImsi());
                        result.addInvalidSim(simInfo);
                    } else {
                        simi_list.add(simInfo.getImsi());
                        boolean isBroken = false;

                        if (!simCard.getIccid().equals(simInfo.getIccid())) {
                            //TODO: 检查iccid，如果为非法，则将isBroken设为true，并记录到运维，iccid有变化
                            logger.warn("SimCard iccid mismatch! old:" + simCard.getIccid() + ", will update to:" + simInfo.getIccid());
                        }
                        //检查所在SimPool是否有变化
                        if (simCard.getSimPoolMacAddr() == null ||                                   //数据库中没有该simCard所在simPool信息
                                !simCard.getSimPoolMacAddr().equals(macAddress) ||    //数据库中的simCard所在simPool编号与上报的不一致
                                simCard.getLocationInSimPool() != simInfo.getLocationInSimPool()) {  //数据库中的simCard的locationInSimPool与上报的不一致
                            logger.warn("SimCard location changed! expected:" + simCard.getLocationInSimPool() + ", got:" + simInfo.getLocationInSimPool());
                            //TODO: 记录到运维，SIM卡位置有变化
                        }
                        //检查sim卡是否已坏
                        try{
                            if(Long.parseLong(simCard.getImsi()) == 0){
                                //TODO: 记录到运维，SIM卡已坏/已修复
                                isBroken = true;
                                logger.debug("SimCard is broken! imsi:" + simCard.getImsi());
                            }
                        }catch(Throwable e){}
                        //检查md5是否变化
                        byte[] img_md5_info = simInfo.getImg_md5();
                        if (simCard.getImgMd5() == null || !Arrays.equals(simCard.getImgMd5(), img_md5_info) || simCard.getSimcardImage() == null) {
                            result.addReqImgSim(simInfo);
                            //保存更新到数据库
                            result.setResult(mongoDbUtil.dbSpSimInfo(simInfo.getImsi(),
                                    simInfo.getIccid(),
                                    macAddress,
                                    simInfo.getLocationInSimPool(),
                                    simInfo.getIsInSimpool(),
                                    isBroken,
                                    simInfo.getImg_md5()));
                        } else {
                            //TODO：分多种情况更新数据库
                            //保存更新到数据库
                            result.setResult(mongoDbUtil.dbSpSimInfo(simInfo.getImsi(),
                                    simInfo.getIccid(),
                                    macAddress,
                                    simInfo.getLocationInSimPool(),
                                    simInfo.getIsInSimpool(),
                                    isBroken,
                                    null));
                        }
                    }
                }

                //更新totalUsed为实际有效的sim总数
                if (isUpload) {
                    mongoDbUtil.dbSpTotalUsed(macAddress, simInfos.size() - result.getInvalidSims().size());
                    //清除sp没有上报的其他的卡
                    recoverSimCards(macAddress,simi_list);
                }
            } else {
                if (isUpload) {
                    mongoDbUtil.dbSpTotalUsed(macAddress, 0);
                    //清除sp没有上报的其他的卡
                    recoverSimCards(macAddress,simi_list);
                }
            }
        } else {
            result.setResult(ServerCode.ERR_USER_NOT_LOGIN);
        }
        return result;
    }

    //清除sp没有上报的其他的卡
    public void recoverSimCards(String macAddress,ArrayList<String> imsi_list){
        if (imsi_list == null)
        {
            imsi_list = new ArrayList<String>();
        }
        for (SimCard card : mongoDbUtil.getDbSimCardsOnSimPool(macAddress)) {
            if (!imsi_list.contains(card.getImsi())){
                //已分配的simCard
                if (card.isInUsed()) {
                    OnLineUser user = mongoDbUtil.getOnLineUserByImsi(card.getImsi());
                    if (user != null) {
                        toMtMsg.sendMtTermRevUimReq(CCM.getCtx(CCM.ClientType.MT, user.getIpAddr()), ErrCode.ServerCode.RUR_SIM_NOT_AVAILABLE.value(), 0);
                    }
                }
                //更新simCard状态为不在位
                if (card.isInSimpool()){
                    mongoDbUtil.dbSimCardOffline(card.getImsi());
                }
            }
        }
    }

    //更新sim卡镜像
    public ServerCode SpUpdateImage(String imsi, byte[] image) {
        assert image != null && image.length > 0;

        SimCard simCard = mongoDbUtil.getDbSimCard(imsi);
        if (simCard == null) {
            return ServerCode.ERR_ILLEGAL_PARAM;
        }

        return mongoDbUtil.dbSpSimImg(imsi, image);
    }

    //SimBank退出登录
    public ServerCode SpCloseConn(ChannelHandlerContext ctx) {
        String ipAddr = ctx.channel().remoteAddress().toString();
        String simPoolId = mongoDbUtil.getOnLineSimPoolId(ipAddr);
        if (simPoolId == null) {
            logger.warn(ctx, "sp is NULL!!!");
            return ServerCode.ERR_USER_NOT_LOGIN;
        }

        //处理SimBank上所有的SimCard
        for (SimCard simCard : mongoDbUtil.getDbSimCardsOnSimPool(simPoolId)) {
            /*
            if (simCard.getIsInUsed()) {    //已分配的simCard
                OnLineUser session = mongoDbUtil.getOnLineUserByImsi(simCard.getImsi());
                if (session != null) {
                    //强行踢掉用户
                    MtMsgHandler.sendMtTermRevUimReq(getOnLineManager().getOnLineUserCtx(session.getUserId()), ServerCode.RUR_SIM_NOT_AVAILABLE.value(), 1);
                }
            }
            */
            //更新simCard状态为不在位
            mongoDbUtil.dbSimCardOffline(simCard.getImsi());
        }
        //simPool下线
        logger.debug(ctx, "#######################");
        logger.debug(ctx, "#  SimPool offline!!! #");
        logger.debug(ctx, "#######################");

        return mongoDbUtil.dbSpLogout(simPoolId,ipAddr);
    }

    public ServerCode disableSim(String imsi, boolean bDisable) {
        ServerCode ret;
        if (StringUtils.isNotEmpty(imsi)) {
            SimCard simCard = mongoDbUtil.getDbSimCard(imsi);

            if (simCard != null) {
                OnLineUser onLineUser = mongoDbUtil.getOnLineUserByImsi(imsi);
                if (onLineUser != null) {
                    toMtMsg.sendMtTermRevUimReq(CCM.getCtx(CCM.ClientType.MT, onLineUser.getIpAddr()), ServerCode.RUR_SIM_NOT_AVAILABLE.value(), 0);
                }
                ret = mongoDbUtil.dbSimCardDisable(imsi, bDisable);
            } else {
                ret = ServerCode.ERR_USER_NOT_EXIST;
            }
        } else {
            ret = ServerCode.ERR_ILLEGAL_PARAM;
        }
        return ret;
    }

    public ServerCode disableSimpool(String macAddress, boolean bDisable) {
        ServerCode ret;
        if (StringUtils.isNotEmpty(macAddress)) {
            //处理Simpool上所有的SimCard
            for (SimCard simCard : mongoDbUtil.getDbSimCardsOnSimPool(macAddress)) {
                //已分配的simCard
                if (simCard.isInUsed()) {
                    OnLineUser onLineUser = mongoDbUtil.getOnLineUserByImsi(simCard.getImsi());
                    if (onLineUser != null) {
                        toMtMsg.sendMtTermRevUimReq(CCM.getCtx(CCM.ClientType.MT, onLineUser.getIpAddr()), ServerCode.RUR_SIM_NOT_AVAILABLE.value(), 0);
                    }
                }
                //更新simCard状态为不在位
                mongoDbUtil.dbSimCardOffline(simCard.getImsi());
            }
            //禁用SimPool
            ret = mongoDbUtil.dbSimPoolDisable(macAddress, bDisable);
        } else {
            ret = ServerCode.ERR_ILLEGAL_PARAM;
        }
        return ret;
    }
}