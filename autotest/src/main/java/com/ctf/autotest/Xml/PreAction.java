//
// 此文件是由 JavaTM Architecture for XML Binding (JAXB) 引用实现 v2.2.8-b130911.1802 生成的
// 请访问 <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 在重新编译源模式时, 对此文件的所有修改都将丢失。
// 生成时间: 2017.08.17 时间 04:05:11 PM CST 
//


package com.ctf.autotest.Xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import com.sun.xml.internal.bind.Locatable;
import com.sun.xml.internal.bind.annotation.XmlLocation;
import org.xml.sax.Locator;


/**
 * <p>anonymous complex type的 Java 类。
 * 
 * <p>以下模式片段指定包含在此类中的预期内容。
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{}actions"/>
 *       &lt;/sequence>
 *       &lt;attribute name="desc" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "sleepOrConnsvrOrDisconn"
})
@XmlRootElement(name = "PreAction")
public class PreAction
    implements Locatable
{

    @XmlElements({
        @XmlElement(name = "sleep", type = Sleep.class),
        @XmlElement(name = "connsvr", type = Connsvr.class),
        @XmlElement(name = "disconn", type = Disconn.class),
        @XmlElement(name = "config", type = Config.class),
        @XmlElement(name = "exec", type = Exec.class),
        @XmlElement(name = "assert_rsp", type = AssertRsp.class),
        @XmlElement(name = "assert_req", type = AssertReq.class),
        @XmlElement(name = "assert_no_rsp", type = AssertNoRsp.class),
        @XmlElement(name = "assert_no_req", type = AssertNoReq.class),
        @XmlElement(name = "mt_login", type = MtLogin.class),
        @XmlElement(name = "mt_loginbyid", type = MtLoginbyid.class),
        @XmlElement(name = "mt_requim", type = MtRequim.class),
        @XmlElement(name = "mt_simimg", type = MtSimimg.class),
        @XmlElement(name = "mt_simpool", type = MtSimpool.class),
        @XmlElement(name = "mt_revuim", type = MtRevuim.class),
        @XmlElement(name = "mt_logout", type = MtLogout.class),
        @XmlElement(name = "mt_alert", type = MtAlert.class),
        @XmlElement(name = "mt_trace", type = MtTrace.class),
        @XmlElement(name = "mt_heartbeat", type = MtHeartbeat.class),
        @XmlElement(name = "mt_loguploadret", type = MtLoguploadret.class),
        @XmlElement(name = "TermMtRevUim_Rsp", type = TermMtRevUimRsp.class),
        @XmlElement(name = "TermMtLogout_Rsp", type = TermMtLogoutRsp.class),
        @XmlElement(name = "TermMtAssignUim_Rsp", type = TermMtAssignUimRsp.class),
        @XmlElement(name = "TermMtTrace_Rsp", type = TermMtTraceRsp.class),
        @XmlElement(name = "TermMtLogupload_Rsp", type = TermMtLoguploadRsp.class),
        @XmlElement(name = "TermSPLogupload_Rsp", type = TermSPLoguploadRsp.class),
        @XmlElement(name = "sp_login", type = SpLogin.class),
        @XmlElement(name = "sp_siminfo", type = SpSiminfo.class),
        @XmlElement(name = "sp_siminfo_update", type = SpSiminfoUpdate.class),
        @XmlElement(name = "sp_heartbeat", type = SpHeartbeat.class),
        @XmlElement(name = "sp_loguploadres", type = SpLoguploadres.class)
    })
    protected List<Object> sleepOrConnsvrOrDisconn;
    @XmlAttribute(name = "desc")
    protected String desc;
    @XmlLocation
    @XmlTransient
    protected Locator locator;

    /**
     * Gets the value of the sleepOrConnsvrOrDisconn property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sleepOrConnsvrOrDisconn property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSleepOrConnsvrOrDisconn().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Sleep }
     * {@link Connsvr }
     * {@link Disconn }
     * {@link Config }
     * {@link Exec }
     * {@link AssertRsp }
     * {@link AssertReq }
     * {@link AssertNoRsp }
     * {@link AssertNoReq }
     * {@link MtLogin }
     * {@link MtLoginbyid }
     * {@link MtRequim }
     * {@link MtSimimg }
     * {@link MtSimpool }
     * {@link MtRevuim }
     * {@link MtLogout }
     * {@link MtAlert }
     * {@link MtTrace }
     * {@link MtHeartbeat }
     * {@link MtLoguploadret }
     * {@link TermMtRevUimRsp }
     * {@link TermMtLogoutRsp }
     * {@link TermMtAssignUimRsp }
     * {@link TermMtTraceRsp }
     * {@link TermMtLoguploadRsp }
     * {@link TermSPLoguploadRsp }
     * {@link SpLogin }
     * {@link SpSiminfo }
     * {@link SpSiminfoUpdate }
     * {@link SpHeartbeat }
     * {@link SpLoguploadres }
     * 
     * 
     */
    public List<Object> getSleepOrConnsvrOrDisconn() {
        if (sleepOrConnsvrOrDisconn == null) {
            sleepOrConnsvrOrDisconn = new ArrayList<Object>();
        }
        return this.sleepOrConnsvrOrDisconn;
    }

    /**
     * 获取desc属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDesc() {
        if (desc == null) {
            return "";
        } else {
            return desc;
        }
    }

    /**
     * 设置desc属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDesc(String value) {
        this.desc = value;
    }

    public Locator sourceLocation() {
        return locator;
    }

    public void setSourceLocation(Locator newLocator) {
        locator = newLocator;
    }

}
