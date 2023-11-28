package com.gobi.util;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;

@UtilityClass
public class Util {
    public String payrollUrl = "https://interactive.ibi.mn/restapi/";

    public HttpHeaders payrollHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("ehuulga", "$3huu1ga2o2o");
        headers.add("X-API-KEY", "69399a188366b8fc8e59b5f0cb127e23a20b65b3");
        return headers;
    }
}
