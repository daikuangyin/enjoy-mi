package cn.enjoy.mall.model;

import java.io.Serializable;
import java.util.List;

public class Spec implements Serializable {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column tp_spec.id
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    private Integer id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column tp_spec.type_id
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    private Integer typeId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column tp_spec.name
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    private String name;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column tp_spec.order
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    private Integer order;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column tp_spec.search_index
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    private Boolean searchIndex;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column tp_spec.id
     *
     * @return the value of tp_spec.id
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public Integer getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column tp_spec.id
     *
     * @param id the value for tp_spec.id
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column tp_spec.type_id
     *
     * @return the value of tp_spec.type_id
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public Integer getTypeId() {
        return typeId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column tp_spec.type_id
     *
     * @param typeId the value for tp_spec.type_id
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column tp_spec.name
     *
     * @return the value of tp_spec.name
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public String getName() {
        return name;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column tp_spec.name
     *
     * @param name the value for tp_spec.name
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column tp_spec.order
     *
     * @return the value of tp_spec.order
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public Integer getOrder() {
        return order;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column tp_spec.order
     *
     * @param order the value for tp_spec.order
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public void setOrder(Integer order) {
        this.order = order;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column tp_spec.search_index
     *
     * @return the value of tp_spec.search_index
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public Boolean getSearchIndex() {
        return searchIndex;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column tp_spec.search_index
     *
     * @param searchIndex the value for tp_spec.search_index
     *
     * @mbggenerated Wed Feb 07 09:55:47 CST 2018
     */
    public void setSearchIndex(Boolean searchIndex) {
        this.searchIndex = searchIndex;
    }

    private List<SpecItem> specItemList; //规格项

    public List<SpecItem> getSpecItemList() {
        return specItemList;
    }

    public void setSpecItemList(List<SpecItem> specItemList) {
        this.specItemList = specItemList;
    }
}