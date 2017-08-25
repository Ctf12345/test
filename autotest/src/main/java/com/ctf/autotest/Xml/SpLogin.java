//
// 此文件是由 JavaTM Architecture for XML Binding (JAXB) 引用实现 v2.2.8-b130911.1802 生成的
// 请访问 <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 在重新编译源模式时, 对此文件的所有修改都将丢失。
// 生成时间: 2017.08.17 时间 04:05:11 PM CST 
//


package com.ctf.autotest.Xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
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
 *       &lt;attribute name="id">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="SP.+"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="waitRsp" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="desc" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="mac_hexstr" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="[0-9a-fA-F][0-9a-fA-F](:[0-9a-fA-F][0-9a-fA-F]){5}"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="max_sim_count" type="{http://www.w3.org/2001/XMLSchema}unsignedShort" />
 *       &lt;attribute name="hw_ver" default="1.00">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="(.){4}"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="sw_ver" default="1.00">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="(.){4}"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="expect_ret" type="{http://www.w3.org/2001/XMLSchema}string" default="0" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "sp_login")
public class SpLogin implements Locatable
{

    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "waitRsp")
    protected Boolean waitRsp;
    @XmlAttribute(name = "desc")
    protected String desc;
    @XmlAttribute(name = "mac_hexstr", required = true)
    protected String macHexstr;
    @XmlAttribute(name = "max_sim_count")
    @XmlSchemaType(name = "unsignedShort")
    protected Integer maxSimCount;
    @XmlAttribute(name = "hw_ver")
    protected String hwVer;
    @XmlAttribute(name = "sw_ver")
    protected String swVer;
    @XmlAttribute(name = "expect_ret")
    protected String expectRet;
    @XmlLocation
    @XmlTransient
    protected Locator locator;

    /**
     * 获取id属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * 设置id属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * 获取waitRsp属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isWaitRsp() {
        if (waitRsp == null) {
            return true;
        } else {
            return waitRsp;
        }
    }

    /**
     * 设置waitRsp属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWaitRsp(Boolean value) {
        this.waitRsp = value;
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
        return desc;
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

    /**
     * 获取macHexstr属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMacHexstr() {
        return macHexstr;
    }

    /**
     * 设置macHexstr属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMacHexstr(String value) {
        this.macHexstr = value;
    }

    /**
     * 获取maxSimCount属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMaxSimCount() {
        return maxSimCount;
    }

    /**
     * 设置maxSimCount属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMaxSimCount(Integer value) {
        this.maxSimCount = value;
    }

    /**
     * 获取hwVer属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHwVer() {
        if (hwVer == null) {
            return "1.00";
        } else {
            return hwVer;
        }
    }

    /**
     * 设置hwVer属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHwVer(String value) {
        this.hwVer = value;
    }

    /**
     * 获取swVer属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSwVer() {
        if (swVer == null) {
            return "1.00";
        } else {
            return swVer;
        }
    }

    /**
     * 设置swVer属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSwVer(String value) {
        this.swVer = value;
    }

    /**
     * 获取expectRet属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExpectRet() {
        if (expectRet == null) {
            return "0";
        } else {
            return expectRet;
        }
    }

    /**
     * 设置expectRet属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExpectRet(String value) {
        this.expectRet = value;
    }

    public Locator sourceLocation() {
        return locator;
    }

    public void setSourceLocation(Locator newLocator) {
        locator = newLocator;
    }

}
