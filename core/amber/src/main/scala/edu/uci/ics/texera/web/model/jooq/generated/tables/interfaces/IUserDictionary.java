/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces;


import java.io.Serializable;

import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public interface IUserDictionary extends Serializable {

    /**
     * Setter for <code>texera_db.user_dictionary.uid</code>.
     */
    public void setUid(UInteger value);

    /**
     * Getter for <code>texera_db.user_dictionary.uid</code>.
     */
    public UInteger getUid();

    /**
     * Setter for <code>texera_db.user_dictionary.key</code>.
     */
    public void setKey(String value);

    /**
     * Getter for <code>texera_db.user_dictionary.key</code>.
     */
    public String getKey();

    /**
     * Setter for <code>texera_db.user_dictionary.value</code>.
     */
    public void setValue(String value);

    /**
     * Getter for <code>texera_db.user_dictionary.value</code>.
     */
    public String getValue();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface IUserDictionary
     */
    public void from(edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IUserDictionary from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface IUserDictionary
     */
    public <E extends edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IUserDictionary> E into(E into);
}
