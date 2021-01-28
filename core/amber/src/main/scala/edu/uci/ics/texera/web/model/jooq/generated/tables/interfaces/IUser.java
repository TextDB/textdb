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
public interface IUser extends Serializable {

    /**
     * Setter for <code>texera_db.user.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>texera_db.user.name</code>.
     */
    public String getName();

    /**
     * Setter for <code>texera_db.user.uid</code>.
     */
    public void setUid(UInteger value);

    /**
     * Getter for <code>texera_db.user.uid</code>.
     */
    public UInteger getUid();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface IUser
     */
    public void from(IUser from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface IUser
     */
    public <E extends IUser> E into(E into);
}
