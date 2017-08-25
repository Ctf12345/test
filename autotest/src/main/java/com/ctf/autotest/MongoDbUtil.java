package com.ctf.autotest;

import com.ctf.ass_public.globals.ErrCode.ServerCode;
import com.ctf.ass_public.struct.*;
import com.ctf.ass_public.utils.CheckUtils;
import com.ctf.ass_public.utils.ConvUtils;
import com.ctf.autotest.spring.SpringContextHandler;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import org.bson.Document;
import org.bson.types.Binary;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static java.lang.Math.random;

public final class MongoDbUtil {
    private static LogWrapper logger = LogWrapper.getLogger(MongoDbUtil.class.getName());

    private static final String DB_Simcards = "SimcardsDB";
    private static final String Tbl_simcards = "simcards";
    private static final String Tbl_simpools = "simpools";

    private static final String DB_CRM = "CRMDB";
    private static final String Tbl_clients = "clients";

    private static final String DB_OSS = "OSS";
    private static final String Tbl_OnLineUsers = "OnLineUsers";
    private static final String Tbl_OnLineSimPools = "OnLineSimPools";
    private static final String Tbl_BlackUsers = "BlackUsers";

    private static MongoClient mongoClient = null;

    //=====================================================================================
    //获取client
    private static MongoClient connectDB(String host, String db_id, String db_psd) {
        try {
            if (mongoClient == null) {
                /*
                ServerAddress serverAddress = new ServerAddress(host);
                List<ServerAddress> serverAddressList = new ArrayList<>();
                serverAddressList.add(serverAddress);

                MongoCredential credential = MongoCredential.createCredential(db_id, "admin", db_psd.toCharArray());
                List<MongoCredential> credentialsList = new ArrayList<>();
                credentialsList.add(credential);

                mongoClient = new MongoClient(serverAddressList, credentialsList);
                */
                mongoClient = new MongoClient(new MongoClientURI("mongodb://" + host));
            }
        } catch (MongoSocketOpenException e) {
            logger.fatal("Connect to MongoDB failed!");
        }

        return mongoClient;
    }

    private static MongoClient getDBClient() {
        return mongoClient;
    }

    //关闭数据库，释放client
    public static void closeDB() {
        mongoClient.close();
        mongoClient = null;
    }

    //初始化数据库
    public static ServerCode initDb(String host, String db_id, String db_psd) {
        ServerCode ret = ServerCode.ERR_OK;

        //连接到数据库
//        connectDB(host, db_id, db_psd);

        resetDb();

        return ret;
    }

    public static void resetDb() {
        UpdateResult result;

        //------------------
        //初始化SimPools
        //------------------
 /*       MongoCollection<Document> tblSimPools = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simpools);

        //将isOnLine置为false
        result = tblSimPools.updateMany(eq("isOnLine", true),
                new Document("$set",
                        new Document("isOnLine", false)
                ));
*/
        //------------------
        //初始化SimCards
        //------------------
/*
        MongoCollection<Document> tblSimCards = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simcards);
*/

        //将isInUsed, isInSimpool置为False
    /*    result = tblSimCards.updateMany(eq("isInSimpool", true),
                new Document("$set",
                        new Document("isInUsed", false).
                                append("isInSimpool", false)
                ));*/
        SpringContextHandler.getAutoTestIgniteService().resetDb();
        SpringContextHandler.getAutoTestMongoDBService().resetDb();
        //------------------
        //初始化OnLineUsers
        //------------------
//        MongoCollection<Document> tblOnLineUsers = getDBClient()
//                .getDatabase(DB_OSS)
//                .getCollection(Tbl_OnLineUsers);

        //将表清空
//        tblOnLineUsers.drop();

        //------------------
        //初始化OnLineSimPools
        //------------------
//        MongoCollection<Document> tblOnLineSimPools = getDBClient()
//                .getDatabase(DB_OSS)
//                .getCollection(Tbl_OnLineSimPools);

        //将表清空
//        tblOnLineSimPools.drop();

        //------------------
        //初始化BlackUsers
        //------------------
//        MongoCollection<Document> tblBlackUsers = getDBClient()
//                .getDatabase(DB_OSS)
//                .getCollection(Tbl_BlackUsers);

        //将表清空
//        tblBlackUsers.drop();

        //---------------------
        //清空测试数据
        //---------------------
        dropTestData();
    }

    //清空测试数据
    public static boolean dropTestData() {
        boolean bRet = true;
/*

        MongoCollection<Document> tblClients = getDBClient()
                .getDatabase(DB_CRM)
                .getCollection(Tbl_clients);

        DeleteResult deleteResult = tblClients.deleteMany(regex("userId", "test.*"));
        if (deleteResult.getDeletedCount() == 0) {
            logger.warn("Remove test [CRMDB.clients] failed!");
            bRet = false;
        }

        Pattern pattern = Pattern.compile("测试.*");
        MongoCollection<Document> tblSimcards = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simcards);
        deleteResult = tblSimcards.deleteMany(regex("name", pattern));
        if (deleteResult.getDeletedCount() == 0) {
            logger.warn("Remove test [SimcardsDB.simcards] failed!");
            bRet = false;
        }

        MongoCollection<Document> tblSimpools = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simpools);

        deleteResult = tblSimpools.deleteMany(regex("simPoolName", pattern));
        if (deleteResult.getDeletedCount() == 0) {
            logger.warn("Remove test [SimcardsDB.simpools] failed!");
            bRet = false;
        }
*/
        SpringContextHandler.getAutoTestMongoDBService().dropTestData("test", "测试", "测试");
        SpringContextHandler.getAutoTestIgniteService().dropTestData("test", "测试", "测试");

        return true;
    }

    //生成测试用CRMDB.clients
    public static void genTestClients(int count) {
        SpringContextHandler.getAutoTestMongoDBService().genTestClients(count);
     /*   MongoCollection<Document> tblClients = getDBClient()
                .getDatabase(DB_CRM)
                .getCollection(Tbl_clients);

        for (int i = 1; i <= count; i++) {
            tblClients.insertOne(new Document("userId", String.format("test%05d", i))
                    .append("password", "123456")
                    .append("name", String.format("测试用户%05d", i))
                    .append("idCard", String.format("123456789%05d", i))
                    .append("status", 0)
                    .append("balance", i)
            );
        }*/
    }

    //生成测试用SimcardsDB.simpools
    public static void genTestSimPools(int count) {
        SpringContextHandler.getAutoTestIgniteService().genTestSimPools(count);
      /*  MongoCollection<Document> tblClients = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simpools);

        for (int i = 1; i <= count; i++) {
            tblClients.insertOne(new Document("macAddress", ConvUtils.mac2str(ConvUtils.hexStrToBytes(String.format("%012d", i))))
                    .append("simPoolName", new String("测试SimPool_" + String.format("%05d", i)))
                    .append("location", String.format("测试SimPool中心%d", new Double(random() * 10).intValue()))
                    .append("softwareVersion", String.format("V1.00"))
                    .append("hardwareVersion", String.format("V1.00"))
                    .append("totalUsed", 0)
                    .append("capacity", 256)
                    .append("isEnabled", true)
                    .append("isOnLine", false)
            );
        }*/
    }

    //生成测试用SimcardsDB.simcards
    public static void genTestSimCards(int count) {
        SpringContextHandler.getAutoTestIgniteService().genTestSimCards(count);
      /*  MongoCollection<Document> tblClients = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simcards);

        byte[] simImage = "testImage".getBytes();
        byte[] imgMd5 = CheckUtils.MD5(simImage);

        for (int i = 1; i <= count; i++) {
            tblClients.insertOne(new Document("imsi", "")
                    .append("iccid", String.format("89860%015d", i))
                    .append("imsi", String.format("12345%010d", i))
                    .append("name", String.format("测试运营商%d", new Double(random() * 10).intValue()))
                    .append("isActivate", true)
                    .append("isInSimpool", false)
                    .append("isDisabled", false)
                    .append("isInUsed", false)
                    .append("isBroken", false)
                    .append("simcardImage", simImage)
                    .append("imgMd5", imgMd5)
            );
        }*/
    }

    //======================================================================================
    // user (CRMDB->clients)
    //======================
    //获取MtUser
    public static MtUser getDbUser(String userId) {
        MongoCollection<Document> tblClients = getDBClient()
                .getDatabase(DB_CRM)
                .getCollection(Tbl_clients);

        Document doc = tblClients.find(eq("userId", userId)).first();
        if (doc != null) {
            MtUser user = new MtUser(doc.getString("userId"),
                    doc.getString("name"),
                    doc.getString("password"),
                    (doc.getInteger("status") != 0));
            return user;
        }

        logger.warn("getDbUser [" + userId + "] is null!!!");

        return null;
    }

    //=======================================================================================
    // simpool (SimcardsDB->simpools)
    //================================
    //获取SimPool
    public static SimPool getDbSimPool(String macAddress) {
        return SpringContextHandler.getSimPoolService().getSimPoolByMacAddress(macAddress).handResult();
   /*     MongoCollection<Document> tblSimPools = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simpools);

        Document doc = tblSimPools.find(eq("macAddress", macAddress.toLowerCase())).first();
        if (doc != null) {
            SimPool simPool = new SimPool(macAddress.toLowerCase(),
                    doc.getString("hardwareVersion").substring(1),  //去掉前面的V
                    doc.getString("softwareVersion").substring(1),  //去掉前面的V
                    doc.getBoolean("isOnLine"),
                    doc.getBoolean("isEnabled"),
                    doc.getInteger("capacity"),
                    doc.getInteger("totalUsed"));
            return simPool;
        }

        logger.warn("getDbSimPool [" + macAddress + "] is null!!!");

        return null;*/
    }

    //=======================================================================================
    // simcard (SimcardsDB->simcards)
    //================================
    //获取SimCard
    public static SimCard getDbSimCard(String imsi) {
        return SpringContextHandler.getSimCardService().getSimCard(imsi).handResult();
        /*MongoCollection<Document> tblSimCards = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simcards);

        Document doc = tblSimCards.find(eq("imsi", imsi)).first();
        if (doc != null) {
            String macAddress = null;

            Document simpool_doc = (Document) doc.get("simPool");
            if (simpool_doc != null) {
                macAddress = simpool_doc.getString("macAddress");
            }
            short locationInSimPool = (short) doc.getInteger("locationInSimPool", -1);
            Binary img_md5 = (Binary) doc.get("imgMd5");
            byte[] imgMd5Data = null;
            if (img_md5 != null) {
                imgMd5Data = img_md5.getData();
            }

            Binary simcardImage = (Binary) doc.get("simcardImage");
            byte[] simImgData = null;
            if (simcardImage != null) {
                simImgData = simcardImage.getData();
            }
            SimCard simCard = new SimCard(doc.getString("imsi"),
                    doc.getString("iccid"),
                    macAddress,
                    locationInSimPool,
                    imgMd5Data,
                    simImgData,
                    doc.getBoolean("isActivate"),
                    doc.getBoolean("isInSimpool"),
                    doc.getBoolean("isDisabled"),
                    doc.getBoolean("isInUsed"),
                    doc.getBoolean("isBroken"),
                    doc.getString("bindUserId")
            );
            return simCard;
        }

        return null;*/
    }

    public static boolean dbSpUpdate(String macAddr,
                                     String newMacAddr,
                                     Integer capacity,
                                     String swVer,
                                     String hwVer,
                                     Boolean isEnabled,
                                     Boolean isOnLine) {
        return SpringContextHandler.getAutoTestIgniteService().dbSpUpdate(macAddr,newMacAddr,capacity,swVer,hwVer,isEnabled,isOnLine).handResult();
       /* MongoCollection<Document> tblSimPools = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simpools);

        Document updateDoc = new Document();
        if (newMacAddr != null) {
            updateDoc.append("macAddress", newMacAddr.toLowerCase());
        }
        if (capacity != null) {
            updateDoc.append("capacity", capacity);
        }
        if (swVer != null) {
            updateDoc.append("softwareVersion", swVer);
        }
        if (hwVer != null) {
            updateDoc.append("hardwareVersion", hwVer);
        }
        if (isEnabled != null) {
            updateDoc.append("isEnabled", isEnabled);
        }
        if (isOnLine != null) {
            updateDoc.append("isOnLine", isOnLine);
        }

        if (updateDoc.size() > 0) {
            UpdateResult result = tblSimPools.updateOne(eq("macAddress", macAddr.toLowerCase()), new Document("$set", updateDoc));
            if (result == null || result.getMatchedCount() != 1) {
                logger.warn("dbSpUpdate [" + macAddr + "] failed!!!");
                return false;
            }
        }

        return true;*/
    }

    public static boolean dbSimUpdate(String imsi,
                                      String newImsi,
                                      String iccid,
                                      String sim_img,
                                      String img_md5,
                                      Boolean isActivate,
                                      Boolean isDisabled,
                                      Boolean isBroken,
                                      Boolean isInSimPool,
                                      Boolean isInUsed,
                                      String bindUserId) {
        return SpringContextHandler.getAutoTestIgniteService().dbSimUpdate(imsi,newImsi,iccid,sim_img,img_md5,isActivate,isDisabled,isBroken,isInSimPool,isInUsed,bindUserId).handResult();
        /*MongoCollection<Document> tblSimCards = getDBClient()
                .getDatabase(DB_Simcards)
                .getCollection(Tbl_simcards);

        Document updateDoc = new Document();
        if (newImsi != null) {
            updateDoc.append("imsi", newImsi);
        }
        if (iccid != null) {
            updateDoc.append("iccid", iccid);
        }
        if (sim_img != null) {
            byte[] simcardImage = ConvUtils.hexStrToBytes(sim_img);
            updateDoc.append("simcardImage", simcardImage);
            //如果没有传入img_md5，则自动计算正确的值
            if (img_md5 == null) {
                byte[] imgMd5 = CheckUtils.MD5(simcardImage);
                updateDoc.append("imgMd5", imgMd5);
            }
        }
        if (img_md5 != null) {
            byte[] imgMd5 = ConvUtils.hexStrToBytes(img_md5);
            updateDoc.append("imgMd5", imgMd5);
        }
        if (isActivate != null) {
            updateDoc.append("isActivate", isActivate);
        }
        if (isDisabled != null) {
            updateDoc.append("isDisabled", isDisabled);
        }
        if (isBroken != null) {
            updateDoc.append("isBroken", isBroken);
        }
        if (isInSimPool != null) {
            updateDoc.append("isInSimPool", isInSimPool);
        }
        if (isInUsed != null) {
            updateDoc.append("isInUsed", isInUsed);
        }
        if (bindUserId != null) {
            updateDoc.append("bindUserId", bindUserId);
        }

        if (updateDoc.size() > 0) {
            UpdateResult result = tblSimCards.updateOne(eq("imsi", imsi), new Document("$set", updateDoc));
            if (result == null || result.getMatchedCount() != 1) {
                logger.warn("dbSimUpdate [" + imsi + "] failed!!!");
                return false;
            }
        }

        return true;*/
    }

    public static boolean dbUserUpdate(String userId, String newUserId, String password, Integer status) {
        return SpringContextHandler.getAutoTestMongoDBService().dbUserUpdate(userId,newUserId,password,status).handResult();
       /* MongoCollection<Document> tblClients = getDBClient()
                .getDatabase(DB_CRM)
                .getCollection(Tbl_clients);

        Document updateDoc = new Document();
        if (newUserId != null) {
            updateDoc.append("userId", newUserId);
        }
        if (password != null) {
            updateDoc.append("password", password);
        }
        if (status != null) {
            updateDoc.append("status", status);
        }

        if (updateDoc.size() > 0) {
            UpdateResult result = tblClients.updateOne(eq("userId", userId), new Document("$set", updateDoc));
            if (result == null || result.getMatchedCount() != 1) {
                logger.warn("dbUserUpdate [" + userId + "] failed!!!");
                return false;
            }
        }

        return true;*/
    }
}
