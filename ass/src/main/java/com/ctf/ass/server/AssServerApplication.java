package com.ctf.ass.server;

import com.alibaba.boot.dubbo.annotation.EnableDubboConfiguration;
import com.ctf.ass.server.component.SpringContextHandler;
import com.ctf.ass.server.configuration.ServerConfig;
import com.ctf.ass.server.utils.LogWrapper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ass服务启动类 Netty 服务在com.ctf.ass.server.netty包下启动
 *
 * @author Charles
 * @create 2017/7/21 10:45
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.ctf")
@EnableDubboConfiguration
public class AssServerApplication {
    private static LogWrapper logger = LogWrapper.getLogger(AssServerApplication.class);
    private static final AtomicBoolean RUNNING = new AtomicBoolean(true);

    public static void main(String[] args) {
        if (parseArgs(args) == 0) {
            //初始化数据库
            try {
                //配置log路径
                System.setProperty("ASSLOGPATH", getJarDirPath() + "/logs");
                //打印本机IP
                InetAddress addr = InetAddress.getLocalHost();
                ServerConfig.localhost = addr.getHostAddress().toString();  //本机IP
                logger.info("localhost:" + ServerConfig.localhost);
            } catch (Exception e) {
                logger.fatal("AssServer init failed!");
            }
            ApplicationContext applicationContext = SpringApplication.run(AssServerApplication.class, args);
            SpringContextHandler.setApplicationContext(applicationContext);
            synchronized (AssServerApplication.class) {
                while (RUNNING.get()) {
                    try {
                        AssServerApplication.class.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //获取jar包运行时目录
    public static String getJarDirPath() {
        String path = AssServerApplication.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }

        File file = new File(path);
        if (file == null)
            return null;
        return file.getParent();
    }

    //解析参数，出错返回-1，其它用途返回>0，正常返回0
    private static int parseArgs(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "print help usages.");
        options.addOption(Option.builder("v").longOpt("version").hasArg(false).desc("print version info").build());
        options.addOption("debug", "print debugging information.");
        options.addOption(Option.builder("f").hasArg(true).argName("config_file").desc("use given file for configuration").build());
        options.addOption(Option.builder("D").hasArgs().numberOfArgs(2).argName("property=value").valueSeparator('=').desc("set value for given property(Will override settings in <config_file>)").build());

        CommandLine cmd;
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.fatal("Invalid command line parameters!!!" + e.getMessage());
            return -1;
        }

        if (cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java AssServer.jar [options]", options);
            return 1;
        }

        if (cmd.hasOption("v")) {
            System.out.println(ServerConfig.VER_INFO);
            return 2;
        }

        if (cmd.hasOption("debug")) {
            //TODO: add code for debug
            System.out.println("switch on debug mode");
        }

        if (cmd.hasOption("f")) {
            //TODO: 添加对配置文件的支持
            String config_filepath = cmd.getOptionValue("f");
            System.out.println("config_file:" + config_filepath);
        }

        if (cmd.hasOption("D")) {
            Properties properties = cmd.getOptionProperties("D");
            String value = properties.getProperty("sp_port");
            if (value != null) {
                ServerConfig.sp_port = Integer.parseInt(value);
            }

            value = properties.getProperty("mt_port");
            if (value != null) {
                ServerConfig.mt_port = Integer.parseInt(value);
            }

            value = properties.getProperty("ws_port");
            if (value != null) {
                ServerConfig.ws_port = Integer.parseInt(value);
            }

            value = properties.getProperty("http_port");
            if (value != null) {
                ServerConfig.http_port = Integer.parseInt(value);
            }

            value = properties.getProperty("db_ip");
            if (value != null) {
                ServerConfig.db_ip = value;
            } else {
                ServerConfig.db_ip = ServerConfig.localhost;
            }

            value = properties.getProperty("db_id");
            if (value != null) {
                ServerConfig.db_id = value;
            }

            value = properties.getProperty("db_psd");
            if (value != null) {
                ServerConfig.db_psd = value;
            }
        }

        return 0;
    }
}
