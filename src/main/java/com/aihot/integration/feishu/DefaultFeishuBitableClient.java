package com.aihot.integration.feishu;

import com.aihot.common.exception.FeishuStorageException;
import com.aihot.config.properties.FeishuBaseProperties;
import com.aihot.config.properties.FeishuProperties;
import com.lark.oapi.Client;
import com.lark.oapi.service.bitable.v1.model.AppTableRecord;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordReq;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordReqBody;
import com.lark.oapi.service.bitable.v1.model.Condition;
import com.lark.oapi.service.bitable.v1.model.FilterInfo;
import com.lark.oapi.service.bitable.v1.model.SearchAppTableRecordReq;
import com.lark.oapi.service.bitable.v1.model.SearchAppTableRecordReqBody;
import com.lark.oapi.service.bitable.v1.model.SearchAppTableRecordResp;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DefaultFeishuBitableClient implements FeishuBitableClient {

    private final Client feishuClient;
    private final FeishuProperties feishuProperties;

    public DefaultFeishuBitableClient(Client feishuClient, FeishuProperties feishuProperties) {
        this.feishuClient = feishuClient;
        this.feishuProperties = feishuProperties;
    }

    @Override
    public Set<String> findExistingFieldValues(Set<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        FeishuBaseProperties base = requireBaseConfig();
        int chunkSize = Math.max(1, base.getDedupChunkSize());
        Set<String> existing = new HashSet<>();
        List<String> valueList = List.copyOf(values);

        for (int i = 0; i < valueList.size(); i += chunkSize) {
            List<String> chunk = valueList.subList(i, Math.min(i + chunkSize, valueList.size()));
            existing.addAll(searchChunk(base, fieldName, chunk));
        }
        return existing;
    }

    @Override
    public void batchCreateRecords(List<Map<String, Object>> recordFieldsList) {
        if (recordFieldsList == null || recordFieldsList.isEmpty()) {
            return;
        }
        FeishuBaseProperties base = requireBaseConfig();
        AppTableRecord[] records = recordFieldsList.stream()
                .map(fields -> AppTableRecord.newBuilder().fields(fields).build())
                .toArray(AppTableRecord[]::new);

        try {
            BatchCreateAppTableRecordReq req = BatchCreateAppTableRecordReq.newBuilder()
                    .appToken(base.getAppToken())
                    .tableId(base.getTableId())
                    .batchCreateAppTableRecordReqBody(BatchCreateAppTableRecordReqBody.newBuilder()
                            .records(records)
                            .build())
                    .build();
            var resp = feishuClient.bitable().v1().appTableRecord().batchCreate(req);
            if (!resp.success()) {
                throw new FeishuStorageException(
                        "批量写入 Base 失败: code=%d msg=%s".formatted(resp.getCode(), resp.getMsg()));
            }
        } catch (FeishuStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FeishuStorageException("批量写入 Base 异常", e);
        }
    }

    private Set<String> searchChunk(FeishuBaseProperties base, String fieldName, List<String> values) {
        Set<String> found = new HashSet<>();
        String pageToken = null;
        do {
            SearchAppTableRecordResp resp = searchOnce(base, fieldName, values, pageToken);
            if (resp.getData() != null && resp.getData().getItems() != null) {
                for (var item : resp.getData().getItems()) {
                    Object value = item.getFields() != null ? item.getFields().get(fieldName) : null;
                    String fieldValue = extractTextValue(value);
                    if (fieldValue != null) {
                        found.add(fieldValue);
                    }
                }
            }
            pageToken = resp.getData() != null ? resp.getData().getPageToken() : null;
            boolean hasMore = resp.getData() != null && Boolean.TRUE.equals(resp.getData().getHasMore());
            if (!hasMore) {
                break;
            }
        } while (pageToken != null);
        return found;
    }

    private SearchAppTableRecordResp searchOnce(
            FeishuBaseProperties base, String fieldName, List<String> values, String pageToken) {
        Condition[] conditions = values.stream()
                .map(value -> Condition.newBuilder()
                        .fieldName(fieldName)
                        .operator("is")
                        .value(new String[] {value})
                        .build())
                .toArray(Condition[]::new);

        try {
            SearchAppTableRecordReq req = SearchAppTableRecordReq.newBuilder()
                    .appToken(base.getAppToken())
                    .tableId(base.getTableId())
                    .pageToken(pageToken)
                    .pageSize(500)
                    .searchAppTableRecordReqBody(SearchAppTableRecordReqBody.newBuilder()
                            .fieldNames(new String[] {fieldName})
                            .filter(FilterInfo.newBuilder()
                                    .conjunction("or")
                                    .conditions(conditions)
                                    .build())
                            .automaticFields(false)
                            .build())
                    .build();
            SearchAppTableRecordResp resp = feishuClient.bitable().v1().appTableRecord().search(req);
            if (!resp.success()) {
                throw new FeishuStorageException(
                        "查询 Base 去重失败: code=%d msg=%s".formatted(resp.getCode(), resp.getMsg()));
            }
            return resp;
        } catch (FeishuStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FeishuStorageException("查询 Base 去重异常", e);
        }
    }

    private FeishuBaseProperties requireBaseConfig() {
        FeishuBaseProperties base = feishuProperties.getBase();
        if (!base.isConfigured()) {
            throw new FeishuStorageException(
                    "飞书 Base 未配置，请在 feishu.yaml 中设置 feishu.base.app-token 与 table-id");
        }
        return base;
    }

    static String extractTextValue(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            return s;
        }
        if (raw instanceof Number n) {
            return n.toString();
        }
        if (raw instanceof List<?> list && !list.isEmpty()) {
            Object first = list.getFirst();
            if (first instanceof Map<?, ?> map && map.get("text") != null) {
                return map.get("text").toString();
            }
        }
        return raw.toString();
    }
}
