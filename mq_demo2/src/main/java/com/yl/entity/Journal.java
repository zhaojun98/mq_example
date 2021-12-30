package com.yl.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author ：jerry
 * @date ：Created in 2021/12/29 14:37
 * @description：bean对象
 * @version: V1.1
 */
@Data
public class Journal {

    private Long id;

    private String title;

    private String titleDesc;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private LocalDateTime createTime;
}
