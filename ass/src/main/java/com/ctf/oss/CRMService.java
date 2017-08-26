package com.ctf.oss;

import com.ctf.ass.server.component.MongoDbUtil;
import com.ctf.ass.server.component.RocketMQMessageSender;
import com.ctf.ass.server.component.ToMtMsg;
import com.ctf.ass.server.utils.CmdUtils;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_public.globals.ClientState;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.globals.GlobalConfig;
import com.ctf.ass_public.struct.OnLineUser;
import com.ctf.ass_public.struct.OpHistory;
import com.ctf.ass_public.struct.SimCard;
import com.ctf.ass_public.struct.UserLoginHistory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Charles
 * @create 2017/7/21 15:17
 */
@Service
public class CRMService {
    private static LogWrapper logger = LogWrapper.getLogger(CRMService.class);
    @Autowired
    private MongoDbUtil mongoDbUtil;
    @Autowired
    private RocketMQMessageSender rocketMQMessageSender;
    @Autowired
    private ToMtMsg toMtMsg;
    @Autowired
    private CRMService crmService;

    public String getIpAddr(ChannelHandlerContext ctx) {
        if (ctx != null && ctx.channel() != null) {
            return ctx.channel().remoteAddress().toString();
        }

        return null;
    }

    //user登录
    public ErrCode.ServerCode userLogin(ChannelHandlerContext userCtx, String userId, String psdMD5, String devId) {
        ErrCode.ServerCode ret = mongoDbUtil.userAuth(userId, psdMD5);  //用户鉴权
        String newIpAddr = getIpAddr(userCtx);

        if (ErrCode.ServerCode.ERR_OK == ret) { //鉴权通过
            OnLineUser onLineUser = mongoDbUtil.getOnLineUserByUserId(userId);  //检查该userId的用户是否已在线
            if (onLineUser != null) {
                String oldIpAddr = onLineUser.getIpAddr();
                if (!oldIpAddr.equals(newIpAddr)) {  //检查该userId的在线用户的ipAddr是否改变
                    //同一帐号在另一IP登录，需要踢掉在线用户
                    logger.debug("force kick old user!");
                    userForceKick(oldIpAddr, ErrCode.ServerCode.ULR_USER_RELOGIN_OTHER);
                } else {
                    //已在线用户再次发登录协议，先同步到未知状态
                    userSyncToState(userCtx, ClientState.CLIENT_STATE_UNKNOWN);
                }
            }
            //添加到在线用户
            mongoDbUtil.addOnLineUser(userId, devId, newIpAddr);
            //记录用户登录历史
            rocketMQMessageSender.sendUserLoginHistory(new UserLoginHistory(userId, devId, newIpAddr));
            //缓存用户id以便于打日志
            CCM.setId(CCM.ClientType.MT, newIpAddr, userId);
        }else{
            //如果密码错误并且在同一台机器重复登录，同步到未知状态
            if(ErrCode.ServerCode.ERR_USER_WRONG_PASSWORD == ret){
                OnLineUser onLineUser = mongoDbUtil.getOnLineUserByUserId(userId);
                if (onLineUser != null && newIpAddr.equals(onLineUser.getIpAddr())) {
                     userSyncToState(userCtx, ClientState.CLIENT_STATE_UNKNOWN);
                }
            }
        }
        return ret;
    }

    //user通过sessionId登录
    public ErrCode.ServerCode userLoginById(ChannelHandlerContext userCtx, byte[] sessionId) {
        //先检查并清除已过期的离线用户
        mongoDbUtil.checkAndDelExpiredOffLineUsers();

        ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_USER_INVALID_SESSION;
        //查找在线用户
        OnLineUser onLineUser = mongoDbUtil.getOnLineUser(sessionId);
        if (onLineUser != null) {
            String newIpAddr = getIpAddr(userCtx);
            String oldIpAddr = onLineUser.getIpAddr();

            switch (onLineUser.getClientState()) {
                case CLIENT_STATE_ASSIGNED: //已分卡
                    ret = mongoDbUtil.userLoginById(sessionId, newIpAddr);
                    if (ret == ErrCode.ServerCode.ERR_OK) {
                        CCM.setId(CCM.ClientType.MT, newIpAddr, onLineUser.getUserId());
                        if (!oldIpAddr.equals(newIpAddr)) {//同一帐号在另一IP登录，需要踢掉在线用户
                            ChannelHandlerContext userOldCtx = CCM.getCtx(CCM.ClientType.MT, oldIpAddr);
                            if (userOldCtx != null) {
                                CmdUtils cmdUtils = CmdUtils.get(userOldCtx);
                                cmdUtils.setWaitClose();
                                toMtMsg.sendMtTermLogoutReq(userOldCtx, ErrCode.ServerCode.ULR_USER_RELOGIN_OTHER.value(), 0);
                            }
                        }
                    } else {
                        ret = ErrCode.ServerCode.SERR_SERVICE_DOWN;
                    }
                    break;

                case CLIENT_STATE_OFFLINE:  //离线
                    ret = mongoDbUtil.userLoginById(sessionId, newIpAddr);
                    if (ret == ErrCode.ServerCode.ERR_OK) {
                        CCM.setId(CCM.ClientType.MT, newIpAddr, onLineUser.getUserId());
                    }
                    break;

                case CLIENT_STATE_WAITING_ASSIGN:   //等待分卡
                case CLIENT_STATE_LOGINED:  //已登录,不处理，认为无效session
                    if (oldIpAddr.equals(newIpAddr)) {
                        mongoDbUtil.removeOnLineUser(onLineUser.getUserId());
                    }
                    break;

                case CLIENT_STATE_UNKNOWN:
                    break;
            }
        }

        return ret;
    }

    public ErrCode.ServerCode userLogout(String ipAddr, ErrCode.ServerCode code) {
        ErrCode.ServerCode ret;
        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(ipAddr);

        if (onLineUser != null) {
            String userId = onLineUser.getUserId();
            mongoDbUtil.userRevUim(userId);
            logger.debug("####################");
            logger.debug("#  User logout!!!  #");
            logger.debug("####################");

            ret = mongoDbUtil.userLogout(userId);
        } else {
            logger.warn("user not Login!!!");
            ret = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }

        return ret;
    }

    //user退出登录
    public ErrCode.ServerCode userLogout(ChannelHandlerContext userCtx, ErrCode.ServerCode cause) {
        ErrCode.ServerCode ret;
        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(getIpAddr(userCtx));

        if (onLineUser != null) {
            String userId = onLineUser.getUserId();
            mongoDbUtil.userRevUim(userId);
            logger.debug(userCtx, "####################");
            logger.debug(userCtx, "#  User logout!!!  #");
            logger.debug(userCtx, "####################");

            ret = mongoDbUtil.userLogout(userId);
        } else {
            logger.warn(userCtx, "user not Login!!!");
            ret = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }

        return ret;
    }

    public ErrCode.ServerCode userCloseConn(ChannelHandlerContext userCtx) {
        String ipAddr = getIpAddr(userCtx);
        rocketMQMessageSender.sendOpHistory(new OpHistory(ipAddr, OpHistory.OpType.MtToSvr_Ind, "disconn", 0));
        logger.debug(userCtx, "#########################");
        logger.debug(userCtx, "#  User conn closed!!!  #");
        logger.debug(userCtx, "#########################");

        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(ipAddr);
        if (onLineUser != null) {
            return mongoDbUtil.dbMtUserCloseConn(onLineUser.getUserId());
        }

        return ErrCode.ServerCode.ERR_OK;
    }

    //来自web的绑卡请求
    public ErrCode.ServerCode userBindUim(String imsi, String userId) {
        if (StringUtils.isNotEmpty(imsi)) {
            OnLineUser onLineUser = mongoDbUtil.getOnLineUserByImsi(imsi);
            //使用该卡的用户排除自己在外还卡
            if (onLineUser != null && userId != null && !onLineUser.getUserId().equals(userId)) {
                mongoDbUtil.userRevUim(onLineUser.getUserId());
                //发送Http命令给终端
                ChannelHandlerContext ctx = CCM.getCtx(CCM.ClientType.MT, onLineUser.getIpAddr());
                if(ctx != null){
                    toMtMsg.sendMtTermRevUimReq(ctx, ErrCode.ServerCode.RUR_USER_VIP_SNATCH.value(), 1);
                }
            }
            return mongoDbUtil.dbBindUserId(imsi, userId);
        }

        return ErrCode.ServerCode.ERR_USER_NOT_EXIST;
    }

    //来自web的用户禁用请求
    public ErrCode.ServerCode userDisable(String userId, boolean bDisable) {
        ErrCode.ServerCode ret;
        if (StringUtils.isNotEmpty(userId)) {
            if (mongoDbUtil.getDbUser(userId) != null) {
                if (bDisable) {
                    OnLineUser onLineUser = mongoDbUtil.getOnLineUserByUserId(userId);
                    if (onLineUser != null) {
                        ChannelHandlerContext ctx = CCM.getCtx(CCM.ClientType.MT, onLineUser.getIpAddr());
                        if (ctx != null) {
                            toMtMsg.sendMtTermLogoutReq(ctx, ErrCode.ServerCode.ULR_USER_DISABLED.value(), 0);
                        } else {
                            mongoDbUtil.removeOnLineUser(userId);
                        }
                    }
                }
                ret = mongoDbUtil.dbMtUserDisable(userId, bDisable);
            } else {
                ret = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
            }
        } else {
            ret = ErrCode.ServerCode.ERR_ILLEGAL_PARAM;
        }
        return ret;
    }

    //强行踢掉user
    public ErrCode.ServerCode userForceKick(String ipAddr, ErrCode.ServerCode cause) {
        ChannelHandlerContext userCtx = CCM.getCtx(CCM.ClientType.MT, ipAddr);
        if (userCtx != null) {
            CmdUtils cmdUtils = CmdUtils.get(userCtx);
            if (cmdUtils != null) {
                userLogout(ipAddr, cause);//先终止再通知终端
                cmdUtils.setWaitClose();
                toMtMsg.sendMtTermLogoutReq(userCtx, cause.value(), 0);
            }
        } else {
            return userLogout(ipAddr, cause);
        }

        return ErrCode.ServerCode.ERR_OK;
    }

    //1.用户绑定Sim卡时，分配且仅分配已绑定卡，需绑定卡在位
    //2.用户未绑定Sim卡时，按plmn分卡
    //imsi是终端已经有的卡，不为null时表示终端请求换卡。换卡时，先将终端已有的卡还掉，再看终端能否分到除imsi以外的其它卡。
    public synchronized ErrCode.ServerCode userReqSim(ChannelHandlerContext userCtx, ArrayList<String> plmn_list, String mt_imsi) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.ERR_PLMN_NO_SIM;
        ;

        //检查并处理过期session
        mongoDbUtil.checkAndDelExpiredOffLineUsers();

        String ipAddr = getIpAddr(userCtx);
        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(ipAddr);
        String userId = onLineUser.getUserId();

        String imsi = null;

        //尝试分绑定的卡
        List<SimCard> simCards = mongoDbUtil.getDbBindSimCards(userId, mt_imsi);

        if (simCards != null) {
            logger.debug(userCtx, "bind user!!!");
            if (simCards.size() > 0) {
                //直接分第一张卡
                logger.debug(userCtx, "got sim:" + simCards.get(0));
                imsi = simCards.get(0).getImsi();
                errCode = ErrCode.ServerCode.ERR_OK;    //分卡/换卡成功
            } else {    //分卡失败
                logger.debug(userCtx, "request bind sim fail.");
                errCode = ErrCode.ServerCode.ERR_BIND_NO_SIM;
            }
        }

        //如果分卡失败，继续尝试通过plmn匹配进行分卡
        if (imsi == null && (GlobalConfig.ASSIGN_BINDUSER_PLMN || errCode != ErrCode.ServerCode.ERR_BIND_NO_SIM)) {
            simCards = mongoDbUtil.getDbPlmnSimCards(mt_imsi);
            if (simCards.size() > 0) {
                String key;
                outer:
                for (String plmn : plmn_list) {
                    if (GlobalConfig.ASSIGN_BY_MCC) {
                        key = plmn.substring(0, 3);
                    } else {
                        key = plmn;
                    }
                    for (SimCard simCard : simCards) {
                        if (simCard.getImsi().startsWith(key)) {
                            logger.debug(userCtx, "got sim:" + simCard);
                            imsi = simCard.getImsi();
                            break outer;
                        }
                    }
                }
            }

            if (imsi != null) {   //分卡/换卡成功
                errCode = ErrCode.ServerCode.ERR_OK;
            } else {    //分卡失败
                logger.debug(userCtx, "request plmn sim fail.");
            }
        }

        //先还卡
        if (mt_imsi != null) {
            if (onLineUser.getImsi().equals(mt_imsi)) {
                mongoDbUtil.dbSimSetUsed(mt_imsi, false);
            }
        }

        //更新分卡结果到数据库
        if (imsi != null) {
            logger.warn(userCtx, "ReqUim succ :)" + imsi);
            mongoDbUtil.dbSimSetUsed(imsi, true);
            mongoDbUtil.dbMtAssignSim(userId, imsi);
        } else {
            mongoDbUtil.dbMtAssignSim(userId, "");
            logger.warn(userCtx, "ReqUim fail!!!");
        }

        return errCode;
    }

    public ErrCode.ServerCode userRevSim(ChannelHandlerContext userCtx, int cause_code) {
        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(getIpAddr(userCtx));
        String userId = onLineUser.getUserId();
        if(cause_code == ErrCode.ServerCode.UIM_CARD_ERROR.value()){
            //卡损坏了不能再分
            mongoDbUtil.dbSimSetBroken(onLineUser.getImsi(),true);
            //TODO: 通知运维，损坏原因
        }
        return mongoDbUtil.userRevUim(userId);
    }

    public ClientState userGetState(String userId) {
        ClientState clientState = ClientState.CLIENT_STATE_UNKNOWN;
        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByUserId(userId);

        if (onLineUser != null) {
            clientState = onLineUser.getClientState();
        }

        return clientState;
    }

    public ClientState userGetState(ChannelHandlerContext ctx) {
        ClientState clientState = ClientState.CLIENT_STATE_UNKNOWN;

        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(getIpAddr(ctx));
        if (onLineUser != null) {
            return onLineUser.getClientState();
        }

        return clientState;
    }

    //同步状态(仅高状态->低状态)
    public ErrCode.ServerCode userSyncToState(OnLineUser onLineUser, ClientState new_state) {
        if (onLineUser != null) {
            ErrCode.ServerCode ret = ErrCode.ServerCode.ERR_OK;
            String userId = onLineUser.getUserId();

            //旧状态为有卡状态时，需要先释放卡
            ClientState old_state = onLineUser.getClientState();
            if (old_state == ClientState.CLIENT_STATE_ASSIGNED || old_state == ClientState.CLIENT_STATE_OFFLINE) {
                ret = mongoDbUtil.userRevUim(userId);
            }

            //同步到新状态
            switch (new_state) {
                case CLIENT_STATE_WAITING_ASSIGN:
                    break;

                case CLIENT_STATE_LOGINED:
                    ret = mongoDbUtil.dbMtWaitAssignSim(userId, null);
                    break;

                case CLIENT_STATE_UNKNOWN:
                    ret = mongoDbUtil.removeOnLineUser(userId);
                    break;
            }

            return ret;
        }

        return ErrCode.ServerCode.ERR_USER_NOT_EXIST;
    }

    //当服务器侧状态与终端不一致时，需要同步到与终端一致的较低的状态
    public ErrCode.ServerCode userSyncToState(ChannelHandlerContext ctx, ClientState new_state) {
        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(getIpAddr(ctx));
        return userSyncToState(onLineUser, new_state);
    }
}
