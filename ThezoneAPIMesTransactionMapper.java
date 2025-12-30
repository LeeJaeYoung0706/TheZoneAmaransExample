package com.lnk.prinics.system.thezoneApi;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ThezoneAPIMesTransactionMapper {
    int saveItem(Map<String, Object> paramMap);
    int failUpdate(Map<String, Object> paramMap);
    int saveOrder(Map<String, Object> paramMap);
    List<Map<String, Object>> selectErpItemInfo(Map<String, Object> param);
    List<Map<String, Object>> searchOrder(Map<String, Object> param);

}
