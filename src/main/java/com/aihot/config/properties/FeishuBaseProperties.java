package com.aihot.config.properties;

/** 飞书多维表格（Base）连接配置，见 feishu.yaml 中 feishu.base。 */
public class FeishuBaseProperties {

    private String appToken = "";
    private String tableId = "";
    private int batchSize = 50;
    private int dedupChunkSize = 20;
    private FeishuBaseFieldMapping fields = new FeishuBaseFieldMapping();

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getDedupChunkSize() {
        return dedupChunkSize;
    }

    public void setDedupChunkSize(int dedupChunkSize) {
        this.dedupChunkSize = dedupChunkSize;
    }

    public FeishuBaseFieldMapping getFields() {
        return fields;
    }

    public void setFields(FeishuBaseFieldMapping fields) {
        this.fields = fields;
    }

    public boolean isConfigured() {
        return appToken != null && !appToken.isBlank()
                && tableId != null && !tableId.isBlank();
    }
}
