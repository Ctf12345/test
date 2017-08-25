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
 *       &lt;attribute name="mac_addr" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="[0-9a-fA-F][0-9a-fA-F](:[0-9a-fA-F][0-9a-fA-F]){5}"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="new_mac_addr">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="[0-9a-fA-F][0-9a-fA-F](:[0-9a-fA-F][0-9a-fA-F]){5}"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="capacity">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}unsignedShort">
 *             &lt;maxInclusive value="65535"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="sw_ver">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="V[0-9]\.[0-9][0-9]"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="hw_ver">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="V[0-9]\.[0-9][0-9]"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="isEnabled" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="isOnLine" type="{http://www.w3.org/2001/XMLSchema}boolean" />
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
@XmlRootElement(name = "SimPool")
public class SimPool
    implements Locatable
{

    @XmlAttribute(name = "mac_addr", required = true)
    protected String macAddr;
    @XmlAttribute(name = "new_mac_addr")
    protected String newMacAddr;
    @XmlAttribute(name = "capacity")
    protected Integer capacity;
    @XmlAttribute(name = "sw_ver")
    protected String swVer;
    @XmlAttribute(name = "hw_ver")
    protected String hwVer;
    @XmlAttribute(name = "isEnabled")
    protected Boolean isEnabled;
    @XmlAttribute(name = "isOnLine")
    protected Boolean isOnLine;
    @XmlAttribute(name = "desc")
    protected String desc;
    @XmlLocation
    @XmlTransient
    protected Locator locator;

    /**
     * 获取macAddr属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMacAddr() {
        return macAddr;
    }

    /**
     * 设置macAddr属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMacAddr(String value) {
        this.macAddr = value;
    }

    /**
     * 获取newMacAddr属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNewMacAddr() {
        return newMacAddr;
    }

    /**
     * 设置newMacAddr属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNewMacAddr(String value) {
        this.newMacAddr = value;
    }

    /**
     * 获取capacity属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getCapacity() {
        return capacity;
    }

    /**
     * 设置capacity属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setCapacity(Integer value) {
        this.capacity = value;
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
        return swVer;
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
     * 获取hwVer属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHwVer() {
        return hwVer;
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
     * 获取isEnabled属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsEnabled() {
        return isEnabled;
    }

    /**
     * 设置isEnabled属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsEnabled(Boolean value) {
        this.isEnabled = value;
    }

    /**
     * 获取isOnLine属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsOnLine() {
        return isOnLine;
    }

    /**
     * 设置isOnLine属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsOnLine(Boolean value) {
        this.isOnLine = value;
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
