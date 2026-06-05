package org.ngphthinh.controller;

import org.ngphthinh.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping
    public ApiResponse<Void> getTest(){
        return ApiResponse.<Void>builder()
                .code(999)
                .message("Get test success")
                .build();
    }

}
