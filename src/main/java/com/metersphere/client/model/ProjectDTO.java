package com.metersphere.client.model;



public class ProjectDTO {
    private String id;
    private String name;
    private String workspaceId;
    private String workspaceName;
    private String description;
    private Long createTime;
    private Long updateTime;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getDescription() {
        return description;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}
