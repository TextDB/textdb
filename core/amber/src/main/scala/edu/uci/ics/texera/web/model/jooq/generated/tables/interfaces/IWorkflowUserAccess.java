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
public interface IWorkflowUserAccess extends Serializable {

    /**
     * Setter for <code>texera_db.workflow_user_access.uid</code>.
     */
    public void setUid(UInteger value);

    /**
     * Getter for <code>texera_db.workflow_user_access.uid</code>.
     */
    public UInteger getUid();

    /**
     * Setter for <code>texera_db.workflow_user_access.wid</code>.
     */
    public void setWid(UInteger value);

    /**
     * Getter for <code>texera_db.workflow_user_access.wid</code>.
     */
    public UInteger getWid();

    /**
     * Setter for <code>texera_db.workflow_user_access.read_privilege</code>.
     */
    public void setReadPrivilege(Boolean value);

    /**
     * Getter for <code>texera_db.workflow_user_access.read_privilege</code>.
     */
    public Boolean getReadPrivilege();

    /**
     * Setter for <code>texera_db.workflow_user_access.write_privilege</code>.
     */
    public void setWritePrivilege(Boolean value);

    /**
     * Getter for <code>texera_db.workflow_user_access.write_privilege</code>.
     */
    public Boolean getWritePrivilege();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface IWorkflowUserAccess
     */
    public void from(edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IWorkflowUserAccess from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface IWorkflowUserAccess
     */
    public <E extends edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IWorkflowUserAccess> E into(E into);
}
