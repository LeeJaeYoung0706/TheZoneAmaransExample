package com.lnk.prinics.system.thezoneApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lnk.prinics.exception.CustomResponseException;
import com.lnk.prinics.exception.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ERROR_FILE_LOGGER")
public class ThezoneApiBusinessService {

    private final ThezoneApiClient client;
    private static final ObjectMapper OM = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ThezoneApiMesTransactionService transactionService;

    /**
     * 품목정보 페이징 조회
     *
     * @param start
     * @param limit
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchItemPage(int start, int limit) {
        Map<String, Object> param = Map.of(
                "start", start,
                "limit", limit,
                "paging", "Y"
        );

        Map<String, Object> res = client.getItemInformation(param);
        Object resultData = (res == null) ? null : res.get("resultData");

        if (!(resultData instanceof List<?> list)) {
            throw new CustomResponseException(
                    "ERP Item 결과 형식 오류 (start=" + start + ", limit=" + limit + ")",
                    500
            );
        }

        return (List<Map<String, Object>>) list;
    }


    public Response itemInformationByMesSave() {

        Response itemInformationResponse = itemInformation();

        if (itemInformationResponse.getCode() != 200)
            throw new CustomResponseException("품목 정보 요청에 실패했습니다." , 500);

        if (!(itemInformationResponse.getData() instanceof List<?> itemList)) {
            throw new CustomResponseException(
                    "품목 정보 리스트 파싱 실패",
                    500
            );
        }

        int success = 0;
        int fail = 0;
        for (Object dataObject : itemList) {
            if (!(dataObject instanceof Map<? , ?> dataObjectMap)) {
                throw new CustomResponseException(
                        "품목 정보 리스트 파싱 실패",
                        500
                );
            }

            Map<String, Object> dataMap = (Map<String, Object>) dataObjectMap;
            try {
                transactionService.saveItem(dataMap);
                success++;
            } catch (CustomResponseException | DataAccessException e) {
                dataMap.put("exceptionMessage" , e.getMessage());
                try {
                    transactionService.failItemSave(dataMap);
                } catch (Exception logEx) {
                    log.error("fail log save failed. itemCd={}", dataMap.get("itemCd"), logEx);
                }
                fail++;
            }
        }
        return Response.builder("품목 동기화 전체 : " + itemList.size() + " 건 \n성공 : " + success + " 건 성공 \n" + fail + " 건 실패" , 200).build();
    }

    /**
     * 품목정보 ERP EndPoint 요청 후 MES SAVE
     *
     * @return
     */
    public Response itemInformation() {

        ExecutorService executor = null;

        try {

            int totalCount = getTotalCountByErp();

            final int THREAD_COUNT = 5;
            final int PAGE_SIZE = 1000;

            executor = Executors.newFixedThreadPool(THREAD_COUNT);

            List<Future<List<Map<String, Object>>>> futures = new ArrayList<>();

            for (int start = 0; start < totalCount; start += PAGE_SIZE) {
                final int pagingStart = start;
                final int limit = Math.min(PAGE_SIZE, totalCount - pagingStart);
                futures.add(executor.submit(() -> fetchItemPage(pagingStart, limit)));
            }

            // 결과 합치기
            long saved = 0;
            List<Map<String,Object>> allResult = new ArrayList<>();

            for (Future<List<Map<String, Object>>> f : futures) {
                allResult.addAll(f.get());
            }

            return Response.builder("품목정보 동기화 완료되었습니다. 동기화 건수 : " + saved, 200).data(allResult).build();
        } catch (Exception e) {
            throw new CustomResponseException("품목정보 동기화 ERP 실패 : " + e.getMessage(), 500);
        } finally {
            if (executor != null) executor.shutdown();
        }
    }

    /**
     * 작업지시 MES 병합정보
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> erpOrderInformationMesMerge(Map<String, Object> param) {

        Map<String, Object> validationResultMap = client.getOrderInformation(param);
        System.out.println(" validationResultMap = " + validationResultMap.toString());
        if (!(validationResultMap.get("resultData") instanceof List<?> resultMap)) {
            throw new CustomResponseException(
                    "ERP Item 결과 형식 오류 작업지시 전체 리스트 파싱 오류",
                    500
            );
        }

        Map<String, Object> paramMap = buildDataListJsonParam((List<Map<String, Object>>) resultMap);
        return transactionService.getWkomStsList(paramMap);
    }

    /**
     *  JSON 변환
     * @param resultList
     * @return
     */
    private Map<String, Object> buildDataListJsonParam(List<Map<String, Object>> resultList) {
        if (resultList == null || resultList.isEmpty()) {
            throw new CustomResponseException("ERP 작업지시 결과 데이터가 존재하지 않습니다.", 500);
        }

        try {
            List<Map<String, Object>> copyList = new ArrayList<>(resultList);
            String dataListJson = OM.writeValueAsString(copyList);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("dataListJson", dataListJson);
            return paramMap;
        } catch (JsonProcessingException e) {
            throw new CustomResponseException("ERP 작업지시 결과 JSON 변환 실패", 500);
        }
    }

    /**
     * ERP에서 가져온 결과 int로 변환
     * @return
     */
    @SuppressWarnings("unchecked")
    private int getTotalCountByErp() {
        Map<String, Object> totalCountResultMap = client.getItemInformationTotalCount();

        if (totalCountResultMap.isEmpty() || totalCountResultMap.get("resultData") == null)
            throw new CustomResponseException("품목 전체 수량 정보 불러오기 실패 \n 사유 : ERP 데이터 미존재", 500);

        if (!(totalCountResultMap.get("resultData") instanceof List<?> resultMap)) {
            throw new CustomResponseException(
                    "ERP Item 결과 형식 오류 품목 전체 리스트 파싱 오류",
                    500
            );
        }

        List<Map<String, Object>> maps = (List<Map<String, Object>>) resultMap;

        if (maps.isEmpty() || maps.get(0).get("totalCount") == null)
            throw new CustomResponseException("ERP 토탈 값 존재하지 않음", 500);

        return (int) maps.get(0).get("totalCount");
    }

    // 실적 등록 시 필수 파라미터 체크 리스트
    private final List<String> operationRequiredList = List.of(
            "coCd", // 회사코드
            "woCd", // 지시번호 코드
            "opNb", //공정번호
            "docFg", // 생산 외주 구분 1 생산 2 외주
            "wrDt", // 실적일
            "baselocCd", // 창고 코드
            "locCd" , // 장소 코드
            "divCd" , // 사업장 코드
            "deptCd" , // 부서 코드
            "empCd" , // 사원 코드
            "itemCd" , // 품목 코드
            "workQt" , // 공정 실적 수량
            "goodQt" , // 합격수량
            "badQt" , // 불량 수량
            "firstYn" , //첫 공정 여부 00 미해당 01 해당
            "lastYn" , // 최종 공정 여부 00 미해당 01 해당
            "moveopNb", // 이동 공정 번호
            "movebaselocCd", // 이동 공정 / 입고 창고 코드
            "movelocCd", // 이동작업장 / 장소 코드
            "qcFg" // 검사 여부 0 무검사 1 검사
    );

    // 실적 삭제시 필수 파라미터 체크 리스트
    private final List<String> operationDeleteRequiredList = List.of(
            "coCd", // 회사코드
            "woCd", // 지시번호 코드
            "wrCd", // 실적번호
            "opNb" // 공정번호
    );


    // 작업지시 실적 등록시 필요한 데이터 생성
    public Response operationErpTransInsertCreateData(Map<String, Object> param) {
        // param 으로 받아서 필요한 값 생성

        ArrayList<String> copyList = new ArrayList<>(operationRequiredList);

        // 필수 파라미터 Map 해당 값에 맞는것 put 추가
        Map<String, Object> operationMap = copyList.stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> null
                ));

        // 해당 맵에 필요한 데이터 생성해서 추가 필요 operationRequiredList 해당 값 필수



        // 필수 파라미터 검증
        for (String key : operationRequiredList) {
            if (!operationMap.containsKey(key)) {
                throw new CustomResponseException("지시 실적 전송 필수 키 누락 : " + key , 400);
            }

            if (operationMap.get(key) == null) {
                throw new CustomResponseException("지시 실적 전송 필수 값 미존재 : " + key , 400);
            }
        }

        Map<String, Object> erpTransResultMap = client.orderSendErpInformation(operationMap);
        // resultMap 200 확인 일 경우 리턴됨

        // 리턴 후 필요 로직 추가

        // message 생성 필요
        return Response.builder("ok" , 200).build();
    }

    /**
     * 실적 삭제 데이터 생성 및 처리
     * @param param
     * @return
     */
    public Response operationErpTransDeleteCreateData(Map<String, Object> param) {
        // param 으로 받아서 필요한 값 생성

        ArrayList<String> copyList = new ArrayList<>(operationDeleteRequiredList);

        // 필수 파라미터 Map 해당 값에 맞는것 put 추가
        Map<String, Object> operationDeleteMap = copyList.stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> null
                ));

        // 해당 맵에 필요한 데이터 생성해서 추가 필요 operationDeleteRequiredList 해당 값 필수
        // client.getRoutingReadList(); 공정경로 가져오는거



        // 필수 파라미터 검증
        for (String key : operationDeleteRequiredList) {
            if (!operationDeleteMap.containsKey(key)) {
                throw new CustomResponseException("지시 실적 삭제 전송 필수 키 누락 : " + key , 400);
            }

            if (operationDeleteMap.get(key) == null) {
                throw new CustomResponseException("지시 실적 삭제 전송 필수 값 미존재 : " + key , 400);
            }
        }

        Map<String, Object> erpTransResultMap = client.orderDeleteSendErpInformation(operationDeleteMap);
        // resultMap 200 확인 일 경우 리턴됨

        // 리턴 후 필요 로직 추가

        // message 생성 필요
        return Response.builder("ok" , 200).build();
    }

    // 동기화 미 생성
    //    @Transactional
    //    public Response orderInformationMesSave(Map<String, Object> param) {
    //
    //        Map<String, Object> orderInformation = client.getOrderInformation(param);
    //
    //
    //
    //        return Response.builder("작업지시 동기화 성공하였습니다." , 200).build();
    //    }
}
