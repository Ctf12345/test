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
 *       &lt;attribute name="imsi" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="([0-9]){15}"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="new_imsi">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="([0-9]){15}"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="iccid">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="([0-9]){20}"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="sim_img">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="([0-9a-fA-F][0-9a-fA-F])*"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="img_md5">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="(([0-9a-fA-F]){32})?"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="isActivate" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="isDisabled" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="isBroken" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="isInSimpool" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="isInUsed" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="bindUserId" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="desc" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "SimCard")
public class SimCard
    implements Locatable
{

    @XmlAttribute(name = "imsi", required = true)
    protected String imsi;
    @XmlAttribute(name = "new_imsi")
    protected String newImsi;
    @XmlAttribute(name = "iccid")
    protected String iccid;
    @XmlAttribute(name = "sim_img")
    protected String simImg;
    @XmlAttribute(name = "img_md5")
    protected String imgMd5;
    @XmlAttribute(name = "isActivate")
    protected Boolean isActivate;
    @XmlAttribute(name = "isDisabled")
    protected Boolean isDisabled;
    @XmlAttribute(name = "isBroken")
    protected Boolean isBroken;
    @XmlAttribute(name = "isInSimpool")
    protected Boolean isInSimpool;
    @XmlAttribute(name = "isInUsed")
    protected Boolean isInUsed;
    @XmlAttribute(name = "bindUserId")
    protected String bindUserId;
    @XmlAttribute(name = "desc")
    protected String desc;
    @XmlLocation
    @XmlTransient
    protected Locator locator;

    /**
     * 获取imsi属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImsi() {
        return imsi;
    }

    /**
     * 设置imsi属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImsi(String value) {
        this.imsi = value;
    }

    /**
     * 获取newImsi属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNewImsi() {
        return newImsi;
    }

    /**
     * 设置newImsi属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNewImsi(String value) {
        this.newImsi = value;
    }

    /**
     * 获取iccid属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIccid() {
        return iccid;
    }

    /**
     * 设置iccid属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIccid(String value) {
        this.iccid = value;
    }

    /**
     * 获取simImg属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSimImg() {
        return simImg;
    }

    /**
     * 设置simImg属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSimImg(String value) {
        this.simImg = value;
    }

    /**
     * 获取imgMd5属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImgMd5() {
        return imgMd5;
    }

    /**
     * 设置imgMd5属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImgMd5(String value) {
        this.imgMd5 = value;
    }

    /**
     * 获取isActivate属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsActivate() {
        return isActivate;
    }

    /**
     * 设置isActivate属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsActivate(Boolean value) {
        this.isActivate = value;
    }

    /**
     * 获取isDisabled属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsDisabled() {
        return isDisabled;
    }

    /**
     * 设置isDisabled属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsDisabled(Boolean value) {
        this.isDisabled = value;
    }

    /**
     * 获取isBroken属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsBroken() {
        return isBroken;
    }

    /**
     * 设置isBroken属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsBroken(Boolean value) {
        this.isBroken = value;
    }

    /**
     * 获取isInSimpool属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsInSimpool() {
        return isInSimpool;
    }

    /**
     * 设置isInSimpool属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsInSimpool(Boolean value) {
        this.isInSimpool = value;
    }

    /**
     * 获取isInUsed属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsInUsed() {
        return isInUsed;
    }

    /**
     * 设置isInUsed属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsInUsed(Boolean value) {
        this.isInUsed = value;
    }

    /**
     * 获取bindUserId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBindUserId() {
        return bindUserId;
    }

    /**
     * 设置bindUserId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBindUserId(String value) {
        this.bindUserId = value;
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

    public Locator sourceLocation() {
        return locator;
    }

    public void setSourceLocation(Locator newLocator) {
        locator = newLocator;
    }

}
