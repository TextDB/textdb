/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.records;


import edu.uci.ics.texera.web.model.jooq.generated.tables.File;
import edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IFile;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class FileRecord extends UpdatableRecordImpl<FileRecord> implements Record6<UInteger, UInteger, UInteger, String, String, String>, IFile {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>texera_db.file.uid</code>.
     */
    @Override
    public void setUid(UInteger value) {
        set(0, value);
    }

    /**
     * Getter for <code>texera_db.file.uid</code>.
     */
    @Override
    public UInteger getUid() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>texera_db.file.fid</code>.
     */
    @Override
    public void setFid(UInteger value) {
        set(1, value);
    }

    /**
     * Getter for <code>texera_db.file.fid</code>.
     */
    @Override
    public UInteger getFid() {
        return (UInteger) get(1);
    }

    /**
     * Setter for <code>texera_db.file.size</code>.
     */
    @Override
    public void setSize(UInteger value) {
        set(2, value);
    }

    /**
     * Getter for <code>texera_db.file.size</code>.
     */
    @Override
    public UInteger getSize() {
        return (UInteger) get(2);
    }

    /**
     * Setter for <code>texera_db.file.name</code>.
     */
    @Override
    public void setName(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>texera_db.file.name</code>.
     */
    @Override
    public String getName() {
        return (String) get(3);
    }

    /**
     * Setter for <code>texera_db.file.path</code>.
     */
    @Override
    public void setPath(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>texera_db.file.path</code>.
     */
    @Override
    public String getPath() {
        return (String) get(4);
    }

    /**
     * Setter for <code>texera_db.file.description</code>.
     */
    @Override
    public void setDescription(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>texera_db.file.description</code>.
     */
    @Override
    public String getDescription() {
        return (String) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<UInteger, UInteger, UInteger, String, String, String> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<UInteger, UInteger, UInteger, String, String, String> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return File.FILE.UID;
    }

    @Override
    public Field<UInteger> field2() {
        return File.FILE.FID;
    }

    @Override
    public Field<UInteger> field3() {
        return File.FILE.SIZE;
    }

    @Override
    public Field<String> field4() {
        return File.FILE.NAME;
    }

    @Override
    public Field<String> field5() {
        return File.FILE.PATH;
    }

    @Override
    public Field<String> field6() {
        return File.FILE.DESCRIPTION;
    }

    @Override
    public UInteger component1() {
        return getUid();
    }

    @Override
    public UInteger component2() {
        return getFid();
    }

    @Override
    public UInteger component3() {
        return getSize();
    }

    @Override
    public String component4() {
        return getName();
    }

    @Override
    public String component5() {
        return getPath();
    }

    @Override
    public String component6() {
        return getDescription();
    }

    @Override
    public UInteger value1() {
        return getUid();
    }

    @Override
    public UInteger value2() {
        return getFid();
    }

    @Override
    public UInteger value3() {
        return getSize();
    }

    @Override
    public String value4() {
        return getName();
    }

    @Override
    public String value5() {
        return getPath();
    }

    @Override
    public String value6() {
        return getDescription();
    }

    @Override
    public FileRecord value1(UInteger value) {
        setUid(value);
        return this;
    }

    @Override
    public FileRecord value2(UInteger value) {
        setFid(value);
        return this;
    }

    @Override
    public FileRecord value3(UInteger value) {
        setSize(value);
        return this;
    }

    @Override
    public FileRecord value4(String value) {
        setName(value);
        return this;
    }

    @Override
    public FileRecord value5(String value) {
        setPath(value);
        return this;
    }

    @Override
    public FileRecord value6(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public FileRecord values(UInteger value1, UInteger value2, UInteger value3, String value4, String value5, String value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IFile from) {
        setUid(from.getUid());
        setFid(from.getFid());
        setSize(from.getSize());
        setName(from.getName());
        setPath(from.getPath());
        setDescription(from.getDescription());
    }

    @Override
    public <E extends IFile> E into(E into) {
        into.from(this);
        return into;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached FileRecord
     */
    public FileRecord() {
        super(File.FILE);
    }

    /**
     * Create a detached, initialised FileRecord
     */
    public FileRecord(UInteger uid, UInteger fid, UInteger size, String name, String path, String description) {
        super(File.FILE);

        setUid(uid);
        setFid(fid);
        setSize(size);
        setName(name);
        setPath(path);
        setDescription(description);
    }
}
