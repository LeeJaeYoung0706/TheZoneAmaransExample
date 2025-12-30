package com.lnk.prinics.system.thezoneApi;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lnk.prinics.exception.CustomResponseException;
import com.lnk.prinics.exception.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ThezoneApiClient {

    private final ThezoneApiProperties properties;
    private final String CO_CD = "1000";
    private static final ObjectMapper OM = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    /**
     * 작업 지시 정보
     *
     * @param param DATE - 1일단위로 검색
     * @return
     */
    public Map<String, Object> getOrderInformation(Map<String, Object> param) {

        if (param.get("DATE") == null || "".equals(String.valueOf(param.get("DATE"))))
            throw new CustomResponseException("작업 시작일이 존재하지 않습니다.", 400);

        int DATE = Integer.parseInt(String.valueOf(param.get("DATE")).replace("-", ""));

        Map<String, Object> parameterMap = Map.of(
                "coCd", CO_CD,
                "woDtFrom", DATE,
                "woDtTo", DATE
        );

        String ERP_ORDER_PARAMETER = parseMapToJSONString(parameterMap, API_URL.ITEM_INFO_URL);

        String orderInfoResultString = theZoneApiServiceInvoke(
                ERP_ORDER_PARAMETER,
                API_URL.ORDER_INFO_URL
        );

        return validateApiResult(orderInfoResultString, API_URL.ORDER_INFO_URL);
    }

    /**
     * 품목 정보 가져오기
     * Paging 이 존재한다면 Paging 값으로 리턴
     *
     * @param param
     * @return
     */
    public Map<String, Object> getItemInformation(Map<String, Object> param) {


        // 페이징 처리
        if (param.get("paging") != null && "Y".equals(String.valueOf(param.get("paging")))) {

            Map<String, Object> parameterMap = Map.of(
                    "coCd", CO_CD,
                    "pagingStart", param.get("start"),
                    "pagingEnd", param.get("limit"),
                    "outputType", "list"

            );

            String ERP_ITEM_INFO_PARAMETER = parseMapToJSONString(parameterMap, API_URL.ITEM_INFO_URL);
            String itemInfoPagingResultString = theZoneApiServiceInvoke(
                    ERP_ITEM_INFO_PARAMETER,
                    API_URL.ITEM_INFO_URL
            );
            return validateApiResult(itemInfoPagingResultString, API_URL.ITEM_INFO_URL);
        // 페이징 미처리
        } else {

            Map<String, Object> parameterMap = Map.of(
                    "coCd", CO_CD
            );

            String ERP_ITEM_INFO_PARAMETER = parseMapToJSONString(parameterMap, API_URL.ITEM_INFO_URL);
            String itemInfoResultString = theZoneApiServiceInvoke(
                    ERP_ITEM_INFO_PARAMETER,
                    API_URL.ITEM_INFO_URL
            );

            return validateApiResult(itemInfoResultString, API_URL.ITEM_INFO_URL);
        }
    }

    /**
     * 공정경로조회
     * itemCd // 필수 품목 코드
     * divCd // 작업장 코드
     * @param param
     * @return  /*
     *
     * * */
    public Map<String, Object> getRoutingReadList(Map<String, Object> param) {

        String itemCd = String.valueOf(param.get("itemCd"));
        String divCd = String.valueOf(param.get("divCd"));

        if (itemCd == null || itemCd.equals("") || divCd == null || divCd.equals(""))
            throw new CustomResponseException("공정경로 조회 파라미터 정보가 없습니다.", 400);

        Map<String, Object> parameterMap = Map.of(
                "coCd", CO_CD,
                "divCd", divCd, // 사업장
                "itemCd", itemCd // 품목번호
        );

        String ERP_ORDER_ROUTING_READ_PARAMETER = parseMapToJSONString(parameterMap, API_URL.ORDER_ROUTING_READ_URL);

        String itemInfoPagingResultString = theZoneApiServiceInvoke(
                ERP_ORDER_ROUTING_READ_PARAMETER,
                API_URL.ORDER_ROUTING_READ_URL
        );
        return validateApiResult(itemInfoPagingResultString, API_URL.ORDER_ROUTING_READ_URL);
    }

    /**
     * 작업지시 실적 등록 전송
     * @param param
     * @return
     */
    public Map<String, Object> orderSendErpInformation(Map<String, Object> param) {

        String ERP_ORDER_FINAL_PARAMETER = parseMapToJSONString(param, API_URL.ORDER_FINAL_URL);

        String orderSencResultString = theZoneApiServiceInvoke(
                ERP_ORDER_FINAL_PARAMETER,
                API_URL.ORDER_FINAL_URL
        );

        return validateApiResult(orderSencResultString, API_URL.ORDER_ROUTING_READ_URL);
    }

    /**
     * 작업지시 실적 삭제 등록 전송
     * @param param
     * @return
     */
    public Map<String, Object> orderDeleteSendErpInformation(Map<String, Object> param) {

        String ERP_ORDER_DELETE_PARAMETER = parseMapToJSONString(param, API_URL.ORDER_FINAL_DELETE_URL);

        String orderSencResultString = theZoneApiServiceInvoke(
                ERP_ORDER_DELETE_PARAMETER,
                API_URL.ORDER_FINAL_DELETE_URL
        );

        return validateApiResult(orderSencResultString, API_URL.ORDER_FINAL_DELETE_URL);
    }


    /**
     * 품목 토탈카운트
     *
     * @return
     * @throws Exception
     */
    public Map<String, Object> getItemInformationTotalCount() {

        Map<String, Object> parameterMap = Map.of(
                "coCd", CO_CD,
                "outputType", "count"
        );

        String ERP_PARAMETER = parseMapToJSONString(parameterMap, API_URL.ITEM_INFO_URL);

        String itemInfoTotalCountResultString = theZoneApiServiceInvoke(
                ERP_PARAMETER,
                API_URL.ITEM_INFO_URL
        );

        return validateApiResult(itemInfoTotalCountResultString, API_URL.ITEM_INFO_URL);
    }



    /**
     * Map -> Json String 변환
     *
     * @param param
     * @param URL
     * @return
     */
    protected static String parseMapToJSONString(Map<String, Object> param, API_URL URL) {

        if (param == null || param.isEmpty())
            throw new CustomResponseException("ERP 파라미터 JSON 변환 실패 \n사유 : 요청 파라미터 값 미존재", 500);

        if (URL == null || URL.getInfo().isEmpty())
            throw new CustomResponseException("ERP 파라미터 JSON 변환 실패 \n사유 : 요청 파라미터 URL 미존재", 500);

        try {
            return OM.writeValueAsString(param);
        } catch (JsonProcessingException e) {
            String messageBuilder = "ERP 파라미터 생성중 JSON 파싱 실패 \n사유 : " +
                    URL.getInfo();
            throw new CustomResponseException(messageBuilder, 500);
        }
    }

    /**
     * Thezone API 전송 파라미터
     *
     * @param parameter
     * @return
     */
    private String theZoneApiServiceInvoke(String parameter, API_URL URL) {

        if (URL == null || URL.getInfo().isEmpty() || URL.getPath().isEmpty())
            throw new CustomResponseException("ERP 전송 파라미터가 존재하지 않습니다." + "\n 위치 : " + URL.getInfo(), 400);

        try {
            return TheZoneAPIService.invoke(
                    URL.getPath(),
                    parameter,
                    properties.baseUrl(),
                    properties.token(),
                    properties.hashKey(),
                    properties.calleName(),
                    properties.groupSeq()
            );
        } catch (Exception e) {
            throw new CustomResponseException("ERP 실패 \n사유 : API Service Error" + "\n 위치 : " + URL.getInfo(), 500);
        }
    }

    /**
     * JSON -> Map
     *
     * @param json
     * @param info
     * @return
     */
    private static Map<String, Object> toMap(String json, String info) {
        try {
            return OM.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new CustomResponseException("ERP 실패 \n사유 : API Service Error" + "\n 위치 : " + info, 500);
        }
    }

    /**
     * API 리턴 검증 및 JSON String to Map
     *
     * @param resultString
     * @param URL
     */
    private Map<String, Object> validateApiResult(String resultString, API_URL URL) {

        String validateString = resultString.trim();
        String info = URL.getInfo();
        if (validateString.equals(""))
            throw new CustomResponseException("ERP 파라미터 JSON 변환 실패 \n사유 : 값 Blank (미존재) \n 위치 : " + info, 500);

        if (URL == null || URL.getInfo().isEmpty())
            throw new CustomResponseException("ERP 파라미터 JSON 변환 실패 \n사유 : 요청 파라미터 URL 미존재 \n 위치 : " + info, 500);


        Map<String, Object> resultMap = toMap(resultString, info);

        if (resultMap == null || resultMap.isEmpty())
            throw new CustomResponseException("ERP 동기화 실패 \n사유 : 데이터가 없습니다. \n 위치 : " + info, 500);
        // 성공 값이 아닐 때
        if (!"SUCCESS".equals(String.valueOf(resultMap.get("resultMsg"))))
            throw new CustomResponseException("ERP 동기화 실패 \n사유 : 연결 실패 사유 . " + (resultMap.get("resultMsg") == null ? "메세지 없습니다." : resultMap.get("resultMsg")) + "\n 위치 : " + info, 500);

        return resultMap;
    }


}
