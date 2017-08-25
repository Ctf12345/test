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
 *       &lt;sequence>
 *         &lt;element ref="{}assert_expr" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="cmd_id" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="CMD_SP_SIM_IMG"/>
 *             &lt;enumeration value="CMD_SP_SIM_APDU"/>
 *             &lt;enumeration value="CMD_SP_LOG_UPLOAD"/>
 *             &lt;enumeration value="SIMPOOL"/>
 *             &lt;enumeration value="TERMINFO"/>
 *             &lt;enumeration value="TERMREVUIM"/>
 *             &lt;enumeration value="TERMLOGOUT"/>
 *             &lt;enumeration value="TERMASSIGNUIM"/>
 *             &lt;enumeration value="TERMTRACE"/>
 *             &lt;enumeration value="TERMLOGUPLOAD"/>
 *             &lt;enumeration value="TERMUPGRADE"/>
 *             &lt;enumeration value="TERMLOGUPLOAD"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="seq_no" type="{http://www.w3.org/2001/XMLSchema}unsignedShort" />
 *       &lt;attribute name="timeout" type="{http://www.w3.org/2001/XMLSchema}int" default="5" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="desc" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "assertExpr"
})
@XmlRootElement(name = "assert_req")
public class AssertReq implements Locatable
{

    @XmlElement(name = "assert_expr")
    protected List<AssertExpr> assertExpr;
    @XmlAttribute(name = "cmd_id", required = true)
    protected String cmdId;
    @XmlAttribute(name = "seq_no")
    @XmlSchemaType(name = "unsignedShort")
    protected Integer seqNo;
    @XmlAttribute(name = "timeout")
    protected Integer timeout;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "desc")
    protected String desc;
    @XmlLocation
    @XmlTransient
    protected Locator locator;

    /**
     * Gets the value of the assertExpr property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the assertExpr property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAssertExpr().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AssertExpr }
     * 
     * 
     */
    public List<AssertExpr> getAssertExpr() {
        if (assertExpr == null) {
            assertExpr = new ArrayList<AssertExpr>();
        }
        return this.assertExpr;
    }

    /**
     * 获取cmdId属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCmdId() {
        return cmdId;
    }

    /**
     * 设置cmdId属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCmdId(String value) {
        this.cmdId = value;
    }

    /**
     * 获取seqNo属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getSeqNo() {
        return seqNo;
    }

    /**
     * 设置seqNo属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setSeqNo(Integer value) {
        this.seqNo = value;
    }

    /**
     * 获取timeout属性的值。
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getTimeout() {
        if (timeout == null) {
            return  5;
        } else {
            return timeout;
        }
    }

    /**
     * 设置timeout属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setTimeout(Integer value) {
        this.timeout = value;
    }

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
