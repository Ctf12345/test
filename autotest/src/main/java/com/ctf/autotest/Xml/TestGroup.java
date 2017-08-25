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
 *         &lt;element ref="{}PreAction" minOccurs="0"/>
 *         &lt;element ref="{}TestCase" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="no" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="desc" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="skip" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="status">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="未解决"/>
 *             &lt;enumeration value="设计如此"/>
 *             &lt;enumeration value="已解决"/>
 *             &lt;enumeration value="暂不修改"/>
 *             &lt;enumeration value="未实现"/>
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
@XmlType(name = "", propOrder = {
    "preAction",
    "testCase"
})
@XmlRootElement(name = "TestGroup")
public class TestGroup
    implements Locatable
{

    @XmlElement(name = "PreAction")
    protected PreAction preAction;
    @XmlElement(name = "TestCase", required = true)
    protected List<TestCase> testCase;
    @XmlAttribute(name = "no")
    protected String no;
    @XmlAttribute(name = "desc", required = true)
    protected String desc;
    @XmlAttribute(name = "skip")
    protected Boolean skip;
    @XmlAttribute(name = "status")
    protected String status;
    @XmlLocation
    @XmlTransient
    protected Locator locator;

    /**
     * 获取preAction属性的值。
     * 
     * @return
     *     possible object is
     *     {@link PreAction }
     *     
     */
    public PreAction getPreAction() {
        return preAction;
    }

    /**
     * 设置preAction属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link PreAction }
     *     
     */
    public void setPreAction(PreAction value) {
        this.preAction = value;
    }

    /**
     * Gets the value of the testCase property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the testCase property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTestCase().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TestCase }
     * 
     * 
     */
    public List<TestCase> getTestCase() {
        if (testCase == null) {
            testCase = new ArrayList<TestCase>();
        }
        return this.testCase;
    }

    /**
     * 获取no属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNo() {
        return no;
    }

    /**
     * 设置no属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNo(String value) {
        this.no = value;
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
     * 获取skip属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isSkip() {
        if (skip == null) {
            return false;
        } else {
            return skip;
        }
    }

    /**
     * 设置skip属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSkip(Boolean value) {
        this.skip = value;
    }

    /**
     * 获取status属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置status属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatus(String value) {
        this.status = value;
    }

    public Locator sourceLocation() {
        return locator;
    }

    public void setSourceLocation(Locator newLocator) {
        locator = newLocator;
    }

}
