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
 *       &lt;attribute name="id">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="MT.+"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="waitRsp" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="desc" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="cells" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="[0-9]+,[0-9](;[0-9]+,[0-9])+"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="imsi">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="$.+|([0-9]){15}|.*"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="expect_ret" type="{http://www.w3.org/2001/XMLSchema}string" default="ERR_OK" />
 *       &lt;attribute name="expect_imsi">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="$.+|([0-9]){1,15}"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="expect_img_md5">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="$.+|(([0-9a-fA-F]){32})"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "mt_requim")
public class MtRequim implements Locatable
{

    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "waitRsp")
    protected Boolean waitRsp;
    @XmlAttribute(name = "desc")
    protected String desc;
    @XmlAttribute(name = "cells", required = true)
    protected String cells;
    @XmlAttribute(name = "imsi")
    protected String imsi;
    @XmlAttribute(name = "expect_ret")
    protected String expectRet;
    @XmlAttribute(name = "expect_imsi")
    protected String expectImsi;
    @XmlAttribute(name = "expect_img_md5")
    protected String expectImgMd5;
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
     * 获取cells属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCells() {
        return cells;
    }

    /**
     * 设置cells属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCells(String value) {
        this.cells = value;
    }

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
     * 获取expectRet属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExpectRet() {
        if (expectRet == null) {
            return "ERR_OK";
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

    /**
     * 获取expectImsi属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExpectImsi() {
        return expectImsi;
    }

    /**
     * 设置expectImsi属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExpectImsi(String value) {
        this.expectImsi = value;
    }

    /**
     * 获取expectImgMd5属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExpectImgMd5() {
        return expectImgMd5;
    }

    /**
     * 设置expectImgMd5属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExpectImgMd5(String value) {
        this.expectImgMd5 = value;
    }

    public Locator sourceLocation() {
        return locator;
    }

    public void setSourceLocation(Locator newLocator) {
        locator = newLocator;
    }

}
