package com.aihot.integration.feishu;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FeishuBitableClient {

    Set<String> findExistingFieldValues(Set<String> values, String fieldName);

    void batchCreateRecords(List<Map<String, Object>> recordFieldsList);
}
