package com.jiwon.payment.controller.parameter;

import lombok.Getter;
import lombok.Setter;

/*
    결제정보조회 API

    Request: 관리번호
*/
@Getter @Setter
public class RetrieveRequestParam {
    private String id;
}