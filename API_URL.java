package com.lnk.prinics.system.thezoneApi;

public enum API_URL {

    DEFAULT_API_URL("도메인URL" , "ERP DOMAIN URL"),
    ITEM_INFO_URL("프록시1URL" , "품목정보"),  // 품목 정보 URL
    ORDER_INFO_URL("프록시2URL" , "작업지시정보"), // 작업 지시 정보 URL
    ORDER_FINAL_URL("프록시2URL" , "작업지시실적등록"),   // 작업 지시 실적 URL
    ORDER_ROUTING_URL("프록시2URL" , "공정전체경로조회"),  // 생산 지시 공정 ㅈ너체 경로 URL
    ORDER_FINAL_DELETE_URL("프록시2URL" , "작업지시실적삭제"),   // 작업 지시 실적 삭제 URL
    ORDER_ROUTING_READ_URL("프록시2URL" , "공정경로조회"); // 공정 경로 조회

    private final String path;
    private final String info;

    API_URL(String path, String info) {
        this.path = path;
        this.info = info;
    }

    public String getPath() {
        return path;
    }

    public String getInfo() {
        return info;
    }
}
