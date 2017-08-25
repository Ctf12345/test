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
 *         &lt;element ref="{}InitDB"/>
 *         &lt;element ref="{}ConfigDB"/>
 *         &lt;element ref="{}TestSet" maxOccurs="unbounded"/>
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
    "initDB",
    "configDB",
    "testSet"
})
@XmlRootElement(name = "TestPlan")
public class TestPlan
    implements Locatable
{

    @XmlElement(name = "InitDB", required = true)
    protected InitDB initDB;
    @XmlElement(name = "ConfigDB", required = true)
    protected ConfigDB configDB;
    @XmlElement(name = "TestSet", required = true)
    protected List<TestSet> testSet;
    @XmlAttribute(name = "desc")
    protected String desc;
    @XmlLocation
    @XmlTransient
    protected Locator locator;

    /**
     * 获取initDB属性的值。
     * 
     * @return
     *     possible object is
     *     {@link InitDB }
     *     
     */
    public InitDB getInitDB() {
        return initDB;
    }

    /**
     * 设置initDB属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link InitDB }
     *     
     */
    public void setInitDB(InitDB value) {
        this.initDB = value;
    }

    /**
     * 获取configDB属性的值。
     * 
     * @return
     *     possible object is
     *     {@link ConfigDB }
     *     
     */
    public ConfigDB getConfigDB() {
        return configDB;
    }

    /**
     * 设置configDB属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link ConfigDB }
     *     
     */
    public void setConfigDB(ConfigDB value) {
        this.configDB = value;
    }

    /**
     * Gets the value of the testSet property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the testSet property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTestSet().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TestSet }
     * 
     * 
     */
    public List<TestSet> getTestSet() {
        if (testSet == null) {
            testSet = new ArrayList<TestSet>();
        }
        return this.testSet;
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
