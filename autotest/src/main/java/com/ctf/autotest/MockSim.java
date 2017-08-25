package com.ctf.autotest;

import com.ctf.ass_public.struct.SimCard;
import com.ctf.ass_public.utils.CheckUtils;
import com.ctf.ass_public.utils.ConvUtils;


public class MockSim {
    private String iccid;
    private String imsi;
    private SimStatus status;
    private byte[] sim_image;
    private byte[] img_md5;
    private String desc;

    public MockSim(String imsi) {
        this.imsi = imsi;
        SimCard simCard = MongoDbUtil.getDbSimCard(imsi);
        if (simCard != null) {
            this.iccid = simCard.getIccid();
            this.status = simCard.isBroken() ? SimStatus.Broken : (simCard.isInSimpool() ? SimStatus.NotInSp : SimStatus.Normal);
            this.sim_image = simCard.getSimcardImage();
            this.img_md5 = simCard.getImgMd5();
        } else {
            this.iccid = null;
            this.status = null;
            this.sim_image = null;
            this.img_md5 = null;
        }
        this.desc = "";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MockSim {");
        sb.append("iccid:" + iccid + ",");
        sb.append("imsi:" + imsi + ",");
        sb.append("status:" + status + ",");
        sb.append("sim_image:" + ConvUtils.bytesToHexStr(sim_image) + ",");
        sb.append("img_md5:" + ConvUtils.bytesToHexStr(img_md5) + ",");
        sb.append("desc:" + desc);
        sb.append("}");

        return sb.toString();
    }

    public String getMissingField() {
        if (iccid == null || iccid.equals("")) {
            return "iccid";
        }

        if (status == null) {
            return "status";
        }

        if (sim_image == null || sim_image.length == 0) {
            return "sim_image";
        }

        if (img_md5 == null || img_md5.length == 0) {
            return "img_md5";
        }

        return null;
    }

    //Sim卡ICCID
    public String getIccid() {
        return iccid;
    }
    public void setIccid(String iccid) {
        this.iccid = iccid;
    }
    //Sim卡IMSI
    public String getImsi() {
        return imsi;
    }
    public void setImsi(String imsi) {
        this.imsi = imsi;
    }
    //卡状态
    public SimStatus getStatus() {
        return status;
    }
    public void setStatus(SimStatus status) {
        this.status = status;
    }
    //sim卡镜像
    public byte[] getSimImage() {
        return sim_image;
    }
    public void setSimImage(byte[] sim_image) {
        this.sim_image = sim_image;
        this.img_md5 = CheckUtils.MD5(sim_image);
    }
    public byte[] getImgMd5() {
        return img_md5;
    }
    public void setImgMd5(byte[] img_md5) {
        this.img_md5 = img_md5;
    }

    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public enum SimStatus {
        Normal(0),      //正常
        Broken(1),      //坏卡
        NotInSp(2);       //无卡

        int value;
        SimStatus(int value) {
            this.value = value;
        }
    }
}
