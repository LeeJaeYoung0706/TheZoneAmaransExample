package com.lnk.prinics.system.thezoneApi;

import com.lnk.prinics.exception.CustomResponseException;
import com.lnk.prinics.womng.woProd.plnUld.WoProdPlnUldMngDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ThezoneApiMesTransactionService {

    private final WoProdPlnUldMngDao woMapper;
    private final ThezoneAPIMesTransactionMapper mapper;

    /**
     * MES 테이블 품목 정보 동기화
     * @param dataObjectMap
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveItem(Map<String, Object> dataObjectMap) {
        int saveResultCount = mapper.saveItem(dataObjectMap);
        if (saveResultCount != 1) {
            throw new CustomResponseException("저장 실패 " + dataObjectMap.get("itemCd"), 500);
        }
    }

    /**
     * MES 품목 동기화 실패 이력
     * @param dataObjectMap
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failItemSave(Map<String ,Object> dataObjectMap) {
        mapper.failUpdate(dataObjectMap);
    }
    /**
     * ERP + MES 작업 지시 데이터 조회
     * @param paramMap
     * @return
     */
    public List<Map<String, Object>> getWkomStsList(Map<String, Object> paramMap) {
        return woMapper.getWkomStsList(paramMap);
    }
}
