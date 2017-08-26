package com.ctf.ass.server.configuration;

import com.ctf.ass_public.globals.GlobalConfig;

/**
 * @author Charles
 * @create 2017/7/21 14:11
 */
public class ServerConfig {
    public static final String VER_INFO = "AssServer version \"1.0_20170331\"\nAuthor:yuanyuefeng@cootf.com\nCopyright 2017-2030 All rights reserved.";;
    //实际配置
    public static String db_ip = null;   //数据库 地址
    public static String db_id = null;	 //数据库 用户名(考虑到安全性，不在程序中内置缺省值。通过加密脚本传入)
    public static String db_psd = null;	 //数据库 密码  (考虑到安全性，不在程序中内置缺省值。通过加密脚本传入)
    public static int sp_port = GlobalConfig.ASS_SP_PORT;			//卡池 服务端口
    public static int mt_port = GlobalConfig.ASS_MT_PORT;			//终端 服务端口
    public static int http_port = GlobalConfig.ASS_HTTP_PORT;		//Http 服务端口
    public static int ws_port = GlobalConfig.ASS_WS_PORT;	        //WebSocket 服务端口
    //本机IP
    public static String localhost = "127.0.0.1";
}
