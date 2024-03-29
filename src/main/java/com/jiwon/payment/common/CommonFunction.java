package com.jiwon.payment.common;

import com.jiwon.payment.controller.parameter.CancelRequestParam;
import com.jiwon.payment.controller.parameter.PaymentRequestParam;
import com.jiwon.payment.entity.Payment;
import com.jiwon.payment.exceptions.InvalidParameterException;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class CommonFunction {
    // Payment 입력값 Validation 체크
    public static void checkPaymentParam(PaymentRequestParam paymentRequestParam) {
        if (Optional.ofNullable(paymentRequestParam.getCardNumber()).isEmpty() || paymentRequestParam.getCardNumber() == "") {
            log.error("The card number field is required.");
            throw new InvalidParameterException("card number");
        }
        if (Optional.ofNullable(paymentRequestParam.getExpirationDate()).isEmpty() || paymentRequestParam.getExpirationDate() == "") {
            log.error("The expiration date field is required.");
            throw new InvalidParameterException("expiration date");
        }
        if (Optional.ofNullable(paymentRequestParam.getCvc()).isEmpty() || paymentRequestParam.getCvc() == "") {
            log.error("The cvc field is required.");
            throw new InvalidParameterException("cvc");
        }
        if (Optional.ofNullable(paymentRequestParam.getInstallmentMonths()).isEmpty() || paymentRequestParam.getInstallmentMonths() == "") {
            log.error("The installment months field is required.");
            throw new InvalidParameterException("installment months");
        }
        if (Optional.ofNullable(paymentRequestParam.getPaymentPrice()).isEmpty()) {
            log.error("The payment price field is required.");
            throw new InvalidParameterException("payment price");
        }
        
        // 부가가치세는 결제금액보다 클 수 없음
        if (Optional.ofNullable(paymentRequestParam.getVat()).isPresent()
                && paymentRequestParam.getPaymentPrice() < Integer.parseInt(paymentRequestParam.getVat())) {

            log.error("The vat is bigger than payment price.");
            throw new InvalidParameterException("vat");
        }
    }

    // Cancel 입력값 Validation 체크
    public static void checkCancelParam(CancelRequestParam cancelRequestParam) {
        if (Optional.ofNullable(cancelRequestParam.getId()).isEmpty() || cancelRequestParam.getId() == "") {
            log.error("The id field is required.");
            throw new InvalidParameterException("id");
        }
        if (Optional.ofNullable(cancelRequestParam.getCancelPrice()).isEmpty()) {
            log.error("The cancel price field is required.");
            throw new InvalidParameterException("cancel price");
        }

        // 부가가치세는 결제금액보다 클 수 없음
        if (Optional.ofNullable(cancelRequestParam.getVat()).isPresent()
                && cancelRequestParam.getCancelPrice() < Integer.parseInt(cancelRequestParam.getVat())) {

            log.error("The vat is bigger than cancel price.");
            throw new InvalidParameterException("vat");
        }
    }

    // 관리번호 (Unique ID, 20자리) 생성
    public static String generateUniqueId() {
        UUID uuid = UUID.randomUUID();
        String base64encoded = Base64.getUrlEncoder().encodeToString(uuid.toString().getBytes());

        // Base64 인코딩 후 20자리로 자름
        return base64encoded.substring(0, 20);
    }

    // 부가가치세 값이 안 들어왔을 때 계산
    public static int computeVat(long price, String vat) {
        // 결제금액 / 11, 소수점 이하 반올림
        if (Optional.ofNullable(vat).isEmpty()) {
            return (int) Math.round(price / 11);

        } else {
            return Integer.valueOf(vat);
        }
    }

    // 카드번호, 유효기간, CVC 데이터를 암호화
    public static String dataEncryption(PaymentRequestParam paymentRequestParam, String password) {
        String card_num = paymentRequestParam.getCardNumber();
        String expire_date = paymentRequestParam.getExpirationDate();
        String cvc = paymentRequestParam.getCvc();

        String full_data = card_num.concat("|").concat(expire_date).concat("|").concat(cvc);
        full_data = EncryptionService.encryptData(full_data, password);

        return full_data;
    }

    // 카드번호 앞 6자리, 뒤 3자리 제외한 나머지 마스킹 처리
    public static String stringMasking(String originalString) {
        if (originalString == null || originalString.length() <= 9) {
            return originalString;
        }

        int length = originalString.length();
        StringBuilder maskedString = new StringBuilder();

        // 앞 6자리는 그대로 표시
        maskedString.append(originalString.substring(0, 6));

        // 나머지 문자열을 '*'로 마스킹 처리
        for (int i = 6; i < length - 3; i++) {
            maskedString.append('*');
        }

        // 뒤 3자리는 그대로 표시
        maskedString.append(originalString.substring(length - 3));

        return maskedString.toString();
    }

    // 카드사로 전송하는 string 데이터 생성 (Payment)
    public static String generatePaymentStringData(PaymentRequestParam paymentRequestParam,
                                                   String manage_num, int vat_price, String encryptData) {
        String data = " 446PAYMENT   ";

        // 관리번호 20자리
        data = data.concat(manage_num);

        // 카드번호 10-16자리
        String card_num = paymentRequestParam.getCardNumber();
        int card_num_length = card_num.length();
        String card_align = " ".repeat(Math.max(0, 20 - card_num_length));

        data = data.concat(card_num);
        data = data.concat(card_align);

        // 할부개월수 2자리
        data = data.concat(paymentRequestParam.getInstallmentMonths());

        // 유효기간 4자리
        data = data.concat(paymentRequestParam.getExpirationDate());

        // CVC 3자리
        data = data.concat(paymentRequestParam.getCvc());

        // 결제금액 3-10자리
        String pay_num = String.valueOf(paymentRequestParam.getPaymentPrice());
        int pay_num_length = pay_num.length();
        String pay_align = " ".repeat(Math.max(0, 10 - pay_num_length));

        data = data.concat(pay_align);
        data = data.concat(pay_num);

        // 부가가치세 (옵션)
        String vat = String.valueOf(vat_price);
        int vat_length = vat.length();
        String vat_align = "0".repeat(Math.max(0, 10 - vat_length));

        data = data.concat(vat_align);
        data = data.concat(vat);

        // 원거래 관리번호 (결제시에는 공백)
        String origin_manage_num = " ".repeat(Math.max(0, 20));

        data = data.concat(origin_manage_num);

        // 카드데이터
        int encrypt_length = encryptData.length();
        String encrypt_align = " ".repeat(Math.max(0, 300 - encrypt_length));

        data = data.concat(encryptData);
        data = data.concat(encrypt_align);

        // 나머지 빈칸으로 채움 (총 450자리)
        int total_length = data.length();
        String total_align = " ".repeat(Math.max(0, 450 - total_length));

        data = data.concat(total_align);

        return data;
    }

    // 카드사로 전송하는 string 데이터 생성 (Cancel)
    public static String generateCancelStringData(Payment payment,
                                                  String manage_num, String card_num, String expire_date, String cvc) {
        String data = " 446CANCEL    ";

        // 관리번호 20자리
        data = data.concat(manage_num);

        // 카드번호 10-16자리
        int card_num_length = card_num.length();
        String card_align = " ".repeat(Math.max(0, 20 - card_num_length));

        data = data.concat(card_num);
        data = data.concat(card_align);

        // 할부개월수 2자리
        data = data.concat(payment.getInstallmentMonths());

        // 유효기간 4자리
        data = data.concat(expire_date);

        // CVC 3자리
        data = data.concat(cvc);

        // 결제취소금액 3-10자리
        String pay_num = String.valueOf(payment.getPaymentPrice());
        int pay_num_length = pay_num.length();
        String pay_align = " ".repeat(Math.max(0, 10 - pay_num_length));

        data = data.concat(pay_align);
        data = data.concat(pay_num);

        // 부가가치세 (옵션)
        String vat = String.valueOf(payment.getVat());
        int vat_length = vat.length();
        String vat_align = "0".repeat(Math.max(0, 10 - vat_length));

        data = data.concat(vat_align);
        data = data.concat(vat);

        // 원거래 관리번호 (결제시에는 공백)
        data = data.concat(payment.getId());

        // 카드데이터
        String encrypt_data = payment.getData();
        int encrypt_length = encrypt_data.length();
        String encrypt_align = " ".repeat(Math.max(0, 300 - encrypt_length));

        data = data.concat(encrypt_data);
        data = data.concat(encrypt_align);

        // 나머지 빈칸으로 채움 (총 450자리)
        int total_length = data.length();
        String total_align = " ".repeat(Math.max(0, 450 - total_length));

        data = data.concat(total_align);

        return data;
    }
}
