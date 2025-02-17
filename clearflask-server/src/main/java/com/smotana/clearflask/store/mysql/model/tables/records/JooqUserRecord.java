/*
 * This file is generated by jOOQ.
 */
package com.smotana.clearflask.store.mysql.model.tables.records;


import com.smotana.clearflask.store.mysql.model.tables.JooqUser;

import java.time.Instant;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record7;
import org.jooq.Row7;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "https://www.jooq.org",
        "jOOQ version:3.16.10"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class JooqUserRecord extends UpdatableRecordImpl<JooqUserRecord> implements Record7<String, String, String, String, Instant, Long, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>user.projectId</code>.
     */
    public void setProjectid(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>user.projectId</code>.
     */
    public String getProjectid() {
        return (String) get(0);
    }

    /**
     * Setter for <code>user.userId</code>.
     */
    public void setUserid(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>user.userId</code>.
     */
    public String getUserid() {
        return (String) get(1);
    }

    /**
     * Setter for <code>user.name</code>.
     */
    public void setName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>user.name</code>.
     */
    public String getName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>user.email</code>.
     */
    public void setEmail(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>user.email</code>.
     */
    public String getEmail() {
        return (String) get(3);
    }

    /**
     * Setter for <code>user.created</code>.
     */
    public void setCreated(Instant value) {
        set(4, value);
    }

    /**
     * Getter for <code>user.created</code>.
     */
    public Instant getCreated() {
        return (Instant) get(4);
    }

    /**
     * Setter for <code>user.balance</code>.
     */
    public void setBalance(Long value) {
        set(5, value);
    }

    /**
     * Getter for <code>user.balance</code>.
     */
    public Long getBalance() {
        return (Long) get(5);
    }

    /**
     * Setter for <code>user.isMod</code>.
     */
    public void setIsmod(Boolean value) {
        set(6, value);
    }

    /**
     * Getter for <code>user.isMod</code>.
     */
    public Boolean getIsmod() {
        return (Boolean) get(6);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<String, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record7 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row7<String, String, String, String, Instant, Long, Boolean> fieldsRow() {
        return (Row7) super.fieldsRow();
    }

    @Override
    public Row7<String, String, String, String, Instant, Long, Boolean> valuesRow() {
        return (Row7) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return JooqUser.USER.PROJECTID;
    }

    @Override
    public Field<String> field2() {
        return JooqUser.USER.USERID;
    }

    @Override
    public Field<String> field3() {
        return JooqUser.USER.NAME;
    }

    @Override
    public Field<String> field4() {
        return JooqUser.USER.EMAIL;
    }

    @Override
    public Field<Instant> field5() {
        return JooqUser.USER.CREATED;
    }

    @Override
    public Field<Long> field6() {
        return JooqUser.USER.BALANCE;
    }

    @Override
    public Field<Boolean> field7() {
        return JooqUser.USER.ISMOD;
    }

    @Override
    public String component1() {
        return getProjectid();
    }

    @Override
    public String component2() {
        return getUserid();
    }

    @Override
    public String component3() {
        return getName();
    }

    @Override
    public String component4() {
        return getEmail();
    }

    @Override
    public Instant component5() {
        return getCreated();
    }

    @Override
    public Long component6() {
        return getBalance();
    }

    @Override
    public Boolean component7() {
        return getIsmod();
    }

    @Override
    public String value1() {
        return getProjectid();
    }

    @Override
    public String value2() {
        return getUserid();
    }

    @Override
    public String value3() {
        return getName();
    }

    @Override
    public String value4() {
        return getEmail();
    }

    @Override
    public Instant value5() {
        return getCreated();
    }

    @Override
    public Long value6() {
        return getBalance();
    }

    @Override
    public Boolean value7() {
        return getIsmod();
    }

    @Override
    public JooqUserRecord value1(String value) {
        setProjectid(value);
        return this;
    }

    @Override
    public JooqUserRecord value2(String value) {
        setUserid(value);
        return this;
    }

    @Override
    public JooqUserRecord value3(String value) {
        setName(value);
        return this;
    }

    @Override
    public JooqUserRecord value4(String value) {
        setEmail(value);
        return this;
    }

    @Override
    public JooqUserRecord value5(Instant value) {
        setCreated(value);
        return this;
    }

    @Override
    public JooqUserRecord value6(Long value) {
        setBalance(value);
        return this;
    }

    @Override
    public JooqUserRecord value7(Boolean value) {
        setIsmod(value);
        return this;
    }

    @Override
    public JooqUserRecord values(String value1, String value2, String value3, String value4, Instant value5, Long value6, Boolean value7) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached JooqUserRecord
     */
    public JooqUserRecord() {
        super(JooqUser.USER);
    }

    /**
     * Create a detached, initialised JooqUserRecord
     */
    public JooqUserRecord(String projectid, String userid, String name, String email, Instant created, Long balance, Boolean ismod) {
        super(JooqUser.USER);

        setProjectid(projectid);
        setUserid(userid);
        setName(name);
        setEmail(email);
        setCreated(created);
        setBalance(balance);
        setIsmod(ismod);
    }
}
