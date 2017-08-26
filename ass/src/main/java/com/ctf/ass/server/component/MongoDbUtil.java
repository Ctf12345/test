package com.ctf.ass.server.component;

import com.alibaba.boot.dubbo.annotation.DubboConsumer;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.cootf.cloudsim.oms.service.api.dto.PagingData;
import com.cootf.cloudsim.oms.service.api.dto.RpcResult;
import com.cootf.cloudsim.oms.service.api.entities.ExtractionRecord;
import com.cootf.cloudsim.oms.service.api.entities.SearchOption;
import com.cootf.cloudsim.oms.service.api.service.*;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_public.globals.ClientState;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.globals.LogsState;
import com.ctf.ass_public.struct.*;

import com.ctf.ass_public.utils.CheckUtils;
import com.ctf.ass_public.utils.ConvUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author Charles
 * @create 2017/7/21 15:06
 */
@Component
public class MongoDbUtil {
    private static LogWrapper logger = LogWrapper.getLogger(MongoDbUtil.class);
    private static final String DB_Simcards = "SimcardsDB";
    private static final String Tbl_simcards = "simcards";
    private static final String Tbl_simpools = "simpools";

    private static final String DB_CRM = "CRMDB";
    private static final String Tbl_clients = "clients";

    private static final String DB_OSS = "OSS";
    private static final String Tbl_DeviceLogs = "DeviceLogs";
    private static final String Tbl_OnLineUsers = "OnLineUsers";
    private static final String Tbl_OnLineSimPools = "OnLineSimPools";
    private static final String Tbl_LogFileFromDevice = "ExtractionRecords";
    private static final String Tbl_UserLoginHistory = "UserLoginHistory";
    private static final String Tbl_UserReqUimHistory = "UserReqUimHistory";
    private static final String Tbl_OpHistory = "OpHistory";
    private static final String Tbl_BlackUsers = "BlackUsers";

    @DubboConsumer
    private MtUserService mtUserService;
    @DubboConsumer
    private OnLineUsersService onLineUsersService;
    @DubboConsumer
    private SimCardService simCardService;
    @DubboConsumer
    private SimPoolService simPoolService;
    @DubboConsumer
    private OnLineSimPoolService onLineSimPoolService;
    @DubboConsumer
    private BlackUserService blackUserService;
    @DubboConsumer
    private ExtractionRecordService extractionRecordService;
    @DubboConsumer
    private SimPoolLogService simPoolLogService;
    @DubboConsumer
    private SimPoolUpgradeService simPoolUpgradeService;

    //获取MtUser
    public MtUser getDbUser(String userId) {
        RpcResult<MtUser> rpcResult = mtUserService.getMtUserByUserId(userId);
        if (rpcResult.isSuccess() && rpcResult.getData() != null) {
            return rpcResult.getData();
        }
        return null;
    }

    //检查并清除过期离线用户
    public ErrCode.ServerCode checkAndDelExpiredOffLineUsers() {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<List<String>> rpcResult = onLineUsersService.listExpiredOffLineUsers(new Date());
        if (rpcResult.isSuccess() && rpcResult.getData() != null && rpcResult.getData().size() > 0) {
            onLineUsersService.batchDeleteOnlineUser(rpcResult.getData());
            simCardService.batchUpdateSimCardUsedByImsiList(rpcResult.getData(), false);
        }
        return ret;
    }

    //移除在线用户
    public ErrCode.ServerCode removeOnLineUser(String userId) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<Long> rpcResult = onLineUsersService.removeOnlineUserByUserId(userId);
        if (!rpcResult.isSuccess() || rpcResult.getData() == null || rpcResult.getData() != 1) {
            ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        }
        return ret;
    }

    //添加在线用户
    public ErrCode.ServerCode addOnLineUser(String userId, String deviceId, String ipAddr) {
        MtUser mtUser = getDbUser(userId);
        String userName = userId;
        if (mtUser != null){
            userName = mtUser.getUserName();
        }
        OnLineUser onLineUser = new OnLineUser();
        onLineUser.setUserName(userName);
        onLineUser.setUserId(userId);
        onLineUser.setDeviceId(deviceId);
        onLineUser.setImsi("");
        onLineUser.setState(ClientState.CLIENT_STATE_LOGINED.name());
        onLineUser.setIpAddr(ipAddr);
        onLineUser.setUserLoginTime(new Date());
        onLineUsersService.insert(onLineUser);
        return ErrCode.ServerCode.ERR_OK;
    }

    //根据sessionId获取在线用户
    public OnLineUser getOnLineUser(byte[] sessionId) {
        RpcResult<OnLineUser> rpcResult = onLineUsersService.findOneBySessionId(sessionId);
        if (rpcResult.isSuccess() && rpcResult.getData() != null) {
            return rpcResult.getData();
        }
        return null;
    }

    //根据userId获取在线用户
    public OnLineUser getOnLineUserByUserId(String userId) {
        RpcResult<OnLineUser> rpcResult = onLineUsersService.findOneByUserId(userId);
        if (rpcResult.isSuccess() && rpcResult.getData() != null) {
            return rpcResult.getData();
        }
        return null;
    }

    //根据ipAddr获取在线用户
    public OnLineUser getOnLineUserByIpAddr(String ipAddr) {
        RpcResult<OnLineUser> rpcResult = onLineUsersService.findOneByIpAddr(ipAddr);
        if (rpcResult.isSuccess() && rpcResult.getData() != null) {
            return rpcResult.getData();
        }
        return null;
    }

    //根据imsi获取在线用户
    public OnLineUser getOnLineUserByImsi(String imsi) {
        RpcResult<OnLineUser> rpcResult = onLineUsersService.findOneByImsi(imsi);
        if (rpcResult.isSuccess() && rpcResult.getData() != null) {
            return rpcResult.getData();
        }
        return null;
    }

    /*
     1.根据userId获取MtUser，如为null，说明用户不存在，M网用户必须是以空格开始的
     2.判断用户是否已被禁用
     3.最后检查用户密码是否匹配
     */
    public ErrCode.ServerCode userAuth(String userId, String psdMd5) {
        ErrCode.ServerCode ret;
        //如果是M网的用户直接鉴权成功
        if (userId.startsWith(" ")){
            if(psdMd5!=null && psdMd5.equals(ConvUtils.bytesToHexStr(CheckUtils.MD5(userId.trim())))){
                logger.debug("M net User Auth userId:" + userId);
                return ErrCode.ServerCode.ERR_OK;
            }else{
                return ErrCode.ServerCode.ERR_USER_WRONG_PASSWORD;
            }
        }
        //登录鉴权
        MtUser mtUser = getDbUser(userId);
        if (mtUser != null) {
            if (!mtUser.isDisabled()) {
                if (mtUser.getPsdMD5().equals(psdMd5)) {
                    ret = ErrCode.ServerCode.ERR_OK;
                } else {
                    ret = ErrCode.ServerCode.ERR_USER_WRONG_PASSWORD;
                }
            } else {
                ret = ErrCode.ServerCode.ERR_USER_DISABLED;
            }
        } else {
            ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
        }

        return ret;
    }

    // 终端用户通过sessionId登录
    public ErrCode.ServerCode userLoginById(byte[] sessionId, String ipAddr) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        RpcResult<ErrCode.ServerCode> rpcResult = onLineUsersService.userLoginById(sessionId, ipAddr);
        if (rpcResult.isSuccess() && rpcResult.getData() != null) {
            ret = rpcResult.getData();
        }
        return ret;
    }

    // 终端用户更新信息
    // TODO: 记录终端信息
    public ErrCode.ServerCode dbMtUserUpdateInfo(String serialno, String model, String sw_ver, String buildno) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        return ret;
    }

    // 终端用户退出登录
    public ErrCode.ServerCode userLogout(String userId) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<Long> rpcResult = onLineUsersService.removeOnlineUserByUserId(userId);
        long updateCount = 0l;
        if (rpcResult.isSuccess() && rpcResult.getData() != null) {
            updateCount = rpcResult.getData();
        }
        if (updateCount != 1) {
            ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        }
        return ret;
    }

    //终端还卡
    public ErrCode.ServerCode userRevUim(String userId) {
        ErrCode.ServerCode ret;
        OnLineUser onLineUser = getOnLineUserByUserId(userId);
        if (onLineUser != null) {
            String imsi = onLineUser.getImsi();
            if (imsi != null) {
                dbSimSetUsed(imsi, false);
                ret = dbMtAssignSim(userId, "");
            } else {
                ret = ErrCode.ServerCode.ERR_USER_NOT_REQUIM;
            }
        } else {
            ret = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }

        return ret;
    }

    // 终端用户断开连接
    public ErrCode.ServerCode dbMtUserCloseConn(String userId) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<OnLineUser> rpcResult = onLineUsersService.findOneByUserId(userId);
        OnLineUser onLineUser = rpcResult.handResult();
        if (onLineUser != null) {
            String imsi = onLineUser.getImsi();
            if (!StringUtils.isEmpty(imsi)) {
                onLineUser.setState(ClientState.CLIENT_STATE_OFFLINE.name());
                onLineUser.setUserOfflineTime(new Date());
                RpcResult<Long> updateResult = onLineUsersService.update(onLineUser);
                Long updateCount = updateResult.handResult();
                if (updateCount == null || updateCount != 1) {
                    ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
                }
            } else {
                RpcResult<Long> removeResult = onLineUsersService.removeOnlineUserByUserId(userId);
                Long removeCount = removeResult.handResult();
                if (removeCount == null || removeCount != 1) {
                    ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
                }
            }
        } else {
            ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
        }
        return ret;
    }

    // =========================================================simpool (SimcardsDB->simpools)
    //获取SimPool
    public SimPool getDbSimPool(String macAddress) {
        RpcResult<SimPool> rpcResult = simPoolService.getSimPoolByMacAddress(macAddress);
        if (rpcResult != null && rpcResult.handResult() != null){
            return rpcResult.handResult();
        }
        return null;
    }

    // Simpool登录
    public ErrCode.ServerCode dbSpLogin(String macAddress,int capacity, String hardwareVersion, String softwareVersion,String ipAddr) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<SimPool> rpcResult = simPoolService.findOneById(macAddress);
        if (rpcResult != null && rpcResult.handResult() != null){
            SimPool simPool = rpcResult.handResult();
            if (simPool.isEnabled()) {//检查isEnabled
                simPool.setOnLine(true);
                if (simPool.getCapacity() != capacity) {
                    simPool.setCapacity(capacity);
                }
                String newHardwareVersion = "V" + hardwareVersion;
                if (!simPool.getHardwareVersion().equals(newHardwareVersion)) {
                    simPool.setHardwareVersion(newHardwareVersion);
                }
                String newSoftwareVersion = "V" + softwareVersion;
                if (!simPool.getSoftwareVersion().equals(newSoftwareVersion)) {
                    simPool.setSoftwareVersion(newSoftwareVersion);
                }
                RpcResult<Long> updateResult = simPoolService.update(simPool);

                //sp在线
                RpcResult<OnLineSimPool> onLineSimPoolRpcResult = onLineSimPoolService.findOneById(macAddress);
                if (onLineSimPoolRpcResult == null || onLineSimPoolRpcResult.handResult() == null){
                    OnLineSimPool onLineSimPool = new OnLineSimPool();
                    onLineSimPool.setMacAddress(simPool.getMacAddress());
                    onLineSimPool.setIpAddr(ipAddr);
                    onLineSimPool.setLoginTime(new Date());
                    onLineSimPoolService.insert(onLineSimPool);
                }
            } else {
                ret = ErrCode.ServerCode.ERR_USER_DISABLED;
            }
        }else {
            ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
        }
        return ret;
    }

    // Sim卡信息
    public ErrCode.ServerCode dbSpTotalUsed(String macAddress, int totalUsed) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<SimPool> rpcResult = simPoolService.findOneById(macAddress);
        if (rpcResult != null && rpcResult.handResult() != null){
            RpcResult<Long> updateResult = simPoolService.updateSimPoolTotalUsedByMacAddress(macAddress, totalUsed);
            Long updateCount = updateResult.handResult();
            if (updateCount == null || updateCount != 1) {
                ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
            }
        }
        return ret;
    }

    // Sim卡镜像
    public ErrCode.ServerCode dbSpSimImg(String imsi, byte[] image) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<SimCard> rpcResult = simCardService.findOneById(imsi);
        if (rpcResult != null && rpcResult.handResult() != null) {
            RpcResult<Long> updateResult = simCardService.setSimcardImageMd5(imsi, image);
            Long updateCount = updateResult.handResult();
            if (updateCount != null && updateCount == 1) {
                ret = ErrCode.ServerCode.ERR_OK;
            } else {
                ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
            }
        } else {
            ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
        }
        return ret;
    }

    // SimBank退出登录
    public ErrCode.ServerCode dbSpLogout(String macAddress,String ipAddr) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;

        RpcResult<SimPool> rpcResult = simPoolService.findOneById(macAddress);
        if (rpcResult != null && rpcResult.handResult() != null) {
            RpcResult<Long> updateResult = simPoolService.updateSimPoolIsOnLineByMacAddress(macAddress, false);
            OnLineSimPool onLineSimPool = new OnLineSimPool();
            onLineSimPool.setMacAddress(macAddress);
            onLineSimPool.setIpAddr(ipAddr);
            RpcResult<Long> deleteResult = onLineSimPoolService.remove(onLineSimPool);
        } else {
            ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
        }
        return ret;
    }

    //获取SimCard
    public SimCard getDbSimCard(String imsi) {
        RpcResult<SimCard> rpcResult = simCardService.findOneById(imsi);
        if(rpcResult != null && rpcResult.handResult() != null){
            return rpcResult.handResult();
        }
        return null;
    }

    //simCard下线
    public ErrCode.ServerCode dbSimCardOffline(String imsi) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<SimCard> rpcResult = simCardService.findOneById(imsi);
        if (rpcResult != null && rpcResult.handResult() != null) {
            SimCard simCard = rpcResult.handResult();
            simCard.setImsi(imsi);
            simCard.setInSimpool(false);
            simCard.setInUsed(false);
            RpcResult<Long> updateResult = simCardService.updateSimCardInfo(simCard);
            Long updateCount = updateResult.handResult();
            if (updateCount == null || updateCount != 1) {
                ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
            }
        }else{
            ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
        }
        return ret;
    }

    //simCard禁用
    public ErrCode.ServerCode dbSimCardDisable(String imsi, boolean bDisabled) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<Long> updateResult = simCardService.setSimCardDisabled(imsi, bDisabled);
        return ret;
    }

    //simPool禁用
    public ErrCode.ServerCode dbSimPoolDisable(String macAddress, boolean bDisabled) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<SimPool> rpcResult = simPoolService.findOneById(macAddress);
        if (rpcResult != null && rpcResult.handResult() != null) {
            SimPool simPool = rpcResult.handResult();
            simPool.setOnLine(false);
            simPool.setEnabled(bDisabled);
            RpcResult<Long> updateResult = simPoolService.update(simPool);
            OnLineSimPool onLineSimPool = new OnLineSimPool();
            onLineSimPool.setMacAddress(macAddress);
            RpcResult<Long> deleteResult = onLineSimPoolService.remove(onLineSimPool);
        } else {
            ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
        }
        return ret;
    }

    // Sim卡信息
    public ErrCode.ServerCode dbSpSimInfo(String imsi, String isscd, String macAddress, int locationInSimPool, boolean isInSimPool, boolean isBroken, byte[] img_md5) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<SimCard> simCardRpcResult = simCardService.findOneById(imsi);
        if (simCardRpcResult != null && simCardRpcResult.handResult() != null) {
            SimCard simCard = simCardRpcResult.handResult();
            simCard.setBroken(isBroken);
            simCard.setIccid(isscd);
            simCard.setLocationInSimPool(locationInSimPool);
            if(img_md5!=null){
                simCard.setImg_md5(img_md5);
                simCard.setSimcardImage(null);
            }
            simCard.setInSimpool(isInSimPool);
            simCard.setSimPoolMacAddr(macAddress);
            //SimCard.simpool属性被去掉了，后期商议是否还原或别的解决方案
            RpcResult<Long> updateResult = simCardService.updateById(simCard);
            Long updateCount = updateResult.handResult();
            if (updateCount == null || updateCount != 1) {
                ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
            }
        }else {
            ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
        }
        return ret;
    }

    //绑卡与解绑
    public ErrCode.ServerCode dbBindUserId(String imsi, String userId) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<SimCard> rpcResult = simCardService.findOneById(imsi);
        if (rpcResult != null && rpcResult.handResult() != null) {
            SimCard simCard = rpcResult.handResult();
            simCard.setBindUserId(userId);
            simCardService.updateById(simCard);
        }else{
            ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
        }
        return ret;
    }

    //禁用终端用户与解禁
    public ErrCode.ServerCode dbMtUserDisable(String userId, boolean bDisable) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        if (!StringUtils.isEmpty(userId)) {
            RpcResult<MtUser> rpcResult = mtUserService.getMtUserByUserId(userId);
            if (rpcResult != null && rpcResult.handResult() != null) {
                MtUser mtUser = rpcResult.handResult();
                mtUser.setDisabled(bDisable);
                RpcResult<Long> updateResult = mtUserService.update(mtUser);
                Long updateCount = updateResult.handResult();
                if (updateCount != null && updateCount == 1) {
                    ret = ErrCode.ServerCode.ERR_OK;
                } else {
                    ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
                }
            }else{
                ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
            }
        }else{
            ret = ErrCode.ServerCode.INVALID_PARAMETER;
        }
        return ret;
    }

    //返回值为null时，表示该user没有绑卡。否则返回该user可以分到的绑定的SimCard列表
    public List<SimCard> getDbBindSimCards(String userId, String mt_imsi) {
        SimCard query = new SimCard();
        query.setActivate(true);
        query.setInSimpool(true);
        query.setDisabled(false);
        query.setBroken(false);
        query.setBindUserId(userId);
        query.setImsi(mt_imsi);
        RpcResult<List<SimCard>> simCardListRpcResult = simCardService.getDbBindSimCards(query);
        if(simCardListRpcResult != null){
            return simCardListRpcResult.handResult();
        }else{
            return null;
        }
    }

    //获取当前可能分到的非绑定的SimCards列表
    public List<SimCard> getDbPlmnSimCards(String mt_imsi) {
        List<SimCard> simCards = new ArrayList<>();
        SimCard simCardPlmnQuery = new SimCard();
        simCardPlmnQuery.setImsi(mt_imsi);
        simCardPlmnQuery.setActivate(true);
        simCardPlmnQuery.setInSimpool(true);
        simCardPlmnQuery.setDisabled(false);
        simCardPlmnQuery.setBroken(false);
        simCardPlmnQuery.setInUsed(false);
        RpcResult<List<SimCard>> rpcResult = simCardService.getDbPlmnSimCards(simCardPlmnQuery);
        if(rpcResult != null && rpcResult.handResult() != null){
            simCards.addAll(rpcResult.handResult());
        }
        return simCards;
    }

    //指定卡是否可分给指定用户
    public boolean isDbSimCardAssignable(String userId, String mt_imsi, boolean isBindUser) {
        SimCard simCard = null;
        SimCard query = new SimCard();
        query.setActivate(true);
        query.setInSimpool(true);
        query.setDisabled(false);
        query.setBroken(false);
        query.setImg_md5(null);
        query.setSimcardImage(null);
        query.setInUsed(false);
        query.setImsi(mt_imsi);
        SearchOption searchOption = new SearchOption();
        searchOption.setPageIndex(1);
        searchOption.setPageSize(1);
        RpcResult<PagingData<SimCard>> rpcResult = simCardService.getBindListByQuery(query, searchOption);
        PagingData<SimCard> pagingData = rpcResult.handResult();
        if (pagingData != null && pagingData.getCurrentPageData() != null) {
            simCard = pagingData.getCurrentPageData().get(0);
        }
        if (simCard != null) { //该卡可用
            boolean bCanBeAssignToUser = false;
            String bindUserId = simCard.getBindUserId();
            boolean isInUsed = simCard.isInUsed();
            if (isBindUser && !StringUtils.isEmpty(bindUserId) && bindUserId.equals(userId)) { //该卡已被绑定给user
                bCanBeAssignToUser = true;
            } else if (!isBindUser && StringUtils.isEmpty(bindUserId)) {
                if (isInUsed) { //虽然已分出，但正好是分给usr
                    OnLineUser param = new OnLineUser();
                    param.setUserId(userId);
                    param.setImsi(mt_imsi);
                    RpcResult<OnLineUser> onLineUserRpcResult = onLineUsersService.findOnLineUser(param);
                    OnLineUser resultOnLineUser = onLineUserRpcResult.handResult();
                    if (resultOnLineUser != null) {
                        bCanBeAssignToUser = true;
                    }
                } else {    //未分出
                    bCanBeAssignToUser = true;
                }
            }
            return bCanBeAssignToUser;
        }
        return false;
    }

    //获取指定SimPool上已分出去的SimCard列表
    public List<SimCard> getDbSimCardsOnSimPool(String macAddress) {
        List<SimCard> simCards = new ArrayList<>();
        RpcResult<List<SimCard>> rpcResult = simCardService.getSimcardsBySimPool(macAddress);
        if (rpcResult != null && rpcResult.handResult() != null) {
            simCards.addAll(rpcResult.handResult());
        }
        simCards.forEach(simCard -> {
            if (simCard.getLocationInSimPool() == 0) {
                simCard.setLocationInSimPool(-1);
            }
        });
        return simCards;
    }

    //设置SIM卡已分配标识
    public ErrCode.ServerCode dbSimSetUsed(String imsi, boolean isInUsed) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        RpcResult<ErrCode.ServerCode> rpcResult = simCardService.setSimcardInUsedOrNot(imsi, isInUsed);
        if (rpcResult.isSuccess() && rpcResult.getData() != null) {
            ret = rpcResult.getData();
        }
        return ret;
    }

    //根据IP获取SimPool的MacAddress
    public String getOnLineSimPoolId(String ipAddr) {
        RpcResult<OnLineSimPool> onLineSimPoolRpcResult = onLineSimPoolService.findOnLineSimPoolByIpAddr(ipAddr);
        if (onLineSimPoolRpcResult != null && onLineSimPoolRpcResult.handResult() != null) {
            OnLineSimPool onLineSimPool = onLineSimPoolRpcResult.handResult();
            return onLineSimPool.getMacAddress();
        }
        return null;
    }

    //根据IP获取MacAddress的SimPool
    public String getOnLineSimPoolIpAddr(String macAddress) {
        RpcResult<OnLineSimPool> onLineSimPoolRpcResult = onLineSimPoolService.findOneById(macAddress);
        if (onLineSimPoolRpcResult != null && onLineSimPoolRpcResult.handResult() != null) {
            OnLineSimPool onLineSimPool = onLineSimPoolRpcResult.handResult();
            return onLineSimPool.getIpAddr();
        }
        return null;
    }

    // 终端用户等待分卡
    public ErrCode.ServerCode dbMtWaitAssignSim(String userId, ArrayList<String> plmn_list) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;

        RpcResult<OnLineUser> rpcResult = onLineUsersService.findOneByUserId(userId);
        if (rpcResult != null && rpcResult.handResult() != null) {
            OnLineUser onLineUser = rpcResult.handResult();
            if (plmn_list != null && plmn_list.size() > 0) {
                onLineUser.setPlmnList(plmn_list);
                onLineUser.setState(ClientState.CLIENT_STATE_WAITING_ASSIGN.name());
                // TODO: 2017/7/22   缺少requim_time属性！
            } else {
                onLineUser.setPlmnList(plmn_list);
                onLineUser.setState(ClientState.CLIENT_STATE_LOGINED.name());
                // TODO: 2017/7/22  提供删除plmn_list，requim_time字段服务
            }
            RpcResult<Long> updateResult = onLineUsersService.update(onLineUser);
            Long updateCount = updateResult.handResult();
            if (updateCount == null || updateCount != 1) {
                ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
            }
        } else {
            ret = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }
        return ret;
    }

    // 终端用户请求分卡
    public ErrCode.ServerCode dbMtAssignSim(String userId, String imsi) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        OnLineUser onLineUser = null;
        RpcResult<OnLineUser> rpcResult = onLineUsersService.findOneByUserId(userId);
        if (rpcResult.isSuccess()) {
            onLineUser = rpcResult.getData();
        }
        if (onLineUser != null) {
            String state_name = ClientState.CLIENT_STATE_ASSIGNED.name();
            if (imsi == null || imsi.equals("")) {
                state_name = ClientState.CLIENT_STATE_LOGINED.name();
            }
            onLineUser.setImsi(imsi);
            onLineUser.setState(state_name);
            RpcResult<Long> updateResult = onLineUsersService.update(onLineUser);
            long updateCount = 0;
            if (updateResult.isSuccess() && updateResult.getData() != null) {
                updateCount = updateResult.getData();
            }
            if (updateCount != 1) {
                ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
            }
        }
        return ret;
    }

    // TODO: 2017/7/22 日志文件上传方案未定
    public ErrCode.ServerCode dbMtUserUploadLogStatus(String _id, int status) {
        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<ErrCode.ServerCode> rpcResult = extractionRecordService.dbMtUserUploadLogStatus(_id, status);
        ret = rpcResult.handResult();
        return ret;
    }

    // TODO: 2017/7/22 日志文件上传方案未定
    public ExtractionRecord dbMtUserInsertLogData(OnLineUser user, long s_second, long e_second, MtProtoMsg.LEVEL level) {
        ExtractionRecord record = new ExtractionRecord();
        record.setDeviceId(user.getDeviceId());
        record.setStartTime(new Date(s_second));
        record.setEndTime(new Date(e_second));
        record.setStatus(LogsState.INIT.value());
        record.setLogCategory(level.getNumber());
        record.setExtractionTime(new Date());
        record.setOperaterId(user.getUserId());
        record.setOperaterName(user.getUserName());
        RpcResult<ExtractionRecord> rpcResult = extractionRecordService.dbMtUserInsertLogData(record);
        record = rpcResult.handResult();
        return record;
    }

    //生成黑名单信息，如果有黑名单信息则修改，次数+1
    public void doSaveBlackUsers(String ipAddr) {
        ipAddr = ipAddr.split(":")[0];
        BlackUser blackUser = new BlackUser();
        blackUser.setIpAddr(ipAddr);
        blackUserService.saveBlackUser(blackUser);
    }

    //判断是否是黑名单用户
    public boolean isBlackUser(String ipAddr) {
        boolean result = false;
        RpcResult<BlackUser> rpcResult = blackUserService.findBlackUserByIp(ipAddr);
        BlackUser blackUser = rpcResult.handResult();
        return blackUser != null;
    }
    //simpool日志上传新增
    public SimPoolLog dbSpInsertLogData(String macAddress,int status) {
        SimPoolLog simPoolLog  = new SimPoolLog();
        simPoolLog.setMacAddress(macAddress);
        simPoolLog.setLogTime(new Date());
        simPoolLog.setLogState(status);
        RpcResult<SimPoolLog> rpcResult = simPoolLogService.insert(simPoolLog);
        if(rpcResult.handResult() != null){
            return rpcResult.handResult();
        }
        return null;
    }
    //simpool日志上传修改
    public  void dbSpUpdateLogState(String _id,int status) {
        RpcResult<SimPoolLog> rpcResult= simPoolLogService.findSimPoolLogById(_id);
        SimPoolLog simPoolLog  = rpcResult.handResult();
        if(simPoolLog != null){
            simPoolLog.setLogState(status);
        }
        simPoolLogService.updateSimPoolLog(simPoolLog);
    }

    //simpool软件升级
    public void doSaveUpgrade(String macAddress,String url,int status) {
        RpcResult<SimPoolUpgrade> rpcResult = simPoolUpgradeService.findOneById(macAddress);
        SimPoolUpgrade simPoolUpgrade = rpcResult.handResult();
        if(simPoolUpgrade==null){
            simPoolUpgrade=new SimPoolUpgrade();
            simPoolUpgrade.setMacAddress(macAddress);
            simPoolUpgrade.setUpState(status);
            simPoolUpgrade.setUrl(url);
            simPoolUpgradeService.insert(simPoolUpgrade);
        }else{
            if(!StringUtils.isEmpty(url)){
                simPoolUpgrade.setUrl(url);
            }
            simPoolUpgrade.setUpState(status);
            simPoolUpgradeService.updateSimPoolUpgrade(simPoolUpgrade);
        }
    }

    //设置SIM卡已损坏
    public ErrCode.ServerCode dbSimSetBroken(String imsi, boolean isBroken) {

        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
        RpcResult<SimCard> rpcResult = simCardService.findOneById(imsi);
        if(rpcResult != null){
            SimCard simCard = rpcResult.handResult();
            if (simCard != null) {
                simCard.setBroken(true);
                simCardService.updateById(simCard);
            }
        }else{
            ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        }

        return ret;
    }
}
