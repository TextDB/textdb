/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.records;


import edu.uci.ics.texera.web.model.jooq.generated.tables.KeywordDictionary;
import edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IKeywordDictionary;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class KeywordDictionaryRecord extends UpdatableRecordImpl<KeywordDictionaryRecord> implements Record5<UInteger, UInteger, String, byte[], String>, IKeywordDictionary {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>texera_db.keyword_dictionary.uid</code>.
     */
    @Override
    public void setUid(UInteger value) {
        set(0, value);
    }

    /**
     * Getter for <code>texera_db.keyword_dictionary.uid</code>.
     */
    @Override
    public UInteger getUid() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>texera_db.keyword_dictionary.kid</code>.
     */
    @Override
    public void setKid(UInteger value) {
        set(1, value);
    }

    /**
     * Getter for <code>texera_db.keyword_dictionary.kid</code>.
     */
    @Override
    public UInteger getKid() {
        return (UInteger) get(1);
    }

    /**
     * Setter for <code>texera_db.keyword_dictionary.name</code>.
     */
    @Override
    public void setName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>texera_db.keyword_dictionary.name</code>.
     */
    @Override
    public String getName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>texera_db.keyword_dictionary.content</code>.
     */
    @Override
    public void setContent(byte[] value) {
        set(3, value);
    }

    /**
     * Getter for <code>texera_db.keyword_dictionary.content</code>.
     */
    @Override
    public byte[] getContent() {
        return (byte[]) get(3);
    }

    /**
     * Setter for <code>texera_db.keyword_dictionary.description</code>.
     */
    @Override
    public void setDescription(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>texera_db.keyword_dictionary.description</code>.
     */
    @Override
    public String getDescription() {
        return (String) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<UInteger, UInteger, String, byte[], String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<UInteger, UInteger, String, byte[], String> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return KeywordDictionary.KEYWORD_DICTIONARY.UID;
    }

    @Override
    public Field<UInteger> field2() {
        return KeywordDictionary.KEYWORD_DICTIONARY.KID;
    }

    @Override
    public Field<String> field3() {
        return KeywordDictionary.KEYWORD_DICTIONARY.NAME;
    }

    @Override
    public Field<byte[]> field4() {
        return KeywordDictionary.KEYWORD_DICTIONARY.CONTENT;
    }

    @Override
    public Field<String> field5() {
        return KeywordDictionary.KEYWORD_DICTIONARY.DESCRIPTION;
    }

    @Override
    public UInteger component1() {
        return getUid();
    }

    @Override
    public UInteger component2() {
        return getKid();
    }

    @Override
    public String component3() {
        return getName();
    }

    @Override
    public byte[] component4() {
        return getContent();
    }

    @Override
    public String component5() {
        return getDescription();
    }

    @Override
    public UInteger value1() {
        return getUid();
    }

    @Override
    public UInteger value2() {
        return getKid();
    }

    @Override
    public String value3() {
        return getName();
    }

    @Override
    public byte[] value4() {
        return getContent();
    }

    @Override
    public String value5() {
        return getDescription();
    }

    @Override
    public KeywordDictionaryRecord value1(UInteger value) {
        setUid(value);
        return this;
    }

    @Override
    public KeywordDictionaryRecord value2(UInteger value) {
        setKid(value);
        return this;
    }

    @Override
    public KeywordDictionaryRecord value3(String value) {
        setName(value);
        return this;
    }

    @Override
    public KeywordDictionaryRecord value4(byte[] value) {
        setContent(value);
        return this;
    }

    @Override
    public KeywordDictionaryRecord value5(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public KeywordDictionaryRecord values(UInteger value1, UInteger value2, String value3, byte[] value4, String value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IKeywordDictionary from) {
        setUid(from.getUid());
        setKid(from.getKid());
        setName(from.getName());
        setContent(from.getContent());
        setDescription(from.getDescription());
    }

    @Override
    public <E extends IKeywordDictionary> E into(E into) {
        into.from(this);
        return into;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached KeywordDictionaryRecord
     */
    public KeywordDictionaryRecord() {
        super(KeywordDictionary.KEYWORD_DICTIONARY);
    }

    /**
     * Create a detached, initialised KeywordDictionaryRecord
     */
    public KeywordDictionaryRecord(UInteger uid, UInteger kid, String name, byte[] content, String description) {
        super(KeywordDictionary.KEYWORD_DICTIONARY);

        setUid(uid);
        setKid(kid);
        setName(name);
        setContent(content);
        setDescription(description);
    }
}
