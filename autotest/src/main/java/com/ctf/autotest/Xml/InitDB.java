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
 *       &lt;attribute name="user_count" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="10" />
 *       &lt;attribute name="simpool_count" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="3" />
 *       &lt;attribute name="simcard_count" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="10" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "InitDB")
public class InitDB
    implements Locatable
{

    @XmlAttribute(name = "user_count")
    @XmlSchemaType(name = "unsignedInt")
    protected Long userCount;
    @XmlAttribute(name = "simpool_count")
    @XmlSchemaType(name = "unsignedInt")
    protected Long simpoolCount;
    @XmlAttribute(name = "simcard_count")
    @XmlSchemaType(name = "unsignedInt")
    protected Long simcardCount;
    @XmlLocation
    @XmlTransient
    protected Locator locator;

    /**
     * 获取userCount属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getUserCount() {
        if (userCount == null) {
            return  10L;
        } else {
            return userCount;
        }
    }

    /**
     * 设置userCount属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setUserCount(Long value) {
        this.userCount = value;
    }

    /**
     * 获取simpoolCount属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getSimpoolCount() {
        if (simpoolCount == null) {
            return  3L;
        } else {
            return simpoolCount;
        }
    }

    /**
     * 设置simpoolCount属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSimpoolCount(Long value) {
        this.simpoolCount = value;
    }

    /**
     * 获取simcardCount属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getSimcardCount() {
        if (simcardCount == null) {
            return  10L;
        } else {
            return simcardCount;
        }
    }

    /**
     * 设置simcardCount属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSimcardCount(Long value) {
        this.simcardCount = value;
    }

    public Locator sourceLocation() {
        return locator;
    }

    public void setSourceLocation(Locator newLocator) {
        locator = newLocator;
    }

}
