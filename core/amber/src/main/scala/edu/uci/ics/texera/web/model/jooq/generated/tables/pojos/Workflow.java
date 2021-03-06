/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.pojos;


import edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IWorkflow;

import java.sql.Timestamp;

import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Workflow implements IWorkflow {

    private static final long serialVersionUID = -1716659515;

    private String    name;
    private UInteger  wid;
    private String    content;
    private Timestamp creationTime;
    private Timestamp lastModifiedTime;

    public Workflow() {}

    public Workflow(IWorkflow value) {
        this.name = value.getName();
        this.wid = value.getWid();
        this.content = value.getContent();
        this.creationTime = value.getCreationTime();
        this.lastModifiedTime = value.getLastModifiedTime();
    }

    public Workflow(
        String    name,
        UInteger  wid,
        String    content,
        Timestamp creationTime,
        Timestamp lastModifiedTime
    ) {
        this.name = name;
        this.wid = wid;
        this.content = content;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public UInteger getWid() {
        return this.wid;
    }

    @Override
    public void setWid(UInteger wid) {
        this.wid = wid;
    }

    @Override
    public String getContent() {
        return this.content;
    }

    @Override
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public Timestamp getCreationTime() {
        return this.creationTime;
    }

    @Override
    public void setCreationTime(Timestamp creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public Timestamp getLastModifiedTime() {
        return this.lastModifiedTime;
    }

    @Override
    public void setLastModifiedTime(Timestamp lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Workflow (");

        sb.append(name);
        sb.append(", ").append(wid);
        sb.append(", ").append(content);
        sb.append(", ").append(creationTime);
        sb.append(", ").append(lastModifiedTime);

        sb.append(")");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IWorkflow from) {
        setName(from.getName());
        setWid(from.getWid());
        setContent(from.getContent());
        setCreationTime(from.getCreationTime());
        setLastModifiedTime(from.getLastModifiedTime());
    }

    @Override
    public <E extends IWorkflow> E into(E into) {
        into.from(this);
        return into;
    }
}
